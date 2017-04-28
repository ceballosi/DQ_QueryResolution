package controllers

import javax.inject._

import controllers.UiUtils._
import dao.Searching.{SearchRequest, SearchResult}
import domain._
import org.pac4j.core.config.Config
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.PlaySessionStore
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.mvc._
import play.libs.concurrent.HttpExecutionContext
import services.{IssueTrackingService, MailService}

import scala.collection.JavaConversions
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import org.pac4j.play.scala._
import org.pac4j.core.profile._

@Singleton
class HomeController @Inject()(val config: Config, val playSessionStore: PlaySessionStore, override val ec: HttpExecutionContext)
                              (issueTracking: IssueTrackingService, mailService: MailService)
                              (implicit executionContext: ExecutionContext) extends Controller with Security[CommonProfile] {

  val log = LoggerFactory getLogger getClass

  private def getProfiles(implicit request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    JavaConversions.asScalaBuffer(profiles).toList
  }

  /** Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def listIssues = Action.async { implicit req =>
    issueTracking.allIssues.map(issues => Ok(views.html.issues(issues, getProfiles(req))))
  }

  //TODO : To be removed (Just a temporary method to create a table using model)
  def tmpMethod = Action.async { implicit req =>
    issueTracking.tmpMethod.map(_ =>
      Redirect(routes.HomeController.listIssues))
  }



  def listAjaxAsync = Action.async { implicit req =>

    val searchRequest: SearchRequest = DatatablesRequestBuilder.build(req.queryString)

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
      case Success(searchResult) =>
        total = searchResult.total
        selectedIssues = searchResult.items
        mailService.send(selectedIssues)

      case Failure(e) => e printStackTrace()
    }

    Future(Ok(Json.toJson("sent email? or queued?")))
  }


  def export = Action.async(parse.tolerantFormUrlEncoded) { implicit req =>

    val searchRequest: SearchRequest = DatatablesRequestBuilder.build(req.queryString)

    val findResult: Future[SearchResult[LoggedIssue]] = issueTracking.findBySearchRequest(searchRequest)

    val csv = new StringBuilder(LoggedIssue.csvHeaderForUI + "\n")

    findResult.map {
      pageResult => {
        pageResult.items.map(issue => csv ++= (issue.toCsvForUI + "\n"))

        log.info(s"export returned ${pageResult.total} rows")

        Ok(csv.toString()).as("text/csv").withHeaders(
          CONTENT_DISPOSITION -> "attachment;filename=\"ExportedIssues.csv\"")
      }
    }
  }


}
