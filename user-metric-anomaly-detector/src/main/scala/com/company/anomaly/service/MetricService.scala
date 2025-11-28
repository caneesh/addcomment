package com.company.anomaly.service

import com.company.anomaly.config.AppConfig
import com.company.anomaly.model.{AnomalyResult, MetricStatistics}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

import java.sql.Date
import scala.util.{Failure, Success, Try}

/**
 * Service class for handling metric data operations
 * Provides methods to load data, calculate statistics, and save results
 *
 * @param spark SparkSession instance
 * @param config Application configuration
 */
class MetricService(spark: SparkSession, config: AppConfig) {

  private val logger = LoggerFactory.getLogger(getClass)

  import spark.implicits._

  /**
   * Load historical user metrics from Hive
   *
   * @param targetDate The date to analyze
   * @param lookbackDays Number of days to look back for historical data
   * @return DataFrame with historical metrics
   */
  def loadHistoricalMetrics(targetDate: Date, lookbackDays: Int): Try[DataFrame] = Try {
    logger.info(s"Loading historical metrics for $targetDate with $lookbackDays days lookback")

    val endDate = targetDate
    val startDate = Date.valueOf(targetDate.toLocalDate.minusDays(lookbackDays))

    val query = s"""
      |SELECT
      |  metric_date,
      |  user_count,
      |  dayofweek(metric_date) as day_of_week
      |FROM ${config.userMetricsTable}
      |WHERE metric_date >= date('$startDate')
      |  AND metric_date < date('$endDate')
      |  AND user_count IS NOT NULL
      |ORDER BY metric_date
      |""".stripMargin

    val df = spark.sql(query)
    val count = df.count()

    logger.info(s"Loaded $count historical metric records")

    if (count == 0) {
      throw new RuntimeException(s"No historical data found between $startDate and $endDate")
    }

    df
  }

  /**
   * Get the actual user count for the target date
   *
   * @param targetDate The date to get metrics for
   * @return User count if available
   */
  def getActualCount(targetDate: Date): Try[Long] = Try {
    logger.info(s"Fetching actual count for $targetDate")

    val query = s"""
      |SELECT user_count
      |FROM ${config.userMetricsTable}
      |WHERE metric_date = date('$targetDate')
      |""".stripMargin

    val result = spark.sql(query).collect()

    if (result.isEmpty) {
      throw new RuntimeException(s"No data found for target date: $targetDate")
    }

    val count = result.head.getAs[Long]("user_count")
    logger.info(s"Actual count for $targetDate: $count")
    count
  }

  /**
   * Calculate statistics for a specific day of week from historical data
   *
   * @param historicalData DataFrame with historical metrics
   * @param dayOfWeek Day of week (1=Monday, 7=Sunday)
   * @return MetricStatistics for the specified day of week
   */
  def calculateStatistics(
    historicalData: DataFrame,
    dayOfWeek: Int
  ): Try[MetricStatistics] = Try {
    logger.info(s"Calculating statistics for day of week: $dayOfWeek")

    // Filter for the same day of week
    val sameDayData = historicalData
      .filter($"day_of_week" === dayOfWeek)
      .select($"user_count".cast("double").as("count"))

    val count = sameDayData.count()
    logger.info(s"Found $count samples for day of week $dayOfWeek")

    if (count < 4) {
      throw new RuntimeException(
        s"Insufficient data for day of week $dayOfWeek: only $count samples (minimum 4 required)"
      )
    }

    // Collect values for statistics calculation
    val values = sameDayData.select("count").as[Double].collect().toSeq

    // Calculate statistics using the model class
    val stats = MetricStatistics.calculate(
      dayOfWeek = dayOfWeek,
      values = values,
      iqrMultiplier = config.iqrMultiplier
    )

    logger.info(s"Statistics for day $dayOfWeek: mean=${stats.meanCount}, " +
      s"stdDev=${stats.stdDev}, median=${stats.median}, " +
      s"bounds=[${stats.lowerBound}, ${stats.upperBound}]")

    stats
  }

  /**
   * Save anomaly detection results to Hive
   *
   * @param results Sequence of AnomalyResult objects to save
   * @return Success or Failure
   */
  def saveAnomalyResults(results: Seq[AnomalyResult]): Try[Unit] = Try {
    logger.info(s"Saving ${results.length} anomaly detection results")

    if (results.isEmpty) {
      logger.warn("No results to save")
      return Success(())
    }

    val resultsDF = results.toDF()

    // Add partition columns
    val dfWithPartitions = resultsDF
      .withColumn("year", year($"metricDate"))
      .withColumn("month", month($"metricDate"))

    // Write to Hive table (partitioned)
    dfWithPartitions.write
      .mode("append")
      .partitionBy("year", "month")
      .insertInto(config.anomalyResultsTable)

    logger.info(s"Successfully saved ${results.length} anomaly results to ${config.anomalyResultsTable}")
  }

  /**
   * Save metric statistics to Hive
   * This can be used to pre-calculate and store statistics for faster lookups
   *
   * @param statistics MetricStatistics to save
   * @return Success or Failure
   */
  def saveMetricStatistics(statistics: MetricStatistics): Try[Unit] = Try {
    logger.info(s"Saving metric statistics for day of week ${statistics.dayOfWeek}")

    val statsDF = Seq(statistics).toDF()

    // Insert or update (overwrite mode for the specific day of week)
    // Note: This is a simplification. In production, consider using MERGE or DELETE+INSERT
    spark.sql(s"DELETE FROM ${config.metricStatisticsTable} WHERE day_of_week = ${statistics.dayOfWeek}")

    statsDF.write
      .mode("append")
      .insertInto(config.metricStatisticsTable)

    logger.info(s"Successfully saved statistics for day of week ${statistics.dayOfWeek}")
  }

  /**
   * Load pre-calculated statistics from Hive (optional optimization)
   *
   * @param dayOfWeek Day of week to load statistics for
   * @return MetricStatistics if available
   */
  def loadStatistics(dayOfWeek: Int): Try[Option[MetricStatistics]] = Try {
    logger.info(s"Loading pre-calculated statistics for day of week $dayOfWeek")

    val query = s"""
      |SELECT *
      |FROM ${config.metricStatisticsTable}
      |WHERE day_of_week = $dayOfWeek
      |""".stripMargin

    val results = spark.sql(query).as[MetricStatistics].collect()

    if (results.isEmpty) {
      logger.info(s"No pre-calculated statistics found for day of week $dayOfWeek")
      None
    } else {
      logger.info(s"Loaded pre-calculated statistics for day of week $dayOfWeek")
      Some(results.head)
    }
  }

  /**
   * Get day of week for a given date (1=Monday, 7=Sunday)
   *
   * @param date The date to check
   * @return Day of week (1-7)
   */
  def getDayOfWeek(date: Date): Int = {
    val calendar = java.util.Calendar.getInstance()
    calendar.setTime(date)
    val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    // Convert from Calendar (1=Sunday) to ISO (1=Monday)
    if (dayOfWeek == 1) 7 else dayOfWeek - 1
  }
}

object MetricService {
  def apply(spark: SparkSession, config: AppConfig): MetricService =
    new MetricService(spark, config)
}
