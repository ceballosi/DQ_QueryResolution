package controllers

import java.util.Date

import controllers.UiUtils._
import dao.SearchCriteria
import dao.Searching.SearchRequest
import domain.{Draft, Status}
import org.joda.time.LocalDate
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

/**
  * This builder services the requirements of the UI Datatables.js component
  */
object DatatablesRequestBuilder {

  val log: Logger = LoggerFactory.getLogger(this.getClass())

  //needs to be kept in sync with ui
  private val uiColumnNames: List[String] = List("select/checkbox", "DT_RowId", "status", "dateLogged", "participantId", "dataSource", "priority", "dataItem", "shortDesc", "gmc", "lsid", "area", "description", "familyId", "openDate", "weeksOpen", "resolutionDate", "escalation", "notes")

  //support both get & post by taking the param map
  def build(request: Map[String, Seq[String]]): SearchRequest = {
    //for security coerce these to int and provide safe fallbacks
    val draw = param(request, "draw").getOrElse("1").toInt
    val offset = param(request, "start").getOrElse("0").toInt
    val pageSize = param(request, "length").getOrElse("10").toInt

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
    var participantId: Option[Int] = None
    var issueStatus = Status.statusFrom(param(request, "status"))
    var dataSource = param(request, "origin")
    var area = param(request, "area")


    var dateLoggedStart = parseDateToOptionDate(param(request, "startDate"))
    var dateLoggedEnd = parseDateToOptionDate(param(request, "endDate"))

    println("startDate=" + param(request, "startDate") + " dateLoggedStart=" + dateLoggedStart)
    println("endDate=" + param(request, "endDate") + " dateLoggedEnd=" + dateLoggedEnd)

    var maybePriority: Option[Int] = param(request, "priority") match {
      case Some(p) if p.length > 0 => Some(p.toInt)
      case None => None
    }


    if (isNew) {
      participantId = None
      gmc = None
      dataSource = None
      maybePriority = None
      area = None
      issueStatus = Some(Draft)
    }

    //search is higher precedence than filters
    if (isSearch) {
      participantId = Some(search.get.toInt)
      gmc = None
      maybePriority = None
      area = None
      dateLoggedStart = None
      dateLoggedEnd = None
      issueStatus = None
      dataSource = None
    }

    val sortCol = param(request, "order[0][column]")
    val sortDir = param(request, "order[0][dir]")

    //default to sort by dateLogged/desc
    val sortColFromUI = uiColumnNames(sortCol.getOrElse("3").toInt)
    val sortOrderFromUI = sortDir.getOrElse("desc")
    log.info(s"sortCol=$sortCol sortDir=$sortDir sortColFromUI=$sortColFromUI sortOrderFromUI=$sortOrderFromUI")

    val sortCriteria: Option[(String, String)] = Some((sortColFromUI, sortOrderFromUI))
    val searchCriteria = SearchCriteria(gmc, issueStatus = issueStatus, dataSource = dataSource, dateLoggedStart = dateLoggedStart, dateLoggedEnd = dateLoggedEnd, priority = maybePriority, area = area, participantId = participantId)

    val searchRequest: SearchRequest = SearchRequest(offset, pageSize, searchCriteria, draw, sortCriteria)
    log.info(s"searchRequest: $searchRequest")
    searchRequest
  }


  //may not be needed now using Dates
  def parseDaysToOptionDate(possibleDays: Option[String]): Option[Date] = {
    //adds custom unapply to Int for String pattern match
    object Int {
      def unapply(s: String): Option[Int] = util.Try(s.toInt).toOption
    }

    possibleDays match {
      case Some(days) => days match {
        case Int(i) => Some(LocalDate.now().minusDays(i).toDate)
        case _ => None //ignore non int strings
      }
      case None => None
    }

  }


  def parseDateToOptionDate(tryStringAsDate: Option[String]): Option[Date] = {
    val dateTimeFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy")

    val tryDate: Try[Date] = Try(LocalDate.parse(tryStringAsDate.getOrElse(""), dateTimeFormat).toDate)

    val maybeDate: Option[Date] = tryDate match {
      case Success(date) => Some(date)
      case Failure(e) => None
    }

    maybeDate

  }

}
