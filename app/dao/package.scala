package object dao {


  object Paging {

    case class PageReq(offset: Int = 1, // Default offset
                       size: Int = 10, // DefaultPageSize
                       sortFields: Option[List[String]] = None,
                       sortDirections: Option[List[String]] = None)

    case class PageResult[T](items: Seq[T], total: Int)

  }


}
