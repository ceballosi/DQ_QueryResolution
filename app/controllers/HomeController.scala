package controllers

import javax.inject._

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

}
