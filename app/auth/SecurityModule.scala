package auth

import java.util

import com.google.inject.AbstractModule
import org.ldaptive.DefaultConnectionFactory
import org.ldaptive.auth.Authenticator
import org.pac4j.core.authorization.authorizer.{ProfileAuthorizer, RequireAnyRoleAuthorizer}
import org.pac4j.core.client.{Client, Clients}
import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.http.client.direct.{DirectBasicAuthClient, DirectFormClient}
import org.pac4j.http.client.indirect.{FormClient, IndirectBasicAuthClient}
import org.pac4j.ldap.profile.LdapProfile
import org.pac4j.ldap.profile.credentials.authenticator.LdapAuthenticator
import org.pac4j.ldap.profile.service.LdapProfileService
import org.pac4j.play.CallbackController
import org.pac4j.play.http.DefaultHttpActionAdapter
import org.pac4j.play.store.{PlaySessionStore, PlayCacheSessionStore}
import play.api.{Configuration, Environment}
import org.pac4j.play.LogoutController

/**
  * Main configuration class for pac4j authentication and authorization along with filters in application.conf
  */
class SecurityModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure() = {

    val authenticator = new LdapProfileService(LdapClient.pooledConnectionFactory,
      LdapClient.ldaptiveAuthenticator, "", "ou=int,ou=people,dc=ge,dc=co,dc=uk")
    authenticator.setUsernameAttribute("uid")
    val formClient = new FormClient("/login", authenticator)
    val clients = new Clients("/callback", formClient)
    val config = new Config(clients)

    config.setHttpActionAdapter(new DefaultHttpActionAdapter())
    bind(classOf[Config]).toInstance(config)

    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheSessionStore])

    val callbackController = new CallbackController()
    callbackController.setDefaultUrl("/")
    callbackController.setMultiProfile(true)
    bind(classOf[CallbackController]).toInstance(callbackController)

    val logoutController = new LogoutController()
    logoutController.setDefaultUrl("/login")
    bind(classOf[LogoutController]).toInstance(logoutController)

  }

}
