package model.dao

import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

object RedisConnection {
  private var redisHost: String = "127.0.0.1"
  private var redisPort: Int = 6379
  private var redisPw: Option[String] = None

  private val DEFAULT_TIMEOUT: Int = 15000

  def setRedisHost(value: String): Unit = redisHost = value

  def REDIS_HOST: String = redisHost

  def setRedisPort(value: Int): Unit = redisPort = value

  def REDIS_PORT: Int = redisPort

  def setRedisPw(value: String): Unit = redisPw = Some(value)

  def REDIS_PW: Option[String] = redisPw

  val config = new JedisPoolConfig()

  private val connection: JedisPool = REDIS_PW match {
    case Some(pw) => new JedisPool(config,redisHost, redisPort, DEFAULT_TIMEOUT, pw)
    case None => new JedisPool(config,redisHost, redisPort, DEFAULT_TIMEOUT)
  }

  def getConnection: Jedis = connection.getResource

  def close: Unit = connection.close()
}
