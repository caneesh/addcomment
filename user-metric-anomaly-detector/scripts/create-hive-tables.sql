-- ============================================================================
-- Hive DDL Scripts for User Metric Anomaly Detection System
-- ============================================================================
-- Description: Creates all required Hive tables for the anomaly detection pipeline
-- Author: Data Engineering Team
-- Version: 1.0.0
-- Date: 2025-11-28
-- ============================================================================

-- Set database (modify as needed)
USE default;

-- ============================================================================
-- Table 1: user_metrics
-- Purpose: Stores daily user registration counts from data pipeline
-- ============================================================================

DROP TABLE IF EXISTS user_metrics;

CREATE TABLE IF NOT EXISTS user_metrics (
    metric_date DATE COMMENT 'Date of the metric (business date)',
    user_count BIGINT COMMENT 'Number of users registered on this date',
    data_source STRING COMMENT 'Source system (e.g., web, mobile, api)',
    load_timestamp TIMESTAMP COMMENT 'When this record was loaded into Hive'
)
COMMENT 'Daily user registration metrics from various sources'
PARTITIONED BY (
    year INT COMMENT 'Year for partitioning (YYYY)',
    month INT COMMENT 'Month for partitioning (1-12)'
)
STORED AS PARQUET
LOCATION '/user/hive/warehouse/user_metrics'
TBLPROPERTIES (
    'parquet.compression' = 'SNAPPY',
    'created_by' = 'data-engineering',
    'retention_days' = '1825',  -- 5 years
    'description' = 'Daily user registration counts for anomaly detection'
);

-- Enable dynamic partitioning for easier data loading
SET hive.exec.dynamic.partition = true;
SET hive.exec.dynamic.partition.mode = nonstrict;

-- ============================================================================
-- Table 2: anomaly_detection_results
-- Purpose: Stores results from anomaly detection runs
-- ============================================================================

DROP TABLE IF EXISTS anomaly_detection_results;

CREATE TABLE IF NOT EXISTS anomaly_detection_results (
    metric_date DATE COMMENT 'Date being analyzed',
    actual_count BIGINT COMMENT 'Actual user count observed',
    expected_range_min DOUBLE COMMENT 'Lower bound of expected range',
    expected_range_max DOUBLE COMMENT 'Upper bound of expected range',
    is_anomaly BOOLEAN COMMENT 'Whether this is flagged as an anomaly',
    anomaly_score DOUBLE COMMENT 'Numeric score indicating degree of anomaly',
    detection_method STRING COMMENT 'Detection method used (IQR, Z-Score, etc.)',
    detection_timestamp TIMESTAMP COMMENT 'When the detection was performed',
    additional_info STRING COMMENT 'JSON string with additional context'
)
COMMENT 'Results from anomaly detection runs'
PARTITIONED BY (
    year INT COMMENT 'Year for partitioning (YYYY)',
    month INT COMMENT 'Month for partitioning (1-12)'
)
STORED AS PARQUET
LOCATION '/user/hive/warehouse/anomaly_detection_results'
TBLPROPERTIES (
    'parquet.compression' = 'SNAPPY',
    'created_by' = 'data-engineering',
    'retention_days' = '730',  -- 2 years
    'description' = 'Anomaly detection results for user metrics'
);

-- ============================================================================
-- Table 3: metric_statistics
-- Purpose: Stores pre-calculated statistics by day of week
-- ============================================================================

DROP TABLE IF EXISTS metric_statistics;

CREATE TABLE IF NOT EXISTS metric_statistics (
    day_of_week INT COMMENT 'Day of week (1=Monday, 7=Sunday)',
    mean_count DOUBLE COMMENT 'Average user count for this day of week',
    std_dev DOUBLE COMMENT 'Standard deviation of user counts',
    q1 DOUBLE COMMENT 'First quartile (25th percentile)',
    median DOUBLE COMMENT 'Median value (50th percentile)',
    q3 DOUBLE COMMENT 'Third quartile (75th percentile)',
    iqr DOUBLE COMMENT 'Interquartile range (Q3 - Q1)',
    lower_bound DOUBLE COMMENT 'Lower bound for anomaly detection (Q1 - 1.5*IQR)',
    upper_bound DOUBLE COMMENT 'Upper bound for anomaly detection (Q3 + 1.5*IQR)',
    sample_size BIGINT COMMENT 'Number of historical data points used',
    last_updated TIMESTAMP COMMENT 'When statistics were last calculated'
)
COMMENT 'Pre-calculated statistics by day of week for faster anomaly detection'
STORED AS PARQUET
LOCATION '/user/hive/warehouse/metric_statistics'
TBLPROPERTIES (
    'parquet.compression' = 'SNAPPY',
    'created_by' = 'data-engineering',
    'description' = 'Day of week statistics for user metrics'
);

-- ============================================================================
-- Sample Data Generation (for testing)
-- ============================================================================

-- Generate sample data for user_metrics (past 90 days)
-- Uncomment and modify dates as needed for testing

/*
INSERT INTO TABLE user_metrics PARTITION (year, month)
SELECT
    date_add(current_date(), -seq) as metric_date,
    -- Simulate realistic user counts with day-of-week patterns
    CAST(
        5000 +  -- Base count
        (CASE dayofweek(date_add(current_date(), -seq))
            WHEN 1 THEN -500  -- Sunday: lower
            WHEN 7 THEN -500  -- Saturday: lower
            ELSE 0
        END) +
        (rand() * 1000 - 500)  -- Random variation
        AS BIGINT
    ) as user_count,
    'web' as data_source,
    current_timestamp() as load_timestamp,
    year(date_add(current_date(), -seq)) as year,
    month(date_add(current_date(), -seq)) as month
FROM (
    SELECT ROW_NUMBER() OVER () - 1 as seq
    FROM (SELECT 1) a
    LATERAL VIEW explode(array(0,1,2,3,4,5,6,7,8,9)) b AS x1
    LATERAL VIEW explode(array(0,1,2,3,4,5,6,7,8,9)) c AS x2
) sequences
WHERE seq < 90;
*/

-- ============================================================================
-- Useful Queries for Monitoring and Validation
-- ============================================================================

-- Check data availability by year/month
-- SELECT year, month, COUNT(*) as record_count, MIN(metric_date), MAX(metric_date)
-- FROM user_metrics
-- GROUP BY year, month
-- ORDER BY year DESC, month DESC;

-- Check for anomalies detected in the last 30 days
-- SELECT metric_date, detection_method, actual_count, expected_range_min, expected_range_max, anomaly_score
-- FROM anomaly_detection_results
-- WHERE is_anomaly = true
--   AND metric_date >= date_add(current_date(), -30)
-- ORDER BY metric_date DESC, anomaly_score DESC;

-- View current statistics by day of week
-- SELECT day_of_week,
--        CAST(mean_count AS BIGINT) as avg_count,
--        CAST(lower_bound AS BIGINT) as lower_bound,
--        CAST(upper_bound AS BIGINT) as upper_bound,
--        sample_size,
--        last_updated
-- FROM metric_statistics
-- ORDER BY day_of_week;

-- Check for data quality issues (nulls, duplicates)
-- SELECT COUNT(*) as total_records,
--        SUM(CASE WHEN user_count IS NULL THEN 1 ELSE 0 END) as null_counts,
--        COUNT(DISTINCT metric_date) as unique_dates
-- FROM user_metrics
-- WHERE metric_date >= date_add(current_date(), -90);

-- ============================================================================
-- Partition Management Commands
-- ============================================================================

-- Repair partitions (if manually adding data to HDFS)
-- MSCK REPAIR TABLE user_metrics;
-- MSCK REPAIR TABLE anomaly_detection_results;

-- Show partitions
-- SHOW PARTITIONS user_metrics;
-- SHOW PARTITIONS anomaly_detection_results;

-- Drop old partitions (example: drop data older than 5 years)
-- ALTER TABLE user_metrics DROP IF EXISTS PARTITION (year=2019);

-- Compute statistics for query optimization
-- ANALYZE TABLE user_metrics PARTITION(year, month) COMPUTE STATISTICS;
-- ANALYZE TABLE anomaly_detection_results PARTITION(year, month) COMPUTE STATISTICS;
-- ANALYZE TABLE metric_statistics COMPUTE STATISTICS;

-- ============================================================================
-- Grant Permissions (modify as needed for your environment)
-- ============================================================================

-- GRANT SELECT ON TABLE user_metrics TO ROLE data_analyst;
-- GRANT SELECT ON TABLE anomaly_detection_results TO ROLE data_analyst;
-- GRANT SELECT ON TABLE metric_statistics TO ROLE data_analyst;
-- GRANT ALL ON TABLE user_metrics TO ROLE data_engineer;
-- GRANT ALL ON TABLE anomaly_detection_results TO ROLE data_engineer;
-- GRANT ALL ON TABLE metric_statistics TO ROLE data_engineer;

-- ============================================================================
-- Index Creation (for faster queries - if your Hive version supports it)
-- ============================================================================

-- CREATE INDEX idx_metric_date ON TABLE user_metrics (metric_date)
-- AS 'COMPACT' WITH DEFERRED REBUILD;

-- CREATE INDEX idx_anomaly_date ON TABLE anomaly_detection_results (metric_date)
-- AS 'COMPACT' WITH DEFERRED REBUILD;

-- ============================================================================
-- Compaction (for ACID tables - optional)
-- ============================================================================

-- ALTER TABLE user_metrics COMPACT 'major';
-- ALTER TABLE anomaly_detection_results COMPACT 'major';

-- ============================================================================
-- Table Properties Update (for metadata)
-- ============================================================================

-- ALTER TABLE user_metrics SET TBLPROPERTIES ('last_modified_by'='data-engineering');
-- ALTER TABLE user_metrics SET TBLPROPERTIES ('last_modified_time'='2025-11-28');

-- ============================================================================
-- End of DDL Scripts
-- ============================================================================

SHOW TABLES;
