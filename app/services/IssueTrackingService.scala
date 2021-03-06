package services

import java.io.File
import java.time.{LocalDate, ZoneId}
import java.util.Date
import javax.inject.{Inject, Singleton}

import dao.Searching.{SearchRequest, SearchResult}
import dao.{IssueTrackingDao, SearchCriteria}
import domain._
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Success, Try}

trait IssueTrackingService {

  def findBySearchRequest(searchRequest: SearchRequest): Future[SearchResult[IssueView]]
  def findByIssueIds(issueIds: List[String]): Future[SearchResult[Issue]]
  def findIssueViewByIssueIds(issueIds: List[String]): Future[SearchResult[IssueView]]
  def findByCriteria(cr : SearchCriteria): Future[Seq[IssueView]]
  def allIssues: Future[Seq[Issue]]

  def listGmcs: Future[Seq[String]]
  def listOrigins: Future[Seq[String]]
  def listPriorities: Future[Seq[Int]]

  def importFile(file: File): List[(Int, Throwable)]
  def changeStatus(newStatus: Status, issueIds: List[String], user: String): Future[List[(String, Throwable)]]
  def save(issue: Issue): (Boolean, String)
  def update(issue: Issue): (Boolean, String)

  def allQc: Future[Seq[QueryChain]]
  def allQcJoin: Future[Seq[(String,String,String)]]
  def findAllJoin: Future[Seq[(String,String,String)]]

  def queryChain(selected: String): Future[Seq[QueryChain]]

  def nextIssueId(gmc: String): Future[String]
  def issueCounts(gmc: String): (Int, Int)
  def issueResolutionDuration(gmc: String): List[(String, Status, Option[Date], Option[Date])]
  def dataItemsGroupedBy(gmc: String): List[(String,Int)]

  //TODO : remove temporary method to populate data
  def tmpMethod: Future[Unit]
}

@Singleton
class IssueTrackingServiceImpl @Inject()(issueTrackingDao: IssueTrackingDao, validator: IssueImportValidator)(implicit ec: ExecutionContext) extends IssueTrackingService {

  val log: Logger = LoggerFactory.getLogger(this.getClass())



  def findBySearchRequest(searchRequest: SearchRequest) : Future[SearchResult[IssueView]] =  {
    issueTrackingDao.findBySearchRequest(searchRequest)
  }

  def findByIssueIds(issueIds: List[String]): Future[SearchResult[Issue]] =  issueTrackingDao.findByIssueIds(issueIds)

  def findIssueViewByIssueIds(issueIds: List[String]): Future[SearchResult[IssueView]] = issueTrackingDao.findIssueViewByIssueIds(issueIds)

  def findByCriteria(searchCriteria : SearchCriteria): Future[Seq[IssueView]] =
    issueTrackingDao.findByCriteria(searchCriteria)


  def allIssues: Future[Seq[Issue]] = issueTrackingDao.findAll




  def listGmcs: Future[Seq[String]] = issueTrackingDao.listGmcs

  def listOrigins: Future[Seq[String]] = issueTrackingDao.listOrigins

  def listPriorities: Future[Seq[Int]] = issueTrackingDao.listPriorities





  //import is done in 3 stages, catching and accumulating failures at each and only passing successful issues to the next
  //1. parse CSV to issue case class (checks fields are correct types)
  //2. validate import fields (values, date ranges,gmcs)
  //3. db insert
  def importFile(file: File): List[(Int, Throwable)] = {

    //1. parse CSV
    val (successes, failures) = validator.parseCsv(file: File)
    val totalIssues = successes.length + failures.length

    //pre-populate successes with defaults
    val validationCandidates = validator.populateDefaults(successes)

    //2. validate import fields
    val (passes,fails) = validator.validateIssues(validationCandidates)


    var mutableFailures: mutable.Buffer[(Int, Throwable)] = failures.toBuffer
    //3. db insert
    passes.map { case (idx, issue) =>
      val dbResult: (Boolean, String) = issueTrackingDao.insert(issue)
      if(!dbResult._1) {
        mutableFailures += ((idx, new Exception(dbResult._2)))

      } else {

        val escalation: Date = issue.dateLogged match {
          case date => (new DateTime(date)).plusDays(14).toDate //always returns a date (even for a null)
        }
        val issueDates = IssueDates(0,issue.issueId,None,None,None,Some(escalation),None,None,None)
        issueTrackingDao.insert(issueDates)

      }
    }
    //return all failures sorted/merged by row number
    mutableFailures = mutableFailures ++ fails
    val allFails: List[(Int, Throwable)] = mutableFailures.sortBy(_._1).toList

    log.info(s"Import File ${file.getName}, ${totalIssues - allFails.length} successes")
    log.error(s"Import File ${file.getName}, ${allFails.length} FAILURES")

    if (allFails.size > 0) {
      allFails.foreach(fail => log.error(fail.toString()))
    }
    allFails
  }




  def changeStatus(newStatus: Status, issueIds: List[String], user: String): Future[List[(String, Throwable)]] = {
    val failures = new ListBuffer[(String, Throwable)]

    findIssuesInOrder(issueIds).foreach { issue =>
      if(!issueTrackingDao.changeStatus(newStatus, issue)) {
        failures += ((issue.issueId, new Exception("changeStatus failed")))
      } else {
        changeStatusDatesUpdate(newStatus, issue, user)
      }
    }

    Future(failures.toList)
  }

  private def changeStatusDatesUpdate(newStatus: Status, issue: Issue, user: String) = {
    newStatus match {
      case Open => {
        issueTrackingDao.updateQueryDate(new Date(), issue, user)
      }
      case Responded => {
        issueTrackingDao.updateRespondedDate(new Date(), issue, user)
      }
      case Resolved => {
        issueTrackingDao.updateResolutionDate(new Date(), issue, user)
      }
      case _ =>
    }
  }



  private def findIssuesInOrder(issueIds: List[String]): List[Issue] = {
    val orderedIssues = ListBuffer[Issue]()

    //split into 2 methods find issues with exception handling & iterate/change with error handling
    var issues = Seq[Issue]()
    val findResult: Try[SearchResult[Issue]] = Await.ready(findByIssueIds(issueIds), 30 seconds).value.get

    findResult match {
      case scala.util.Success(searchResult) => issues = searchResult.items
      case scala.util.Failure(e) => {
        log.error("error finding issues " + e.toString)
        throw e
      }
    }

    // re-order to match the request order, allows errors to be viewed in order
    issueIds.foreach { issueId =>
      val foundIssue = issues.find(_.issueId == issueId)
      orderedIssues += foundIssue.getOrElse(null)
    }
    orderedIssues.toList
  }

  def save(issue: Issue): (Boolean, String) = {
    var result = issueTrackingDao.insert(issue)

    if (result._1) {
      val escalation: Date = issue.dateLogged match {
        case date => (new DateTime(date)).plusDays(14).toDate //always returns a date (even for a null)
      }
      val issueDates = IssueDates(0, issue.issueId, None, None, None, Some(escalation), None, None, None)
      result = issueTrackingDao.insert(issueDates)
    }
    result
  }


  def update(issue: Issue): (Boolean, String) = issueTrackingDao.update(issue)



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



  def nextIssueId(gmc: String) : Future[String] = issueTrackingDao.nextIssueId(gmc)

  def issueCounts(gmc: String) = issueTrackingDao.issueCounts(gmc)

  def issueResolutionDuration(gmc: String) =   issueTrackingDao.issueResolutionDuration(gmc)

  def dataItemsGroupedBy(gmc: String): List[(String,Int)] = issueTrackingDao.dataItemsGroupedBy(gmc)

  //TODO : To be removed (temporary method to provide a handler to the controller for creating a table using sample model)
  def tmpMethod = Future {
    val tuple: (List[Issue], List[IssueDates]) = tmpPopulateIssues
    issueTrackingDao.tableSetup(tuple._1.toSeq, tuple._2.toSeq)
  }

  //TODO : To be removed
  private def tmpPopulateIssues: (List[Issue], List[IssueDates]) = {
    import java.time.temporal.ChronoUnit.DAYS

    import scala.util.Random

    val r = new Random
    val statuses = Status.validStatuses
    val gmcList = List("GEL","RRK", "RGT", "RJ1", "RW3", "RTD", "RP4", "REP", "RTH", "RHM", "RH8", "RYJ", "RA7", "RHQ", "NI1")
    val priorityList = List(2,2,2,2,2,2,2,2,2,1) //fasttrack is 1 in 10
    val dataSourceList = List("ServiceDesk", "DataQuality", "RedTeam", "Informatics", "BioInformatics")
    val dataItemList = List("Gender", "FamilyId", "DOB", "Excision Margin", "Biological Relationship to Proband")
    val shortDescList = List("missing", "bad data", "incorrect entry", "invalid", "re-submission", "out of range")
    val areaList = List("Cancer", "RD")
    val whoList = List("fred","wilma","barney","betty")

    def randomDateBetween(from: LocalDate, to: LocalDate) = {
      val d = from.plusDays(r.nextInt(DAYS.between(from, to).toInt))
      Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }
    val issue = Issue(
      1,
      "RIP-000022",
      statuses(r.nextInt(statuses.length)),
      randomDateBetween(LocalDate.of(2017, 1, 1), LocalDate.now),
      110000000,
      "DataQuality",
      1,
      "Group size",
      "Some desc",
      "RIP",
      Some(("lsid"+ r.nextInt(1000) + 110000000).toString),
      "RD",
      "Issue with family group size: Please see 'Group Issues' tab for more details and potential resolutions",
      None,
      None
    )

    val issueDates = IssueDates(
      1,
      "RIP-000022",
      None,
      None,
      None,
      None,
      None,
      None,
      None
    )

    val issues = ListBuffer(issue)
    val issueDatesLs = ListBuffer(issueDates)

//    issue.copy(status = statuses(r.nextInt(statuses.length)))
    (1 to 10000).foreach { x =>

      val randGmc = gmcList(r.nextInt(gmcList.length))
      val statusChosen= statuses(r.nextInt(statuses.length))
      val dateCreated = randomDateBetween(LocalDate.of(2016, 12, 1), LocalDate.now().minusDays(3))
      val who = whoList(r.nextInt(whoList.length))
      var openWho: Option[String] = None
      var respondedWho: Option[String] = None
      var resolutionWho: Option[String] = None
      val lsidList = List(null,"lsid" + "%05d".format(x))

      var familyOption: Option[String] = Some("%05d".format(x))
      if (x % 5 != 0) {
        familyOption = None
      }

      var openDate: Option[Date] = None
      var respondedDate: Option[Date] = None
      var resolutionDate: Option[Date] = None

      if (Open == statusChosen) {
        if(x%3==0) {
          openDate = Some(org.joda.time.LocalDate.fromDateFields(dateCreated).plusDays(2).toDate)
          openWho = Some(who)
        }
      }

      if (Responded == statusChosen) {
        if(x%6==0) {
          respondedDate = Some(org.joda.time.LocalDate.fromDateFields(dateCreated).plusDays(8).toDate)
          respondedWho = Some(who)
        }
      }

      if (statusChosen == Resolved) {
        resolutionDate = Some(org.joda.time.LocalDate.fromDateFields(dateCreated).plusDays(25).toDate)
        resolutionWho = Some(who)
      }

      val escalation = org.joda.time.LocalDate.fromDateFields(dateCreated).plusDays(14).toDate

      var nextIssueId: String = ""

      val result: Try[String] = Await.ready(issueTrackingDao.nextIssueId(randGmc), 30 seconds).value.get
      result match {
        case scala.util.Success(nextId) => nextIssueId = nextId
        case scala.util.Failure(e) => log.error(e.toString)
      }

      val copy = issue.copy(
        gmc = randGmc,
        issueId = nextIssueId,
        status = statusChosen,
        dateLogged = dateCreated,
        participantId = r.nextInt(1000) + 110000000,
        dataSource = dataSourceList(r.nextInt(dataSourceList.length)),
        priority = priorityList(r.nextInt(priorityList.length)),
        dataItem = dataItemList(r.nextInt(dataItemList.length)),
        shortDesc = shortDescList(r.nextInt(shortDescList.length)),
        lsid = Option(lsidList(r.nextInt(lsidList.length))),
        area = areaList(r.nextInt(areaList.length)),
        familyId = familyOption
      )

      val copyd = issueDates.copy(
        issueId = nextIssueId,
        openDate = openDate,
        respondedDate = respondedDate,
        resolutionDate = resolutionDate,
        escalation = Some(escalation),
        openWho = openWho,
        respondedWho = respondedWho,
        resolutionWho = resolutionWho
      )

      issues += copy
      issueDatesLs += copyd
    }

    (issues.toList, issueDatesLs.toList)
  }
}
