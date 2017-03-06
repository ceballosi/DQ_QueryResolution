package controllers

import javax.inject._

import domain.{LoggedIssue, Open}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json._
import play.api.libs.functional.syntax._
import play.api.mvc._
import services.IssueTrackingService

import scala.concurrent.ExecutionContext

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
    val draw = drawReq.get.toInt    //security

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

  def container = Action {
    Ok(views.html.container())
  }

}
