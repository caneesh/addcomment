package com.company.anomaly.detector

import com.company.anomaly.model.{AnomalyResult, MetricStatistics}
import java.sql.{Date, Timestamp}

/**
 * Base trait for anomaly detection algorithms
 * Implementations should provide specific detection logic
 */
trait AnomalyDetector {
  /**
   * Name of the detection method
   */
  def methodName: String

  /**
   * Detect if a value is anomalous based on historical statistics
   *
   * @param metricDate The date being analyzed
   * @param actualValue The actual user count for the date
   * @param statistics Historical statistics for the same day of week
   * @return AnomalyResult containing detection results
   */
  def detect(
    metricDate: Date,
    actualValue: Long,
    statistics: MetricStatistics
  ): AnomalyResult

  /**
   * Common helper to create AnomalyResult
   */
  protected def createResult(
    metricDate: Date,
    actualValue: Long,
    minBound: Double,
    maxBound: Double,
    score: Double,
    isAnomaly: Boolean,
    additionalInfo: Option[String] = None
  ): AnomalyResult = {
    AnomalyResult(
      metricDate = metricDate,
      actualCount = actualValue,
      expectedRangeMin = minBound,
      expectedRangeMax = maxBound,
      isAnomaly = isAnomaly,
      anomalyScore = score,
      detectionMethod = methodName,
      detectionTimestamp = new Timestamp(System.currentTimeMillis()),
      additionalInfo = additionalInfo
    )
  }
}
