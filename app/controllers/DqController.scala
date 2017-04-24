package controllers

import java.io.{PrintWriter, StringWriter}
import javax.inject._

import controllers.UiUtils._
import dao.Searching.{SearchRequest, SearchResult}
import domain._
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import services.{IssueTrackingService, MailService}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class DqController @Inject()(issueTracking: IssueTrackingService, saveIssueHelper: SaveIssueHelper, mailService: MailService)(implicit ec: ExecutionContext) extends Controller {

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
      Redirect(routes.DqController.listIssues))
  }



  def listAjaxAsync = Action.async(parse.tolerantFormUrlEncoded) { implicit req =>

    val searchRequest: SearchRequest = DatatablesRequestBuilder.build(req.body ++ req.queryString)

    val findResult: Future[SearchResult[Issue]] = issueTracking.findBySearchRequest(searchRequest)

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


  def listGmcs = Action.async { implicit req =>
    issueTracking.listGmcs.map(gmcs => Ok(gmcsToJson(gmcs)))
  }

  def listOrigins = Action.async { implicit req =>
    issueTracking.listOrigins.map(origins => Ok(originsToJson(origins)))
  }


  def container = Action {
    Ok(views.html.container())
  }



  def sendNotifications() = Action.async { implicit req =>
    val selected = param(req, "selectedIssues").get
    log.debug(s"selected issues=$selected")
    val issueIds = selected.split(",").toList

    val findResult: Future[SearchResult[Issue]] = issueTracking.findByIssueIds(issueIds)
    var total: Int = 0
    var selectedIssues: Seq[Issue] = null

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


  def export = Action.async(parse.tolerantFormUrlEncoded) { implicit req =>

    val searchRequest: SearchRequest = DatatablesRequestBuilder.build(req.body ++ req.queryString)

    val findResult: Future[SearchResult[Issue]] = issueTracking.findBySearchRequest(searchRequest)

    val csv = new StringBuilder(Issue.csvHeaderForUI + "\n")

    findResult.map {
      pageResult => {
        pageResult.items.map(issue => csv ++= (issue.toCsvForUI + "\n"))

        log.info(s"export returned ${pageResult.total} rows")

        Ok(csv.toString()).as("text/csv").withHeaders(
          CONTENT_DISPOSITION -> "attachment;filename=\"ExportedIssues.csv\"")
      }
    }
  }


  def upload = Action(parse.multipartFormData) { implicit request =>
    val result = Try {

      val filePart: FilePart[TemporaryFile] = request.body.files.head
      import java.io.File
      val filename = filePart.filename
      val contentType = filePart.contentType
      val toFile: File = new File(s"/tmp/filePart/$filename")
      toFile.delete()     //previous
      filePart.ref.moveTo(toFile)
      val failures: List[(Int, Throwable)] = issueTracking.importFile(toFile)

      if(failures.length == 0) Ok("OK")
      else {
        Ok(failuresToJsonRowIds(failures))
      }
    }
    result.getOrElse {
      val e: Throwable = result.failed.get
      val sw = new StringWriter
      e.printStackTrace(new PrintWriter(sw))
      log.error(s"File upload failed ${sw.toString}")
      Ok("File upload failed")
    }
  }



  def changeStatus = Action.async(parse.multipartFormData) { implicit req =>
    val result = Try {

      val body: Map[String, Seq[String]] = req.body.dataParts

      val selected = param(body, "selectedIssues").get
      val issueIds = selected.split(",").toList

      val change = param(body, "change").get
      val newStatus = domain.Status.allStatuses.find(_.toString == change).get
      log.debug(s"selected issues=$selected change=$change newStatus=$newStatus")

      val failures: Future[List[(String, Throwable)]] = issueTracking.changeStatus(newStatus,issueIds)

      val eventualResult: Future[Result] = failures.map { failed => {
        if (failed.isEmpty == 0) Ok("OK")
        else {
          log.error("failed.length=" + failed.length)
          Ok(failuresToJsonIssueIds(failed))
        }
      }
      }
      eventualResult
    }
    result.getOrElse {
      val e: Throwable = result.failed.get
      val sw = new StringWriter
      e.printStackTrace(new PrintWriter(sw))
      log.error(s"Change Status failed ${sw.toString}")
      Future(Ok("Change Status failed"))
    }
  }

  def reportsWorks = Action { implicit req =>
    val findResult: Future[Seq[QueryChain]] = issueTracking.allQc
    var total: Int = 0
    var selectedIssues: Seq[Issue] = null

    findResult.onComplete{
      case Success(chains) => {
        chains.map{println}
      }
      case Failure(e) => {e.printStackTrace}
    }

    Ok(Json.toJson("printed chains?"))
  }

  def reports = Action { implicit req =>
    val findResult: Future[Seq[(String,String,String)]] = issueTracking.findAllJoin

    findResult.onComplete{
      case Success(chains) => {
        chains.map{tuple => println(s"1:${tuple._1} 2:${tuple._2} 3:${tuple._3}")}
      }
      case Failure(e) => {e.printStackTrace}
    }

    Ok(reportsToJson(null))
  }


  def queryChain = Action.async(parse.multipartFormData) { implicit req =>
    val result = Try {

      val body: Map[String, Seq[String]] = req.body.dataParts

      val selected = param(body, "selectedIssue").get
      log.debug(s"selected issue=$selected")

      val eventualQueryChains: Future[Seq[QueryChain]] = issueTracking.queryChain(selected)

      val eventualResult: Future[Result] = eventualQueryChains.map {
        queryChains => {
          log.debug(s"issue:$selected queryChains.length= ${queryChains.length}")
          if (queryChains.isEmpty == 0) Ok("OK")
          else {
            Ok(queryChainsToJson(queryChains))
          }
        }
      }
      eventualResult
    }
    result.getOrElse {
      val e: Throwable = result.failed.get
      val sw = new StringWriter
      e.printStackTrace(new PrintWriter(sw))
      log.error(s"Retrieving Query Chain failed ${sw.toString}")
      Future(Ok("Retrieving Query Chain failed"))
    }
  }


  def nextIssueId = Action(parse.multipartFormData) { implicit req =>
    val body: Map[String, Seq[String]] = req.body.dataParts

    val gmc = param(body, "gmc").getOrElse("")

    import scala.concurrent.duration._
    var nextIssueId = ""
    val result: Try[String] = Await.ready(issueTracking.nextIssueId(gmc), 30 seconds).value.get
    result match {
      case scala.util.Success(nextId) => nextIssueId = nextId
      case scala.util.Failure(e) => log.error(e.toString)
    }
    Ok(nextIssueId)
  }

  def save = Action(parse.multipartFormData) { implicit req =>
    Ok(saveIssueHelper.validateAndSave(req.body.dataParts).toString)
  }
}
