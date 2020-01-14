package model.logger

import model.dao.{ClientRedis, LogError}
import model.utilities.UNKNOWN
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.{Level, LogManager}


object Log {
  private val logger = LogManager.getLogger(Log.getClass)
  Configurator.setLevel(logger.getName, Level.DEBUG)



  def debug(msg: String): Unit = logger.debug(msg)

  def info(msg: String): Unit = logger.info(msg)

  def warn(msg: String): Unit = logger.warn(msg)

  def error(msg: String): Unit = {
    logger.error(msg)
    ClientRedis.addError(LogError(msg))
  }

  def error(cause: Throwable, device: String = UNKNOWN, zone: String = UNKNOWN): Unit = {
    logger.error(cause.getMessage)
    ClientRedis.addError(LogError(cause.getMessage, device, zone))
  }
}