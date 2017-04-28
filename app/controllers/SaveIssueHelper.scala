package controllers

import java.util.Date
import javax.inject.Inject

import controllers.UiUtils._
import domain.{Draft, Issue, Status}
import org.joda.time.{DateTime, LocalDateTime}
import org.joda.time.format.{DateTimeFormatter, DateTimeFormat}
import org.slf4j.{Logger, LoggerFactory}
import services.{IssueImportValidator, IssueTrackingService}

import scala.concurrent.ExecutionContext

class SaveIssueHelper @Inject()(issueTrackingService: IssueTrackingService, validator: IssueImportValidator)(implicit ec: ExecutionContext) {
  val log: Logger = LoggerFactory.getLogger(this.getClass())
  val dateTimeFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss")


  def validateAndSave(request: Map[String, Seq[String]]): String = {

    val newIssue = createIssue(request)

    val (pass, error) = validate(newIssue)
    if (pass) {
      save(newIssue)
    } else {
      log.error(error)
      error
    }
  }


  def createIssue(request: Map[String, Seq[String]]): Issue = {
    //gather params
    val issueId = param(request, "issueId").getOrElse("")
    val status = Status.statusFrom(param(request, "status")).getOrElse(Draft)

    val dateToParse = param(request, "dateLogged").getOrElse(dateTimeFormat.print(new DateTime)) //formatted default
    val dateLogged: Date = LocalDateTime.parse(dateToParse, dateTimeFormat).toDate

    val participantId: Int = param(request, "participant").getOrElse("0").toInt
    val dataSource = param(request, "dataSource").getOrElse("")
    val priority: Int = param(request, "priority").getOrElse("0").toInt
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

    log.debug("newIssue=" + newIssue)
    newIssue
  }


  def validate(issue: Issue): (Boolean, String) = {
    val (pass, error) = validator.validateIssue(1, issue)

    if (pass) {
      (pass, error)
    } else {
      (pass, s"Validation failed, ${error}")
    }
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
