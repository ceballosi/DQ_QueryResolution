package controllers

import java.util.Date
import javax.inject._

import dao.Searching.{SearchRequest, SearchResult}
import domain._
import org.joda.time.LocalDate
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.IssueTrackingService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(issueTracking: IssueTrackingService)(implicit ec: ExecutionContext) extends Controller {

  private val uiColumnNames: List[String] = List("status", "DT_RowId", "loggedBy", "dateLogged", "issueOrigin", "GMC", "description", "familyId")

  /** Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def listIssues = Action.async { implicit req =>
    issueTracking.allIssues.map(issues => Ok(views.html.issues(issues)))
  }

  //TODO : To be removed (Just a temporary method to create a table using model)
  def tmpMethod = Action.async { implicit req =>
    issueTracking.tmpMethod.map(_ =>
      Redirect(routes.HomeController.listIssues))
  }


  // not async atm...
  def listAjax = Action { implicit req =>
    val drawReq: Option[String] = req.getQueryString("draw")
    val draw = drawReq.get.toInt //security

    val recordsTotal: String = issueTracking.allIssuesNow.length.toString
    val issues: List[LoggedIssue] = issueTracking.allIssuesNow.take(10)
    val recordsFiltered: String = issues.length.toString

    val json: JsValue = Json.obj(
      "draw" -> draw,
      "recordsTotal" -> recordsTotal,
      "recordsFiltered" -> recordsTotal,
      "data" -> Json.toJson(issues)
    )
    Ok(json)
  }


  def listAjaxAsync = Action.async { implicit req =>

    //for security coerce these to int
    val draw = req.getQueryString("draw").get.toInt
    val offset = req.getQueryString("start").get.toInt
    val pageSize = req.getQueryString("length").get.toInt

    val filter = req.getQueryString("filter")
    var isNew = false

    filter match {
      case Some(s) if s.equalsIgnoreCase("new") => isNew = true
      case Some(s) => println("filter val? " + s)
      case None => println("none")
    }

    if (isNew) {
      val findResult: Future[SearchResult[LoggedIssue]] = issueTracking.findByParam(offset, pageSize)
    }

    val findResult: Future[SearchResult[LoggedIssue]] = issueTracking.findByParam(offset, pageSize)

    findResult.map {
      pageResult => {
        val json = Json.obj(
          "draw" -> draw,
          "recordsTotal" -> pageResult.total.toString,
          "recordsFiltered" -> pageResult.total.toString,
          "data" -> Json.toJson(pageResult.items)
        )
        Ok(json)
      }
    }
  }


  def listAjaxAsync2 = Action.async { implicit req =>

    //for security coerce these to int
    val draw = req.getQueryString("draw").get.toInt
    val offset = req.getQueryString("start").get.toInt
    val pageSize = req.getQueryString("length").get.toInt

    val queryString = req.queryString
    val filter = param(queryString, "filter")
    var isNew = false

    filter match {
      case Some(s) if s.equalsIgnoreCase("new") => {
        isNew = true
        println("new issues selected")
      }
      case Some(s) => println("filter value?= " + s)
      case None => println("no filter selected")
    }


    val gmc = param(queryString, "gmc")


    var dateLogged: Option[Date] = None
    var issueStatus: Option[domain.Status] = None

    if (isNew) {
      issueStatus = Some(Open)
      val days = param(queryString, "days").getOrElse("0").toInt
      dateLogged = Some(LocalDate.now().minusDays(days).toDate)
    }


    var sortCol = param(queryString, "order[0][column]")
    val sortDir = param(queryString, "order[0][dir]")
    println("sortCol=" + sortCol)
    println("sortDir=" + sortDir)



    val sortCOlFromUI = uiColumnNames(sortCol.getOrElse("3").toInt)
    println("sortCOlFromUI=" + sortCOlFromUI)

    val sortOrderFromUI = sortDir.getOrElse("asc")
    println("sortOrderFromUI=" + sortOrderFromUI)

    val sortFields: Option[List[String]] = Some(List(sortCOlFromUI))
    val sortDirections: Option[List[String]] = Some(List(sortOrderFromUI))

    val searchCriteria = SearchCriteria(gmc, issueStatus = issueStatus, dateLogged = dateLogged)
    val searchRequest = SearchRequest(offset, pageSize, searchCriteria, sortFields, sortDirections)

    val findResult: Future[SearchResult[LoggedIssue]] = issueTracking.findBySearchRequest(searchRequest)

    findResult.map {
      pageResult => {
        val json = Json.obj(
          "draw" -> draw,
          "recordsTotal" -> pageResult.total.toString,
          "recordsFiltered" -> pageResult.total.toString,
          "data" -> Json.toJson(pageResult.items)
        )
        Ok(json)
      }
    }
  }

  def param(request: Map[String, Seq[String]], field: String): Option[String] =
    request.get(field).flatMap(_.headOption)


  def container = Action {
    Ok(views.html.container())
  }

}
