package dao

import javax.inject.{ Inject, Singleton }

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext

trait IssueTrackingDao {

}

/**
  * @param dbConfigProvider Play db config provider. Play injects this
  */
@Singleton
class IssueTrackingDaoImpl @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
    extends IssueTrackingDao {
  //JdbcProfile for this provider
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  // To bring db in to the current scope
  // To bring slick DSL into scope

}