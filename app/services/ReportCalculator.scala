package services

import org.joda.time.{DateTime, Days}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await

case class IssueStats(
                       gmc: String,
                       open: String,
                       responded: String,
                       outstanding: String, //Number of issues outstanding (by GMC) Open + Responded
                       resolved: String, //Number of queries resolved (by GMC)
                       avgDaysOutstanding: String, //Average length of time queries are outstanding for (by GMC) (days?)
                       dataItem: String, //Frequency of data items (by GMC) (Item,number)
                       itemCount: String //Frequency of data items (by GMC) (Item,number)
                     ) {

  def toCsvForUI(): String = {
    s"$gmc,$outstanding,$resolved,$avgDaysOutstanding,$dataItem,$itemCount"
  }
}

object ReportCalculator {

  val log: Logger = LoggerFactory.getLogger(getClass)

  def csvHeaderForUI(): String = "Gmc, Outstanding, Resolved, Average days outstanding, Data item, Frequency of data item"
  
  def statistics(issueTracking: IssueTrackingService): List[IssueStats] = {
    val statsList = new ListBuffer[IssueStats]()
    import scala.concurrent.duration._

    val gmcs = Await.result(issueTracking.listGmcs, 30 seconds)

    gmcs.foreach { gmc =>
      statsList ++= generateStatsForGmc(gmc, issueTracking)
    }

    statsList.toList
  }


  def generateStatsForGmc(gmc: String, issueTracking: IssueTrackingService): List[IssueStats] = {
    val statsGmcList = new ListBuffer[IssueStats]()

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

      statsGmcList += IssueStats(gmc, "0", "0", outstanding.toString, resolved.toString, f"$avgDaysToResolve%1.2f" , "", "")
    }else {

      log.debug(s"gmc $gmc , num resolved =${daysToResolve.length}")
      statsGmcList += IssueStats(gmc, "0", "0", outstanding.toString, resolved.toString, "0" , "", "")
    }


    val dataItemsGroupedByGmc: List[(String, Int)] = issueTracking.dataItemsGroupedBy(gmc)
    dataItemsGroupedByGmc.foreach(itemTuple => {
      statsGmcList += IssueStats(gmc, "0", "0", "", "", "", itemTuple._1, itemTuple._2.toString)
    })

    statsGmcList.toList
  }
}