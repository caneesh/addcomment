#!/bin/bash

################################################################################
# Local Testing Script for User Metric Anomaly Detector
################################################################################
# Description: Runs the Spark application in local mode for testing
# Usage: ./run-local.sh [target-date]
# Example: ./run-local.sh 2025-11-27
#          ./run-local.sh              # defaults to current date
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

# Local Spark configuration
MASTER="local[4]"  # Use 4 local threads
DRIVER_MEMORY="2g"

# ============================================================================
# Parse Command Line Arguments
# ============================================================================

TARGET_DATE="${1:-$(date +%Y-%m-%d)}"

echo "============================================================================"
echo "User Metric Anomaly Detector - Local Testing"
echo "============================================================================"
echo "Target Date: $TARGET_DATE"
echo "Mode: LOCAL"
echo "============================================================================"

# ============================================================================
# Build Project if JAR Doesn't Exist
# ============================================================================

if [ ! -f "$JAR_PATH" ]; then
    echo "⚠ JAR file not found. Building project..."
    cd "$PROJECT_ROOT"
    sbt assembly

    if [ $? -ne 0 ]; then
        echo "ERROR: Build failed"
        exit 1
    fi

    echo "✓ Build completed successfully"
fi

echo "✓ JAR file found: $JAR_PATH"
JAR_SIZE=$(ls -lh "$JAR_PATH" | awk '{print $5}')
echo "  JAR size: $JAR_SIZE"

# ============================================================================
# Check for Derby Metastore (for local Hive)
# ============================================================================

DERBY_DIR="$PROJECT_ROOT/metastore_db"

if [ ! -d "$DERBY_DIR" ]; then
    echo ""
    echo "⚠ Local Hive metastore not found."
    echo "  This is expected for first-time setup."
    echo "  Derby metastore will be created automatically."
    echo ""
fi

# ============================================================================
# Set Up Local Environment
# ============================================================================

# Create local directories for Spark
mkdir -p "$PROJECT_ROOT/spark-warehouse"
mkdir -p "$PROJECT_ROOT/spark-logs"

# Set environment variables for local testing
export SPARK_LOCAL_DIRS="$PROJECT_ROOT/tmp"
mkdir -p "$SPARK_LOCAL_DIRS"

# Optional: Set SMTP credentials for email testing
if [ -f "$PROJECT_ROOT/.env" ]; then
    echo "✓ Loading environment variables from .env file"
    source "$PROJECT_ROOT/.env"
fi

# ============================================================================
# Display Configuration
# ============================================================================

echo ""
echo "Local Spark Configuration:"
echo "  Master: $MASTER"
echo "  Driver Memory: $DRIVER_MEMORY"
echo "  Warehouse: $PROJECT_ROOT/spark-warehouse"
echo ""

# ============================================================================
# Build spark-submit Command
# ============================================================================

SPARK_SUBMIT_CMD="spark-submit \
    --class $MAIN_CLASS \
    --master $MASTER \
    --driver-memory $DRIVER_MEMORY \
    --conf spark.app.name=\"User Metric Anomaly Detector - LOCAL - $TARGET_DATE\" \
    --conf spark.sql.warehouse.dir=\"$PROJECT_ROOT/spark-warehouse\" \
    --conf spark.sql.catalogImplementation=in-memory \
    --conf spark.sql.shuffle.partitions=4 \
    --conf spark.ui.enabled=true \
    --conf spark.ui.port=4040 \
    --conf spark.eventLog.enabled=false \
    --conf spark.driver.extraJavaOptions=\"-Dlog4j.configuration=log4j.properties\" \
    --files $PROJECT_ROOT/src/main/resources/application.conf \
    $JAR_PATH \
    $TARGET_DATE"

# ============================================================================
# Pre-Flight Checks
# ============================================================================

echo "Pre-flight checks:"
echo ""

# Check if Spark is available
if ! command -v spark-submit &> /dev/null; then
    echo "✗ ERROR: spark-submit not found in PATH"
    echo "  Please install Apache Spark or add it to your PATH"
    echo "  Download: https://spark.apache.org/downloads.html"
    exit 1
fi

echo "✓ spark-submit found: $(which spark-submit)"

# Check Spark version
SPARK_VERSION=$(spark-submit --version 2>&1 | grep "version" | head -1 | awk '{print $NF}')
echo "✓ Spark version: $SPARK_VERSION"

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
echo "✓ Java version: $JAVA_VERSION"

echo ""

# ============================================================================
# Execute spark-submit
# ============================================================================

echo "============================================================================"
echo "Starting Spark application in local mode..."
echo ""
echo "Command:"
echo "$SPARK_SUBMIT_CMD"
echo ""
echo "============================================================================"
echo ""
echo "⚠ NOTE: For local testing, this will use in-memory catalog instead of Hive."
echo "   To test with actual Hive tables, use a development cluster."
echo ""
echo "Spark UI will be available at: http://localhost:4040"
echo ""
echo "============================================================================"
echo ""

# Execute the command
eval $SPARK_SUBMIT_CMD

EXIT_CODE=$?

echo ""
echo "============================================================================"

if [ $EXIT_CODE -eq 0 ]; then
    echo "✓ Application completed successfully"
    echo ""
    echo "Output files (if any) are in: $PROJECT_ROOT/spark-warehouse"
else
    echo "✗ Application failed with exit code: $EXIT_CODE"
    echo ""
    echo "Troubleshooting:"
    echo "  1. Check the logs above for error messages"
    echo "  2. Verify application.conf settings"
    echo "  3. Ensure test data is available (or modify code for testing)"
    echo "  4. Check that all dependencies are included in the JAR"
fi

echo "============================================================================"

# ============================================================================
# Cleanup (Optional)
# ============================================================================

# Uncomment to clean up temporary files after run
# echo ""
# read -p "Clean up temporary files? (y/n) " -n 1 -r
# echo
# if [[ $REPLY =~ ^[Yy]$ ]]; then
#     echo "Cleaning up..."
#     rm -rf "$SPARK_LOCAL_DIRS"
#     echo "✓ Cleanup complete"
# fi

exit $EXIT_CODE
