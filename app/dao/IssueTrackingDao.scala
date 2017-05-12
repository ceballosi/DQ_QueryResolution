package dao

import java.io.{PrintWriter, StringWriter}
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

  def findBySearchRequest(searchRequest: SearchRequest): Future[SearchResult[IssueView]]
  def findByIssueIds(issueIds: List[String]): Future[SearchResult[Issue]]
  def findIssueViewByIssueIds(issueIds: List[String]): Future[SearchResult[IssueView]]
  def findByCriteria(cr : SearchCriteria): Future[Seq[IssueView]]

  def listGmcs: Future[Seq[String]]
  def listOrigins: Future[Seq[String]]
  def listPriorities: Future[Seq[Int]]

  def insert(issueDate: IssueDates): (Boolean, String)

  def updateQueryDate(newDate: Date, issue: Issue, user: String): Future[Int]
  def updateRespondedDate(newDate: Date, issue: Issue, user: String): Future[Int]
  def updateResolutionDate(newDate: Date, issue: Issue, user: String): Future[Int]
  def changeStatus(newStatus: Status, issue: Issue): Boolean

  def findAllJoin: Future[Seq[(String,String,String)]]
  def findQueryChain(selected: String): Future[Seq[QueryChain]]

  def nextIssueId: Future[Int]
  def nextIssueId(gmc: String) : Future[String]
  def issueCounts(gmc: String): (Int, Int)
  def issueResolutionDuration(gmc: String): List[(String, Status, Option[Date], Option[Date])]
  def dataItemsGroupedBy(gmc: String): List[(String,Int)]

    // TODO To be removed
  def tableSetup(issues: Seq[Issue], issuesDatesSeq: Seq[IssueDates])
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
  private val issueDates = TableQuery[IssueDatesTable]
  private val issuesView = TableQuery[IssueViewTable]
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
    "status" -> { (t: IssueViewTable) => t.status },
    "DT_RowId" -> { (t: IssueViewTable) => t.issueId},
    "dateLogged" -> { (t: IssueViewTable) => t.dateLogged},
    "participantId" -> { (t: IssueViewTable) => t.participantId},
    "dataSource" -> { (t: IssueViewTable) => t.dataSource},
    "priority" -> { (t: IssueViewTable) => t.priority},
    "dataItem" -> { (t: IssueViewTable) => t.dataItem},
    "shortDesc" -> { (t: IssueViewTable) => t.shortDesc},
    "description" -> { (t: IssueViewTable) => t.description},
    "gmc" -> { (t: IssueViewTable) => t.gmc},
    "lsid" -> { (t: IssueViewTable) => t.lsid},
    "area" -> { (t: IssueViewTable) => t.area},
    "familyId" -> { (t: IssueViewTable) => t.familyId},
    "openDate" -> { (t: IssueViewTable) => t.openDate},
    "resolutionDate" -> { (t: IssueViewTable) => t.resolutionDate},
    "weeksOpen" -> { (t: IssueViewTable) => t.weeksOpen},
    "escalation" -> { (t: IssueViewTable) => t.escalation},
    "notes" -> { (t: IssueViewTable) => t.notes}
  )



  private def queryBySortCriteria(query: Query[IssueViewTable, IssueViewTable#TableElementType, Seq], sort: (String, String)) = {
    val rep = columnMap.getOrElse(sort._1, throw new RuntimeException(s"Invalid column used for sorting ${sort._1}"))
    val orderedRep = sort._2 match {
      case "desc" => (t: IssueViewTable) => ColumnOrdered(rep(t), slick.ast.Ordering(slick.ast.Ordering.Desc).nullsLast)
      case _ => (t: IssueViewTable) => ColumnOrdered(rep(t), slick.ast.Ordering(slick.ast.Ordering.Asc).nullsFirst)
    }
    query.sortBy(_.id).sortBy(orderedRep) //mysteriously this generates sql with reversed col order e.g. 'order by "weeks_open" nulls first, "id" '
  }

  private def queryBySearchCriteria(cr : SearchCriteria)  = {
    MaybeFilter(issuesView)
      .filter(cr.gmc)(v => d => d.gmc === v) // v => parameter value passed in  d=> Table data element
      .filter(cr.issueId)(v => d => d.issueId === v)
      .filter(cr.issueStatus)(v => d => d.status === v)
      .filter(cr.priority)(v => d => d.priority === v)
      .filter(cr.dataSource)(v => d => d.dataSource === v)
      .filter(cr.area)(v => d => d.area === v)
      .filter(cr.dateLoggedStart)(v => d => d.dateLogged >= v)
      .filter(cr.dateLoggedEnd)(v => d => d.dateLogged <= v)
      .filter(cr.participantId)(v => d => d.participantId === v)
      .query
  }



  def findAllIssuesView: Future[Seq[IssueView]] = db.run(issuesView.result)

  def findAll: Future[Seq[Issue]] = db.run(loggedIssues.result)

  def findBySearchRequest(searchRequest: SearchRequest): Future[SearchResult[IssueView]] = {
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

    val eventualPageResult: Future[SearchResult[IssueView]] = filterIssuesCount.flatMap {
      total => issuesFuture.map {
        issues => {
          SearchResult(issues, total)
        }
      }
    }
    eventualPageResult
  }

  def findByCriteria(cr : SearchCriteria): Future[Seq[IssueView]] = {
    db.run(queryBySearchCriteria(cr).result)
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

  //same as above method but for view - should re-factor really
  def findIssueViewByIssueIds(issueIds: List[String]): Future[SearchResult[IssueView]] = {

    val query = issuesView.filter(_.issueId inSetBind issueIds )

    val issuesFuture = db.run(
      query.result
    )

    val filterIssuesCount: Future[Int] = db.run(
      query.length.result
    )

    val eventualPageResult: Future[SearchResult[IssueView]] = filterIssuesCount.flatMap {
      total => issuesFuture.map {
        issues => {
          SearchResult(issues, total)
        }
      }
    }
    eventualPageResult
  }


  //we don't want generic update issue method, we want to control precisely which fields we update
  //only Notes field to start
  def update(issue: Issue): (Boolean, String) = {
    val updateQuery = loggedIssues.filter( _.issueId === issue.issueId).map(iss => (iss.notes)) update (issue.notes)

    val futureResult: Try[Int] = Await.ready(db.run(updateQuery), 30 seconds).value.get

    val result = futureResult match {
      case scala.util.Success(numRows) => {
        if (numRows > 0) (true,"")           // relies on numRows changed to determine success/fail
        else {
          val msg: String = s"update issue db failed"
          log.error(msg)
          (false, msg)
        }
      }
      case scala.util.Failure(e) => {
        val msg = s"issue ${issue.issueId} update issue error " + e.toString
        val sw = new StringWriter
        e.printStackTrace(new PrintWriter(sw))
        log.error(sw.toString)
        (false, msg)
      }
    }

    result
  }

  def updateQueryDate(newDate: Date, issue: Issue, user: String): Future[Int] = {
    db.run(
      issueDates.filter( _.issueId === issue.issueId).map(iss => (iss.openDate, iss.openWho)).update((Some(newDate), Some(user)))
    )
  }

  def updateRespondedDate(newDate: Date, issue: Issue, user: String): Future[Int] = {
    db.run(
      issueDates.filter( _.issueId === issue.issueId).map(iss => (iss.respondedDate, iss.respondedWho)).update((Some(newDate), Some(user)))
    )
  }

  def updateResolutionDate(newDate: Date, issue: Issue, user: String): Future[Int] = {
    db.run(
      issueDates.filter( _.issueId === issue.issueId).map(iss => (iss.resolutionDate, iss.resolutionWho)).update((Some(newDate), Some(user)))
    )
  }

  @Override
  def insert(issue: Issue): (Boolean, String) = {

//    testing only
//    var issue: Issue = null
//    if (nissue.issueId.contains("02") || nissue.issueId.contains("01")) {
//      println("blowing for " + nissue.issueId)
//      issue = nissue.copy(issueId = null)
//
//    } else {
//      issue = nissue.copy()
//    }

    val insertQuery = loggedIssues += issue
    val futureResult: Try[Int] = Await.ready(db.run(insertQuery), 30 seconds).value.get

    val result = futureResult match {
      case scala.util.Success(numRows) => {
        if (numRows > 0) (true,"")
        else {
          val msg = s"insert db failed"
          log.error(msg)
          (false, msg)
        }
      }
      case scala.util.Failure(ex) => {
        val msg = s"insert db error ex=" + ex.toString
        log.error(msg)
        val sw = new StringWriter
        ex.printStackTrace(new PrintWriter(sw))
        log.error(sw.toString)
        (false, msg)
      }
    }
    result
  }

  //doesn't override as not in BaseDao
  def insert(issueDate: IssueDates): (Boolean, String) = {

    val insertQuery = issueDates += issueDate
    val futureResult: Try[Int] = Await.ready(db.run(insertQuery), 30 seconds).value.get

    val result = futureResult match {
      case scala.util.Success(numRows) => {
        if (numRows > 0) (true,"")
        else {
          val msg = s"insert dates db failed"
          log.error(msg)
          (false, msg)
        }
      }
      case scala.util.Failure(ex) => {
        val msg = s"insert dates db error ex=" + ex.toString
        log.error(msg)
        val sw = new StringWriter
        ex.printStackTrace(new PrintWriter(sw))
        log.error(sw.toString)
        (false, msg)
      }
    }
    result
  }


  def changeStatus(newStatus: Status, issue: Issue): Boolean = {

//    testing only
//    var nnewStatus = newStatus
//    if (issue.issueId.contains("00001") || issue.issueId.contains("00023")) {
//      println("blowing for " + issue.issueId)
//      nnewStatus = null
//    }

    val updateQuery = loggedIssues.filter(_.issueId === issue.issueId).map(iss => (iss.status)) update (newStatus)

    val futureResult: Try[Int] = Await.ready(db.run(updateQuery), 30 seconds).value.get

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





  def listGmcs: Future[Seq[String]] = {
    val query = loggedIssues.map(_.gmc ).distinct.sorted
    db.run(query.result)
  }

  def listOrigins: Future[Seq[String]] = {
    val query = loggedIssues.map(_.dataSource ).distinct.sorted
    db.run(query.result)
  }

  def listPriorities: Future[Seq[Int]] = {
    val query = loggedIssues.map(_.priority ).distinct.sorted
    db.run(query.result)
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

  def issueCounts(gmc: String): (Int, Int) = {
    val outstandingStatus = List(Open, Responded)
    val query1 = loggedIssues.filter(iss => iss.gmc === gmc).filter(_.status inSetBind outstandingStatus).length
    val outstanding = Await.result(db.run(query1.result), 30 seconds)

    val resolvedStatus = List(Resolved)
    val query2 = loggedIssues.filter(iss => iss.gmc === gmc).filter(_.status inSetBind resolvedStatus).length
    val resolved = Await.result(db.run(query2.result), 30 seconds)
    (outstanding, resolved)
  }

  def issueResolutionDuration(gmc: String): List[(String, Status, Option[Date], Option[Date])] = {
    val resolved = List(Resolved)

    val resolvedIssues = loggedIssues.filter(iss => iss.gmc === gmc).filter(_.status inSetBind resolved)
    val validDates = issueDates.filter(issd => issd.resolutionDate > issd.openDate)

    val innerJoin = for {
      (iss, issd) <- resolvedIssues join validDates on (_.issueId === _.issueId)

    } yield (iss.issueId, iss.status, issd.openDate, issd.resolutionDate)

    val resolutionDurations = Await.result(db.run(innerJoin.result), 30 seconds)
    resolutionDurations.toList
  }

  //count dataItemsGroupedBy gmc - this is the equivalent SQL
  //    select data_item, count(data_item)
  //    from issue
  //      where gmc='GEL'
  //    group by data_item
  //    order by data_item
  def dataItemsGroupedBy(gmc: String): List[(String, Int)] = {
    val qDataItemsByGmc = loggedIssues.filter(iss => iss.gmc === gmc)
      .groupBy(iss => iss.dataItem)
      .map { case (dataItem, group) => (dataItem, group.map(_.dataItem).length) }
      .sortBy(_._1)

    val dataItemsByGmc = Await.result(db.run(qDataItemsByGmc.result), 30 seconds)
    dataItemsByGmc.toList
  }


  // TODO: To be removed
  def tableSetup(issues: Seq[Issue], issuesDatesSeq: Seq[IssueDates]) = {
    val issueResults = db.run(DBIO.seq(
      //      loggedIssues.schema.create, --actually this creates funny schema with uppercase names & forces double quotes "ID"
      // bulk insert our sample data
      loggedIssues ++= issues
    ))
    Await.result(issueResults, 30 seconds)

    val issueDatesResults = db.run(DBIO.seq(
      issueDates ++= issuesDatesSeq
    ))
    Await.result(issueDatesResults, 30 seconds)
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

    def notes = column[Option[String]]("notes")


    override def * : ProvenShape[Issue] = (id, issueId, status, dateLogged, participantId, dataSource, priority,
      dataItem, shortDesc, gmc, lsid, area, description, familyId, notes) <>((Issue.apply _).tupled, Issue.unapply)

  }

  /** **************  Table definition ***************/
  class IssueDatesTable(tag: Tag) extends Table[IssueDates](tag, "issuedates") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def issueId = column[String]("issue_id")
//    def issue = foreignKey("issue_fkeyA", issueId, loggedIssues)(_.issueId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def openDate = column[Option[Date]]("open_date")

    def respondedDate = column[Option[Date]]("responded_date")

    def resolutionDate = column[Option[Date]]("resolution_date")

    def escalation = column[Option[Date]]("escalation")

    def openWho = column[Option[String]]("open_who")

    def respondedWho = column[Option[String]]("responded_who")

    def resolutionWho = column[Option[String]]("resolution_who")


    override def * : ProvenShape[IssueDates] = (id, issueId, openDate, respondedDate, resolutionDate, escalation,
      openWho, respondedWho, resolutionWho) <>((IssueDates.apply _).tupled, IssueDates.unapply)

  }

  /** **************  Table definition ***************/
  class IssueViewTable(tag: Tag) extends Table[IssueView](tag, "issueview") {
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

    def openDate = column[Option[Date]]("open_date")

    def weeksOpen = column[Option[Int]]("weeks_open")

    def resolutionDate = column[Option[Date]]("resolution_date")

    def escalation = column[Option[Date]]("escalation")

    def notes = column[Option[String]]("notes")


    override def * : ProvenShape[IssueView] = (id, issueId, status, dateLogged, participantId, dataSource, priority,
      dataItem, shortDesc, gmc, lsid, area, description, familyId, openDate, weeksOpen, resolutionDate,
      escalation, notes) <>((IssueView.apply _).tupled, IssueView.unapply)

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