package domain

import java.util.Date

case class Issue(
                 id: Long,
                 issueId: String,
                 status: Status,
                 dateLogged: Date,
                 participantId: Int,
                 dataSource: String,
                 priority: Int,
                 dataItem: String,
                 shortDesc: String,
                 gmc: String,
                 lsid: Option[String],
                 area: String,
                 description: String,
                 familyId: Option[String],
                 notes: Option[String]
                ) {}

