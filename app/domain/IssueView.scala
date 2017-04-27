package domain

import java.text.SimpleDateFormat
import java.util.Date
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

case class IssueView(//19 fields (out of possible 22!)
                 id: Long,
                 issueId: String,
                 status: Status,
                 dateLogged: Date,
                 participantId: Int,
                 dataSource: String,
                 priority: Int,
                 dataItem: String,
                 shortDesc: String,
                 gmc: String,
                 lsid: Option[String],
                 area: String,
                 description: String,
                 familyId: Option[String],
                 queryDate: Option[Date],
                 weeksOpen: Option[Int],
                 resolutionDate: Option[Date],
                 escalation: Option[Date],
                 notes: Option[String]
                ) extends {

  def toCsvForUI(): String = {
    val dateLoggedStr = IssueView.dateToIsoStr(dateLogged)
    val lsidStr = lsid.getOrElse("")
    val family = familyId.getOrElse("")
    val querySent = IssueView.dateToIsoStr(queryDate)
    val weeks = weeksOpen.getOrElse("")
    val resolvedDt = IssueView.dateToIsoStr(resolutionDate)
    val escalateDt = IssueView.dateToIsoStr(escalation)

    s"$issueId,$status,$dateLoggedStr,$participantId,$dataSource,$priority,$dataItem,$shortDesc,$gmc,$lsidStr,$area,$description,$family,$querySent,$weeks,$resolvedDt,$escalateDt,$notes"
  }
}

object IssueView {

  implicit val loggedIssueViewWrites = new Writes[IssueView] {
    def writes(c: IssueView): JsValue = {
      Json.obj(
        "select" -> "", //UI selection col
        "DT_RowId" -> Json.toJson(c.issueId),
        "status" -> c.status.toString,
        "dateLogged" -> dateToStr(c.dateLogged),
        "participantId" -> Json.toJson(c.participantId),
        "dataSource" -> Json.toJson(c.dataSource),
        "priority" -> Json.toJson(c.priority),
        "dataItem" -> Json.toJson(c.dataItem),
        "shortDesc" -> Json.toJson(c.shortDesc),
        "gmc" -> Json.toJson(c.gmc),
        "lsid" -> Json.toJson(c.lsid),
        "area" -> Json.toJson(c.area),
        "description" -> Json.toJson(c.description),
        "familyId" -> Json.toJson(c.familyId),
        "queryDate" -> dateToStr(c.queryDate),
        "weeksOpen" -> Json.toJson(c.weeksOpen),
        "resolutionDate" -> dateToStr(c.resolutionDate),
        "escalation" -> dateToStr(c.escalation),
        "notes" -> Json.toJson(c.notes)
      )
    }
  }

  def csvHeaderForUI(): String = "IssueId,Status,Date Logged,ParticipantId,Data Source,Priority,Data Item,Short Desc,GMC,LSID,Therapeutic Area,Description,FamilyId,Query Date,Weeks Open,Resolution Date,Escalation,Notes"

  def dateToStr(date: Option[Date]): String = {
    date match {
      case Some(date) => new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(date)
      case None => ""
    }
  }

  def dateToStr(date: Date): String = {
    new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(date)
  }

  def dateToIsoStr(date: Option[Date]): String = {
    date match {
      case Some(date) => ISODateTimeFormat.dateTime().print(date.getTime)
      case None => ""
    }
  }

  def dateToIsoStr(date: Date): String = {
    ISODateTimeFormat.dateTime().print(date.getTime)
  }

}





