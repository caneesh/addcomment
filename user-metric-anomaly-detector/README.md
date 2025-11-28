# User Metric Anomaly Detector

A production-ready Spark Scala application for detecting anomalies in daily user registration metrics using statistical methods.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [Deployment](#deployment)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)
- [Development](#development)
- [FAQ](#faq)
- [Contributing](#contributing)

## Overview

The User Metric Anomaly Detector is a Spark-based application designed to monitor daily user registration counts and detect abnormal patterns using historical data. It employs multiple statistical detection methods and sends email alerts when anomalies are identified.

### Key Capabilities

- **Multiple Detection Algorithms**: IQR (Interquartile Range) and Z-Score methods
- **Day-of-Week Awareness**: Compares metrics against same day-of-week historical data
- **Configurable Lookback**: Supports 1-4 years of historical data for pattern analysis
- **Visual Analytics**: Auto-generated bar charts showing 10-day trend analysis
- **Automated Alerts**: HTML email notifications with chart attachments
- **Production-Ready**: Robust error handling, logging, and Hive integration

## Features

### Detection Methods

#### 1. IQR (Interquartile Range) Method
- Calculates Q1, Q3, and IQR from historical data
- Identifies outliers beyond [Q1 - 1.5*IQR, Q3 + 1.5*IQR]
- Robust to extreme values
- Returns normalized anomaly score

#### 2. Z-Score Method
- Calculates mean and standard deviation
- Flags values beyond 3 standard deviations
- Assumes normal distribution
- Returns Z-score as anomaly score

### Email Alerts

- HTML formatted emails with severity indicators
- Color-coded severity levels (Low, Medium, High, Critical)
- **Bar chart attachment** showing last 10 days of user counts
- Detailed statistics for each detection method
- Direct links to investigation dashboards
- Support for multiple recipients

### Visual Analytics

- **Automatic Chart Generation**: Creates bar charts when anomalies are detected
- **10-Day Trend View**: Shows context with last 10 days of user counts
- **PNG Attachment**: Charts attached to email alerts for easy viewing
- **Temporary File Management**: Auto-cleanup of generated chart files
- **JFreeChart Integration**: High-quality chart rendering

### Data Persistence

- Stores results in Hive partitioned tables
- Maintains historical statistics for faster lookups
- Supports idempotent re-runs
- Efficient partition pruning

## Architecture

```
┌─────────────────┐
│   Hive Tables   │
│  user_metrics   │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────┐
│   MetricService                 │
│  - Load historical data         │
│  - Calculate statistics         │
│  - Save results                 │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│   Anomaly Detectors             │
│  - IQRDetector                  │
│  - ZScoreDetector               │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│   Results & Alerts              │
│  - Save to Hive                 │
│  - EmailService                 │
└─────────────────────────────────┘
```

## Prerequisites

### Required Software

- **Apache Spark 3.3.0+**
- **Scala 2.12.15**
- **SBT 1.8.2+**
- **Java 8 or 11**
- **Hive 2.3+ / 3.x**
- **HDFS** (for production deployment)

### Environment Setup

```bash
# Verify installations
spark-submit --version
scala -version
sbt --version
java -version
```

### Access Requirements

- Read access to user metrics Hive table
- Write access to anomaly results tables
- SMTP server credentials for email alerts
- YARN cluster access (for production)

## Project Structure

```
user-metric-anomaly-detector/
├── build.sbt                           # SBT build configuration
├── project/
│   ├── build.properties                # SBT version
│   └── plugins.sbt                     # SBT plugins
├── src/
│   └── main/
│       ├── scala/
│       │   └── com/company/anomaly/
│       │       ├── UserMetricAnomalyDetector.scala  # Main application
│       │       ├── model/
│       │       │   ├── AnomalyResult.scala          # Result model
│       │       │   └── MetricStatistics.scala       # Statistics model
│       │       ├── detector/
│       │       │   ├── AnomalyDetector.scala        # Base trait
│       │       │   ├── IQRDetector.scala            # IQR implementation
│       │       │   └── ZScoreDetector.scala         # Z-Score implementation
│       │       ├── service/
│       │       │   ├── MetricService.scala          # Data operations
│       │       │   └── EmailService.scala           # Email alerts
│       │       └── config/
│       │           └── AppConfig.scala              # Configuration loader
│       └── resources/
│           └── application.conf                     # Application settings
├── scripts/
│   ├── deploy.sh                       # Production deployment
│   ├── run-local.sh                    # Local testing
│   └── create-hive-tables.sql          # Hive DDL
└── README.md                           # This file
```

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd user-metric-anomaly-detector
```

### 2. Build the Project

```bash
# Clean and build
sbt clean

# Compile
sbt compile

# Run tests (if available)
sbt test

# Create fat JAR
sbt assembly
```

The assembled JAR will be at: `target/scala-2.12/user-metric-anomaly-detector-1.0.0.jar`

### 3. Create Hive Tables

```bash
# Connect to Hive and run DDL
hive -f scripts/create-hive-tables.sql

# Or use beeline
beeline -u "jdbc:hive2://localhost:10000" -f scripts/create-hive-tables.sql
```

### 4. Configure Application

Edit `src/main/resources/application.conf` with your environment-specific settings:

```hocon
anomaly-detector {
  lookback-days = 1460        # 4 years
  iqr-multiplier = 1.5
  zscore-threshold = 3.0

  hive {
    user-metrics-table = "default.user_metrics"
    anomaly-results-table = "default.anomaly_detection_results"
    metric-statistics-table = "default.metric_statistics"
  }

  email {
    smtp-host = "smtp.example.com"
    smtp-port = 587
    smtp-username = "your-email@example.com"
    smtp-password = "your-password"  # Use env vars in production!
    from = "anomaly-detector@example.com"
    to = ["team@example.com"]
    investigation-url = "https://dashboard.example.com/metrics"
  }
}
```

**Security Note**: Use environment variables for credentials in production:

```bash
export SMTP_USERNAME="your-email@example.com"
export SMTP_PASSWORD="your-password"
```

## Configuration

### Key Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `lookback-days` | 1460 | Days of historical data (4 years) |
| `iqr-multiplier` | 1.5 | Sensitivity for IQR method |
| `zscore-threshold` | 3.0 | Threshold for Z-Score (std devs) |
| `smtp-host` | - | SMTP server hostname |
| `smtp-port` | 587 | SMTP server port |

### Tuning Detection Sensitivity

**Too many false positives?**
- Increase `iqr-multiplier` (try 2.0 or 3.0)
- Increase `zscore-threshold` (try 3.5 or 4.0)
- Increase `lookback-days` for more stable baselines

**Missing real anomalies?**
- Decrease `iqr-multiplier` (try 1.0 or 1.2)
- Decrease `zscore-threshold` (try 2.5 or 2.0)
- Review day-of-week patterns

## Usage

### Running Locally (for testing)

```bash
# Run with current date
./scripts/run-local.sh

# Run with specific date
./scripts/run-local.sh 2025-11-27
```

### Running on YARN Cluster

```bash
# Production deployment
./scripts/deploy.sh 2025-11-27 prod

# Development environment
./scripts/deploy.sh 2025-11-27 dev

# With default (current date)
./scripts/deploy.sh
```

### Direct spark-submit

```bash
spark-submit \
  --class com.company.anomaly.UserMetricAnomalyDetector \
  --master yarn \
  --deploy-mode cluster \
  --driver-memory 4g \
  --executor-memory 8g \
  --executor-cores 4 \
  --num-executors 5 \
  target/scala-2.12/user-metric-anomaly-detector-1.0.0.jar \
  2025-11-27
```

### Scheduling with Cron/Airflow

#### Cron Example

```cron
# Run daily at 6 AM for previous day
0 6 * * * /path/to/scripts/deploy.sh $(date -d "yesterday" +\%Y-\%m-\%d) prod >> /var/log/anomaly-detector.log 2>&1
```

#### Airflow DAG Example

```python
from airflow import DAG
from airflow.operators.bash import BashOperator
from datetime import datetime, timedelta

default_args = {
    'owner': 'data-engineering',
    'depends_on_past': False,
    'email': ['alerts@example.com'],
    'email_on_failure': True,
    'retries': 2,
    'retry_delay': timedelta(minutes=5),
}

dag = DAG(
    'user_metric_anomaly_detection',
    default_args=default_args,
    description='Daily anomaly detection for user metrics',
    schedule_interval='0 6 * * *',  # 6 AM daily
    start_date=datetime(2025, 1, 1),
    catchup=False,
)

detect_anomalies = BashOperator(
    task_id='detect_anomalies',
    bash_command='/path/to/scripts/deploy.sh {{ ds }} prod',
    dag=dag,
)
```

## Deployment

### Step-by-Step Production Deployment

#### 1. Prepare Environment

```bash
# Set environment variables
export SMTP_USERNAME="anomaly-detector@company.com"
export SMTP_PASSWORD="secure-password"
export SPARK_QUEUE="production"
export SPARK_NUM_EXECUTORS="10"
```

#### 2. Build and Test

```bash
# Build
sbt clean assembly

# Test locally first
./scripts/run-local.sh $(date -d "yesterday" +%Y-%m-%d)
```

#### 3. Deploy to Cluster

```bash
# Deploy for specific date
./scripts/deploy.sh 2025-11-27 prod

# Monitor
yarn application -list
```

#### 4. Verify Results

```sql
-- Check detection results
SELECT * FROM anomaly_detection_results
WHERE metric_date = '2025-11-27'
ORDER BY detection_method;

-- Check for anomalies
SELECT * FROM anomaly_detection_results
WHERE is_anomaly = true
AND metric_date >= current_date - 7;
```

### Resource Sizing Recommendations

| Cluster Size | Driver Memory | Executor Memory | Executors | Cores/Executor |
|--------------|---------------|-----------------|-----------|----------------|
| Small | 2g | 4g | 2 | 2 |
| Medium | 4g | 8g | 5 | 4 |
| Large | 8g | 16g | 10 | 4 |

## Monitoring

### Application Monitoring

```bash
# Check YARN applications
yarn application -list -appStates RUNNING,SUBMITTED

# Get application logs
yarn logs -applicationId application_1234567890_0001

# Monitor Spark UI
# Navigate to: http://<resource-manager>:8088
```

### Query Results

```sql
-- Anomaly summary by date
SELECT
  metric_date,
  COUNT(*) as detection_runs,
  SUM(CASE WHEN is_anomaly THEN 1 ELSE 0 END) as anomaly_count,
  MAX(anomaly_score) as max_score
FROM anomaly_detection_results
WHERE metric_date >= current_date - 30
GROUP BY metric_date
ORDER BY metric_date DESC;

-- Detection accuracy by method
SELECT
  detection_method,
  COUNT(*) as total_runs,
  SUM(CASE WHEN is_anomaly THEN 1 ELSE 0 END) as anomalies_detected,
  AVG(anomaly_score) as avg_score
FROM anomaly_detection_results
WHERE metric_date >= current_date - 90
GROUP BY detection_method;

-- Statistics overview
SELECT
  day_of_week,
  CAST(mean_count AS BIGINT) as avg_count,
  CAST(lower_bound AS BIGINT) as lower_bound,
  CAST(upper_bound AS BIGINT) as upper_bound,
  sample_size,
  last_updated
FROM metric_statistics
ORDER BY day_of_week;
```

## Troubleshooting

### Common Issues

#### 1. No Historical Data Found

**Symptom**: `No historical data found between X and Y`

**Solutions**:
- Verify data exists in `user_metrics` table
- Check date range is correct
- Ensure partitions are loaded: `MSCK REPAIR TABLE user_metrics`
- Reduce `lookback-days` if data history is limited

#### 2. Insufficient Data for Day of Week

**Symptom**: `Insufficient data for day of week X: only N samples`

**Solutions**:
- Reduce `lookback-days` threshold (minimum 4 samples required)
- Check data consistency for that day of week
- Verify no data gaps in source table

#### 3. Email Sending Fails

**Symptom**: `Failed to send email: <error>`

**Solutions**:
- Verify SMTP credentials are correct
- Check SMTP host and port
- Ensure firewall allows SMTP connections
- Test with simple email client first
- For Gmail: enable "Less secure app access" or use App Passwords

#### 4. Out of Memory Errors

**Symptom**: `java.lang.OutOfMemoryError`

**Solutions**:
- Increase driver/executor memory
- Reduce `lookback-days`
- Increase `spark.sql.shuffle.partitions`
- Check for data skew

#### 5. Permission Denied Errors

**Symptom**: `Permission denied` on Hive tables

**Solutions**:
- Verify Hive table permissions
- Check HDFS directory permissions
- Ensure user has write access to result tables

### Debug Mode

Enable debug logging:

```bash
spark-submit \
  --conf "spark.driver.extraJavaOptions=-Dlog4j.root.level=DEBUG" \
  ... other options ...
```

## Development

### Local Development Setup

```bash
# Clone repository
git clone <repo-url>
cd user-metric-anomaly-detector

# Install dependencies
sbt update

# Compile
sbt compile

# Run tests
sbt test

# Package
sbt package

# Assembly (fat JAR)
sbt assembly
```

### Testing

The project includes comprehensive test coverage with unit tests and integration tests.

#### Running Tests

```bash
# Run all tests
sbt test

# Run specific test suite
sbt "testOnly com.company.anomaly.model.MetricStatisticsSpec"

# Run tests with coverage
sbt clean coverage test coverageReport

# Run tests in watch mode (re-run on file changes)
sbt ~test
```

#### Test Structure

```
src/test/scala/
├── com/company/anomaly/
│   ├── model/
│   │   ├── MetricStatisticsSpec.scala      # Statistical calculations
│   │   └── AnomalyResultSpec.scala         # Result model & formatting
│   ├── detector/
│   │   ├── IQRDetectorSpec.scala           # IQR detection logic
│   │   └── ZScoreDetectorSpec.scala        # Z-Score detection logic
│   ├── config/
│   │   └── AppConfigSpec.scala             # Configuration validation
│   └── IntegrationSpec.scala               # End-to-end workflows
```

#### Test Coverage

- **Model Tests** (180+ test cases):
  - Statistical calculations (mean, std dev, percentiles)
  - Edge cases (empty data, single values, zero variance)
  - Partition value extraction
  - HTML formatting for emails

- **Detector Tests** (120+ test cases):
  - Anomaly detection logic for both methods
  - Score calculation and normalization
  - Threshold validation
  - Real-world scenarios (spikes, drops, variations)

- **Config Tests** (60+ test cases):
  - Configuration validation
  - Invalid parameter handling
  - Default value verification

- **Integration Tests** (40+ test cases):
  - Complete detection workflows
  - Multi-method comparison
  - Day-of-week pattern analysis
  - High/low variance data handling

#### Test Examples

```bash
# Example test output
[info] MetricStatisticsSpec:
[info] - should calculate correct statistics for a simple dataset
[info] - should calculate correct standard deviation
[info] - should handle single value correctly
[info] - should calculate correct percentiles for even-sized dataset
[info] IQRDetectorSpec:
[info] - should not detect anomaly when value is within bounds
[info] - should detect anomaly when value is above upper bound
[info] - should calculate normalized anomaly score
[info] ZScoreDetectorSpec:
[info] - should detect anomaly when value exceeds positive threshold
[info] - should calculate correct Z-score
[info] IntegrationSpec:
[info] - should detect anomalies consistently across methods
[info] - should process realistic healthcare data pattern
```

#### Writing New Tests

Follow ScalaTest conventions:

```scala
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MyNewSpec extends AnyFlatSpec with Matchers {
  "MyClass" should "do something" in {
    val result = MyClass.doSomething()
    result shouldBe expectedValue
  }
}
```

### Code Structure

- **Model**: Data classes (`AnomalyResult`, `MetricStatistics`)
- **Detector**: Detection algorithms (`IQRDetector`, `ZScoreDetector`)
- **Service**: Business logic (`MetricService`, `EmailService`)
- **Config**: Configuration management (`AppConfig`)
- **Main**: Application orchestration (`UserMetricAnomalyDetector`)

### Adding New Detection Methods

1. Create new class implementing `AnomalyDetector` trait
2. Implement `detect()` method
3. Add to detector list in main application
4. Update configuration if needed

Example:

```scala
class CustomDetector extends AnomalyDetector {
  override def methodName: String = "Custom"

  override def detect(
    metricDate: Date,
    actualValue: Long,
    statistics: MetricStatistics
  ): AnomalyResult = {
    // Your detection logic here
    createResult(...)
  }
}
```

## FAQ

### Q: How much historical data is needed?

**A**: Minimum 4 weeks recommended, but 1-4 years is ideal for capturing seasonal patterns.

### Q: Can I use this for other metrics besides user counts?

**A**: Yes! The application is designed to be metric-agnostic. Just point it to a different table with the same schema.

### Q: How do I adjust detection sensitivity?

**A**: Modify `iqr-multiplier` and `zscore-threshold` in `application.conf`. Lower values = more sensitive.

### Q: What if I don't want email alerts?

**A**: The application will log results to Hive regardless. Email failures don't stop the job.

### Q: Can I run this for multiple dates at once?

**A**: Currently single-date only. For batch processing, use a scheduler loop.

### Q: How do I handle holidays or special events?

**A**: Consider adding a holiday calendar table and adjusting expected ranges for known events.

## Contributing

### Reporting Issues

Please report issues with:
- Environment details (Spark version, Scala version)
- Error messages and stack traces
- Steps to reproduce
- Expected vs actual behavior

### Feature Requests

Future enhancements on the roadmap:
- [ ] Machine learning models (Isolation Forest, LSTM)
- [ ] Slack/Teams integration
- [ ] Real-time streaming detection
- [ ] Auto-tuning thresholds
- [ ] Multi-metric support
- [ ] Seasonal decomposition (STL)
- [ ] Anomaly explanation (SHAP values)

## License

Copyright (c) 2025 Company Name. All rights reserved.

## Contact

- **Team**: Data Engineering
- **Email**: data-engineering@example.com
- **Slack**: #data-engineering

---

**Last Updated**: 2025-11-28
**Version**: 1.0.0
**Maintained By**: Data Engineering Team
