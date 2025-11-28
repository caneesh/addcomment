package com.company.anomaly.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.sql.Timestamp

class MetricStatisticsSpec extends AnyFlatSpec with Matchers {

  "MetricStatistics.calculate" should "calculate correct statistics for a simple dataset" in {
    val values = Seq(10.0, 20.0, 30.0, 40.0, 50.0)
    val stats = MetricStatistics.calculate(1, values, iqrMultiplier = 1.5)

    stats.dayOfWeek shouldBe 1
    stats.meanCount shouldBe 30.0
    stats.median shouldBe 30.0
    stats.q1 shouldBe 20.0
    stats.q3 shouldBe 40.0
    stats.iqr shouldBe 20.0
    stats.lowerBound shouldBe -10.0 // Q1 - 1.5 * IQR = 20 - 30
    stats.upperBound shouldBe 70.0  // Q3 + 1.5 * IQR = 40 + 30
    stats.sampleSize shouldBe 5
  }

  it should "calculate correct standard deviation" in {
    val values = Seq(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
    val stats = MetricStatistics.calculate(2, values)

    stats.meanCount shouldBe 5.0
    stats.stdDev should be(2.0 +- 0.01)
  }

  it should "handle single value correctly" in {
    val values = Seq(100.0)
    val stats = MetricStatistics.calculate(3, values)

    stats.meanCount shouldBe 100.0
    stats.stdDev shouldBe 0.0
    stats.median shouldBe 100.0
    stats.q1 shouldBe 100.0
    stats.q3 shouldBe 100.0
    stats.iqr shouldBe 0.0
  }

  it should "calculate correct percentiles for even-sized dataset" in {
    val values = Seq(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0)
    val stats = MetricStatistics.calculate(4, values)

    stats.median shouldBe 4.5
    stats.q1 shouldBe 2.5
    stats.q3 shouldBe 6.5
  }

  it should "calculate correct percentiles for odd-sized dataset" in {
    val values = Seq(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0)
    val stats = MetricStatistics.calculate(5, values)

    stats.median shouldBe 4.0
    stats.q1 shouldBe 2.0
    stats.q3 shouldBe 6.0
  }

  it should "use custom IQR multiplier" in {
    val values = Seq(10.0, 20.0, 30.0, 40.0, 50.0)
    val stats = MetricStatistics.calculate(6, values, iqrMultiplier = 3.0)

    stats.iqr shouldBe 20.0
    stats.lowerBound shouldBe -40.0 // Q1 - 3.0 * IQR = 20 - 60
    stats.upperBound shouldBe 100.0 // Q3 + 3.0 * IQR = 40 + 60
  }

  it should "handle realistic user count data" in {
    val values = Seq(
      4500.0, 4800.0, 5000.0, 5200.0, 5100.0,
      4900.0, 5300.0, 5150.0, 4850.0, 5050.0
    )
    val stats = MetricStatistics.calculate(7, values)

    stats.meanCount shouldBe 5095.0
    stats.median should be(5050.0 +- 0.1)
    stats.sampleSize shouldBe 10
    stats.stdDev should be > 0.0
  }

  it should "throw exception for empty values" in {
    assertThrows[IllegalArgumentException] {
      MetricStatistics.calculate(1, Seq.empty)
    }
  }

  it should "set correct timestamp" in {
    val before = System.currentTimeMillis()
    val stats = MetricStatistics.calculate(1, Seq(10.0, 20.0, 30.0))
    val after = System.currentTimeMillis()

    stats.lastUpdated.getTime should (be >= before and be <= after)
  }

  "MetricStatistics" should "have correct field types" in {
    val stats = MetricStatistics(
      dayOfWeek = 1,
      meanCount = 100.0,
      stdDev = 10.0,
      q1 = 90.0,
      median = 100.0,
      q3 = 110.0,
      iqr = 20.0,
      lowerBound = 70.0,
      upperBound = 130.0,
      sampleSize = 100,
      lastUpdated = new Timestamp(System.currentTimeMillis())
    )

    stats.dayOfWeek shouldBe a[Int]
    stats.meanCount shouldBe a[Double]
    stats.stdDev shouldBe a[Double]
    stats.sampleSize shouldBe a[Long]
    stats.lastUpdated shouldBe a[Timestamp]
  }

  "percentile calculation" should "handle boundary cases" in {
    val values = Seq(1.0, 2.0, 3.0, 4.0, 5.0)
    val stats = MetricStatistics.calculate(1, values)

    // 0th percentile (min)
    stats.q1 should be < stats.median
    // 100th percentile (max)
    stats.q3 should be > stats.median
  }

  it should "be monotonic" in {
    val values = Seq(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    val stats = MetricStatistics.calculate(1, values)

    stats.q1 should be < stats.median
    stats.median should be < stats.q3
  }
}
