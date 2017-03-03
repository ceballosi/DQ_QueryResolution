package domain

import java.util.Date

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


trait Status

case object Open extends Status

case object Close extends Status

case object InvalidStatus extends Status

object Status extends Status{
  def validStatuses = Vector(Open, Close)
}
