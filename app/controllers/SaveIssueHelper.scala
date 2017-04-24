package controllers

import java.util.Date
import javax.inject.Inject

import controllers.UiUtils._
import domain.{Draft, Issue, Status}
import org.slf4j.{Logger, LoggerFactory}
import services.{IssueImportValidator, IssueTrackingService}

import scala.concurrent.ExecutionContext

class SaveIssueHelper @Inject()(issueTrackingService: IssueTrackingService, validator: IssueImportValidator)(implicit ec: ExecutionContext) {
  val log: Logger = LoggerFactory.getLogger(this.getClass())


  def validateAndSave(request: Map[String, Seq[String]]): String = {

    val newIssue = createIssue(request)

    var (pass, error) = validate(newIssue)
    pass = true
//    var finalResult = ""
    if (pass) {
//      finalResult = save(newIssue)
       save(newIssue)
    } else {
      log.error(error)
//      finalResult = error
      error
    }
//    finalResult
  }


  def createIssue(request: Map[String, Seq[String]]): Issue = {
    //gather params
    val issueId = param(request, "issueId").getOrElse("")
    val status = Status.statusFrom(param(request, "status")).getOrElse(Draft)
    //    val dateLogged: Date = param(request, "dateLogged").getOrElse(new Date())
    val dateLogged: Date = null
    //    val participantId: Int = Option(param(request, "participantId")).getOrElse(0).toInt
    val participantId = 1
    val dataSource = param(request, "dataSource").getOrElse("")
    //    val priority: Int = Option(param(request, "priority")).getOrElse(0).toInt
    val priority = 0
    val dataItem = param(request, "dataItem").getOrElse("")
    val shortDesc = param(request, "shortDesc").getOrElse("")
    val gmc = param(request, "gmc").getOrElse("")
    val lsid = param(request, "lsid")
    val area = param(request, "area").getOrElse("")
    val description = param(request, "description").getOrElse("")
    val familyId = param(request, "familyId")
    val notes = param(request, "notes")

    val newIssue = new Issue(0, issueId, status, dateLogged, participantId, dataSource, priority, dataItem, shortDesc, gmc, lsid, area,
      description, familyId, queryDate = None, weeksOpen = None, resolutionDate = None, escalation = None, notes)

    log.info("newIssue=" + newIssue)
    newIssue
  }


  def validate(issue: Issue): (Boolean, String) = {
    validator.validateIssue(1, issue)
  }


  def save(newIssue: Issue): String = {
    val (pass, error) = issueTrackingService.save(newIssue)

    if (pass) {
      "Save ok"
    } else {
      log.error(s"Saving issue failed ${error}")
      s"Save failed, ${error}"
    }
  }


}
