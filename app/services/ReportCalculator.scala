package services

import org.joda.time.{DateTime, Days}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await

case class IssueStats(
                       gmc: String,
                       open: Int,
                       responded: Int,
                       outstanding: Int, //Number of issues outstanding (by GMC) Open + Responded
                       resolved: Int, //Number of queries resolved (by GMC)
                       avgDaysOutstanding: String, //Average length of time queries are outstanding for (by GMC) (days?)
                       freqDataItems: List[(String, Int)] //Frequency of data items (by GMC) (Item,number)
                     )

object ReportCalculator {

  val log: Logger = LoggerFactory.getLogger(getClass)


  def statistics(issueTracking: IssueTrackingService): List[IssueStats] = {
    val statsList = new ListBuffer[IssueStats]()
    import scala.concurrent.duration._

    val gmcs = Await.result(issueTracking.listGmcs, 30 seconds)

    gmcs.foreach { gmc =>
      val gmcStats: IssueStats = generateStatsForGmc(gmc, issueTracking)
      statsList += gmcStats
    }

    statsList.toList
  }


  def generateStatsForGmc(gmc: String, issueTracking: IssueTrackingService): IssueStats = {
    val (outstanding, resolved) = issueTracking.issueCounts(gmc)
    val resolutionDurations = issueTracking.issueResolutionDuration(gmc)

    val daysToResolve: List[Int] = resolutionDurations.map {
      case (issuedId, status, openDate, resolveDate) => {
        val days: Int = Days.daysBetween(new DateTime(openDate.get).toLocalDate(), new DateTime(resolveDate.get).toLocalDate()).getDays()
        days
      }
    }

    if (daysToResolve.length != 0) {
      val foldLeftSum = daysToResolve.foldLeft(0)(_ + _)
      val avgDaysToResolve: Double = foldLeftSum.toDouble / daysToResolve.length
      log.debug(s"sum/avg of days for gmc $gmc , daysToResolve total = $foldLeftSum num resolved=${daysToResolve.length} avgDaysToResolve=$avgDaysToResolve")

      IssueStats(gmc, 0, 0, outstanding, resolved, f"$avgDaysToResolve%1.2f" , List(("item", 3)))
    }else {

      log.debug(s"gmc $gmc , num resolved =${daysToResolve.length}")
      IssueStats(gmc, 0, 0, outstanding, resolved, "0" , List(("item", 3)))
    }

  }
}