package controllers

import javax.inject._

import dao.Paging.PageResult
import domain.LoggedIssue
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.IssueTrackingService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(issueTracking: IssueTrackingService)(implicit ec: ExecutionContext) extends Controller {

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

    val draw = req.getQueryString("draw").get.toInt //security
    val offset = req.getQueryString("start").get.toInt
    val pageSize = req.getQueryString("length").get.toInt

    val findResult: Future[PageResult[LoggedIssue]] = issueTracking.findByParam(offset, pageSize)

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


  def container = Action {
    Ok(views.html.container())
  }

}
