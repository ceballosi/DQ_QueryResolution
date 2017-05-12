package auth

import java.time.Duration

import com.typesafe.config.ConfigFactory
import org.ldaptive.auth.{Authenticator => LdaptiveAuthenticator, PooledBindAuthenticationHandler, SearchDnResolver}
import org.ldaptive.pool._
import org.ldaptive.{ConnectionConfig, DefaultConnectionFactory}

/**
  * Configure LDAP connection here.
  * Ldaptive authenticator also provided by this class
  */
object LdapClient {

  val ldapUrl = ConfigFactory.load().getString("dqms.ldap.url")
  val ldapPort = ConfigFactory.load().getString("dqms.ldap.port")
  val baseDn = ConfigFactory.load().getString("dqms.ldap.basedn")

  val connectionConfig = new ConnectionConfig()
  connectionConfig setConnectTimeout Duration.ofMillis(500)
  connectionConfig setResponseTimeout Duration.ofMillis(1000)
  connectionConfig setLdapUrl(ldapUrl + ldapPort) //works through GeL core services ('cs') VPN

  val connectionFactory = new DefaultConnectionFactory()
  connectionFactory.setConnectionConfig(connectionConfig)

  val poolConfig = new PoolConfig()
  poolConfig setMinPoolSize 1
  poolConfig setMaxPoolSize 2
  poolConfig setValidateOnCheckOut true
  poolConfig setValidateOnCheckIn true
  poolConfig setValidatePeriodically false

  val connectionPool = new BlockingConnectionPool()
  connectionPool setPoolConfig poolConfig
  connectionPool setBlockWaitTime Duration.ofMillis(1000)
  connectionPool setValidator new SearchValidator()
  connectionPool setPruneStrategy new IdlePruneStrategy()
  connectionPool setConnectionFactory connectionFactory
  connectionPool initialize()

  val pooledConnectionFactory = new PooledConnectionFactory()
  pooledConnectionFactory setConnectionPool connectionPool

  val authHandler = new PooledBindAuthenticationHandler()
  authHandler setConnectionFactory pooledConnectionFactory

  val dnResolver = new SearchDnResolver(connectionFactory)
  dnResolver.setBaseDn(baseDn)
  dnResolver.setUserFilter("(uid={user})")

  val ldaptiveAuthenticator = new LdaptiveAuthenticator()
  ldaptiveAuthenticator setDnResolver dnResolver
  ldaptiveAuthenticator setAuthenticationHandler authHandler

}