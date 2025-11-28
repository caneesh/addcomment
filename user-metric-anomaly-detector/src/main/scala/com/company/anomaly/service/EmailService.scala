package com.company.anomaly.service

import com.company.anomaly.config.AppConfig
import com.company.anomaly.model.AnomalyResult
import org.slf4j.LoggerFactory

import java.io.File
import java.sql.Date
import java.util.Properties
import javax.activation.{DataHandler, FileDataSource}
import javax.mail._
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart}
import scala.util.{Failure, Success, Try}

/**
 * Service class for sending email notifications
 * Uses JavaMail API to send HTML formatted emails
 *
 * @param config Application configuration
 */
class EmailService(config: AppConfig) {

  private val logger = LoggerFactory.getLogger(getClass)

  /**
   * Send anomaly alert email
   *
   * @param targetDate The date being analyzed
   * @param actualCount Actual user count
   * @param results All anomaly detection results
   * @param chartFile Optional chart image file to attach
   * @return Success or Failure
   */
  def sendAnomalyAlert(
    targetDate: Date,
    actualCount: Long,
    results: Seq[AnomalyResult],
    chartFile: Option[File] = None
  ): Try[Unit] = Try {
    logger.info(s"Preparing to send anomaly alert for $targetDate")

    // Filter for anomalies only
    val anomalies = results.filter(_.isAnomaly)

    if (anomalies.isEmpty) {
      logger.info("No anomalies detected, skipping email")
      return Success(())
    }

    val subject = s"⚠️ User Metric Anomaly Detected - $targetDate"
    val htmlBody = buildHtmlEmail(targetDate, actualCount, results, chartFile.isDefined)

    sendEmail(subject, htmlBody, chartFile)

    logger.info(s"Anomaly alert email sent successfully to ${config.emailTo.mkString(", ")}")
  }

  /**
   * Build HTML formatted email body
   *
   * @param targetDate The date being analyzed
   * @param actualCount Actual user count
   * @param results All detection results
   * @param hasChart Whether a chart attachment is included
   * @return HTML string
   */
  private def buildHtmlEmail(
    targetDate: Date,
    actualCount: Long,
    results: Seq[AnomalyResult],
    hasChart: Boolean = false
  ): String = {
    val anomalies = results.filter(_.isAnomaly)
    val maxSeverity = if (anomalies.nonEmpty) anomalies.map(_.severity).maxBy(severityRank) else "NORMAL"

    val headerColor = maxSeverity match {
      case "CRITICAL" => "#d32f2f"
      case "HIGH" => "#f57c00"
      case "MEDIUM" => "#fbc02d"
      case "LOW" => "#388e3c"
      case _ => "#757575"
    }

    val resultsHtml = results.map(AnomalyResult.toHtml).mkString("\n")

    s"""
       |<!DOCTYPE html>
       |<html>
       |<head>
       |  <meta charset="UTF-8">
       |  <style>
       |    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
       |    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
       |    .header { background-color: $headerColor; color: white; padding: 20px; border-radius: 5px; }
       |    .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; border-radius: 5px; }
       |    .metric-box { background-color: white; padding: 15px; margin: 10px 0; border-left: 4px solid $headerColor; }
       |    .footer { margin-top: 20px; padding: 20px; background-color: #e0e0e0; border-radius: 5px; font-size: 12px; }
       |    table { width: 100%; border-collapse: collapse; margin: 10px 0; }
       |    th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
       |    th { background-color: #f0f0f0; font-weight: bold; }
       |  </style>
       |</head>
       |<body>
       |  <div class="container">
       |    <div class="header">
       |      <h1>⚠️ User Metric Anomaly Alert</h1>
       |      <p style="margin: 0; font-size: 18px;">Date: $targetDate | Severity: $maxSeverity</p>
       |    </div>
       |
       |    <div class="content">
       |      <div class="metric-box">
       |        <h2>Metric Summary</h2>
       |        <table>
       |          <tr>
       |            <th>Metric</th>
       |            <th>Value</th>
       |          </tr>
       |          <tr>
       |            <td>Target Date</td>
       |            <td><strong>$targetDate</strong></td>
       |          </tr>
       |          <tr>
       |            <td>Actual User Count</td>
       |            <td><strong>$actualCount</strong></td>
       |          </tr>
       |          <tr>
       |            <td>Expected Range (IQR)</td>
       |            <td>${results.find(_.detectionMethod == "IQR").map(r =>
                      s"${r.expectedRangeMin.toInt} - ${r.expectedRangeMax.toInt}"
                    ).getOrElse("N/A")}</td>
       |          </tr>
       |          <tr>
       |            <td>Expected Range (Z-Score)</td>
       |            <td>${results.find(_.detectionMethod == "Z-Score").map(r =>
                      s"${r.expectedRangeMin.toInt} - ${r.expectedRangeMax.toInt}"
                    ).getOrElse("N/A")}</td>
       |          </tr>
       |        </table>
       |      </div>
       |
       |      <h2>Detection Results</h2>
       |      $resultsHtml
       |      ${if (hasChart) """
       |      <div class="metric-box">
       |        <h3>📊 Visual Trend</h3>
       |        <p>A bar chart showing daily user counts for the last 10 days is attached to this email.</p>
       |        <p>The chart provides visual context to help identify patterns and trends.</p>
       |      </div>
       |      """ else ""}
       |
       |      <div class="metric-box">
       |        <h3>📊 Action Required</h3>
       |        <p>Please investigate the following:</p>
       |        <ul>
       |          <li>Review data pipeline logs for the target date</li>
       |          <li>Check for any system outages or maintenance windows</li>
       |          <li>Validate data source integrity</li>
       |          <li>Compare with known business events (holidays, campaigns, etc.)</li>
       |        </ul>
       |        <p>
       |          <a href="${config.investigationUrl}" style="display: inline-block; padding: 10px 20px; background-color: $headerColor; color: white; text-decoration: none; border-radius: 5px;">
       |            Investigate in Dashboard →
       |          </a>
       |        </p>
       |      </div>
       |    </div>
       |
       |    <div class="footer">
       |      <p><strong>User Metric Anomaly Detector</strong></p>
       |      <p>This is an automated alert generated by the anomaly detection system.</p>
       |      <p>For questions or to adjust alert settings, contact the Data Engineering team.</p>
       |    </div>
       |  </div>
       |</body>
       |</html>
       |""".stripMargin
  }

  /**
   * Send email using JavaMail API
   *
   * @param subject Email subject
   * @param htmlBody HTML email body
   * @param attachmentFile Optional file to attach
   * @return Success or Failure
   */
  private def sendEmail(
    subject: String,
    htmlBody: String,
    attachmentFile: Option[File] = None
  ): Try[Unit] = Try {
    logger.info(s"Sending email to ${config.emailTo.mkString(", ")}")

    // Set up mail server properties
    val props = new Properties()
    props.put("mail.smtp.host", config.smtpHost)
    props.put("mail.smtp.port", config.smtpPort.toString)
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.starttls.enable", "true")
    props.put("mail.smtp.ssl.protocols", "TLSv1.2")

    // Create authenticator if credentials are provided
    val authenticator = new Authenticator {
      override def getPasswordAuthentication: PasswordAuthentication = {
        new PasswordAuthentication(config.smtpUsername, config.smtpPassword)
      }
    }

    // Create session
    val session = Session.getInstance(props, authenticator)

    // Create message
    val message = new MimeMessage(session)
    message.setFrom(new InternetAddress(config.emailFrom))

    // Add recipients
    config.emailTo.foreach { recipient =>
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient))
    }

    message.setSubject(subject)

    // Create multipart message if attachment exists
    attachmentFile match {
      case Some(file) if file.exists() =>
        logger.info(s"Adding attachment: ${file.getName}")

        val multipart = new MimeMultipart()

        // HTML body part
        val htmlPart = new MimeBodyPart()
        htmlPart.setContent(htmlBody, "text/html; charset=utf-8")
        multipart.addBodyPart(htmlPart)

        // Attachment part
        val attachmentPart = new MimeBodyPart()
        val source = new FileDataSource(file)
        attachmentPart.setDataHandler(new DataHandler(source))
        attachmentPart.setFileName(file.getName)
        multipart.addBodyPart(attachmentPart)

        message.setContent(multipart)

      case Some(file) =>
        logger.warn(s"Attachment file does not exist: ${file.getAbsolutePath}")
        message.setContent(htmlBody, "text/html; charset=utf-8")

      case None =>
        message.setContent(htmlBody, "text/html; charset=utf-8")
    }

    // Send message
    Transport.send(message)

    logger.info("Email sent successfully")
  } match {
    case Success(_) => Success(())
    case Failure(ex) =>
      logger.error(s"Failed to send email: ${ex.getMessage}", ex)
      Failure(ex)
  }

  /**
   * Helper to rank severity levels
   */
  private def severityRank(severity: String): Int = severity match {
    case "CRITICAL" => 4
    case "HIGH" => 3
    case "MEDIUM" => 2
    case "LOW" => 1
    case _ => 0
  }
}

object EmailService {
  def apply(config: AppConfig): EmailService = new EmailService(config)
}
