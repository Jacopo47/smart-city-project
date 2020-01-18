package model.dao

import java.sql.{SQLType, Timestamp}

import model.logger.Log
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object FactTableComponent {
  def defineSchema(): Unit = {
    val facts = TableQuery[FactTable]

    val create = DBIO.seq(
      facts.schema.createIfNotExists
    )

    executeSimpleCommand(create)
  }

  def insert(fact: Fact): Unit = {
    val facts = TableQuery[FactTable]

    val insertNewRecord = DBIO.seq(
      facts += fact
    )

    executeSimpleCommand(insertNewRecord)
  }

  private def executeSimpleCommand[U <: Effect](cmd: DBIOAction[Unit, NoStream, U]): Unit = {
    val db = getDbConnection

    db.run(cmd) andThen {
      case Success(value) => Log.info(value.toString)
      case Failure(exception) => Log.error(exception)
    } onComplete (_ => db.close())
  }

  private def getDbConnection = Database.forConfig("mydb")

  def select(from: Timestamp, to: Timestamp, zone: String): Future[Seq[Fact]] = {
    val facts = TableQuery[FactTable]

    val query = facts
      .filter(_.dateTime >= from)
      .filter(_.dateTime <= to)
      .filter(_.zone === zone)


    val action = query.result

    val db = getDbConnection
    db.run(action)
  }

  case class Fact(id: Option[Int], zone: String, dateTime: Timestamp, temperature: Double)

  class FactTable(tag: Tag) extends Table[Fact](tag, "FactTable") {
    override def * = (id.?, zone, dateTime, temperature) <> (Fact.tupled, Fact.unapply)

    def id = column[Int]("ID", SqlType("Serial"),O.PrimaryKey, O.AutoInc)

    def zone = column[String]("ZONE")

    def dateTime = column[Timestamp]("DATETIME")

    def temperature = column[Double]("TEMPERATURE")

    def idx = index("idx_1", (zone, dateTime), unique = true)
  }
}

