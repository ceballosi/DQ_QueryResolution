package domain

import java.util.Date

case class QueryChain(
                        id : Long,
                        issueId: String,
                        status: String,
                        comment: String,
                        date: Date,
                        username: String,
                        partyId: Int
                      ) {

}

object QueryChain {
}
