package services

import java.io.File
import java.time.{LocalDate, ZoneId}
import java.util.Date
import javax.inject.{Inject, Singleton}

import dao.{SearchCriteria, IssueTrackingDao}
import dao.Searching.{SearchRequest, SearchResult}
import domain._
import org.joda.time.format.ISODateTimeFormat
import org.slf4j.{Logger, LoggerFactory}
import purecsv.safe.converter.StringConverter

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.Try

trait IssueTrackingService {
  def allIssues: Future[Seq[Issue]]
  def listGmcs: Future[Seq[String]]
  def listOrigins: Future[Seq[String]]
  def allIssuesNow: List[Issue]
  def findByCriteria(cr : SearchCriteria): Future[Seq[Issue]]
  def findByIssueIds(issueIds: List[String]): Future[SearchResult[Issue]]
  def findBySearchRequest(searchRequest: SearchRequest): Future[SearchResult[Issue]]
  def importFile(file: File): List[(Int, Throwable)]
  def changeStatus(newStatus: Status, issueIds: List[String]): Future[List[(String, Throwable)]]
  //TODO : To be removed (temporary method to create a table and populate data)
  def tmpMethod: Future[Unit]

  def allQc: Future[Seq[QueryChain]]
  def allQcJoin: Future[Seq[(String,String,String)]]
  def findAllJoin: Future[Seq[(String,String,String)]]

  def queryChain(selected: String): Future[Seq[QueryChain]]
}

@Singleton
class IssueTrackingServiceImpl @Inject()(issueTrackingDao: IssueTrackingDao)(implicit ec: ExecutionContext) extends IssueTrackingService {

  val log: Logger = LoggerFactory.getLogger(this.getClass())


  //PureCSV custom Date converter
  implicit val dateStringConverter = new StringConverter[Date] {
    override def tryFrom(str: String): Try[Date] = {
      Try(ISODateTimeFormat.dateTimeParser().parseDateTime(str).toDate)
    }

    override def to(date: Date): String = {
      ISODateTimeFormat.dateTime().print(date.getTime)
    }
  }

  //PureCSV custom Status converter
  implicit val statusStringConverter = new StringConverter[Status] {
    override def tryFrom(str: String): Try[Status] = {
      Try(Status.validStatuses.find(_.toString == str).get)
    }

    override def to(status: Status): String = {
      status.toString
    }
  }


  def findBySearchRequest(searchRequest: SearchRequest) : Future[SearchResult[Issue]] =  issueTrackingDao.findBySearchRequest(searchRequest)

  def findByIssueIds(issueIds: List[String]): Future[SearchResult[Issue]] =  issueTrackingDao.findByIssueIds(issueIds)


  def allIssues: Future[Seq[Issue]] = issueTrackingDao.findAll
   /*{
    Future {
      var issues: List[LoggedIssue] = findAllIssues
      for(x <- 1 to 5){
        issues = issues ::: issues
  }
      issues
    }
  } */

  def listGmcs: Future[Seq[String]] = issueTrackingDao.listGmcs

  def listOrigins: Future[Seq[String]] = issueTrackingDao.listOrigins

  def findByCriteria(searchCriteria : SearchCriteria): Future[Seq[Issue]] =
    issueTrackingDao.findByCriteria(searchCriteria)

  def allIssuesNow: List[Issue] = {
    var issues: List[Issue] = findAllIssues
    for(x <- 1 to 5){
      issues = issues ::: issues
    }
    issues
  }

  def allQc: Future[Seq[QueryChain]] = ???
//  def allQc: Future[Seq[QueryChain]] = {
//    var qc: Future[Seq[QueryChain]] = qcDao.findAll
//    qc
//  }

 def allQcJoin: Future[Seq[(String,String,String)]] = ???
// def allQcJoin: Future[Seq[(String,String,String)]] = {
//   val qc: Future[Seq[(String, String, String)]] = qcDao.findAllJoin
//   qc
//  }

  def findAllJoin: Future[Seq[(String,String,String)]]= {
    val alljoin: Future[Seq[(String, String, String)]] = issueTrackingDao.findAllJoin
    alljoin
  }

  def queryChain(selected: String): Future[Seq[QueryChain]] = issueTrackingDao.findQueryChain(selected)

  def findAllIssues: List[Issue] = {
    tmpPopulateIssues
  }


  def importFile(file: File): List[(Int, Throwable)] = {
    val fileContent = Source.fromFile(file).mkString

    import purecsv.safe._
    import purecsv.safe.tryutil._

    val result = CSVReader[Issue].readCSVFromString(fileContent)

    //TODO - add stronger validation on issue import e.g format of issue-id, and required fields
    // e.g. copy successes via another validating constructor?
    val (successes, failures: List[(Int, Throwable)]) = result.getSuccessesAndFailures
    if (failures.size > 0) {
      log.error(s"Import File ${file.getName}, ${failures.length} FAILURES" )
      failures.foreach(println)
    }

    if (successes.size > 0) {
      log.info(s"Import File ${file.getName}, ${successes.length} successes" )
    }

    var mutableFailures: mutable.Buffer[(Int, Throwable)] = failures.toBuffer
    //get 2nd set of failures from import  into db
    successes.map { case (idx, issue) =>
      val result = Try {
        issueTrackingDao.insert(issue)
      }
      result.getOrElse {
        val e: Throwable = result.failed.get
        mutableFailures += ((idx, e))
      }
    }
    //return both sets of failures sorted/merged by row number
    mutableFailures ++ failures
    mutableFailures.sortBy(_._1).toList
  }


  def changeStatus(newStatus: Status, issueIds: List[String]): Future[List[(String, Throwable)]] = {
    val failures = new ListBuffer[(String, Throwable)]

    val findResult: Future[SearchResult[Issue]] = findByIssueIds(issueIds)


    findResult.map { searchResult =>
      searchResult.items.map { issue =>

        val result = Try {
          //TODO - implement allowable state change logic before performing actual update
          issueTrackingDao.changeStatus(newStatus, issue)
        }
        result.getOrElse {
          val e: Throwable = result.failed.get
          failures += ((issue.issueId, e))
          println("svc=" + issue)
        }

      }
    }

    Future(failures.toList)
  }


  //TODO : To be removed (temporary method to provide a handler to the controller for creating a table using sample model)
  def tmpMethod = Future {
    issueTrackingDao.tableSetup(tmpPopulateIssues.toSeq)
  }

  //TODO : To be removed
  private def tmpPopulateIssues = {
    import java.time.temporal.ChronoUnit.DAYS

    import scala.util.Random

    val r = new Random
    val statuses = Status.validStatuses
    val gmcList = List("RRK", "RGT", "RJ1", "RW3", "RTD", "RP4", "REP", "RTH", "RHM", "RH8", "RYJ", "RA7", "RHQ", "NI1")
    val priorityList = List(0,0,0,0,0,0,0,0,0,1)
    val dataSourceList = List("ServiceDesk", "DataQuality", "RedTeam", "Informatics", "BioInformatics")
    val dataItemList = List("Gender", "FamilyId", "DOB", "Excision Margin", "Biological Relationship to Proband")
    val shortDescList = List("missing", "bad data", "incorrect entry", "invalid", "re-submission", "out of range")
    val areaList = List("Cancer", "RD")

    def randomDateBetween(from: LocalDate, to: LocalDate) = {
      val d = from.plusDays(r.nextInt(DAYS.between(from, to).toInt))
      Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }
    val data = Issue(
      1,
      "RIP-000022",
      statuses(r.nextInt(statuses.length)),
      randomDateBetween(LocalDate.of(2017, 1, 1), LocalDate.now),
      "DataQuality",
      1,
      "Group size",
      "Some desc",
      "Issue with family group size: Please see 'Group Issues' tab for more details and potential resolutions",
      "RIP",
      Some(("lsid"+ r.nextInt(1000) + 110000000).toString),
      "RD",
      None,
      None,
      None,
      None,
      None,
      110000000
    )

    val issueLs = ListBuffer(data)

    data.copy(status = statuses(r.nextInt(statuses.length)))
    (1 to 10000).foreach { x =>

      val randGmc = gmcList(r.nextInt(gmcList.length))
      val statusChosen= statuses(r.nextInt(statuses.length))
      val dateCreated = randomDateBetween(LocalDate.of(2016, 12, 1), LocalDate.now().minusDays(3))

      val lsidList = List(null,"lsid" + "%05d".format(x))
      var familyOption: Option[Int] = Some(x)
      if (x % 5 != 0) {
        familyOption = None
      }

      var resolutionDate: Option[Date] = None
      if (statusChosen == Resolved) {
        resolutionDate = Some(org.joda.time.LocalDate.fromDateFields(dateCreated).plusDays(25).toDate)
      }

      var queryDate: Option[Date] = None
      if (List(Open,Responded).contains(statusChosen)) {
        if(x%3==0) queryDate = Some(org.joda.time.LocalDate.fromDateFields(dateCreated).plusDays(2).toDate)
      }

      val escalation = org.joda.time.LocalDate.fromDateFields(dateCreated).plusDays(14).toDate

      val c = data.copy(
        gmc = randGmc,
        issueId = randGmc + "-" + "%07d".format(x),
        status = statusChosen,
        dateLogged = dateCreated,
        dataSource = dataSourceList(r.nextInt(dataSourceList.length)),
        priority = priorityList(r.nextInt(priorityList.length)),
        dataItem = dataItemList(r.nextInt(dataItemList.length)),
        shortDesc = shortDescList(r.nextInt(shortDescList.length)),
        lsid = Option(lsidList(r.nextInt(lsidList.length))),
        area = areaList(r.nextInt(areaList.length)),
        familyId = familyOption,
        queryDate = queryDate,
        resolutionDate = resolutionDate,
        escalation = Some(escalation),
        participantId = r.nextInt(1000) + 110000000
      )
      issueLs += c
    }
    issueLs.toList
  }
}
