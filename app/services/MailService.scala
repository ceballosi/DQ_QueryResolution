package services

import java.io.File

import play.api.Environment
import org.apache.commons.mail.EmailAttachment
import play.api.libs.mailer._
import play.api.mvc.{Action, Controller}
import javax.inject.Inject

import play.api.libs.mailer.MailerClient

class MailService @Inject()(mailerClient: MailerClient, environment: Environment, sMTPConfiguration: SMTPConfiguration) {

  def send(issues: String) = {
    val id = mailerClient.send(MailService.createIssuesEmail(issues))
  }


  @Deprecated
  def configureAndSend(issues: String) = {

    // injected SMTPConfiguration into our mailer
    val mailer = new SMTPMailer(sMTPConfiguration)
    val id = mailer.send(MailService.createIssuesEmail(issues))

    // explicit SMTPConfiguration
//    val mailer = new SMTPMailer(SMTPConfiguration("smtp.gmail.com", 587, false, true, Some("rickreesrr@gmail.com"), Some("password")))
//    val id = mailer.send(createIssuesEmail(issues))
  }


}


object MailService {
  //load template from db? - can probably just use scala interpolated string
  def createIssuesEmail(id: String): Email = {
    val email = Email(
      "GEL DQ issue",
      "Rick Rees<a_rick_rees@yahoo.co.uk>",
      Seq("Rick Y Rees<rick.rees@genomicsengland.co.uk>"),
      attachments = Seq(
        //        AttachmentFile("favicon.png", new File(environment.classLoader.getResource("public/images/favicon.png").getPath), contentId = Some(cid)),
        //        AttachmentData("data.txt", "data".getBytes, "text/plain", Some("Simple data"), Some(EmailAttachment.INLINE))
      ),
      bodyText = Some("A text fallback message from (configureAndSend)"),
      bodyHtml = Some(s"""<html><body><p>This is to inform you that a Data Quality problem has been found with <b>Issue Id=$id</b>. </br>Please resolve this urgently.</br>regards</br>GEL DQ Team</p></body></html>""")
    )
    email
  }

}
