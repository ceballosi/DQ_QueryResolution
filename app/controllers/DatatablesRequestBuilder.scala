package controllers

import java.util.Date

import controllers.UiUtils._
import dao.SearchCriteria
import dao.Searching.SearchRequest
import domain.{Draft, Status}
import org.joda.time.LocalDate
import org.slf4j.{Logger, LoggerFactory}

/**
  * This builder is specifically to service the requirements of the UI Datatables.js component
  */
object DatatablesRequestBuilder {

  val log: Logger = LoggerFactory.getLogger(this.getClass())

  //needs to be kept in sync with ui
  private val uiColumnNames: List[String] = List("select/checkbox", "DT_RowId", "status", "dateLogged", "participantId", "dataSource", "priority", "dataItem", "shortDesc", "gmc", "lsid", "area", "description", "familyId", "queryDate", "weeksOpen", "resolutionDate", "escalation", "participantId")

  //support both get & post by taking the param map
  def build(request: Map[String, Seq[String]]): SearchRequest = {
    //for security coerce these to int and provide safe fallbacks
    val draw = param(request,"draw").getOrElse("1").toInt
    val offset = param(request,"start").getOrElse("0").toInt
    val pageSize = param(request,"length").getOrElse("10").toInt

    println(s"pageSize=$pageSize")
    val filter = param(request, "filter")
    val search = param(request, "search[value]")
    var isNew = false
    var isSearch = false

    filter match {
      case Some(s) if s.equalsIgnoreCase("new") => {
        isNew = true
        log.info("new issues selected")
      }
      case Some(s) => log.info("filter value?= " + s)
      case None => log.info("no filter param,check others")
    }

    search match {
      case Some(s) if s.length > 0 => {
        isSearch = true
        log.info(s"search for $s")
      }
      case Some(s) => log.info("search value?= " + s)
      case None => log.info("no search param,check others")
    }

    var gmc = param(request, "gmc")
    var dateLogged: Option[Date] = None
    var participantId: Option[Int] = None
    var issueStatus = Status.statusFrom(param(request, "status"))
    var dataSource = param(request, "origin")

    if (isNew) {
      participantId = None
      gmc = None
      dataSource = None
      issueStatus = Some(Draft)
      val days = param(request, "days").getOrElse("0").toInt
      dateLogged = Some(LocalDate.now().minusDays(days).toDate)
    }

    //search is higher precedence than filters
    if (isSearch) {
      participantId = Some(search.get.toInt)
      gmc = None
      dateLogged = None
      issueStatus = None
      dataSource = None
    }

    val sortCol = param(request, "order[0][column]")
    val sortDir = param(request, "order[0][dir]")

    //default to sort by dateLogged/desc
    val sortColFromUI = uiColumnNames(sortCol.getOrElse("3").toInt)
    val sortOrderFromUI = sortDir.getOrElse("desc")
    log.info(s"sortCol=$sortCol sortDir=$sortDir sortColFromUI=$sortColFromUI sortOrderFromUI=$sortOrderFromUI")

    val sortCriteria : Option[(String, String)] = Some((sortColFromUI,sortOrderFromUI))
    val searchCriteria = SearchCriteria(gmc, issueStatus = issueStatus, dataSource = dataSource, dateLogged = dateLogged, participantId = participantId)

    val searchRequest: SearchRequest = SearchRequest(offset, pageSize, searchCriteria, draw, sortCriteria)
    log.info(s"searchRequest: $searchRequest")
    searchRequest
  }

}
