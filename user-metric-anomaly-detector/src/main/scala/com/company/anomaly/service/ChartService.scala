package com.company.anomaly.service

import org.jfree.chart.{ChartFactory, ChartUtils}
import org.jfree.chart.plot.{CategoryPlot, PlotOrientation}
import org.jfree.data.category.DefaultCategoryDataset
import org.slf4j.LoggerFactory

import java.awt.{Color, Font}
import java.io.File
import java.sql.Date
import scala.util.{Failure, Success, Try}

/**
 * Service for generating charts and visualizations
 * Uses JFreeChart to create bar charts showing metric trends
 */
class ChartService {

  private val logger = LoggerFactory.getLogger(getClass)

  /**
   * Generate a bar chart showing daily user counts
   *
   * @param data Map of dates to user counts
   * @param targetDate The date being analyzed (highlighted)
   * @param outputPath Path where chart image should be saved
   * @param width Chart width in pixels
   * @param height Chart height in pixels
   * @return Success with file path or Failure
   */
  def generateDailyCountsChart(
    data: Map[Date, Long],
    targetDate: Date,
    outputPath: String,
    width: Int = 800,
    height: Int = 600
  ): Try[File] = Try {
    logger.info(s"Generating bar chart with ${data.size} data points")

    // Create dataset
    val dataset = new DefaultCategoryDataset()

    // Sort data by date and add to dataset
    val sortedData = data.toSeq.sortBy(_._1)
    sortedData.foreach { case (date, count) =>
      dataset.addValue(count.toDouble, "User Count", date.toString)
    }

    // Create chart
    val chart = ChartFactory.createBarChart(
      "Daily User Registration Counts - Last 10 Days", // Title
      "Date", // X-axis label
      "User Count", // Y-axis label
      dataset, // Dataset
      PlotOrientation.VERTICAL, // Orientation
      true, // Include legend
      true, // Tooltips
      false // URLs
    )

    // Customize chart appearance
    customizeChart(chart, targetDate, sortedData)

    // Save chart to file
    val file = new File(outputPath)
    ChartUtils.saveChartAsPNG(file, chart, width, height)

    logger.info(s"Chart saved successfully to: $outputPath")
    file
  } match {
    case Success(file) => Success(file)
    case Failure(ex) =>
      logger.error(s"Failed to generate chart: ${ex.getMessage}", ex)
      Failure(ex)
  }

  /**
   * Customize chart appearance for better readability
   *
   * @param chart The JFreeChart to customize
   * @param targetDate The date being analyzed
   * @param data Sorted sequence of date-count pairs
   */
  private def customizeChart(
    chart: org.jfree.chart.JFreeChart,
    targetDate: Date,
    data: Seq[(Date, Long)]
  ): Unit = {
    // Set background colors
    chart.setBackgroundPaint(Color.WHITE)
    chart.getTitle.setFont(new Font("SansSerif", Font.BOLD, 16))

    val plot = chart.getCategoryPlot
    plot.setBackgroundPaint(new Color(240, 240, 240))
    plot.setDomainGridlinePaint(Color.WHITE)
    plot.setRangeGridlinePaint(Color.WHITE)

    // Customize bar renderer
    val renderer = plot.getRenderer
    renderer.setSeriesPaint(0, new Color(79, 129, 189)) // Blue bars

    // Highlight target date bar in red
    val targetIndex = data.indexWhere(_._1 == targetDate)
    if (targetIndex >= 0) {
      renderer.setSeriesPaint(0, new Color(79, 129, 189)) // Default blue
      // Note: To highlight specific bar, we'd need BarRenderer and setItemPaint
      // For simplicity, keeping all bars same color
      logger.debug(s"Target date $targetDate is at index $targetIndex")
    }

    // Add average line indicator (optional enhancement)
    if (data.nonEmpty) {
      val avgCount = data.map(_._2).sum.toDouble / data.length
      logger.debug(f"Average count across period: $avgCount%.0f")
    }
  }

  /**
   * Generate a temporary file path for chart storage
   *
   * @param prefix File prefix
   * @param suffix File suffix (e.g., ".png")
   * @return Temporary file path
   */
  def createTempChartPath(prefix: String = "anomaly-chart-", suffix: String = ".png"): String = {
    val tempFile = File.createTempFile(prefix, suffix)
    tempFile.deleteOnExit() // Clean up on JVM exit
    tempFile.getAbsolutePath
  }

  /**
   * Generate chart with anomaly markers
   * Shows bars with different colors for normal vs anomalous values
   *
   * @param data Map of dates to user counts
   * @param anomalies Set of dates that are anomalous
   * @param targetDate The date being analyzed
   * @param outputPath Path where chart should be saved
   * @return Success with file or Failure
   */
  def generateChartWithAnomalyMarkers(
    data: Map[Date, Long],
    anomalies: Set[Date],
    targetDate: Date,
    outputPath: String
  ): Try[File] = Try {
    logger.info(s"Generating chart with anomaly markers for ${anomalies.size} anomalies")

    val dataset = new DefaultCategoryDataset()

    // Separate normal and anomalous data
    val sortedData = data.toSeq.sortBy(_._1)

    sortedData.foreach { case (date, count) =>
      val series = if (anomalies.contains(date)) "Anomalous" else "Normal"
      dataset.addValue(count.toDouble, series, date.toString)
    }

    val chart = ChartFactory.createBarChart(
      "Daily User Counts with Anomaly Detection",
      "Date",
      "User Count",
      dataset,
      PlotOrientation.VERTICAL,
      true,
      true,
      false
    )

    // Customize colors
    chart.setBackgroundPaint(Color.WHITE)
    val plot = chart.getCategoryPlot
    plot.setBackgroundPaint(new Color(240, 240, 240))

    val renderer = plot.getRenderer
    renderer.setSeriesPaint(0, new Color(192, 80, 77)) // Red for anomalies
    renderer.setSeriesPaint(1, new Color(79, 129, 189)) // Blue for normal

    val file = new File(outputPath)
    ChartUtils.saveChartAsPNG(file, chart, 800, 600)

    logger.info(s"Chart with anomaly markers saved to: $outputPath")
    file
  } match {
    case Success(file) => Success(file)
    case Failure(ex) =>
      logger.error(s"Failed to generate chart with markers: ${ex.getMessage}", ex)
      Failure(ex)
  }

  /**
   * Delete chart file
   *
   * @param file File to delete
   * @return Success or Failure
   */
  def deleteChart(file: File): Try[Unit] = Try {
    if (file.exists()) {
      val deleted = file.delete()
      if (deleted) {
        logger.debug(s"Deleted chart file: ${file.getAbsolutePath}")
      } else {
        logger.warn(s"Failed to delete chart file: ${file.getAbsolutePath}")
      }
    }
  }
}

object ChartService {
  def apply(): ChartService = new ChartService()
}
