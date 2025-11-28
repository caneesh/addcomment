package com.company.anomaly.model

import java.sql.{Date, Timestamp}

/**
 * Represents the result of anomaly detection for a specific metric date
 *
 * @param metricDate The date being analyzed
 * @param actualCount The actual user count for this date
 * @param expectedRangeMin Lower bound of expected range
 * @param expectedRangeMax Upper bound of expected range
 * @param isAnomaly Whether this value is considered anomalous
 * @param anomalyScore Numeric score indicating degree of anomaly (higher = more anomalous)
 * @param detectionMethod Method used for detection (e.g., "IQR", "Z-Score")
 * @param detectionTimestamp When the detection was performed
 * @param additionalInfo Optional JSON string with additional context
 */
case class AnomalyResult(
  metricDate: Date,
  actualCount: Long,
  expectedRangeMin: Double,
  expectedRangeMax: Double,
  isAnomaly: Boolean,
  anomalyScore: Double,
  detectionMethod: String,
  detectionTimestamp: Timestamp,
  additionalInfo: Option[String] = None
) {
  /**
   * Get severity level based on anomaly score
   * @return Severity level: "LOW", "MEDIUM", "HIGH", or "CRITICAL"
   */
  def severity: String = {
    if (!isAnomaly) "NORMAL"
    else if (anomalyScore < 2.0) "LOW"
    else if (anomalyScore < 3.0) "MEDIUM"
    else if (anomalyScore < 5.0) "HIGH"
    else "CRITICAL"
  }

  /**
   * Get percentage deviation from expected range
   * @return Percentage deviation (positive if above range, negative if below)
   */
  def percentageDeviation: Double = {
    if (actualCount < expectedRangeMin) {
      ((actualCount - expectedRangeMin) / expectedRangeMin) * 100
    } else if (actualCount > expectedRangeMax) {
      ((actualCount - expectedRangeMax) / expectedRangeMax) * 100
    } else {
      0.0
    }
  }

  /**
   * Format anomaly result as human-readable string
   * @return Formatted string representation
   */
  def toSummary: String = {
    s"""
       |Metric Date: $metricDate
       |Actual Count: $actualCount
       |Expected Range: [${expectedRangeMin.toInt}, ${expectedRangeMax.toInt}]
       |Is Anomaly: $isAnomaly
       |Severity: $severity
       |Anomaly Score: ${f"$anomalyScore%.2f"}
       |Detection Method: $detectionMethod
       |Deviation: ${f"$percentageDeviation%.2f"}%
       |""".stripMargin
  }
}

object AnomalyResult {
  /**
   * Convert partition values to year and month
   * @param metricDate The date to convert
   * @return Tuple of (year, month)
   */
  def getPartitionValues(metricDate: Date): (Int, Int) = {
    val calendar = java.util.Calendar.getInstance()
    calendar.setTime(metricDate)
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH) + 1 // Calendar months are 0-indexed
    (year, month)
  }

  /**
   * Create HTML representation of anomaly result for email alerts
   * @param result The anomaly result to format
   * @return HTML string
   */
  def toHtml(result: AnomalyResult): String = {
    val severityColor = result.severity match {
      case "CRITICAL" => "#d32f2f"
      case "HIGH" => "#f57c00"
      case "MEDIUM" => "#fbc02d"
      case "LOW" => "#388e3c"
      case _ => "#757575"
    }

    s"""
       |<div style="border-left: 4px solid $severityColor; padding-left: 12px; margin: 10px 0;">
       |  <h3 style="color: $severityColor; margin: 5px 0;">
       |    ${result.detectionMethod} - ${result.severity}
       |  </h3>
       |  <p><strong>Anomaly Score:</strong> ${f"${result.anomalyScore}%.2f"}</p>
       |  <p><strong>Deviation:</strong> ${f"${result.percentageDeviation}%.2f"}%</p>
       |</div>
       |""".stripMargin
  }
}
