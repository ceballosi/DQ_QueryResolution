package auth

import javax.inject.Inject

import org.pac4j.play.filters.SecurityFilter
import play.api.http.HttpFilters

/**
  * Created by dexterawoyemi on 26/04/17.
  */
class Filters @Inject()(securityFilter: SecurityFilter) extends HttpFilters {

  def filters = Seq(securityFilter)

}
