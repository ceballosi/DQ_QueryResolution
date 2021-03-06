package dao

import slick.lifted.TableQuery

import scala.concurrent.Future

/**
  *
  * @tparam T Model which maps to the table
  * @tparam U Identifier Type (Primary key Type)
  */
trait BaseDao[T, U] {
  def findAll: Future[Seq[T]]

  def findById(id: U): Future[Option[T]]

  def update(o: T): (Boolean, String)

  def insert(o: T): (Boolean, String)

  // To get Table functional relational mapping (FRM) for the DAO
  def toTable: TableQuery[_]

}
