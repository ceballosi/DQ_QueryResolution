package services

import java.util.Date
import javax.inject.{Inject, Singleton}

import dao.IssueTrackingDao
import domain._
import org.joda.time.LocalDateTime

import scala.concurrent.{ExecutionContext, Future}

trait IssueTrackingService {
  def allIssues: Future[Seq[LoggedIssue]]

  //TODO : To be removed (temporary method to create a table and populate data)
  def tmpMethod: Future[Unit]
}

@Singleton
class IssueTrackingServiceImpl @Inject()(issueTrackingDao: IssueTrackingDao)(implicit ec: ExecutionContext) extends IssueTrackingService {

  def allIssues = issueTrackingDao.findAll


  //TODO : To be removed (temporary method to provide a handler to the controller for creating a table using sample model)
  def tmpMethod = Future {
    issueTrackingDao.tableSetup(tmpPopulateIssues.toSeq)
  }

  //TODO : To be removed
  private def tmpPopulateIssues = {
    val r = scala.util.Random
    var issues = List(LoggedIssue(
      1,
      "RYJ-Orp-031",
      Open,

      "RJ",
      new Date(),
      "DataQuality",
      "RYJ",
      None,
      "110120123",
      None,
      Some("Group size"),
      "Issue with family group size: Please see 'Group Issues' tab for more details and potential resolutions",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("Comments on issue")
    ), LoggedIssue(
      2,
      "RYJ-Orp-032",
      Close,
      "RJ",
      new LocalDateTime().minusDays(10).toDate,
      "DataQuality",
      "RYJ",
      None,
      "110110124",
      None,
      Some("Group size"),
      "Issue with family group size: Please see 'Group Issues' tab for more details and potential resolutions",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("Comments on issue")
    ), LoggedIssue(
      3,
      "RYJ-Orp-033",
      Open,

      "RJ",
      new LocalDateTime().minusDays(4).toDate,
      "DataQuality",
      "RYJ",
      None,
      "110100125",
      None,
      Some("Group size"),
      "Issue with family group size: Please see 'Group Issues' tab for more details and potential resolutions",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("Comments on issue")
    ), LoggedIssue(
      4,
      "RYJ-Orp-034",
      Open,

      "RJ",
      new LocalDateTime().minusDays(20).toDate,
      "DataQuality",
      "RYJ",
      None,
      "110020126",
      None,
      Some("Group size"),
      "Issue with family group size: Please see 'Group Issues' tab for more details and potential resolutions",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("Comments on issue")
    ), LoggedIssue(
      5,
      "RYJ-Orp-035",
      Open,
      "RJ",
      new LocalDateTime().minusDays(110).toDate,
      "DataQuality",
      "RYJ",
      None,
      "110920127",
      None,
      Some("Group size"),
      "Issue with family group size: Please see 'Group Issues' tab for more details and potential resolutions",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("Comments on issue")
    ))

    for (x <- 1 to 5) {
      issues = issues ::: issues
    }
    issues
  }
}
