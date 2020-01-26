package model.dao


import java.sql.{Date, Timestamp}
import java.time.LocalDate

import model.dao.Granularity.GranularityState
import model.logger.Log
import org.joda.time.DateTime
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object FactTableComponent {
  implicit def jodaTimeMapping: BaseColumnType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
    dateTime => new Timestamp(dateTime.getMillis),
    timeStamp => new DateTime(timeStamp.getTime)
  )

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

  def select(from: Timestamp, to: Timestamp, zone: String, granularity: GranularityState = Granularity.DAY): Future[Seq[(String, Option[Double])]] = {
    val facts = TableQuery[FactTable]

    val timestampToChar = SimpleFunction.binary[Timestamp, String, String]("to_char")
    val getHours = (input: FactTable) => (timestampToChar(input.dateTime, "DD/MM/YYYY HH24"), input.temperature, input.dateTime)
    val getDay = (input: FactTable) => (timestampToChar(input.dateTime, "DD/MM/YYYY"), input.temperature, input.dateTime)
    val getMonthYear = (input: FactTable) => (timestampToChar(input.dateTime, "MM/YYYY"), input.temperature, input.dateTime)
    val getYear = (input: FactTable) => (timestampToChar(input.dateTime, "YYYY"), input.temperature, input.dateTime)

    val mapping = granularity match {
      case Granularity.DAY => getDay
      case Granularity.MONTH => getMonthYear
      case Granularity.YEAR => getYear
      case _ => getHours
    }

    val query = facts
      .filter(_.dateTime >= from)
      .filter(_.dateTime <= to)
      .filter(_.zone === zone)
      .map(mapping).groupBy(_._1).map { case (date, e) => (date, e.map(_._2).avg)}

    val action = query.result

    val db = getDbConnection


    val f = db.run(action)
    f onComplete (_ => db.close())

    f
  }

  private def getDbConnection = Database.forConfig("mydb")

  case class Fact(id: Option[Int], zone: String, dateTime: Timestamp, temperature: Double)

  class FactTable(tag: Tag) extends Table[Fact](tag, "FactTable") {
    override def * = (id.?, zone, dateTime, temperature) <> (Fact.tupled, Fact.unapply)

    def id = column[Int]("ID", SqlType("Serial"), O.PrimaryKey, O.AutoInc)

    def temperature = column[Double]("TEMPERATURE")

    def idx = index("idx_1", (zone, dateTime), unique = true)

    def zone = column[String]("ZONE")

    def dateTime = column[Timestamp]("DATETIME")
  }

}


object Granularity {

  def apply(input: String): GranularityState = valueOf(input)

  def valueOf(str: String): GranularityState = str.toLowerCase match {
    case HOUR.asString => HOUR
    case DAY.asString => DAY
    case MONTH.asString => MONTH
    case YEAR.asString => YEAR
    case _ => throw new Throwable("Granularity " + str + " not found")
  }

  def values: Iterable[GranularityState] = Iterable(HOUR, DAY, MONTH, YEAR)

  sealed trait GranularityState {
    def asString: String
  }

  case object DAY extends GranularityState {
    override val asString: String = "day"
  }

  case object MONTH extends GranularityState {
    override val asString: String = "month"
  }

  case object YEAR extends GranularityState {
    override val asString: String = "year"
  }

  case object HOUR extends GranularityState {
    override val asString: String = "hour"
  }
}
