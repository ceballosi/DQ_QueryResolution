package services

import javax.inject.Inject

import domain.LoggedIssue
import org.slf4j.{LoggerFactory, Logger}
import play.api.Environment
import play.api.libs.mailer.{MailerClient, _}

class MailService @Inject()(mailerClient: MailerClient, environment: Environment, sMTPConfiguration: SMTPConfiguration) {

  val log: Logger = LoggerFactory.getLogger(this.getClass())

  def send(issues: Seq[LoggedIssue]) = {
    val email: Email = MailService.createIssuesEmail(issues)
    log.info("Sending email ->" + email)
    val id = mailerClient.send(email)
  }


  @Deprecated
  def configureAndSend(issue: String) = {
    // injected SMTPConfiguration into our mailer
    val mailer = new SMTPMailer(sMTPConfiguration)
    val id = mailer.send(MailService.createIssuesEmail(issue))
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



  def createIssuesEmail(issues: Seq[LoggedIssue]): Email = {

    val odd = "background-color: #CDDFEE;"
    val even = "background-color: #FFFFF;"
    var rowContent = new StringBuilder

    issues.zipWithIndex.foreach { case (issue, i) =>
      val rowcolor = i % 2 match {
        case 0 => odd
        case 1 => even
      }
      rowContent++= s"<tr style='${rowcolor}' ><td>${issue.issueId}</td><td>${issue.dateLogged}</td><td>${issue.GMC}</td><td>${issue.description}</td><td>${issue.patientId.get}</td></tr>"
    }


    val email = Email(
      "GEL DQ issue",
      "Rick Rees<a_rick_rees@yahoo.co.uk>",
      Seq("Rick Y Rees<rick.rees@genomicsengland.co.uk>"),
      attachments = Seq(
        //        AttachmentFile("favicon.png", new File(environment.classLoader.getResource("public/images/favicon.png").getPath), contentId = Some(cid)),
        //        AttachmentData("data.txt", "data".getBytes, "text/plain", Some("Simple data"), Some(EmailAttachment.INLINE))
      ),
      bodyText = Some("A text fallback message from (configureAndSend)"),
      bodyHtml = Some(
        s"""<html><body><p>This is to inform you that a Data Quality problem has been found with the following issues
           </br>
           <table id="issuesTable" cellspacing="0">
                    <thead style="background-color: #a9c2d8;">
                        <tr>
                            <th> Issue Id </th>
                            <th> Date Logged </th>
                            <th> GMC </th>
                            <th> Description </th>
                            <th> PatientId </th>
                        </tr>
                    </thead>
                    <tbody>"""
          + rowContent.toString() +
            """        </tbody>
             </table>
           </br>Please resolve this urgently.</br>regards</br>GEL DQ Team</p></body></html>""".stripMargin)
    )
    email
  }

}
