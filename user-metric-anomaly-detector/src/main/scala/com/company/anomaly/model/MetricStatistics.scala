package com.company.anomaly.model

import java.sql.Timestamp

/**
 * Represents statistical metrics for a specific day of week
 * Used for calculating expected ranges and detecting anomalies
 *
 * @param dayOfWeek Day of week (1=Monday, 7=Sunday)
 * @param meanCount Average user count for this day of week
 * @param stdDev Standard deviation of user counts
 * @param q1 First quartile (25th percentile)
 * @param median Median value (50th percentile)
 * @param q3 Third quartile (75th percentile)
 * @param iqr Interquartile range (Q3 - Q1)
 * @param lowerBound Lower bound for anomaly detection (Q1 - 1.5*IQR)
 * @param upperBound Upper bound for anomaly detection (Q3 + 1.5*IQR)
 * @param sampleSize Number of historical data points used
 * @param lastUpdated Timestamp when statistics were last calculated
 */
case class MetricStatistics(
  dayOfWeek: Int,
  meanCount: Double,
  stdDev: Double,
  q1: Double,
  median: Double,
  q3: Double,
  iqr: Double,
  lowerBound: Double,
  upperBound: Double,
  sampleSize: Long,
  lastUpdated: Timestamp
)

object MetricStatistics {
  /**
   * Calculate statistics from a sequence of values
   *
   * @param dayOfWeek The day of week for these statistics
   * @param values Historical user counts for this day of week
   * @param iqrMultiplier Multiplier for IQR bounds calculation (default 1.5)
   * @return MetricStatistics object with calculated values
   */
  def calculate(
    dayOfWeek: Int,
    values: Seq[Double],
    iqrMultiplier: Double = 1.5
  ): MetricStatistics = {
    require(values.nonEmpty, "Cannot calculate statistics from empty values")

    val sorted = values.sorted
    val n = values.length
    val mean = values.sum / n
    val variance = values.map(v => math.pow(v - mean, 2)).sum / n
    val stdDev = math.sqrt(variance)

    // Calculate percentiles
    val q1 = percentile(sorted, 0.25)
    val median = percentile(sorted, 0.50)
    val q3 = percentile(sorted, 0.75)
    val iqr = q3 - q1

    // Calculate bounds using IQR method
    val lowerBound = q1 - (iqrMultiplier * iqr)
    val upperBound = q3 + (iqrMultiplier * iqr)

    MetricStatistics(
      dayOfWeek = dayOfWeek,
      meanCount = mean,
      stdDev = stdDev,
      q1 = q1,
      median = median,
      q3 = q3,
      iqr = iqr,
      lowerBound = lowerBound,
      upperBound = upperBound,
      sampleSize = n,
      lastUpdated = new Timestamp(System.currentTimeMillis())
    )
  }

  /**
   * Calculate percentile from sorted values
   *
   * @param sortedValues Sorted sequence of values
   * @param p Percentile (0.0 to 1.0)
   * @return Calculated percentile value
   */
  private def percentile(sortedValues: Seq[Double], p: Double): Double = {
    require(p >= 0.0 && p <= 1.0, "Percentile must be between 0 and 1")

    val n = sortedValues.length
    val index = p * (n - 1)
    val lower = index.floor.toInt
    val upper = index.ceil.toInt
    val weight = index - lower

    if (lower == upper) {
      sortedValues(lower)
    } else {
      sortedValues(lower) * (1 - weight) + sortedValues(upper) * weight
    }
  }
}
