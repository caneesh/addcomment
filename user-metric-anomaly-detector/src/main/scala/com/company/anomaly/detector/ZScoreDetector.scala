package com.company.anomaly.detector

import com.company.anomaly.model.{AnomalyResult, MetricStatistics}
import java.sql.Date

/**
 * Anomaly detector using the Z-Score (Standard Score) method
 *
 * The Z-Score method identifies outliers based on standard deviations from the mean.
 * - Z-Score = (value - mean) / standard_deviation
 * - Typically, |Z-Score| > 3 indicates an outlier (99.7% confidence)
 * - |Z-Score| > 2 indicates 95% confidence
 *
 * This method assumes the data follows a normal distribution.
 *
 * @param zScoreThreshold Threshold for Z-score (default 3.0)
 */
class ZScoreDetector(zScoreThreshold: Double = 3.0) extends AnomalyDetector {

  require(zScoreThreshold > 0, "Z-score threshold must be positive")

  override def methodName: String = "Z-Score"

  /**
   * Detect anomalies using Z-Score method
   *
   * @param metricDate The date being analyzed
   * @param actualValue The actual user count for the date
   * @param statistics Historical statistics for the same day of week
   * @return AnomalyResult with detection outcome
   */
  override def detect(
    metricDate: Date,
    actualValue: Long,
    statistics: MetricStatistics
  ): AnomalyResult = {

    val mean = statistics.meanCount
    val stdDev = statistics.stdDev

    // Calculate Z-Score
    val zScore = if (stdDev > 0) {
      math.abs((actualValue - mean) / stdDev)
    } else {
      // If standard deviation is 0, all values are identical
      // Any different value is an extreme outlier
      if (actualValue == mean) 0.0 else Double.PositiveInfinity
    }

    // Determine if value is an anomaly
    val isAnomaly = zScore > zScoreThreshold

    // Calculate bounds based on Z-score threshold
    val lowerBound = mean - (zScoreThreshold * stdDev)
    val upperBound = mean + (zScoreThreshold * stdDev)

    // Anomaly score is the Z-score itself
    val anomalyScore = zScore

    // Additional context information
    val info = Some(s"""
      |{
      |  "mean": $mean,
      |  "std_dev": $stdDev,
      |  "z_score": $zScore,
      |  "z_score_threshold": $zScoreThreshold,
      |  "sample_size": ${statistics.sampleSize}
      |}
      |""".stripMargin.replaceAll("\n", "").replaceAll("\\s+", " "))

    createResult(
      metricDate = metricDate,
      actualValue = actualValue,
      minBound = lowerBound,
      maxBound = upperBound,
      score = anomalyScore,
      isAnomaly = isAnomaly,
      additionalInfo = info
    )
  }
}

object ZScoreDetector {
  /**
   * Factory method to create ZScoreDetector with custom threshold
   * @param zScoreThreshold Custom threshold (default 3.0)
   * @return New ZScoreDetector instance
   */
  def apply(zScoreThreshold: Double = 3.0): ZScoreDetector =
    new ZScoreDetector(zScoreThreshold)
}
