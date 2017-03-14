package dao

import java.util.Date
import javax.inject.{Inject, Singleton}

import dao.DaoUtils._
import dao.Searching.{SearchRequest, SearchResult}
import domain._
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.{ColumnOrdered, ProvenShape}

import scala.concurrent.{Await, ExecutionContext, Future}

trait IssueTrackingDao extends BaseDao[LoggedIssue, Long] {
  def findBySearchRequest(searchRequest: SearchRequest): Future[SearchResult[LoggedIssue]]

  def findByCriteria(cr : SearchCriteria): Future[Seq[LoggedIssue]]
  // TODO To be removed
  def tableSetup(data: Seq[LoggedIssue])
}

/**
  * @param dbConfigProvider Play db config provider. Play injects this
  */
@Singleton
class IssueTrackingDaoImpl @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends IssueTrackingDao {
  //JdbcProfile for this provider
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  // To bring db in to the current scope
  import dbConfig._

  // To bring slick DSL into scope to define table and other queries
  import driver.api._

  override def toTable = TableQuery[LoggedIssueTable]

  private val loggedIssues = toTable

  // Custom column mapping
  implicit val statusMapper = MappedColumnType.base[Status, String](
    d => d.toString, d => Status.validStatuses.find(_.toString == d).getOrElse(InvalidStatus)
  )

  implicit val dateMapper = MappedColumnType.base[java.util.Date, java.sql.Timestamp](
    d => new java.sql.Timestamp(d.getTime),
    d => new java.util.Date(d.getTime)
  )

  private val columnMap = Map(
    "status" -> { (t: LoggedIssueTable) => t.status },
    "DT_RowId" -> { (t: LoggedIssueTable) => t.issueId},
    "loggedBy" -> { (t: LoggedIssueTable) => t.loggedBy},
    "dateLogged" -> { (t: LoggedIssueTable) => t.dateLogged},
    "issueOrigin" -> { (t: LoggedIssueTable) => t.issueOrigin},
    "GMC" -> { (t: LoggedIssueTable) => t.GMC},
    "description" -> { (t: LoggedIssueTable) => t.description},
    "familyId" -> { (t: LoggedIssueTable) => t.familyId},
    "patientId" -> { (t: LoggedIssueTable) => t.patientId}
  )


  private def queryBySortCriteria(query: Query[LoggedIssueTable, LoggedIssueTable#TableElementType, Seq], sort: (String, String)) = {
        val rep = columnMap.getOrElse(sort._1, throw new RuntimeException(s"Invalid column used for sorting ${sort._1}"))
      val orderedRep = sort._2 match {
        case "desc" => (t: LoggedIssueTable) => ColumnOrdered(rep(t), slick.ast.Ordering(slick.ast.Ordering.Desc))
        case _ => (t: LoggedIssueTable) => ColumnOrdered(rep(t), slick.ast.Ordering(slick.ast.Ordering.Asc))
      }
      query.sortBy(orderedRep)

  }

  private def queryBySearchCriteria(cr : SearchCriteria)  = {
    MaybeFilter(loggedIssues)
      .filter(cr.gmc)(v => d => d.GMC === v) // v => parameter value passed in  d=> Table data element
      .filter(cr.issueId)(v => d => d.issueId === v)
      .filter(cr.issueStatus)(v => d => d.status === v)
      .filter(cr.urgent)(v => d => d.urgent === v)
      .filter(cr.issueOrigin)(v => d => d.issueOrigin === v)
      .filter(cr.dateLogged)(v => d => d.dateLogged > v)
      .filter(cr.patientId)(v => d => d.patientId === v)
      .query
  }


  // TODO: To be removed
  def tableSetup(data: Seq[LoggedIssue]) = {
    val g = db.run(DBIO.seq(
      // create the schema
      loggedIssues.schema.create,
      // to bulk insert our sample data
      loggedIssues ++= data
    ))
    import scala.concurrent.duration._
    Await.result(g, 30 seconds)
  }

  def findAll: Future[Seq[LoggedIssue]] = db.run(loggedIssues.result)

  def findByCriteria(cr : SearchCriteria): Future[Seq[LoggedIssue]] = {
    db.run(queryBySearchCriteria(cr).result)
  }

  def update(o: LoggedIssue): Future[Unit] = ???

  def findById(id: Long): Future[Option[LoggedIssue]] = db.run(loggedIssues.filter(_.id === id).result.headOption)



  def findBySearchRequest(searchRequest: SearchRequest): Future[SearchResult[LoggedIssue]] = {
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

    val eventualPageResult: Future[SearchResult[LoggedIssue]] = filterIssuesCount.flatMap {
      total => issuesFuture.map {
        issues => {
          SearchResult(issues, total)
        }
      }
    }
    eventualPageResult
  }



  /** **************  Table definition ***************/
  class LoggedIssueTable(tag: Tag) extends Table[LoggedIssue](tag, "loggedIssue") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def issueId: Rep[String] = column[String]("ISSUE_ID")

    def status = column[Status]("STATUS")

    def loggedBy = column[String]("LOGGED_BY")

    def dateLogged = column[Date]("DATE_LOGGED")

    def issueOrigin = column[String]("ISSUE_ORIGIN")

    def GMC = column[String]("GMC")

    def urgent = column[Option[Boolean]]("URGENT")

    def familyId = column[String]("FAMILY_ID")

    def patientId = column[Option[String]]("PATIENT_ID")

    def dataItem = column[Option[String]]("DATA_ITEM")

    def description = column[String]("DESCRIPTION")

    def fileReference = column[Option[String]]("FILE_REFERENCE")

    def dateSent = column[Option[Date]]("DATE_SENT")

    def weeksOpen = column[Option[Int]]("WEEKS_OPEN")

    def escalation = column[Option[String]]("ESCALATION")

    def dueForEscalation = column[Option[Boolean]]("DUE_FOR_ESCALATION")

    def resolution = column[Option[String]]("RESOLUTION")

    def resolutionDate = column[Option[Date]]("RESOLUTION_DATE")

    def comments = column[Option[String]]("COMMENTS")

    override def * : ProvenShape[LoggedIssue] = (id, issueId, status, loggedBy, dateLogged, issueOrigin, GMC, urgent,
      familyId, patientId, dataItem, description, fileReference, dateSent, weeksOpen, escalation, dueForEscalation,
      resolution, resolutionDate, comments) <>((LoggedIssue.apply _).tupled, LoggedIssue.unapply)

  }

}