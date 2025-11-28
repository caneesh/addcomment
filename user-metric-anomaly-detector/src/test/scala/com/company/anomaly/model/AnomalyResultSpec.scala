package com.company.anomaly.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.sql.{Date, Timestamp}
import java.time.LocalDate

class AnomalyResultSpec extends AnyFlatSpec with Matchers {

  val testDate: Date = Date.valueOf("2025-11-27")
  val testTimestamp: Timestamp = new Timestamp(System.currentTimeMillis())

  "AnomalyResult.severity" should "return NORMAL when not anomaly" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 5000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = false,
      anomalyScore = 0.0,
      detectionMethod = "IQR",
      detectionTimestamp = testTimestamp
    )

    result.severity shouldBe "NORMAL"
  }

  it should "return LOW for score < 2.0" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 6000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = true,
      anomalyScore = 1.5,
      detectionMethod = "IQR",
      detectionTimestamp = testTimestamp
    )

    result.severity shouldBe "LOW"
  }

  it should "return MEDIUM for score between 2.0 and 3.0" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 6000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = true,
      anomalyScore = 2.5,
      detectionMethod = "Z-Score",
      detectionTimestamp = testTimestamp
    )

    result.severity shouldBe "MEDIUM"
  }

  it should "return HIGH for score between 3.0 and 5.0" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 7000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = true,
      anomalyScore = 4.0,
      detectionMethod = "Z-Score",
      detectionTimestamp = testTimestamp
    )

    result.severity shouldBe "HIGH"
  }

  it should "return CRITICAL for score >= 5.0" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 10000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = true,
      anomalyScore = 6.0,
      detectionMethod = "Z-Score",
      detectionTimestamp = testTimestamp
    )

    result.severity shouldBe "CRITICAL"
  }

  "AnomalyResult.percentageDeviation" should "return 0 when within range" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 5000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = false,
      anomalyScore = 0.0,
      detectionMethod = "IQR",
      detectionTimestamp = testTimestamp
    )

    result.percentageDeviation shouldBe 0.0
  }

  it should "calculate positive deviation when above range" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 6000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5000.0,
      isAnomaly = true,
      anomalyScore = 2.0,
      detectionMethod = "IQR",
      detectionTimestamp = testTimestamp
    )

    result.percentageDeviation shouldBe 20.0 // (6000 - 5000) / 5000 * 100
  }

  it should "calculate negative deviation when below range" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 3000,
      expectedRangeMin = 4000.0,
      expectedRangeMax = 5000.0,
      isAnomaly = true,
      anomalyScore = 2.0,
      detectionMethod = "IQR",
      detectionTimestamp = testTimestamp
    )

    result.percentageDeviation shouldBe -25.0 // (3000 - 4000) / 4000 * 100
  }

  "AnomalyResult.toSummary" should "generate readable summary" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 6000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = true,
      anomalyScore = 2.5,
      detectionMethod = "IQR",
      detectionTimestamp = testTimestamp
    )

    val summary = result.toSummary

    summary should include("2025-11-27")
    summary should include("6000")
    summary should include("true")
    summary should include("MEDIUM")
    summary should include("IQR")
  }

  "AnomalyResult.getPartitionValues" should "extract correct year and month" in {
    val date = Date.valueOf("2025-11-27")
    val (year, month) = AnomalyResult.getPartitionValues(date)

    year shouldBe 2025
    month shouldBe 11
  }

  it should "handle January correctly" in {
    val date = Date.valueOf("2025-01-15")
    val (year, month) = AnomalyResult.getPartitionValues(date)

    year shouldBe 2025
    month shouldBe 1
  }

  it should "handle December correctly" in {
    val date = Date.valueOf("2024-12-31")
    val (year, month) = AnomalyResult.getPartitionValues(date)

    year shouldBe 2024
    month shouldBe 12
  }

  "AnomalyResult.toHtml" should "generate valid HTML" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 6000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = true,
      anomalyScore = 2.5,
      detectionMethod = "IQR",
      detectionTimestamp = testTimestamp
    )

    val html = AnomalyResult.toHtml(result)

    html should include("IQR")
    html should include("MEDIUM")
    html should include("2.50")
    html should include("<div")
    html should include("</div>")
  }

  it should "use correct color for CRITICAL severity" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 10000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = true,
      anomalyScore = 6.0,
      detectionMethod = "Z-Score",
      detectionTimestamp = testTimestamp
    )

    val html = AnomalyResult.toHtml(result)

    html should include("#d32f2f") // CRITICAL color
  }

  it should "use correct color for LOW severity" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 5600,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = true,
      anomalyScore = 1.5,
      detectionMethod = "IQR",
      detectionTimestamp = testTimestamp
    )

    val html = AnomalyResult.toHtml(result)

    html should include("#388e3c") // LOW color
  }

  "AnomalyResult" should "support optional additional info" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 5000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = false,
      anomalyScore = 0.0,
      detectionMethod = "IQR",
      detectionTimestamp = testTimestamp,
      additionalInfo = Some("{\"sample_size\": 100}")
    )

    result.additionalInfo shouldBe defined
    result.additionalInfo.get should include("sample_size")
  }

  it should "handle None for additional info" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 5000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = false,
      anomalyScore = 0.0,
      detectionMethod = "IQR",
      detectionTimestamp = testTimestamp
    )

    result.additionalInfo shouldBe None
  }

  "Edge cases" should "handle very large anomaly scores" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 100000,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = true,
      anomalyScore = 100.0,
      detectionMethod = "Z-Score",
      detectionTimestamp = testTimestamp
    )

    result.severity shouldBe "CRITICAL"
    result.percentageDeviation should be > 0.0
  }

  it should "handle zero actual count" in {
    val result = AnomalyResult(
      metricDate = testDate,
      actualCount = 0,
      expectedRangeMin = 4500.0,
      expectedRangeMax = 5500.0,
      isAnomaly = true,
      anomalyScore = 5.0,
      detectionMethod = "IQR",
      detectionTimestamp = testTimestamp
    )

    result.percentageDeviation should be < 0.0
  }
}
