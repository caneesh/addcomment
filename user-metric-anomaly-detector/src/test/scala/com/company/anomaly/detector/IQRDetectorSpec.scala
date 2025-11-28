package com.company.anomaly.detector

import com.company.anomaly.model.MetricStatistics
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.sql.{Date, Timestamp}

class IQRDetectorSpec extends AnyFlatSpec with Matchers {

  val testDate: Date = Date.valueOf("2025-11-27")
  val testTimestamp: Timestamp = new Timestamp(System.currentTimeMillis())

  def createTestStatistics(
    mean: Double = 5000.0,
    stdDev: Double = 500.0,
    q1: Double = 4500.0,
    median: Double = 5000.0,
    q3: Double = 5500.0
  ): MetricStatistics = {
    val iqr = q3 - q1
    MetricStatistics(
      dayOfWeek = 1,
      meanCount = mean,
      stdDev = stdDev,
      q1 = q1,
      median = median,
      q3 = q3,
      iqr = iqr,
      lowerBound = q1 - 1.5 * iqr,
      upperBound = q3 + 1.5 * iqr,
      sampleSize = 100,
      lastUpdated = testTimestamp
    )
  }

  "IQRDetector" should "have correct method name" in {
    val detector = new IQRDetector()
    detector.methodName shouldBe "IQR"
  }

  it should "not detect anomaly when value is within bounds" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics()
    val result = detector.detect(testDate, 5000, stats)

    result.isAnomaly shouldBe false
    result.anomalyScore shouldBe 0.0
    result.detectionMethod shouldBe "IQR"
  }

  it should "detect anomaly when value is above upper bound" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics(q1 = 4500.0, q3 = 5500.0) // IQR = 1000, upper = 7000
    val result = detector.detect(testDate, 8000, stats)

    result.isAnomaly shouldBe true
    result.anomalyScore should be > 0.0
    result.expectedRangeMax shouldBe 7000.0
  }

  it should "detect anomaly when value is below lower bound" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics(q1 = 4500.0, q3 = 5500.0) // IQR = 1000, lower = 3000
    val result = detector.detect(testDate, 2000, stats)

    result.isAnomaly shouldBe true
    result.anomalyScore should be > 0.0
    result.expectedRangeMin shouldBe 3000.0
  }

  it should "calculate normalized anomaly score" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics(q1 = 4500.0, q3 = 5500.0) // IQR = 1000
    // Upper bound = 5500 + 1500 = 7000
    // Actual = 9000
    // Distance = 2000
    // Score = 2000 / 1000 = 2.0
    val result = detector.detect(testDate, 9000, stats)

    result.anomalyScore shouldBe 2.0
  }

  it should "use custom IQR multiplier" in {
    val detector = new IQRDetector(iqrMultiplier = 3.0)
    val stats = createTestStatistics(q1 = 4000.0, q3 = 6000.0) // IQR = 2000
    // Upper bound with 3.0 multiplier = 6000 + 6000 = 12000

    val result1 = detector.detect(testDate, 10000, stats)
    result1.isAnomaly shouldBe false // Within 3.0 * IQR

    val result2 = detector.detect(testDate, 13000, stats)
    result2.isAnomaly shouldBe true // Beyond 3.0 * IQR
  }

  it should "handle edge case when value equals lower bound" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics(q1 = 4500.0, q3 = 5500.0) // lower = 3000
    val result = detector.detect(testDate, 3000, stats)

    result.isAnomaly shouldBe false
  }

  it should "handle edge case when value equals upper bound" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics(q1 = 4500.0, q3 = 5500.0) // upper = 7000
    val result = detector.detect(testDate, 7000, stats)

    result.isAnomaly shouldBe false
  }

  it should "include additional info in result" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics()
    val result = detector.detect(testDate, 5000, stats)

    result.additionalInfo shouldBe defined
    result.additionalInfo.get should include("q1")
    result.additionalInfo.get should include("q3")
    result.additionalInfo.get should include("iqr")
    result.additionalInfo.get should include("sample_size")
  }

  it should "handle zero IQR gracefully" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics(q1 = 5000.0, q3 = 5000.0) // IQR = 0
    val result = detector.detect(testDate, 6000, stats)

    // When IQR is 0, any different value should be detected
    result.isAnomaly shouldBe true
    result.anomalyScore shouldBe 1000.0 // Distance from bound
  }

  it should "set correct timestamp" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics()
    val before = System.currentTimeMillis()
    val result = detector.detect(testDate, 5000, stats)
    val after = System.currentTimeMillis()

    result.detectionTimestamp.getTime should (be >= before and be <= after)
  }

  it should "preserve metric date in result" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics()
    val result = detector.detect(testDate, 5000, stats)

    result.metricDate shouldBe testDate
  }

  it should "preserve actual count in result" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics()
    val result = detector.detect(testDate, 5000, stats)

    result.actualCount shouldBe 5000
  }

  "IQRDetector factory method" should "create detector with default multiplier" in {
    val detector = IQRDetector()
    val stats = createTestStatistics()
    val result = detector.detect(testDate, 5000, stats)

    result.detectionMethod shouldBe "IQR"
  }

  it should "create detector with custom multiplier" in {
    val detector = IQRDetector(iqrMultiplier = 2.0)
    // Test that custom multiplier is used (indirectly through bounds)
    val stats = createTestStatistics(q1 = 4500.0, q3 = 5500.0)

    // This should not be anomaly with 2.0 multiplier but would be with 1.5
    val result = detector.detect(testDate, 7500, stats)
    result.isAnomaly shouldBe false // Within 2.0 * IQR bounds
  }

  "IQRDetector validation" should "reject negative multiplier" in {
    assertThrows[IllegalArgumentException] {
      new IQRDetector(iqrMultiplier = -1.5)
    }
  }

  it should "reject zero multiplier" in {
    assertThrows[IllegalArgumentException] {
      new IQRDetector(iqrMultiplier = 0.0)
    }
  }

  "Real-world scenarios" should "detect sudden spike in user registrations" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics(
      mean = 5000.0,
      q1 = 4500.0,
      median = 5000.0,
      q3 = 5500.0
    )

    // Sudden spike to 10x normal
    val result = detector.detect(testDate, 50000, stats)

    result.isAnomaly shouldBe true
    result.anomalyScore should be > 5.0
  }

  it should "detect significant drop in user registrations" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics(
      mean = 5000.0,
      q1 = 4500.0,
      median = 5000.0,
      q3 = 5500.0
    )

    // Significant drop to 10% of normal
    val result = detector.detect(testDate, 500, stats)

    result.isAnomaly shouldBe true
    result.anomalyScore should be > 2.0
  }

  it should "not flag small variations as anomalies" in {
    val detector = new IQRDetector()
    val stats = createTestStatistics(
      mean = 5000.0,
      q1 = 4500.0,
      median = 5000.0,
      q3 = 5500.0
    )

    // Small variation (5% increase)
    val result = detector.detect(testDate, 5250, stats)

    result.isAnomaly shouldBe false
  }
}
