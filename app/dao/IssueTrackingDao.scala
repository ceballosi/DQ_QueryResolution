package dao

import java.util.Date
import javax.inject.{Inject, Singleton}

import dao.DaoUtils._
import dao.Searching.{SearchRequest, SearchResult}
import domain._
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.{Await, ExecutionContext, Future}

trait IssueTrackingDao extends BaseDao[LoggedIssue, Long] {
  def findBySearchRequest(pageRequest: SearchRequest): Future[SearchResult[LoggedIssue]]

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


  // TODO: To be removed
  def tableSetup(data: Seq[LoggedIssue]) = {
    val g = db.run(DBIO.seq(
      // create the schema
      loggedIssues.schema.create,
      // to bulk insert our sample data
      loggedIssues ++= data
    ))
    import scala.concurrent.duration._
    Await.result(g, 10 seconds)
  }

  def findAll: Future[Seq[LoggedIssue]] = db.run(loggedIssues.result)

  def findByCriteria(cr : SearchCriteria): Future[Seq[LoggedIssue]] = {
    val q = MaybeFilter(loggedIssues)
      .filter(cr.gmc)(v => d => d.GMC === v) // v => parameter value passed in  d=> Table data element
      .filter(cr.issueId)(v => d => d.issueId === v)
      .filter(cr.issueStatus)(v => d => d.status === v)
      .filter(cr.urgent)(v => d => d.urgent === v)
      .filter(cr.issueOrigin)(v => d => d.issueOrigin === v)
      .filter(cr.dateLogged)(v => d => d.dateLogged > v)
      .filter(cr.patientId)(v => d => d.patientId === v)
      .query
    db.run(q.result)
  }

  def update(o: LoggedIssue): Future[Unit] = ???

  def findById(id: Long): Future[Option[LoggedIssue]] = db.run(loggedIssues.filter(_.id === id).result.headOption)



  def findBySearchRequest(searchRequest: SearchRequest): Future[SearchResult[LoggedIssue]] = {
    val cr: SearchCriteria = searchRequest.searchCriteria
    val sortField = (searchRequest.sortFields.get)(0)
    val sortDir = (searchRequest.sortDirections.get)(0)

    val filterQuery = MaybeFilter(loggedIssues)
      .filter(cr.gmc)(v => d => d.GMC === v) // v => parameter value passed in  d=> Table data element
      .filter(cr.issueId)(v => d => d.issueId === v)
      .filter(cr.issueStatus)(v => d => d.status === v)
      .filter(cr.urgent)(v => d => d.urgent === v)
      .filter(cr.issueOrigin)(v => d => d.issueOrigin === v)
      .filter(cr.dateLogged)(v => d => d.dateLogged > v)
      .filter(cr.patientId)(v => d => d.patientId === v)
      .query

    //working
    val issuesFuture = db.run(
      filterQuery.sortBy(_.dateLogged.desc).drop(searchRequest.offset).take(searchRequest.size).result
    )

//clean attempt using map - 2 issues to fix 1. status/date mapping 2. dynamically choose sort direction
//    val issuesFuture = db.run(
//      filterQuery.sortBy(_.columnMap(sortField)).drop(pageRequest.offset).take(pageRequest.size).result
//    )

    val filterIssuesCount: Future[Int] = db.run(
      filterQuery.length.result
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


  def findByParam(offset: Int, pageSize: Int) : Future[SearchResult[LoggedIssue]] = {

    val issuesFuture = db.run(
      loggedIssues.filter(_.loggedBy === "JS").drop(offset).take(pageSize).result
    )
    val filterIssuesCount: Future[Int] = db.run(
      loggedIssues.filter(_.loggedBy === "JS").length.result
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

    //map matches subset of fields from page that can be sorted
    def columnMap = Map("status" -> status, "DT_RowId" -> issueId, "loggedBy" -> loggedBy, "dateLogged" -> dateLogged, "issueOrigin" -> issueOrigin, "GMC" -> GMC, "description" -> description, "familyId" -> familyId)
  }

}