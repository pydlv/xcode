#!/bin/sh

# Set timeout for Qodana scan (default: 10 minutes)
QODANA_TIMEOUT=${QODANA_TIMEOUT:-600}

# Set non-interactive mode
export NONINTERACTIVE=1

# Set Gradle properties for faster execution during analysis
export GRADLE_OPTS="-Dorg.gradle.daemon=true -Dorg.gradle.parallel=true -Dorg.gradle.caching=true"

echo "Starting Qodana scan with ${QODANA_TIMEOUT}s timeout..."

# Run qodana with timeout
timeout ${QODANA_TIMEOUT} qodana scan --print-problems

# Check the exit status
EXIT_CODE=$?

if [ $EXIT_CODE -eq 124 ]; then
    echo "⚠️  Qodana scan timed out after ${QODANA_TIMEOUT} seconds"
    echo "Consider increasing QODANA_TIMEOUT environment variable or optimizing the project"
    exit 1
elif [ $EXIT_CODE -ne 0 ]; then
    echo "❌ Qodana scan failed with exit code $EXIT_CODE"
    exit $EXIT_CODE
else
    echo "✅ Qodana scan completed successfully"
fi

