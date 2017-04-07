package domain

import java.text.SimpleDateFormat
import java.util.Date
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

case class LoggedIssue(
                        id : Long,
                        issueId: String,
                        status: Status,
                        loggedBy: String,
                        dateLogged: Date,
                        issueOrigin: String,
                        GMC: String,
                        urgent: Option[Boolean],
                        familyId: String,
                        patientId: Option[String],
                        dataItem: Option[String],
                        description: String,
                        fileReference: Option[String],
                        dateSent: Option[Date],
                        weeksOpen: Option[Int],
                        escalation: Option[String],
                        dueForEscalation: Option[Boolean],
                        resolution: Option[String],
                        resolutionDate: Option[Date],
                        comments: Option[String]
                      ) {

  def toCsvForUI(): String = {
    this.issueId + "," + this.status + "," + this.loggedBy + "," + ISODateTimeFormat.dateTime().print(this.dateLogged.getTime) + "," + this.issueOrigin + "," + this.GMC + "," + this.description + "," + this.patientId.getOrElse("")
  }
}

object LoggedIssue {

  implicit val loggedIssueWrites = new Writes[LoggedIssue] {
    def writes(c: LoggedIssue): JsValue = {
      val dateLogged = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(c.dateLogged)

      val dateResolved = c.resolutionDate match {
        case Some(date) => new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(date)
        case None => ""
      }

      Json.obj(
        "select" -> "", //UI selection col
        "status" -> c.status.toString,
        "DT_RowId" -> Json.toJson(c.issueId),
        "loggedBy" -> Json.toJson(c.loggedBy),
        "dateLogged" -> dateLogged,
        "issueOrigin" -> Json.toJson(c.issueOrigin),
        "GMC" -> Json.toJson(c.GMC),
        "urgent" -> Json.toJson(c.urgent),
        "familyId" -> Json.toJson(c.familyId),
        "patientId" -> Json.toJson(c.patientId),
        "dataItem" -> Json.toJson(c.dataItem),
        "description" -> Json.toJson(c.description),
        "fileReference" -> Json.toJson(c.fileReference),
        "dateSent" -> Json.toJson(c.dateSent),
        "weeksOpen" -> Json.toJson(c.weeksOpen),
        "escalation" -> Json.toJson(c.escalation),
        "dueForEscalation" -> Json.toJson(c.dueForEscalation),
        "resolution" -> Json.toJson(c.resolution),
        "resolutionDate" -> dateResolved,
        "comments" -> Json.toJson(c.comments)
      )
    }
  }

  def csvHeaderForUI(): String = "IssueId,Status,LoggedBy,DateLogged,IssueOrigin,GMC,Description,PatientId"
}

trait Status

case object Draft extends Status

case object Open extends Status

case object Closed extends Status

//TODO - remove this - don't think we should allow invalid status at all!
case object InvalidStatus extends Status

object Status {
  def validStatuses = Vector(Draft, Open, Closed)
  def allStatuses = Vector(Draft, Open, Closed, InvalidStatus)

  def statusFrom(strStatus: String): Status = {
    domain.Status.allStatuses.find(_.toString == strStatus).get
  }

  def statusFrom(optionStrStatus: Option[String]): Option[Status] = {
    optionStrStatus match {
      case Some(value) => Some(statusFrom(value))
      case _ => None
    }
  }

}

case class SearchCriteria(gmc: Option[String] = None,
                          issueId: Option[String] = None,
                          issueStatus: Option[Status] = None,
                          loggedBy: Option[Boolean] = None,
                          urgent: Option[Boolean] = None,
                          issueOrigin: Option[String] = None,
                          dateLogged: Option[Date] = None,
                          patientId: Option[String] = None,
                          searchValue: Option[String] = None
                         )


