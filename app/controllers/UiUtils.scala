package controllers

import java.text.SimpleDateFormat

import domain.QueryChain
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request}
import services.IssueStats

object UiUtils {

  def param(request: Map[String, Seq[String]], field: String): Option[String] =
    request.get(field).flatMap(_.headOption)

  def param(request: Request[AnyContent], field: String): Option[String] =
    param(request.queryString,field )

  //toJson without building a 'failure' case class & writes method
  def failuresToJsonRowIds(failures: List[(Int, Throwable)]): JsValue = {

    val list: List[JsObject] = failures.map { case (i, throwable) =>
      Json.obj("rownum" -> JsString(i.toString),
        "error" -> JsString(throwable.toString))
    }
    Json.toJson(list)
  }


  def failuresToJsonIssueIds(failures: List[(String, Throwable)]): JsValue = {

    val list: List[JsObject] = failures.map { case (s, throwable) =>
      Json.obj("rownum" -> JsString(s),
        "error" -> JsString(throwable.toString))
    }
    Json.toJson(list)
  }


  def gmcsToJson(gmcs: Seq[String]): JsValue = {
    val list: Seq[JsObject] = gmcs.map { gmc =>
      Json.obj("gmc" -> JsString(gmc))
    }
    Json.toJson(list)
  }


  def originsToJson(origins: Seq[String]): JsValue = {
    val list: Seq[JsObject] = origins.map { origin =>
      Json.obj("origin" -> JsString(origin))
    }
    Json.toJson(list)
  }

  def prioritiesToJson(priorities: Seq[Int]): JsValue = {
    val list: Seq[JsObject] = priorities.map { priority =>
      Json.obj("priority" -> JsString(priority.toString))
    }
    Json.toJson(list)
  }


  def reportsToJson(statsList: Seq[IssueStats]): JsValue = {
    val list: Seq[JsObject] = statsList.map {
      stats => {
        Json.obj(
          "gmc" -> stats.gmc,
          "outstanding" -> stats.outstanding,
          "resolved" -> stats.resolved,
          "qtime" -> stats.avgDaysOutstanding,
          "item" -> Json.toJson(stats.dataItem),
          "count" -> Json.toJson(stats.itemCount)
        )
      }
    }
    Json.toJson(list)
  }


  def queryChainsToJson(queryChains: Seq[QueryChain]): JsValue = {
    val list: Seq[JsObject] = queryChains.map { queryChain =>

      val dateStr = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(queryChain.date)

      Json.obj("status" -> JsString(queryChain.status),
        "date" -> JsString(dateStr),
        "user" -> JsString(queryChain.username),
        "partyid" -> JsNumber(queryChain.partyId),
        "comment" -> JsString(queryChain.comment))
    }
    Json.toJson(list)
  }

}
