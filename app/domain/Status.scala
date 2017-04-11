package domain

trait Status

case object Draft extends Status

case object Open extends Status

case object Responded extends Status

case object Resolved extends Status

case object Archived extends Status


object Status {
  def validStatuses = Vector(Draft, Open, Responded, Resolved)
  def allStatuses = Vector(Draft, Open, Responded, Resolved,Archived)

  def statusFrom(strStatus: String): Status = {
    domain.Status.allStatuses.find(_.toString == strStatus).get
  }

  def statusFrom(optionStrStatus: Option[String]): Option[Status] = {
    optionStrStatus match {
      case Some(value) => Some(statusFrom(value))
      case _ => None
    }
  }

}
