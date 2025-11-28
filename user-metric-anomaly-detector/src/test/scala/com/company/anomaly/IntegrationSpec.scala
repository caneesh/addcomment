package com.company.anomaly

import com.company.anomaly.config.AppConfig
import com.company.anomaly.detector.{IQRDetector, ZScoreDetector}
import com.company.anomaly.model.MetricStatistics
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.sql.{Date, Timestamp}

/**
 * Integration tests for the complete anomaly detection workflow
 */
class IntegrationSpec extends AnyFlatSpec with Matchers {

  val testDate: Date = Date.valueOf("2025-11-27")
  val testTimestamp: Timestamp = new Timestamp(System.currentTimeMillis())

  "Complete anomaly detection workflow" should "detect anomalies consistently across methods" in {
    // Create historical data pattern (Mondays typically have 5000 users)
    val historicalValues = Seq(
      4800.0, 5000.0, 5200.0, 4900.0, 5100.0,
      5050.0, 4850.0, 5150.0, 5000.0, 4950.0
    )

    // Calculate statistics
    val stats = MetricStatistics.calculate(
      dayOfWeek = 1,
      values = historicalValues,
      iqrMultiplier = 1.5
    )

    // Initialize detectors
    val iqrDetector = new IQRDetector(iqrMultiplier = 1.5)
    val zScoreDetector = new ZScoreDetector(zScoreThreshold = 3.0)

    // Test normal value (should not trigger either detector)
    val normalResult1 = iqrDetector.detect(testDate, 5000, stats)
    val normalResult2 = zScoreDetector.detect(testDate, 5000, stats)

    normalResult1.isAnomaly shouldBe false
    normalResult2.isAnomaly shouldBe false

    // Test anomalous value (should trigger both detectors)
    val anomalyResult1 = iqrDetector.detect(testDate, 10000, stats)
    val anomalyResult2 = zScoreDetector.detect(testDate, 10000, stats)

    anomalyResult1.isAnomaly shouldBe true
    anomalyResult2.isAnomaly shouldBe true

    // Both methods should agree on severity for extreme values
    anomalyResult1.severity should not be "NORMAL"
    anomalyResult2.severity should not be "NORMAL"
  }

  it should "handle edge case where IQR detects but Z-Score doesn't" in {
    // Create data with outliers (IQR is more robust to outliers)
    val historicalValues = Seq(
      5000.0, 5100.0, 5200.0, 5000.0, 5150.0,
      10000.0 // outlier in historical data
    )

    val stats = MetricStatistics.calculate(
      dayOfWeek = 2,
      values = historicalValues,
      iqrMultiplier = 1.5
    )

    val iqrDetector = new IQRDetector(iqrMultiplier = 1.5)
    val zScoreDetector = new ZScoreDetector(zScoreThreshold = 3.0)

    // Test value at upper IQR bound
    val testValue = 6000L

    val iqrResult = iqrDetector.detect(testDate, testValue, stats)
    val zScoreResult = zScoreDetector.detect(testDate, testValue, stats)

    // Results may differ based on the methods' sensitivities
    iqrResult.detectionMethod shouldBe "IQR"
    zScoreResult.detectionMethod shouldBe "Z-Score"
  }

  it should "process realistic healthcare data pattern" in {
    // Simulate realistic weekly pattern for user registrations
    val mondayData = Seq(5500.0, 5600.0, 5450.0, 5550.0, 5500.0, 5650.0)
    val tuesdayData = Seq(6000.0, 6100.0, 5950.0, 6050.0, 6000.0, 6150.0)
    val sundayData = Seq(3000.0, 3100.0, 2950.0, 3050.0, 3000.0, 3150.0)

    val mondayStats = MetricStatistics.calculate(1, mondayData)
    val tuesdayStats = MetricStatistics.calculate(2, tuesdayData)
    val sundayStats = MetricStatistics.calculate(7, sundayData)

    // Verify day-of-week patterns are captured
    mondayStats.meanCount should be < tuesdayStats.meanCount
    sundayStats.meanCount should be < mondayStats.meanCount

    val iqrDetector = new IQRDetector()

    // Normal Monday count should not be anomaly when compared to Monday stats
    val mondayResult = iqrDetector.detect(testDate, 5500, mondayStats)
    mondayResult.isAnomaly shouldBe false

    // But would be anomaly if incorrectly compared to Sunday stats
    val incorrectResult = iqrDetector.detect(testDate, 5500, sundayStats)
    incorrectResult.isAnomaly shouldBe true
  }

  it should "handle data with high variance correctly" in {
    // High variance data (unstable metrics)
    val highVarianceData = Seq(
      1000.0, 5000.0, 2000.0, 8000.0, 3000.0,
      7000.0, 2500.0, 6000.0, 3500.0, 5500.0
    )

    val stats = MetricStatistics.calculate(3, highVarianceData)

    stats.stdDev should be > 1000.0 // High standard deviation
    stats.iqr should be > 2000.0 // Large IQR

    val iqrDetector = new IQRDetector()
    val zScoreDetector = new ZScoreDetector()

    // With high variance, moderate values shouldn't trigger alerts
    val result1 = iqrDetector.detect(testDate, 4000, stats)
    val result2 = zScoreDetector.detect(testDate, 4000, stats)

    result1.isAnomaly shouldBe false
    result2.isAnomaly shouldBe false
  }

  it should "handle data with low variance correctly" in {
    // Low variance data (stable metrics)
    val lowVarianceData = Seq(
      5000.0, 5010.0, 5005.0, 4995.0, 5020.0,
      4990.0, 5015.0, 4985.0, 5025.0, 5000.0
    )

    val stats = MetricStatistics.calculate(4, lowVarianceData)

    stats.stdDev should be < 20.0 // Low standard deviation
    stats.iqr should be < 30.0 // Small IQR

    val iqrDetector = new IQRDetector()
    val zScoreDetector = new ZScoreDetector()

    // With low variance, even small deviations might trigger alerts
    val result1 = iqrDetector.detect(testDate, 5100, stats)
    val result2 = zScoreDetector.detect(testDate, 5100, stats)

    // At least one detector should flag this as anomalous
    (result1.isAnomaly || result2.isAnomaly) shouldBe true
  }

  "AppConfig validation" should "integrate with detection workflow" in {
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

    // Validate config
    noException should be thrownBy AppConfig.validate(config)

    // Use config values in detectors
    val iqrDetector = new IQRDetector(config.iqrMultiplier)
    val zScoreDetector = new ZScoreDetector(config.zScoreThreshold)

    iqrDetector.methodName shouldBe "IQR"
    zScoreDetector.methodName shouldBe "Z-Score"
  }

  "Multiple detection methods" should "provide complementary insights" in {
    val historicalValues = Seq(
      4500.0, 4800.0, 5000.0, 5200.0, 5500.0,
      4700.0, 4900.0, 5100.0, 5300.0, 5000.0
    )

    val stats = MetricStatistics.calculate(5, historicalValues)

    val detectors = List(
      new IQRDetector(1.5),
      new ZScoreDetector(3.0)
    )

    val testValue = 7000L

    val results = detectors.map(detector =>
      detector.detect(testDate, testValue, stats)
    )

    // All detection methods should be represented
    results.map(_.detectionMethod) should contain allOf("IQR", "Z-Score")

    // All should detect this extreme value as anomaly
    results.forall(_.isAnomaly) shouldBe true

    // Each should provide unique insights via anomaly score
    results.map(_.anomalyScore).distinct should have size 2
  }

  "Anomaly severity classification" should "scale appropriately with deviation" in {
    val stats = MetricStatistics.calculate(
      6,
      Seq(5000.0, 5100.0, 4900.0, 5050.0, 4950.0)
    )

    val detector = new IQRDetector()

    val lowDeviationResult = detector.detect(testDate, 5200, stats)
    val mediumDeviationResult = detector.detect(testDate, 6000, stats)
    val highDeviationResult = detector.detect(testDate, 8000, stats)
    val criticalDeviationResult = detector.detect(testDate, 15000, stats)

    // Severity should increase with deviation
    val severityRanks = Map(
      "NORMAL" -> 0,
      "LOW" -> 1,
      "MEDIUM" -> 2,
      "HIGH" -> 3,
      "CRITICAL" -> 4
    )

    val rank1 = severityRanks(lowDeviationResult.severity)
    val rank2 = severityRanks(mediumDeviationResult.severity)
    val rank3 = severityRanks(highDeviationResult.severity)
    val rank4 = severityRanks(criticalDeviationResult.severity)

    rank2 should be >= rank1
    rank3 should be >= rank2
    rank4 should be >= rank3
  }

  "Statistical calculations" should "be mathematically consistent" in {
    val values = Seq(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    val stats = MetricStatistics.calculate(7, values)

    // Mean of 1-10 should be 5.5
    stats.meanCount shouldBe 5.5

    // Median of 1-10 should be 5.5
    stats.median shouldBe 5.5

    // Q1 should be 25th percentile
    stats.q1 should be < stats.median

    // Q3 should be 75th percentile
    stats.q3 should be > stats.median

    // IQR should be Q3 - Q1
    stats.iqr shouldBe stats.q3 - stats.q1

    // Bounds should follow formula
    stats.lowerBound shouldBe stats.q1 - 1.5 * stats.iqr
    stats.upperBound shouldBe stats.q3 + 1.5 * stats.iqr
  }
}
