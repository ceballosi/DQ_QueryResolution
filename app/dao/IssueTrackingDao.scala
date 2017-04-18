package dao

import java.util.Date
import javax.inject.{Inject, Singleton}

import dao.DaoUtils._
import dao.Searching.{SearchRequest, SearchResult}
import domain._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.{ColumnOrdered, ProvenShape}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

trait IssueTrackingDao extends BaseDao[Issue, Long] {
  def listGmcs: Future[Seq[String]]
  def listOrigins: Future[Seq[String]]
  def findBySearchRequest(searchRequest: SearchRequest): Future[SearchResult[Issue]]
  def findByIssueIds(issueIds: List[String]): Future[SearchResult[Issue]]
  def findByCriteria(cr : SearchCriteria): Future[Seq[Issue]]
  def updateQueryDate(newDate: Date, issue: Issue): Future[Int]
  def updateResolutionDate(newDate: Date, issue: Issue): Future[Int]
  def changeStatus(newStatus: Status, issue: Issue): Future[Unit]
  def changeStatusInd(newStatus: Status, issue: Issue): Boolean
    // TODO To be removed
  def tableSetup(data: Seq[Issue])
  def findAllJoin: Future[Seq[(String,String,String)]]

  def findQueryChain(selected: String): Future[Seq[QueryChain]]

  def nextIssueId: Future[Int]
  def nextIssueId(gmc: String) : Future[String]
}

/**
  * @param dbConfigProvider Play db config provider. Play injects this
  */
@Singleton
class IssueTrackingDaoImpl @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends IssueTrackingDao {
  val log: Logger = LoggerFactory.getLogger(this.getClass())

  //JdbcProfile for this provider
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  // To bring db in to the current scope
  import dbConfig._

  // To bring slick DSL into scope to define table and other queries
  import driver.api._

  override def toTable = TableQuery[IssueTable]

  private val loggedIssues = toTable
  private val queryChains = TableQuery[QueryChainTable]

  // Custom column mapping
  implicit val statusMapper = MappedColumnType.base[Status, String](
    d => d.toString, d => Status.allStatuses.find(_.toString == d).getOrElse(null)
  )

  implicit val dateMapper = MappedColumnType.base[java.util.Date, java.sql.Timestamp](
    d => new java.sql.Timestamp(d.getTime),
    d => new java.util.Date(d.getTime)
  )

  private val columnMap = Map(
    //id is not in map - i guess this just means we don't expect to query on it (never exposed to UI)
    "status" -> { (t: IssueTable) => t.status },
    "DT_RowId" -> { (t: IssueTable) => t.issueId},
    "dateLogged" -> { (t: IssueTable) => t.dateLogged},
    "participantId" -> { (t: IssueTable) => t.participantId},
    "dataSource" -> { (t: IssueTable) => t.dataSource},
    "priority" -> { (t: IssueTable) => t.priority},
    "dataItem" -> { (t: IssueTable) => t.dataItem},
    "shortDesc" -> { (t: IssueTable) => t.shortDesc},
    "description" -> { (t: IssueTable) => t.description},
    "gmc" -> { (t: IssueTable) => t.gmc},
    "lsid" -> { (t: IssueTable) => t.lsid},
    "area" -> { (t: IssueTable) => t.area},
    "familyId" -> { (t: IssueTable) => t.familyId},
    "queryDate" -> { (t: IssueTable) => t.queryDate},
    "resolutionDate" -> { (t: IssueTable) => t.resolutionDate},
    "weeksOpen" -> { (t: IssueTable) => t.weeksOpen},
    "escalation" -> { (t: IssueTable) => t.escalation},
    "notes" -> { (t: IssueTable) => t.notes}
  )

  //next issue id from separate explicit db sequence - issueid_id_seq
  def nextIssueId(): Future[Int] = {
    val value = db.run(
      sql"select nextval('issueid_id_seq')".as[Int].head
    )
    value
  }


  def nextIssueId(gmc: String) : Future[String] =  {
    val fullId: Future[String] = nextIssueId.map(id => f"${gmc}%s-${id}%07d")
    fullId
  }


  def findAllJoin: Future[Seq[(String, String, String)]] = {
//        val innerJoin = for {
//          iss <- loggedIssues
//          qcs <- queryChains if iss.issueId === qcs.issueId
//        } yield (iss.issueId, qcs.comment, qcs.date)

    val innerJoin = for {
      (iss, qcs) <- loggedIssues join queryChains on (_.issueId === _.issueId)
    } yield (iss.dataSource, qcs.comment, qcs.date)

    val joinFuture = db.run(innerJoin.result)
    joinFuture.map(println)
    val aReport = ("fake", "data", "for")
    val reports = ListBuffer(aReport)
    Future(reports.toList.toSeq)
  }

  def findQueryChain(selected: String): Future[Seq[QueryChain]] = {
    findAllQueryChain
  }

  def findAllQueryChain: Future[Seq[QueryChain]] = db.run(queryChains.sortBy(_.id).result)

  private def queryBySortCriteria(query: Query[IssueTable, IssueTable#TableElementType, Seq], sort: (String, String)) = {
        val rep = columnMap.getOrElse(sort._1, throw new RuntimeException(s"Invalid column used for sorting ${sort._1}"))
      val orderedRep = sort._2 match {
        case "desc" => (t: IssueTable) => ColumnOrdered(rep(t), slick.ast.Ordering(slick.ast.Ordering.Desc))
        case _ => (t: IssueTable) => ColumnOrdered(rep(t), slick.ast.Ordering(slick.ast.Ordering.Asc))
      }
      query.sortBy(orderedRep)

  }

  private def queryBySearchCriteria(cr : SearchCriteria)  = {
    MaybeFilter(loggedIssues)
      .filter(cr.gmc)(v => d => d.gmc === v) // v => parameter value passed in  d=> Table data element
      .filter(cr.issueId)(v => d => d.issueId === v)
      .filter(cr.issueStatus)(v => d => d.status === v)
      .filter(cr.priority)(v => d => d.priority === v)
      .filter(cr.dataSource)(v => d => d.dataSource === v)
      .filter(cr.dateLogged)(v => d => d.dateLogged > v)
      .filter(cr.participantId)(v => d => d.participantId === v)
      .query
  }


  // TODO: To be removed
  def tableSetup(data: Seq[Issue]) = {
    val g = db.run(DBIO.seq(
      // create the schema
//      loggedIssues.schema.create, --actually this creates funny schema with uppercase names & forces double quotes "ID"
      // to bulk insert our sample data
      loggedIssues ++= data
    ))
    Await.result(g, 30 seconds)
  }

  def findAll: Future[Seq[Issue]] = db.run(loggedIssues.result)

  def findByCriteria(cr : SearchCriteria): Future[Seq[Issue]] = {
    db.run(queryBySearchCriteria(cr).result)
  }

  def listGmcs: Future[Seq[String]] = {
    val query = loggedIssues.map(_.gmc ).distinct.sorted
    db.run(query.result)
  }

  def listOrigins: Future[Seq[String]] = {
    val query = loggedIssues.map(_.dataSource ).distinct.sorted
    db.run(query.result)
  }

  def update(issue: Issue): Future[Int] = {
    db.run(
      loggedIssues.filter( _.issueId === issue.issueId) update (issue)
    )
  }

  def updateQueryDate(newDate: Date, issue: Issue): Future[Int] = {
    db.run(
      loggedIssues.filter( _.issueId === issue.issueId).map(iss => (iss.queryDate)) update (Some(newDate))
    )
  }

  def updateResolutionDate(newDate: Date, issue: Issue): Future[Int] = {
    db.run(
      loggedIssues.filter( _.issueId === issue.issueId).map(iss => (iss.resolutionDate)) update (Some(newDate))
    )
  }

  def insert(issue: Issue) = {
    db.run(
      loggedIssues += issue
    )
  }


  def changeStatus(newStatus: Status, issue: Issue): Future[Unit] = {
    db.run(
      loggedIssues.filter( _.issueId === issue.issueId).map(iss => (iss.status)) update (newStatus)
    )
    Future(println(issue))
  }

  def changeStatusInd(newStatus: Status, issue: Issue): Boolean = {

    val updateQuery = loggedIssues.filter(_.issueId === issue.issueId).map(iss => (iss.status)) update (newStatus)

    val futureResult: Try[Int] = Await.ready(db.run(updateQuery.transactionally), 30 seconds).value.get

    val isSuccess = futureResult match {
      case scala.util.Success(numRows) => {
        if (numRows > 0) true           // relies on numRows changed to determine success/fail
        else false
      }
      case scala.util.Failure(e) => {
        log.error(s"issue ${issue.issueId} changeStatus error " + e.toString)
        false
      }
    }

    isSuccess
  }



  def findById(id: Long): Future[Option[Issue]] = db.run(loggedIssues.filter(_.id === id).result.headOption)


  def findByIssueIds(issueIds: List[String]): Future[SearchResult[Issue]] = {

    //sorting may not be needed...but input order does seem to be destroyed
//    val query = loggedIssues.filter(_.issueId inSetBind issueIds ).sortBy(issue => (issue.dateLogged.desc,issue.gmc))
    val query = loggedIssues.filter(_.issueId inSetBind issueIds )

    val issuesFuture = db.run(
      query.result
    )

    val filterIssuesCount: Future[Int] = db.run(
      query.length.result
    )

    val eventualPageResult: Future[SearchResult[Issue]] = filterIssuesCount.flatMap {
      total => issuesFuture.map {
        issues => {
          SearchResult(issues, total)
        }
      }
    }
    eventualPageResult
  }



  def findBySearchRequest(searchRequest: SearchRequest): Future[SearchResult[Issue]] = {
    var query = queryBySearchCriteria(searchRequest.searchCriteria)

    searchRequest.sortCriteria.foreach { field =>
      query = queryBySortCriteria(query, (field._1, field._2))
    }
    val issuesFuture = db.run(
      query.drop(searchRequest.offset).take(searchRequest.size).result
    )

    val filterIssuesCount: Future[Int] = db.run(
      query.length.result
    )

    val eventualPageResult: Future[SearchResult[Issue]] = filterIssuesCount.flatMap {
      total => issuesFuture.map {
        issues => {
          SearchResult(issues, total)
        }
      }
    }
    eventualPageResult
  }



  /** **************  Table definition ***************/
  class IssueTable(tag: Tag) extends Table[Issue](tag, "issue") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def issueId: Rep[String] = column[String]("issue_id")

    def status = column[Status]("status")

    def dateLogged = column[Date]("date_logged")

    def participantId = column[Int]("participant_id")

    def dataSource = column[String]("data_source")

    def priority = column[Int]("priority")

    def dataItem = column[String]("data_item")

    def shortDesc = column[String]("short_desc")

    def gmc = column[String]("gmc")

    def lsid = column[Option[String]]("lsid")

    def area = column[String]("area")

    def description = column[String]("description")

    def familyId = column[Option[String]]("family_id")

    def queryDate = column[Option[Date]]("query_date")

    def weeksOpen = column[Option[Int]]("weeks_open")

    def resolutionDate = column[Option[Date]]("resolution_date")

    def escalation = column[Option[Date]]("escalation")

    def notes = column[Option[String]]("notes")


    override def * : ProvenShape[Issue] = (id, issueId, status, dateLogged, participantId, dataSource, priority,
      dataItem, shortDesc, gmc, lsid, area, description, familyId, queryDate, weeksOpen, resolutionDate,
      escalation, notes) <>((Issue.apply _).tupled, Issue.unapply)

  }

  /** **************  Table definition ***************/
  class QueryChainTable(tag: Tag) extends Table[QueryChain](tag, "querychain") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def issueId = column[String]("issue_id")

    def status = column[String]("status")

    def comment = column[String]("comment")

    def date = column[Date]("date")

    def username = column[String]("username")

    def partyId = column[Int]("party_id")

    override def * : ProvenShape[QueryChain] = (id, issueId, status, comment, date, username, partyId) <>((QueryChain.apply _).tupled, QueryChain.unapply)
  }

}