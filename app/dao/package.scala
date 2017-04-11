import java.util.Date

import domain.Status

package object dao {


  object Searching {

    case class SearchRequest(offset: Int = 1, // Default offset
                             size: Int = 10, // DefaultPageSize
                             searchCriteria: SearchCriteria,
                             uiRequestToken: Int = 0,
                             sortCriteria : Option[(String, String)] = None)
    //this was the mechanism to support multiple sort cols, leaving here for the moment
//                             sortFields: Option[List[String]] = None,
//                             sortDirections: Option[List[String]] = None)

    case class SearchResult[T](items: Seq[T], total: Int)

  }


  case class SearchCriteria(gmc: Option[String] = None,
                            issueId: Option[String] = None,
                            issueStatus: Option[Status] = None,
                            priority: Option[Int] = None,
                            dataSource: Option[String] = None,
                            dateLogged: Option[Date] = None,
                            participantId: Option[Int] = None,
                            searchValue: Option[String] = None
                           )


}
