package model.dao

import java.sql.Timestamp
import org.joda.time.DateTime
import scala.collection.JavaConverters._

case class StreamGroupsInfo(name: String, consumers: Long, pendingMessages: Long, lastDeliveredIdTime: Timestamp)
object StreamGroupsInfo {
  def apply(name: String, consumers: Long, pendingMessages: Long, lastDeliveredIdTime: DateTime): StreamGroupsInfo = new StreamGroupsInfo(name, consumers, pendingMessages, lastDeliveredIdTime)

  def apply(generic: AnyRef): StreamGroupsInfo = {
    val values = generic.asInstanceOf[java.util.ArrayList[AnyRef]].asScala
    val timestamp = new DateTime(values(7).asInstanceOf[String].split("-")(0).toLong)

    new StreamGroupsInfo(values(1).toString, values(3).asInstanceOf[Long], values(5).asInstanceOf[Long], timestamp)
  }
}


case class ConsumerInfo(name: String, pendingMessages: Long, idle: Long)
object ConsumerInfo {
  def apply(name: String, pendingMessages: Long, idle: Long): ConsumerInfo = new ConsumerInfo(name, pendingMessages, idle)

  def apply(generic: AnyRef): ConsumerInfo = {
    val values = generic.asInstanceOf[java.util.ArrayList[AnyRef]].asScala

    new ConsumerInfo(values(1).toString, values(3).asInstanceOf[Long], values(5).asInstanceOf[Long])
  }
}
