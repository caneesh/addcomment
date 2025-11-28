package com.company.anomaly.detector

import com.company.anomaly.model.MetricStatistics
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.sql.{Date, Timestamp}

class ZScoreDetectorSpec extends AnyFlatSpec with Matchers {

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

  "ZScoreDetector" should "have correct method name" in {
    val detector = new ZScoreDetector()
    detector.methodName shouldBe "Z-Score"
  }

  it should "not detect anomaly when value is within threshold" in {
    val detector = new ZScoreDetector(zScoreThreshold = 3.0)
    val stats = createTestStatistics(mean = 5000.0, stdDev = 500.0)
    // Z-score = (5500 - 5000) / 500 = 1.0
    val result = detector.detect(testDate, 5500, stats)

    result.isAnomaly shouldBe false
    result.anomalyScore shouldBe 1.0
  }

  it should "detect anomaly when value exceeds positive threshold" in {
    val detector = new ZScoreDetector(zScoreThreshold = 3.0)
    val stats = createTestStatistics(mean = 5000.0, stdDev = 500.0)
    // Z-score = (7000 - 5000) / 500 = 4.0
    val result = detector.detect(testDate, 7000, stats)

    result.isAnomaly shouldBe true
    result.anomalyScore shouldBe 4.0
  }

  it should "detect anomaly when value exceeds negative threshold" in {
    val detector = new ZScoreDetector(zScoreThreshold = 3.0)
    val stats = createTestStatistics(mean = 5000.0, stdDev = 500.0)
    // Z-score = |3000 - 5000| / 500 = 4.0
    val result = detector.detect(testDate, 3000, stats)

    result.isAnomaly shouldBe true
    result.anomalyScore shouldBe 4.0
  }

  it should "calculate correct Z-score" in {
    val detector = new ZScoreDetector()
    val stats = createTestStatistics(mean = 1000.0, stdDev = 100.0)
    // Z-score = |1300 - 1000| / 100 = 3.0
    val result = detector.detect(testDate, 1300, stats)

    result.anomalyScore shouldBe 3.0
  }

  it should "use custom Z-score threshold" in {
    val detector = new ZScoreDetector(zScoreThreshold = 2.0)
    val stats = createTestStatistics(mean = 5000.0, stdDev = 500.0)

    // Z-score = 2.5, should be anomaly with threshold 2.0
    val result1 = detector.detect(testDate, 6250, stats)
    result1.isAnomaly shouldBe true

    // Z-score = 1.5, should not be anomaly with threshold 2.0
    val result2 = detector.detect(testDate, 5750, stats)
    result2.isAnomaly shouldBe false
  }

  it should "calculate correct bounds based on threshold" in {
    val detector = new ZScoreDetector(zScoreThreshold = 3.0)
    val stats = createTestStatistics(mean = 5000.0, stdDev = 500.0)
    val result = detector.detect(testDate, 5000, stats)

    result.expectedRangeMin shouldBe 3500.0 // mean - 3 * stdDev
    result.expectedRangeMax shouldBe 6500.0 // mean + 3 * stdDev
  }

  it should "handle zero standard deviation" in {
    val detector = new ZScoreDetector()
    val stats = createTestStatistics(mean = 5000.0, stdDev = 0.0)

    // When stdDev = 0, same value should have z-score = 0
    val result1 = detector.detect(testDate, 5000, stats)
    result1.isAnomaly shouldBe false
    result1.anomalyScore shouldBe 0.0

    // Different value should be extreme outlier (infinite z-score)
    val result2 = detector.detect(testDate, 5001, stats)
    result2.isAnomaly shouldBe true
    result2.anomalyScore shouldBe Double.PositiveInfinity
  }

  it should "handle edge case when value equals mean" in {
    val detector = new ZScoreDetector()
    val stats = createTestStatistics(mean = 5000.0, stdDev = 500.0)
    val result = detector.detect(testDate, 5000, stats)

    result.isAnomaly shouldBe false
    result.anomalyScore shouldBe 0.0
  }

  it should "handle edge case when value at exact threshold" in {
    val detector = new ZScoreDetector(zScoreThreshold = 3.0)
    val stats = createTestStatistics(mean = 5000.0, stdDev = 500.0)
    // Exactly 3 std devs away
    val result = detector.detect(testDate, 6500, stats)

    result.isAnomaly shouldBe false // threshold is exclusive (>)
  }

  it should "include additional info in result" in {
    val detector = new ZScoreDetector()
    val stats = createTestStatistics()
    val result = detector.detect(testDate, 5000, stats)

    result.additionalInfo shouldBe defined
    result.additionalInfo.get should include("mean")
    result.additionalInfo.get should include("std_dev")
    result.additionalInfo.get should include("z_score")
    result.additionalInfo.get should include("sample_size")
  }

  it should "use absolute value of Z-score" in {
    val detector = new ZScoreDetector(zScoreThreshold = 3.0)
    val stats = createTestStatistics(mean = 5000.0, stdDev = 500.0)

    val resultAbove = detector.detect(testDate, 7000, stats) // +4.0
    val resultBelow = detector.detect(testDate, 3000, stats) // -4.0

    resultAbove.anomalyScore shouldBe resultBelow.anomalyScore
    resultAbove.isAnomaly shouldBe resultBelow.isAnomaly
  }

  it should "set correct timestamp" in {
    val detector = new ZScoreDetector()
    val stats = createTestStatistics()
    val before = System.currentTimeMillis()
    val result = detector.detect(testDate, 5000, stats)
    val after = System.currentTimeMillis()

    result.detectionTimestamp.getTime should (be >= before and be <= after)
  }

  it should "preserve metric date in result" in {
    val detector = new ZScoreDetector()
    val stats = createTestStatistics()
    val result = detector.detect(testDate, 5000, stats)

    result.metricDate shouldBe testDate
  }

  it should "preserve actual count in result" in {
    val detector = new ZScoreDetector()
    val stats = createTestStatistics()
    val result = detector.detect(testDate, 5000, stats)

    result.actualCount shouldBe 5000
  }

  "ZScoreDetector factory method" should "create detector with default threshold" in {
    val detector = ZScoreDetector()
    val stats = createTestStatistics()
    val result = detector.detect(testDate, 5000, stats)

    result.detectionMethod shouldBe "Z-Score"
  }

  it should "create detector with custom threshold" in {
    val detector = ZScoreDetector(zScoreThreshold = 2.5)
    val stats = createTestStatistics(mean = 5000.0, stdDev = 500.0)

    // Z-score = 2.8, should be anomaly with threshold 2.5
    val result = detector.detect(testDate, 6400, stats)
    result.isAnomaly shouldBe true
  }

  "ZScoreDetector validation" should "reject negative threshold" in {
    assertThrows[IllegalArgumentException] {
      new ZScoreDetector(zScoreThreshold = -3.0)
    }
  }

  it should "reject zero threshold" in {
    assertThrows[IllegalArgumentException] {
      new ZScoreDetector(zScoreThreshold = 0.0)
    }
  }

  "Real-world scenarios" should "detect sudden spike in user registrations" in {
    val detector = new ZScoreDetector(zScoreThreshold = 3.0)
    val stats = createTestStatistics(
      mean = 5000.0,
      stdDev = 500.0
    )

    // Sudden spike to 10x normal
    // Z-score = (50000 - 5000) / 500 = 90.0
    val result = detector.detect(testDate, 50000, stats)

    result.isAnomaly shouldBe true
    result.anomalyScore should be > 10.0
  }

  it should "detect significant drop in user registrations" in {
    val detector = new ZScoreDetector(zScoreThreshold = 3.0)
    val stats = createTestStatistics(
      mean = 5000.0,
      stdDev = 500.0
    )

    // Significant drop to 10% of normal
    // Z-score = |(500 - 5000) / 500| = 9.0
    val result = detector.detect(testDate, 500, stats)

    result.isAnomaly shouldBe true
    result.anomalyScore shouldBe 9.0
  }

  it should "not flag small variations as anomalies" in {
    val detector = new ZScoreDetector(zScoreThreshold = 3.0)
    val stats = createTestStatistics(
      mean = 5000.0,
      stdDev = 500.0
    )

    // Small variation (5% increase = 250)
    // Z-score = 250 / 500 = 0.5
    val result = detector.detect(testDate, 5250, stats)

    result.isAnomaly shouldBe false
    result.anomalyScore shouldBe 0.5
  }

  it should "be more sensitive than IQR for normally distributed data" in {
    val detector = new ZScoreDetector(zScoreThreshold = 2.0)
    val stats = createTestStatistics(
      mean = 5000.0,
      stdDev = 500.0
    )

    // Value that's 2.5 std devs away
    val result = detector.detect(testDate, 6250, stats)

    result.isAnomaly shouldBe true
    result.anomalyScore shouldBe 2.5
  }

  "Statistical properties" should "follow 68-95-99.7 rule for normal distribution" in {
    val detector = new ZScoreDetector(zScoreThreshold = 3.0)
    val stats = createTestStatistics(mean = 5000.0, stdDev = 500.0)

    // Within 1 std dev (68%)
    val result1 = detector.detect(testDate, 5500, stats)
    result1.isAnomaly shouldBe false

    // Within 2 std devs (95%)
    val result2 = detector.detect(testDate, 6000, stats)
    result2.isAnomaly shouldBe false

    // Within 3 std devs (99.7%)
    val result3 = detector.detect(testDate, 6500, stats)
    result3.isAnomaly shouldBe false

    // Beyond 3 std devs
    val result4 = detector.detect(testDate, 6501, stats)
    result4.isAnomaly shouldBe true
  }
}
