package domain


import java.util.Date

case class LoggedIssue(
                        status: Status,
                        id: String,
                        loggedBy: String,
                        dateLogged: Date,
                        issueOrigin: Origin,
                        GMC: String,
                        urgent: Option[Boolean],
                        familyId: String,
                        patientId: Option[String],
                        dataItem: Option[String],
                        description: String,
                        fileReference: Option[String],
                        dateSent: Option[Date],
                        weeksOpen: Option[Int],
                        escalation : Option[String],
                        dueForEscalation: Option[Boolean],
                        resolution: Option[String],
                        resolutionDate: Option[Date],
                        comments: Option[String]
                      )

trait Status

case object Open extends Status

case object Closed extends Status

trait Origin

case object DataQuality extends Origin

case object BioInformatics extends Origin

case object RedTeam extends Origin

case object CancerProg extends Origin

case object SampleTracking extends Origin

case object ServiceDesk extends Origin

case object Informatics extends Origin
