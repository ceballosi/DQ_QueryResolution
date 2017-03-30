package controllers

import play.api.libs.json.{JsString, Json, JsObject, JsValue}
import play.api.mvc.{AnyContent, Request}

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

  //toJson without building a 'failure' case class & writes method
  def failuresToJsonIssueIds(failures: List[(String, Throwable)]): JsValue = {

    val list: List[JsObject] = failures.map { case (s, throwable) =>
      Json.obj("rownum" -> JsString(s),
        "error" -> JsString(throwable.toString))
    }
    Json.toJson(list)
  }

}
