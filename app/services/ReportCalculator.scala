package services

case class IssueStats(
                       gmc: String,
                       open: Int,
                       responded: Int,
                       outstanding: Int, //Number of issues outstanding (by GMC) Open + Responded
                       resolved: Int, //Number of queries resolved (by GMC)
                       avgIssueTime: Long, //Average length of time queries are outstanding for (by GMC) (secs?)
                       freqDataItems: List[(String, Int)] //Frequency of data items (by GMC) (Item,number)
                     )

object ReportCalculator {

  def statistics(issueTracking: IssueTrackingService): Seq[(String)] = {
    null
  }

}