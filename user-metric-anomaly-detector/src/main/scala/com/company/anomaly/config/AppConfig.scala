package com.company.anomaly.config

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._
import scala.util.Try

/**
 * Application configuration loaded from application.conf
 *
 * @param lookbackDays Number of days to look back for historical data
 * @param iqrMultiplier Multiplier for IQR bounds calculation
 * @param zScoreThreshold Threshold for Z-score anomaly detection
 * @param userMetricsTable Hive table name for user metrics
 * @param anomalyResultsTable Hive table name for anomaly results
 * @param metricStatisticsTable Hive table name for metric statistics
 * @param smtpHost SMTP server host
 * @param smtpPort SMTP server port
 * @param smtpUsername SMTP username
 * @param smtpPassword SMTP password
 * @param emailFrom Sender email address
 * @param emailTo List of recipient email addresses
 * @param investigationUrl URL for investigation dashboard
 */
case class AppConfig(
  // Detection parameters
  lookbackDays: Int,
  iqrMultiplier: Double,
  zScoreThreshold: Double,

  // Hive table names
  userMetricsTable: String,
  anomalyResultsTable: String,
  metricStatisticsTable: String,

  // Email configuration
  smtpHost: String,
  smtpPort: Int,
  smtpUsername: String,
  smtpPassword: String,
  emailFrom: String,
  emailTo: List[String],
  investigationUrl: String
)

object AppConfig {
  private val logger = LoggerFactory.getLogger(getClass)

  /**
   * Load configuration from application.conf
   * Falls back to reference.conf if application.conf is not found
   *
   * @return AppConfig instance
   */
  def load(): AppConfig = {
    logger.info("Loading application configuration")

    val config: Config = ConfigFactory.load()

    try {
      val appConfig = AppConfig(
        // Detection parameters
        lookbackDays = config.getInt("anomaly-detector.lookback-days"),
        iqrMultiplier = config.getDouble("anomaly-detector.iqr-multiplier"),
        zScoreThreshold = config.getDouble("anomaly-detector.zscore-threshold"),

        // Hive table names
        userMetricsTable = config.getString("anomaly-detector.hive.user-metrics-table"),
        anomalyResultsTable = config.getString("anomaly-detector.hive.anomaly-results-table"),
        metricStatisticsTable = config.getString("anomaly-detector.hive.metric-statistics-table"),

        // Email configuration
        smtpHost = config.getString("anomaly-detector.email.smtp-host"),
        smtpPort = config.getInt("anomaly-detector.email.smtp-port"),
        smtpUsername = config.getString("anomaly-detector.email.smtp-username"),
        smtpPassword = config.getString("anomaly-detector.email.smtp-password"),
        emailFrom = config.getString("anomaly-detector.email.from"),
        emailTo = config.getStringList("anomaly-detector.email.to").asScala.toList,
        investigationUrl = config.getString("anomaly-detector.email.investigation-url")
      )

      logger.info("Configuration loaded successfully")
      logger.info(s"Lookback days: ${appConfig.lookbackDays}")
      logger.info(s"IQR multiplier: ${appConfig.iqrMultiplier}")
      logger.info(s"Z-Score threshold: ${appConfig.zScoreThreshold}")
      logger.info(s"User metrics table: ${appConfig.userMetricsTable}")
      logger.info(s"Email recipients: ${appConfig.emailTo.mkString(", ")}")

      appConfig
    } catch {
      case ex: Exception =>
        logger.error("Failed to load configuration", ex)
        throw new RuntimeException(s"Configuration error: ${ex.getMessage}", ex)
    }
  }

  /**
   * Load configuration with overrides from command line arguments
   * Useful for testing or dynamic configuration
   *
   * @param overrides Map of configuration overrides
   * @return AppConfig instance with overrides applied
   */
  def loadWithOverrides(overrides: Map[String, Any]): AppConfig = {
    logger.info(s"Loading configuration with overrides: $overrides")

    val baseConfig = ConfigFactory.load()
    val overrideConfig = ConfigFactory.parseMap(overrides.asJava)
    val config = overrideConfig.withFallback(baseConfig)

    AppConfig(
      lookbackDays = Try(config.getInt("anomaly-detector.lookback-days"))
        .getOrElse(baseConfig.getInt("anomaly-detector.lookback-days")),
      iqrMultiplier = Try(config.getDouble("anomaly-detector.iqr-multiplier"))
        .getOrElse(baseConfig.getDouble("anomaly-detector.iqr-multiplier")),
      zScoreThreshold = Try(config.getDouble("anomaly-detector.zscore-threshold"))
        .getOrElse(baseConfig.getDouble("anomaly-detector.zscore-threshold")),
      userMetricsTable = Try(config.getString("anomaly-detector.hive.user-metrics-table"))
        .getOrElse(baseConfig.getString("anomaly-detector.hive.user-metrics-table")),
      anomalyResultsTable = Try(config.getString("anomaly-detector.hive.anomaly-results-table"))
        .getOrElse(baseConfig.getString("anomaly-detector.hive.anomaly-results-table")),
      metricStatisticsTable = Try(config.getString("anomaly-detector.hive.metric-statistics-table"))
        .getOrElse(baseConfig.getString("anomaly-detector.hive.metric-statistics-table")),
      smtpHost = Try(config.getString("anomaly-detector.email.smtp-host"))
        .getOrElse(baseConfig.getString("anomaly-detector.email.smtp-host")),
      smtpPort = Try(config.getInt("anomaly-detector.email.smtp-port"))
        .getOrElse(baseConfig.getInt("anomaly-detector.email.smtp-port")),
      smtpUsername = Try(config.getString("anomaly-detector.email.smtp-username"))
        .getOrElse(baseConfig.getString("anomaly-detector.email.smtp-username")),
      smtpPassword = Try(config.getString("anomaly-detector.email.smtp-password"))
        .getOrElse(baseConfig.getString("anomaly-detector.email.smtp-password")),
      emailFrom = Try(config.getString("anomaly-detector.email.from"))
        .getOrElse(baseConfig.getString("anomaly-detector.email.from")),
      emailTo = Try(config.getStringList("anomaly-detector.email.to").asScala.toList)
        .getOrElse(baseConfig.getStringList("anomaly-detector.email.to").asScala.toList),
      investigationUrl = Try(config.getString("anomaly-detector.email.investigation-url"))
        .getOrElse(baseConfig.getString("anomaly-detector.email.investigation-url"))
    )
  }

  /**
   * Validate configuration values
   * Throws exception if validation fails
   *
   * @param config AppConfig to validate
   */
  def validate(config: AppConfig): Unit = {
    require(config.lookbackDays > 0, "lookback-days must be positive")
    require(config.iqrMultiplier > 0, "iqr-multiplier must be positive")
    require(config.zScoreThreshold > 0, "zscore-threshold must be positive")
    require(config.userMetricsTable.nonEmpty, "user-metrics-table must not be empty")
    require(config.anomalyResultsTable.nonEmpty, "anomaly-results-table must not be empty")
    require(config.metricStatisticsTable.nonEmpty, "metric-statistics-table must not be empty")
    require(config.smtpHost.nonEmpty, "smtp-host must not be empty")
    require(config.smtpPort > 0, "smtp-port must be positive")
    require(config.emailFrom.nonEmpty, "email-from must not be empty")
    require(config.emailTo.nonEmpty, "email-to list must not be empty")

    logger.info("Configuration validation passed")
  }
}
