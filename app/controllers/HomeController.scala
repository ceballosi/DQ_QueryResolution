package controllers

import javax.inject._

import play.api._
import play.api.mvc._
import services.{IssueTrackingService, IssueTrackingServiceImpl}

@Singleton
class HomeController @Inject()(issueTracking : IssueTrackingService) extends Controller {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def listIssues = Action {
    val issueList = issueTracking.allIssues
    Ok(views.html.issues(issueList))
  }

}
