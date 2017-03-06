package domain

import java.util.Date
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
                      )

object LoggedIssue {

  implicit val loggedIssueWrites = new Writes[LoggedIssue] {
    def writes(c: LoggedIssue): JsValue = {
      Json.obj(
        "status" -> c.status.toString,
        "DT_RowId" -> Json.toJson(c.issueId),
        "loggedBy" -> Json.toJson(c.loggedBy),
        "dateLogged" -> c.dateLogged.toString,
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
        "resolutionDate" -> Json.toJson(c.resolutionDate),
        "comments" -> Json.toJson(c.comments)
      )
    }
  }
}

trait Status

case object Open extends Status

case object Close extends Status

case object InvalidStatus extends Status

object Status extends Status{
  def validStatuses = Vector(Open, Close)
}


