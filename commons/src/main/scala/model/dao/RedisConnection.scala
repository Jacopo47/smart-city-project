package model.dao

import io.lettuce.core.{RedisClient, RedisURI}
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

object RedisConnection {
  private val redisHost: String = Option(System.getenv("REDIS_HOST")).getOrElse("127.0.0.1")
  private val redisPort: Int = Option(System.getenv("REDIS_PORT")).getOrElse("6379").toInt
  private val redisPw: Option[String] = Option(System.getenv("REDIS_PW"))

  private val DEFAULT_TIMEOUT: Int = 15000

  val config = new JedisPoolConfig()

  private val connection: JedisPool = redisPw match {
    case Some(pw) => new JedisPool(config,redisHost, redisPort, DEFAULT_TIMEOUT, pw)
    case None => new JedisPool(config,redisHost, redisPort, DEFAULT_TIMEOUT)
  }

  def getConnection: Jedis = connection.getResource


  def getLettuceConnection: RedisClient = {
    val redisURI = redisPw match {
      case Some(pw) => RedisURI.Builder.redis(redisHost, redisPort).withPassword(pw).build()

      case None => RedisURI.Builder.redis(redisHost, redisPort).build()
    }

    RedisClient.create(redisURI)
  }

  def close: Unit = connection.close()
}
