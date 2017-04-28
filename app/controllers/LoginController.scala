package controllers

import javax.inject.{Inject, Singleton}

import org.pac4j.core.config.Config
import org.pac4j.http.client.indirect.FormClient
import org.slf4j.LoggerFactory
import play.api.mvc.{Action, Controller}

/**
  * The callback url for pac4j's indirect client is formed in part by the name of the client so we need to resolve
  * the url in the controller and pass to the template
  * @param config pac4j config
  */
@Singleton
class LoginController @Inject()(val config: Config) extends Controller {

  def login() = Action {
    val formClient = config.getClients.findClient("FormClient").asInstanceOf[FormClient]
    Ok(views.html.login.render(formClient.getCallbackUrl))
  }

}
