package auth

import java.time.Duration

import org.ldaptive.auth.{Authenticator => LdaptiveAuthenticator, PooledBindAuthenticationHandler, SearchDnResolver}
import org.ldaptive.pool._
import org.ldaptive.{ConnectionConfig, DefaultConnectionFactory}

/**
  * Configure LDAP connection here.
  * Ldaptive authenticator also provided by this class
  */
object LdapClient {

  val port = 389
  val cn = "cn=%s,ou=people,dc=ge,dc=co,dc=uk"

  val connectionConfig = new ConnectionConfig()
  connectionConfig setConnectTimeout Duration.ofMillis(500)
  connectionConfig setResponseTimeout Duration.ofMillis(1000)
  connectionConfig setLdapUrl("ldap://10.10.0.20:" + port)

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
  dnResolver.setBaseDn("ou=int,ou=people,dc=ge,dc=co,dc=uk")
  dnResolver.setUserFilter("(uid={user})")

  val ldaptiveAuthenticator = new LdaptiveAuthenticator()
  ldaptiveAuthenticator setDnResolver dnResolver
  ldaptiveAuthenticator setAuthenticationHandler authHandler

}