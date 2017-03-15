package controllers

import javax.inject._

import dao.Searching.{SearchRequest, SearchResult}
import UiUtils._
import domain._
import org.slf4j.{LoggerFactory, Logger}
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.mvc._
import services.{MailService, IssueTrackingService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class HomeController @Inject()(issueTracking: IssueTrackingService, mailService: MailService)(implicit ec: ExecutionContext) extends Controller {

  val log: Logger = LoggerFactory.getLogger(this.getClass())

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



  def sendNotifications() = Action.async { implicit req =>
    val selected = param(req, "selectedIssues").get
    log.debug(s"selected issues=$selected")
    val issueIds = selected.split(",").toList

    val findResult: Future[SearchResult[LoggedIssue]] = issueTracking.findByIssueIds(issueIds)
    var total: Int = 0
    var selectedIssues: Seq[LoggedIssue] = null

    findResult.onComplete{
      case Success(searchResult) => {
        total = searchResult.total
        selectedIssues = searchResult.items
        mailService.send(selectedIssues)

      }
      case Failure(e) => {e.printStackTrace}
    }

    Future(Ok(Json.toJson("sent email? or queued?")))
  }
}
