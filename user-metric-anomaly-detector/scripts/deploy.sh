#!/bin/bash

################################################################################
# Deployment Script for User Metric Anomaly Detector
################################################################################
# Description: Submits the Spark application to YARN cluster
# Usage: ./deploy.sh [target-date] [environment]
# Example: ./deploy.sh 2025-11-27 prod
#          ./deploy.sh                    # defaults to current date and prod
################################################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# ============================================================================
# Configuration Variables
# ============================================================================

# Script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# JAR configuration
JAR_NAME="user-metric-anomaly-detector-1.0.0.jar"
JAR_PATH="$PROJECT_ROOT/target/scala-2.12/$JAR_NAME"
MAIN_CLASS="com.company.anomaly.UserMetricAnomalyDetector"

# Default Spark configuration
DEFAULT_MASTER="yarn"
DEFAULT_DEPLOY_MODE="cluster"
DEFAULT_DRIVER_MEMORY="4g"
DEFAULT_EXECUTOR_MEMORY="8g"
DEFAULT_EXECUTOR_CORES="4"
DEFAULT_NUM_EXECUTORS="5"
DEFAULT_QUEUE="default"

# Application configuration
DEFAULT_ENV="prod"

# ============================================================================
# Parse Command Line Arguments
# ============================================================================

TARGET_DATE="${1:-$(date +%Y-%m-%d)}"
ENVIRONMENT="${2:-$DEFAULT_ENV}"

echo "============================================================================"
echo "User Metric Anomaly Detector - Deployment Script"
echo "============================================================================"
echo "Target Date: $TARGET_DATE"
echo "Environment: $ENVIRONMENT"
echo "============================================================================"

# ============================================================================
# Validate JAR File
# ============================================================================

if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: JAR file not found at $JAR_PATH"
    echo "Please build the project first:"
    echo "  cd $PROJECT_ROOT"
    echo "  sbt assembly"
    exit 1
fi

echo "✓ JAR file found: $JAR_PATH"
JAR_SIZE=$(ls -lh "$JAR_PATH" | awk '{print $5}')
echo "  JAR size: $JAR_SIZE"

# ============================================================================
# Environment-Specific Configuration
# ============================================================================

# Load environment-specific settings
case $ENVIRONMENT in
    dev)
        MASTER="${SPARK_MASTER:-local[*]}"
        DEPLOY_MODE="client"
        DRIVER_MEMORY="2g"
        EXECUTOR_MEMORY="4g"
        EXECUTOR_CORES="2"
        NUM_EXECUTORS="2"
        QUEUE="dev"
        ;;
    prod)
        MASTER="${SPARK_MASTER:-$DEFAULT_MASTER}"
        DEPLOY_MODE="${SPARK_DEPLOY_MODE:-$DEFAULT_DEPLOY_MODE}"
        DRIVER_MEMORY="${SPARK_DRIVER_MEMORY:-$DEFAULT_DRIVER_MEMORY}"
        EXECUTOR_MEMORY="${SPARK_EXECUTOR_MEMORY:-$DEFAULT_EXECUTOR_MEMORY}"
        EXECUTOR_CORES="${SPARK_EXECUTOR_CORES:-$DEFAULT_EXECUTOR_CORES}"
        NUM_EXECUTORS="${SPARK_NUM_EXECUTORS:-$DEFAULT_NUM_EXECUTORS}"
        QUEUE="${SPARK_QUEUE:-$DEFAULT_QUEUE}"
        ;;
    *)
        echo "ERROR: Unknown environment: $ENVIRONMENT"
        echo "Valid environments: dev, prod"
        exit 1
        ;;
esac

# ============================================================================
# Display Configuration
# ============================================================================

echo ""
echo "Spark Configuration:"
echo "  Master: $MASTER"
echo "  Deploy Mode: $DEPLOY_MODE"
echo "  Driver Memory: $DRIVER_MEMORY"
echo "  Executor Memory: $EXECUTOR_MEMORY"
echo "  Executor Cores: $EXECUTOR_CORES"
echo "  Number of Executors: $NUM_EXECUTORS"
echo "  Queue: $QUEUE"
echo ""

# ============================================================================
# Set Environment Variables for Configuration
# ============================================================================

# Export SMTP credentials if available (security best practice)
if [ -n "${SMTP_USERNAME:-}" ]; then
    export SMTP_USERNAME
fi

if [ -n "${SMTP_PASSWORD:-}" ]; then
    export SMTP_PASSWORD
fi

# ============================================================================
# Build spark-submit Command
# ============================================================================

SPARK_SUBMIT_CMD="spark-submit \
    --class $MAIN_CLASS \
    --master $MASTER \
    --deploy-mode $DEPLOY_MODE \
    --driver-memory $DRIVER_MEMORY \
    --executor-memory $EXECUTOR_MEMORY \
    --executor-cores $EXECUTOR_CORES \
    --num-executors $NUM_EXECUTORS \
    --queue $QUEUE \
    --conf spark.app.name=\"User Metric Anomaly Detector - $TARGET_DATE\" \
    --conf spark.sql.shuffle.partitions=200 \
    --conf spark.sql.adaptive.enabled=true \
    --conf spark.dynamicAllocation.enabled=false \
    --conf spark.yarn.maxAppAttempts=2 \
    --conf spark.task.maxFailures=4 \
    --conf spark.speculation=false \
    --conf spark.hadoop.hive.exec.dynamic.partition=true \
    --conf spark.hadoop.hive.exec.dynamic.partition.mode=nonstrict \
    --conf spark.sql.sources.partitionOverwriteMode=dynamic \
    --conf spark.eventLog.enabled=true \
    --conf spark.eventLog.dir=hdfs:///spark-logs \
    --conf spark.driver.extraJavaOptions=\"-Dlog4j.configuration=log4j.properties\" \
    --conf spark.executor.extraJavaOptions=\"-Dlog4j.configuration=log4j.properties\" \
    --files $PROJECT_ROOT/src/main/resources/application.conf \
    $JAR_PATH \
    $TARGET_DATE"

# ============================================================================
# Execute spark-submit
# ============================================================================

echo "Submitting Spark application..."
echo ""
echo "Command:"
echo "$SPARK_SUBMIT_CMD"
echo ""
echo "============================================================================"

# Execute the command
eval $SPARK_SUBMIT_CMD

EXIT_CODE=$?

echo "============================================================================"

if [ $EXIT_CODE -eq 0 ]; then
    echo "✓ Application submitted successfully"
    echo ""
    echo "To monitor the application:"
    echo "  - YARN ResourceManager UI: http://<resource-manager>:8088"
    echo "  - Spark History Server: http://<history-server>:18080"
    echo "  - yarn application -list"
    echo "  - yarn logs -applicationId <app-id>"
else
    echo "✗ Application submission failed with exit code: $EXIT_CODE"
    echo ""
    echo "Troubleshooting:"
    echo "  1. Check that Spark is properly configured"
    echo "  2. Verify JAR file is accessible"
    echo "  3. Check YARN cluster resources"
    echo "  4. Review logs for errors"
fi

echo "============================================================================"

exit $EXIT_CODE
