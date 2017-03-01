package services

import javax.inject.{ Inject, Singleton }

import domain._
import java.util.Date

import dao.IssueTrackingDao

trait IssueTrackingService {
  def allIssues: List[LoggedIssue]
}

@Singleton
class IssueTrackingServiceImpl @Inject() (issueTrackingDao: IssueTrackingDao) extends IssueTrackingService {

  def allIssues: List[LoggedIssue] = {
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
