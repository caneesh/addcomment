package com.company.anomaly.detector

import com.company.anomaly.model.{AnomalyResult, MetricStatistics}
import java.sql.Date

/**
 * Anomaly detector using the Interquartile Range (IQR) method
 *
 * The IQR method identifies outliers based on the spread of the middle 50% of data.
 * - IQR = Q3 - Q1
 * - Lower Bound = Q1 - (multiplier * IQR)
 * - Upper Bound = Q3 + (multiplier * IQR)
 * - Typical multiplier is 1.5 for outliers, 3.0 for extreme outliers
 *
 * Anomaly score is calculated as the distance from the nearest bound,
 * normalized by the IQR (distance / IQR).
 *
 * @param iqrMultiplier Multiplier for IQR bounds (default 1.5)
 */
class IQRDetector(iqrMultiplier: Double = 1.5) extends AnomalyDetector {

  require(iqrMultiplier > 0, "IQR multiplier must be positive")

  override def methodName: String = "IQR"

  /**
   * Detect anomalies using IQR method
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

    // Use pre-calculated bounds from statistics
    val lowerBound = statistics.lowerBound
    val upperBound = statistics.upperBound
    val iqr = statistics.iqr

    // Determine if value is an anomaly
    val isAnomaly = actualValue < lowerBound || actualValue > upperBound

    // Calculate anomaly score as normalized distance from bounds
    val anomalyScore = if (!isAnomaly) {
      0.0
    } else {
      val distanceFromBound = if (actualValue < lowerBound) {
        lowerBound - actualValue
      } else {
        actualValue - upperBound
      }
      // Normalize by IQR to get a dimensionless score
      // Avoid division by zero
      if (iqr > 0) distanceFromBound / iqr else distanceFromBound
    }

    // Additional context information
    val info = Some(s"""
      |{
      |  "q1": ${statistics.q1},
      |  "median": ${statistics.median},
      |  "q3": ${statistics.q3},
      |  "iqr": ${statistics.iqr},
      |  "iqr_multiplier": $iqrMultiplier,
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

object IQRDetector {
  /**
   * Factory method to create IQRDetector with custom multiplier
   * @param iqrMultiplier Custom multiplier (default 1.5)
   * @return New IQRDetector instance
   */
  def apply(iqrMultiplier: Double = 1.5): IQRDetector =
    new IQRDetector(iqrMultiplier)
}
