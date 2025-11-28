package com.company.anomaly

import com.company.anomaly.config.AppConfig
import com.company.anomaly.detector.{IQRDetector, ZScoreDetector}
import com.company.anomaly.model.AnomalyResult
import com.company.anomaly.service.{EmailService, MetricService}
import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory

import java.sql.Date
import java.time.LocalDate
import scala.util.{Failure, Success, Try}

/**
 * Main application for detecting anomalies in user registration metrics
 *
 * This application:
 * 1. Loads historical user metrics from Hive (configurable lookback period)
 * 2. Calculates statistics for the same day of week as the target date
 * 3. Runs multiple anomaly detection algorithms (IQR and Z-Score)
 * 4. Saves results to Hive
 * 5. Sends email alerts if anomalies are detected
 *
 * Usage:
 *   spark-submit --class com.company.anomaly.UserMetricAnomalyDetector \
 *     user-metric-anomaly-detector-1.0.0.jar [target-date]
 *
 * Arguments:
 *   target-date: Date to analyze in YYYY-MM-DD format (default: current date)
 */
object UserMetricAnomalyDetector {

  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    logger.info("Starting User Metric Anomaly Detector")

    // Parse command line arguments
    val targetDate = parseTargetDate(args)
    logger.info(s"Target date: $targetDate")

    // Load configuration
    val config = AppConfig.load()
    AppConfig.validate(config)

    // Create Spark session
    val spark = createSparkSession()

    try {
      // Run anomaly detection
      val result = runAnomalyDetection(spark, config, targetDate)

      result match {
        case Success(_) =>
          logger.info("Anomaly detection completed successfully")
          System.exit(0)

        case Failure(ex) =>
          logger.error("Anomaly detection failed", ex)
          System.exit(1)
      }
    } finally {
      // Clean up resources
      logger.info("Stopping Spark session")
      spark.stop()
    }
  }

  /**
   * Parse target date from command line arguments
   *
   * @param args Command line arguments
   * @return Target date (defaults to current date if not provided)
   */
  private def parseTargetDate(args: Array[String]): Date = {
    if (args.isEmpty) {
      // Default to current date
      val today = LocalDate.now()
      logger.info(s"No target date provided, using current date: $today")
      Date.valueOf(today)
    } else {
      try {
        val localDate = LocalDate.parse(args(0))
        logger.info(s"Parsed target date: $localDate")
        Date.valueOf(localDate)
      } catch {
        case ex: Exception =>
          logger.error(s"Invalid date format: ${args(0)}. Expected: YYYY-MM-DD", ex)
          throw new IllegalArgumentException(
            s"Invalid date format: ${args(0)}. Expected: YYYY-MM-DD"
          )
      }
    }
  }

  /**
   * Create Spark session with Hive support
   *
   * @return SparkSession instance
   */
  private def createSparkSession(): SparkSession = {
    logger.info("Creating Spark session")

    val spark = SparkSession.builder()
      .appName("User Metric Anomaly Detector")
      .enableHiveSupport()
      .config("spark.sql.shuffle.partitions", "200")
      .config("spark.sql.adaptive.enabled", "true")
      .config("hive.exec.dynamic.partition", "true")
      .config("hive.exec.dynamic.partition.mode", "nonstrict")
      .getOrCreate()

    logger.info(s"Spark version: ${spark.version}")
    logger.info(s"Spark master: ${spark.sparkContext.master}")

    spark
  }

  /**
   * Main orchestration logic for anomaly detection
   *
   * @param spark SparkSession instance
   * @param config Application configuration
   * @param targetDate Date to analyze
   * @return Success or Failure
   */
  private def runAnomalyDetection(
    spark: SparkSession,
    config: AppConfig,
    targetDate: Date
  ): Try[Unit] = {

    logger.info(s"Running anomaly detection for $targetDate")

    for {
      // Initialize services
      metricService = MetricService(spark, config)
      emailService = EmailService(config)

      // Get actual count for target date
      actualCount <- metricService.getActualCount(targetDate)
      _ = logger.info(s"Actual user count for $targetDate: $actualCount")

      // Load historical data
      historicalData <- metricService.loadHistoricalMetrics(targetDate, config.lookbackDays)
      _ = logger.info(s"Loaded historical data: ${historicalData.count()} records")

      // Get day of week for target date
      dayOfWeek = metricService.getDayOfWeek(targetDate)
      _ = logger.info(s"Day of week for $targetDate: $dayOfWeek")

      // Calculate statistics for same day of week
      statistics <- metricService.calculateStatistics(historicalData, dayOfWeek)
      _ = logger.info(s"Calculated statistics: mean=${statistics.meanCount}, " +
        s"stdDev=${statistics.stdDev}, bounds=[${statistics.lowerBound}, ${statistics.upperBound}]")

      // Run anomaly detection algorithms
      detectors = List(
        IQRDetector(config.iqrMultiplier),
        ZScoreDetector(config.zScoreThreshold)
      )

      results = detectors.map { detector =>
        logger.info(s"Running ${detector.methodName} detection")
        val result = detector.detect(targetDate, actualCount, statistics)
        logger.info(s"${detector.methodName} result: isAnomaly=${result.isAnomaly}, " +
          s"score=${result.anomalyScore}, severity=${result.severity}")
        result
      }

      // Save results to Hive
      _ <- metricService.saveAnomalyResults(results)
      _ = logger.info(s"Saved ${results.length} detection results to Hive")

      // Optionally save statistics for future use
      _ <- metricService.saveMetricStatistics(statistics)
      _ = logger.info(s"Saved statistics for day of week $dayOfWeek")

      // Send email alert if any anomalies detected
      _ <- handleAnomalyAlert(emailService, targetDate, actualCount, results)

    } yield {
      logger.info("Anomaly detection pipeline completed successfully")
      printSummary(targetDate, actualCount, results)
    }
  }

  /**
   * Handle email alerting based on detection results
   *
   * @param emailService Email service instance
   * @param targetDate Target date
   * @param actualCount Actual user count
   * @param results Detection results
   * @return Success or Failure
   */
  private def handleAnomalyAlert(
    emailService: EmailService,
    targetDate: Date,
    actualCount: Long,
    results: Seq[AnomalyResult]
  ): Try[Unit] = {

    val anomalies = results.filter(_.isAnomaly)

    if (anomalies.isEmpty) {
      logger.info("No anomalies detected, skipping email alert")
      Success(())
    } else {
      logger.warn(s"Anomalies detected by ${anomalies.length} method(s): " +
        s"${anomalies.map(_.detectionMethod).mkString(", ")}")

      emailService.sendAnomalyAlert(targetDate, actualCount, results) match {
        case Success(_) =>
          logger.info("Email alert sent successfully")
          Success(())
        case Failure(ex) =>
          // Log error but don't fail the entire job if email fails
          logger.error("Failed to send email alert", ex)
          logger.warn("Continuing despite email failure")
          Success(())
      }
    }
  }

  /**
   * Print summary of detection results to console
   *
   * @param targetDate Target date
   * @param actualCount Actual user count
   * @param results Detection results
   */
  private def printSummary(
    targetDate: Date,
    actualCount: Long,
    results: Seq[AnomalyResult]
  ): Unit = {
    println("\n" + "=" * 80)
    println("ANOMALY DETECTION SUMMARY")
    println("=" * 80)
    println(s"Target Date: $targetDate")
    println(s"Actual User Count: $actualCount")
    println("-" * 80)

    results.foreach { result =>
      println(s"\n${result.detectionMethod} Method:")
      println(s"  Expected Range: [${result.expectedRangeMin.toInt}, ${result.expectedRangeMax.toInt}]")
      println(s"  Is Anomaly: ${result.isAnomaly}")
      println(s"  Anomaly Score: ${f"${result.anomalyScore}%.2f"}")
      println(s"  Severity: ${result.severity}")
      println(s"  Deviation: ${f"${result.percentageDeviation}%.2f"}%")
    }

    println("-" * 80)

    val anomalyCount = results.count(_.isAnomaly)
    if (anomalyCount > 0) {
      println(s"⚠️  ALERT: Anomalies detected by $anomalyCount method(s)")
    } else {
      println("✓  No anomalies detected")
    }

    println("=" * 80 + "\n")
  }

  // TODO: Future enhancements
  // - Implement machine learning models (Isolation Forest, LSTM)
  // - Add Slack/Teams integration for notifications
  // - Support multiple metrics in single run
  // - Add real-time streaming detection
  // - Implement auto-tuning of detection thresholds
  // - Add visualization dashboard integration
  // - Support for seasonal decomposition (STL)
  // - Add anomaly explanation using SHAP values
}
