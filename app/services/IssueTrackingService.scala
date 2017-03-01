package services

import java.util.Date
import javax.inject.{Inject, Singleton}

import dao.IssueTrackingDao
import domain._

import scala.concurrent.{ExecutionContext, Future}

trait IssueTrackingService {
  def allIssues: Future[List[LoggedIssue]]
}

@Singleton
class IssueTrackingServiceImpl @Inject()(issueTrackingDao: IssueTrackingDao)(implicit ec: ExecutionContext) extends IssueTrackingService {

  def allIssues: Future[List[LoggedIssue]] = {
    Future {
      List(LoggedIssue(
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
      ))
    }
  }

}
