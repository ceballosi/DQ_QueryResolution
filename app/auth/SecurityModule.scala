package auth

import com.google.inject.AbstractModule
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.http.client.indirect.FormClient
import org.pac4j.ldap.profile.service.LdapProfileService
import org.pac4j.play.http.DefaultHttpActionAdapter
import org.pac4j.play.store.{PlayCacheSessionStore, PlaySessionStore}
import org.pac4j.play.{CallbackController, LogoutController}
import play.api.{Configuration, Environment}

/**
  * Main configuration class for pac4j authentication and authorization along with filters in application.conf
  */
class SecurityModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure() = {

    val baseUrl = configuration.getString("play.http.context").get
    val baseDn: String = configuration.getString("dqms.ldap.basedn").get

    val authenticator = new LdapProfileService(LdapClient.pooledConnectionFactory,
      LdapClient.ldaptiveAuthenticator, "", baseDn)
//    val authenticator = new LdapProfileService()    // no ldap version

    authenticator.setUsernameAttribute("uid")
    val formClient = new FormClient(s"$baseUrl/login", authenticator)
    val clients = new Clients(s"$baseUrl/callback", formClient)
    val config = new Config(clients)

    config.setHttpActionAdapter(new DefaultHttpActionAdapter())
    bind(classOf[Config]).toInstance(config)

    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheSessionStore])

    val callbackController = new CallbackController()
    callbackController.setDefaultUrl(s"$baseUrl")
    callbackController.setMultiProfile(true)
    bind(classOf[CallbackController]).toInstance(callbackController)

    val logoutController = new LogoutController()
    logoutController.setDefaultUrl(s"$baseUrl/login")
    bind(classOf[LogoutController]).toInstance(logoutController)

  }

}
