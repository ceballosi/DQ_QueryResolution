package services

import java.io.File
import java.util.Date
import javax.inject.{Inject, Singleton}

import com.wix.accord._
import com.wix.accord.dsl._

import dao.IssueTrackingDao
import domain._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.slf4j.{Logger, LoggerFactory}
import purecsv.safe.converter.StringConverter

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source
import scala.util.Try

@Singleton
class IssueImportValidator @Inject()(issueTrackingDao: IssueTrackingDao)(implicit ec: ExecutionContext) {
  val log: Logger = LoggerFactory.getLogger(this.getClass())
  val EARLIEST_DATE = new DateTime("2014-01-01T00:00:00.00-00:00");

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
  //End PureCsv
  //Accord validator
  import ViolationBuilder._

  def oneOf[T <: AnyRef](options: T*): Validator[T] =
    new NullSafeValidator[T](
      test = options.contains,
      failure = _ -> s"is not one of (${options.mkString(",")})"
    )

  implicit val issueValidator = validator[Issue] { issue =>
    issue.issueId as "invalid IssueId" is notEmpty
    //    issue.status.toString as "invalid Status" is notEmpty           // shouldn't need to validate a default
//    issue.status.toString as "invalid Status not Draft" is equalTo("Draft") // ditto

    //    val now: Instant = DateTime.now().toInstant
    //    val lastYear: ReadableInstant = now.minus( Duration.standardDays( 365 ) )
    //    (new DateTime(issue.dateLogged)).toInstant is between( lastYear, now )
    //should work! but doesn't recognise "<" prob because can't do "import com.wix.accord.joda" doesn't exist in snapshot
    //    (new DateTime(issue.dateLogged)).toInstant should be < now
//                                                              10000000, 999999999
    issue.participantId as "invalid participantId" should be >= 10000000
    issue.dataSource as "invalid dataSource" is notEmpty
    issue.priority as "invalid priority" is within(1 to 9) // Inclusive
    issue.dataItem as "invalid dataItem" is notEmpty
    issue.shortDesc as "invalid shortDesc" is notEmpty
    issue.description as "invalid description" is notEmpty
    issue.gmc.toUpperCase as "invalid gmc" is oneOf("GEL", "RRK", "RGT", "RJ1", "RW3", "RTD", "RP4", "REP", "RTH", "RHM", "RH8", "RYJ", "RA7", "RHQ", "NI1")
    issue.area as "invalid therapeutic area" is oneOf("Cancer", "RD")
  }
  //End Accord validator

  def validateIssues(candidates: List[(Int, Issue)]): (List[(Int, Issue)], List[(Int, Throwable)]) = {
    val passes: ListBuffer[(Int, Issue)] = ListBuffer[(Int, Issue)]()
    val fails: ListBuffer[(Int, Throwable)] = ListBuffer[(Int, Throwable)]()

    candidates.foreach(candidate => {
      val (pass, error) = validateIssue(candidate, isNew = true)

      if (pass) passes += candidate
      else {
        log.error(s"validation fail for $candidate")
        fails += ((candidate._1, new scala.Throwable(error)))
      }
    })
    (passes.toList, fails.toList)
  }


  def validateIssue(candidate: (Int, Issue), isNew: Boolean): (Boolean, String) = {
    import com.wix.accord._
    val result: Result = validate(candidate._2)

    var resultTuple = result match {
      case com.wix.accord.Success => (true, "")
      case _ => {
        (false, extractErrors(result))
      }
    }
    //shouldn't need this if validation worked for date/status
    resultTuple = additionalValidation(candidate, resultTuple, isNew)
    log.debug(s"row ${candidate._1} validation result $resultTuple")
    resultTuple
  }


  def extractErrors(result: Result): String = {
    val errors = ListBuffer[String]()

    result.asInstanceOf[com.wix.accord.Failure].violations.foreach {
      v => errors += Descriptions.render(v.description)
    }

    val allErrors: String = errors.mkString(",")
    allErrors
  }

  //Yuk - wix.accord should have date valdn but doesn't work - may do next release?
  def additionalValidation(candidate: (Int, Issue), resultTuple: (Boolean, String), isNew: Boolean): (Boolean, String) = {

    val issue = candidate._2
    val date = issue.dateLogged
    var booleanResult = resultTuple._1
    var errs = ListBuffer(resultTuple._2)
    val updateStates = List(Draft,Open,Responded,Resolved,Archived)

    if (isNew && issue.status != Draft) {
      booleanResult = false
      errs += "invalid Status (not Draft)"
    }
    if (!isNew && (!updateStates.contains(issue.status))) {
      booleanResult = false
      errs += "invalid Status"
    }
    if (date == null || (date.before(EARLIEST_DATE.toDate) || date.after(new DateTime().toDate))) {
      booleanResult = false
      errs += "invalid dateLogged"
    }

    (booleanResult, errs.mkString(","))
  }

  def populateDefaults(candidates: List[(Int, Issue)]) = {
    val populated = candidates.map { case (row, candidate) =>
      (row, populate(candidate))
    }
    populated
  }

  //populate candidate issue with generated issueId and default values
  def populate(candidate: Issue): Issue = {

    var nextIssueId: String = ""

    val result: Try[String] = Await.ready(issueTrackingDao.nextIssueId(candidate.gmc), 30 seconds).value.get
    val resultEither = result match {
      case scala.util.Success(nextId) => nextIssueId = nextId
      case scala.util.Failure(e) => log.error(e.toString)
    }

    candidate.copy(issueId = nextIssueId, status = Draft)
  }


  def parseCsv(file: File): (List[(Int, Issue)], List[(Int, Throwable)]) = {
    import purecsv.safe._
    import purecsv.safe.tryutil._

    val fileContent = Source.fromFile(file).mkString
    val result = CSVReader[Issue].readCSVFromString(fileContent)

    result.getSuccessesAndFailures
  }
}

