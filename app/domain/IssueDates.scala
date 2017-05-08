package domain

import java.util.Date


case class IssueDates(
                       id: Long,
                       issueId: String,
                       openDate: Option[Date],
                       respondedDate: Option[Date],
                       resolutionDate: Option[Date],
                       escalation: Option[Date],
                       openWho: Option[String],
                       respondedWho: Option[String],
                       resolutionWho: Option[String]
                     ) {}



