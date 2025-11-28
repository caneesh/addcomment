package com.company.anomaly.config

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AppConfigSpec extends AnyFlatSpec with Matchers {

  "AppConfig" should "have all required fields" in {
    val config = AppConfig(
      lookbackDays = 1460,
      iqrMultiplier = 1.5,
      zScoreThreshold = 3.0,
      userMetricsTable = "default.user_metrics",
      anomalyResultsTable = "default.anomaly_detection_results",
      metricStatisticsTable = "default.metric_statistics",
      smtpHost = "smtp.example.com",
      smtpPort = 587,
      smtpUsername = "test@example.com",
      smtpPassword = "password",
      emailFrom = "test@example.com",
      emailTo = List("recipient@example.com"),
      investigationUrl = "https://dashboard.example.com"
    )

    config.lookbackDays shouldBe 1460
    config.iqrMultiplier shouldBe 1.5
    config.zScoreThreshold shouldBe 3.0
    config.userMetricsTable shouldBe "default.user_metrics"
    config.smtpHost shouldBe "smtp.example.com"
    config.smtpPort shouldBe 587
    config.emailTo should contain("recipient@example.com")
  }

  "AppConfig.validate" should "pass for valid configuration" in {
    val config = AppConfig(
      lookbackDays = 365,
      iqrMultiplier = 1.5,
      zScoreThreshold = 3.0,
      userMetricsTable = "default.user_metrics",
      anomalyResultsTable = "default.anomaly_detection_results",
      metricStatisticsTable = "default.metric_statistics",
      smtpHost = "smtp.example.com",
      smtpPort = 587,
      smtpUsername = "test@example.com",
      smtpPassword = "password",
      emailFrom = "test@example.com",
      emailTo = List("recipient@example.com"),
      investigationUrl = "https://dashboard.example.com"
    )

    noException should be thrownBy AppConfig.validate(config)
  }

  it should "fail for negative lookback days" in {
    val config = AppConfig(
      lookbackDays = -100,
      iqrMultiplier = 1.5,
      zScoreThreshold = 3.0,
      userMetricsTable = "default.user_metrics",
      anomalyResultsTable = "default.anomaly_detection_results",
      metricStatisticsTable = "default.metric_statistics",
      smtpHost = "smtp.example.com",
      smtpPort = 587,
      smtpUsername = "test@example.com",
      smtpPassword = "password",
      emailFrom = "test@example.com",
      emailTo = List("recipient@example.com"),
      investigationUrl = "https://dashboard.example.com"
    )

    assertThrows[IllegalArgumentException] {
      AppConfig.validate(config)
    }
  }

  it should "fail for negative IQR multiplier" in {
    val config = AppConfig(
      lookbackDays = 365,
      iqrMultiplier = -1.5,
      zScoreThreshold = 3.0,
      userMetricsTable = "default.user_metrics",
      anomalyResultsTable = "default.anomaly_detection_results",
      metricStatisticsTable = "default.metric_statistics",
      smtpHost = "smtp.example.com",
      smtpPort = 587,
      smtpUsername = "test@example.com",
      smtpPassword = "password",
      emailFrom = "test@example.com",
      emailTo = List("recipient@example.com"),
      investigationUrl = "https://dashboard.example.com"
    )

    assertThrows[IllegalArgumentException] {
      AppConfig.validate(config)
    }
  }

  it should "fail for negative Z-score threshold" in {
    val config = AppConfig(
      lookbackDays = 365,
      iqrMultiplier = 1.5,
      zScoreThreshold = -3.0,
      userMetricsTable = "default.user_metrics",
      anomalyResultsTable = "default.anomaly_detection_results",
      metricStatisticsTable = "default.metric_statistics",
      smtpHost = "smtp.example.com",
      smtpPort = 587,
      smtpUsername = "test@example.com",
      smtpPassword = "password",
      emailFrom = "test@example.com",
      emailTo = List("recipient@example.com"),
      investigationUrl = "https://dashboard.example.com"
    )

    assertThrows[IllegalArgumentException] {
      AppConfig.validate(config)
    }
  }

  it should "fail for empty table names" in {
    val config = AppConfig(
      lookbackDays = 365,
      iqrMultiplier = 1.5,
      zScoreThreshold = 3.0,
      userMetricsTable = "",
      anomalyResultsTable = "default.anomaly_detection_results",
      metricStatisticsTable = "default.metric_statistics",
      smtpHost = "smtp.example.com",
      smtpPort = 587,
      smtpUsername = "test@example.com",
      smtpPassword = "password",
      emailFrom = "test@example.com",
      emailTo = List("recipient@example.com"),
      investigationUrl = "https://dashboard.example.com"
    )

    assertThrows[IllegalArgumentException] {
      AppConfig.validate(config)
    }
  }

  it should "fail for empty email list" in {
    val config = AppConfig(
      lookbackDays = 365,
      iqrMultiplier = 1.5,
      zScoreThreshold = 3.0,
      userMetricsTable = "default.user_metrics",
      anomalyResultsTable = "default.anomaly_detection_results",
      metricStatisticsTable = "default.metric_statistics",
      smtpHost = "smtp.example.com",
      smtpPort = 587,
      smtpUsername = "test@example.com",
      smtpPassword = "password",
      emailFrom = "test@example.com",
      emailTo = List(),
      investigationUrl = "https://dashboard.example.com"
    )

    assertThrows[IllegalArgumentException] {
      AppConfig.validate(config)
    }
  }

  it should "fail for invalid SMTP port" in {
    val config = AppConfig(
      lookbackDays = 365,
      iqrMultiplier = 1.5,
      zScoreThreshold = 3.0,
      userMetricsTable = "default.user_metrics",
      anomalyResultsTable = "default.anomaly_detection_results",
      metricStatisticsTable = "default.metric_statistics",
      smtpHost = "smtp.example.com",
      smtpPort = -1,
      smtpUsername = "test@example.com",
      smtpPassword = "password",
      emailFrom = "test@example.com",
      emailTo = List("recipient@example.com"),
      investigationUrl = "https://dashboard.example.com"
    )

    assertThrows[IllegalArgumentException] {
      AppConfig.validate(config)
    }
  }

  "AppConfig" should "support multiple email recipients" in {
    val config = AppConfig(
      lookbackDays = 365,
      iqrMultiplier = 1.5,
      zScoreThreshold = 3.0,
      userMetricsTable = "default.user_metrics",
      anomalyResultsTable = "default.anomaly_detection_results",
      metricStatisticsTable = "default.metric_statistics",
      smtpHost = "smtp.example.com",
      smtpPort = 587,
      smtpUsername = "test@example.com",
      smtpPassword = "password",
      emailFrom = "test@example.com",
      emailTo = List("user1@example.com", "user2@example.com", "user3@example.com"),
      investigationUrl = "https://dashboard.example.com"
    )

    config.emailTo should have size 3
    config.emailTo should contain allOf("user1@example.com", "user2@example.com", "user3@example.com")
  }

  "AppConfig default values" should "be reasonable for production use" in {
    val config = AppConfig(
      lookbackDays = 1460, // 4 years
      iqrMultiplier = 1.5,
      zScoreThreshold = 3.0,
      userMetricsTable = "default.user_metrics",
      anomalyResultsTable = "default.anomaly_detection_results",
      metricStatisticsTable = "default.metric_statistics",
      smtpHost = "smtp.example.com",
      smtpPort = 587,
      smtpUsername = "test@example.com",
      smtpPassword = "password",
      emailFrom = "test@example.com",
      emailTo = List("recipient@example.com"),
      investigationUrl = "https://dashboard.example.com"
    )

    // 4 years of data is reasonable for pattern detection
    config.lookbackDays should be >= 365

    // Standard IQR multiplier for outlier detection
    config.iqrMultiplier shouldBe 1.5

    // Standard Z-score threshold (99.7% confidence)
    config.zScoreThreshold shouldBe 3.0

    // Standard SMTP port for TLS
    config.smtpPort shouldBe 587
  }
}
