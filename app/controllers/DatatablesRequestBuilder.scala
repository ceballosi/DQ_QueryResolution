package controllers

import java.util.Date

import dao.Searching.SearchRequest
import domain.{Open, SearchCriteria}
import org.joda.time.LocalDate
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{AnyContent, Request}

/**
  * This builder is specifically to service the requirements of the UI Datatables.js component
  */
object DatatablesRequestBuilder {

  val log: Logger = LoggerFactory.getLogger(this.getClass())

  //needs to be kept in sync with ui
  private val uiColumnNames: List[String] = List("status", "DT_RowId", "loggedBy", "dateLogged", "issueOrigin", "GMC", "description", "familyId")

  def build(request: Request[AnyContent]): SearchRequest = {
    //for security coerce these to int and provide safe fallbacks
    val draw = request.getQueryString("draw").getOrElse("1").toInt
    val offset = request.getQueryString("start").getOrElse("0").toInt
    val pageSize = request.getQueryString("length").getOrElse("10").toInt

    val queryString = request.queryString
    val filter = param(queryString, "filter")
    var isNew = false

    filter match {
      case Some(s) if s.equalsIgnoreCase("new") => {
        isNew = true
        log.info("new issues selected")
      }
      case Some(s) => log.info("filter value?= " + s)
      case None => log.info("no filter param,check others")
    }

    val gmc = param(queryString, "gmc")
    var dateLogged: Option[Date] = None
    var issueStatus: Option[domain.Status] = None

    if (isNew) {
      issueStatus = Some(Open)
      val days = param(queryString, "days").getOrElse("0").toInt
      dateLogged = Some(LocalDate.now().minusDays(days).toDate)
    }

    val sortCol = param(queryString, "order[0][column]")
    val sortDir = param(queryString, "order[0][dir]")

    //default to sort by dateLogged/desc
    val sortColFromUI = uiColumnNames(sortCol.getOrElse("3").toInt)
    val sortOrderFromUI = sortDir.getOrElse("desc")

    val sortFields: Option[List[String]] = Some(List(sortColFromUI))
    val sortDirections: Option[List[String]] = Some(List(sortOrderFromUI))

    val searchCriteria = SearchCriteria(gmc, issueStatus = issueStatus, dateLogged = dateLogged)

    val searchRequest: SearchRequest = SearchRequest(offset, pageSize, searchCriteria, draw, sortFields, sortDirections)
    log.info(s"searchRequest: $searchRequest")
    searchRequest
  }

  def param(request: Map[String, Seq[String]], field: String): Option[String] =
    request.get(field).flatMap(_.headOption)

}
