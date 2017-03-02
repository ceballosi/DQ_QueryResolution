package services

import javax.inject.{Inject, Singleton}

import domain._
import java.util.Date

import dao.IssueTrackingDao
import org.joda.time.LocalDateTime

import scala.concurrent.{ExecutionContext, Future}

trait IssueTrackingService {
  def allIssues: Future[List[LoggedIssue]]
}

@Singleton
class IssueTrackingServiceImpl @Inject()(issueTrackingDao: IssueTrackingDao)(implicit ec: ExecutionContext) extends IssueTrackingService {

  def allIssues: Future[List[LoggedIssue]] = {
    Future {
      var issues = List(LoggedIssue(
        Open,
        "RYJ-Orp-031",
        "RJ",
        new Date(),
        DataQuality,
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
        Open,
        "RYJ-Orp-032",
        "RJ",
        new LocalDateTime().minusDays(10).toDate,
        DataQuality,
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
        Open,
        "RYJ-Orp-033",
        "RJ",
        new LocalDateTime().minusDays(4).toDate,
        DataQuality,
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
        Open,
        "RYJ-Orp-034",
        "RJ",
        new LocalDateTime().minusDays(20).toDate,
        DataQuality,
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
        Open,
        "RYJ-Orp-035",
        "RJ",
        new LocalDateTime().minusDays(110).toDate,
        DataQuality,
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
      for(x <- 1 to 5){
        issues = issues ::: issues
      }
      issues
    }
  }

}
