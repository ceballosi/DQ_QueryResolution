package controllers

import javax.inject._

import dao.Searching.{SearchRequest, SearchResult}
import UiUtils._
import domain._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.mvc._
import services.{MailService, IssueTrackingService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(issueTracking: IssueTrackingService, mailService: MailService)(implicit ec: ExecutionContext) extends Controller {

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



  def listAjaxAsync = Action.async { implicit req =>

    val searchRequest: SearchRequest = DatatablesRequestBuilder.build(req)

    val findResult: Future[SearchResult[LoggedIssue]] = issueTracking.findBySearchRequest(searchRequest)

    findResult.map {
      pageResult => {
        val json = Json.obj(
          "draw" -> searchRequest.uiRequestToken,
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


  def sendNotifications() = Action.async{ implicit req =>
    val selected = param(req, "selectedIssues").get
    println(s"yippee! selected issues=$selected")
//    mailService.configureAndSend(selected)
    mailService.send(selected)
    Future(Ok(Json.toJson("should send selected notifications here")))
  }
}
