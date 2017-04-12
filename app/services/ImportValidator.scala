package services

import java.io.File

import domain.Issue

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

class ImportValidator {
  def validate(successes: List[(Int, Issue)]) = ???

  //  def validate(successes: List[(Int, Issue)]) = ???


  def rowsFromFullFile (file: File): Array[String] = {
    val fileContent = Source.fromFile(file).mkString
    val rows: Array[String] = fileContent.split("\r\n")
    rows
  }

  def validateRows(rows: Array[String]) = {

        rows.map(row => {
          val fields: Array[String] = row.split(",")
//          validateRow(row)
        } )

  }


  def validateRow(row: Array[String]): (Boolean, Option[ArrayBuffer[String]]) = {
    var failedColumnInfo: Option[ArrayBuffer[String]] = None // Hold list of failed column info (column name, index position, cell data)

    // CONSIDER CREATING Persistable Error Here! and passing that back with packed data appropriate
    val buffer = ArrayBuffer(row(0))
    buffer.append(row(1))
    buffer.append(row(2))
    buffer.append("validation")
    buffer.append("somethingdidn'tvalidate")
    buffer.append("some-long-errorcontext")
    buffer.append(row(6))

    //    val nextInt = Random.nextInt(2)
    if (!row(6).toBoolean) {
      (false, Some(buffer))
    } else {
      (true, Some(buffer))
    }
  }

}

