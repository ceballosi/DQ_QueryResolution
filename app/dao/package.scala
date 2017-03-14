import domain. SearchCriteria

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

}
