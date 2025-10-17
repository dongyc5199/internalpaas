#!/bin/bash
################################################################################
# Performance Test Suite for Metrics Hub (T8)
# 
# This script runs comprehensive performance tests across 4 scenarios:
# 1. Baseline: 100 hosts, 15s interval, 30min duration
# 2. Scale: 1000 hosts, 10s interval, 30min duration
# 3. Spike: 2000 hosts, 5s interval, 10min duration
# 4. Endurance: 500 hosts, 15s interval, 24h duration
#
# Prerequisites:
# - Metrics Hub running on localhost:4318
# - PostgreSQL/TimescaleDB running
# - LoadSimulator.java compiled
#
# Usage:
#   ./run-perf-tests.sh [scenario]
#   
#   scenario: baseline | scale | spike | endurance | all (default: all)
################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENDPOINT="http://localhost:4318/v1/metrics"
REPORT_DIR="./perf-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Java classpath (adjust if needed)
CLASSPATH="target/test-classes:target/classes:$(find ~/.m2/repository -name '*.jar' | tr '\n' ':')"

echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}        Metrics Hub Performance Test Suite${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo ""

# Create report directory
mkdir -p "$REPORT_DIR"

# Check if Metrics Hub is running
echo -e "${YELLOW}🔍 Checking Metrics Hub availability...${NC}"
if curl -s -f -o /dev/null "$ENDPOINT"; then
    echo -e "${GREEN}✓ Metrics Hub is running at $ENDPOINT${NC}"
else
    echo -e "${RED}✗ Metrics Hub is not accessible at $ENDPOINT${NC}"
    echo -e "${YELLOW}  Please start Metrics Hub first:${NC}"
    echo -e "${YELLOW}    ./mvnw.cmd spring-boot:run${NC}"
    exit 1
fi
echo ""

# Function to run a test scenario
run_test() {
    local name=$1
    local hosts=$2
    local interval=$3
    local duration=$4
    local report_file="$REPORT_DIR/${TIMESTAMP}_${name}.txt"

    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}📊 Running Test: $name${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "   Hosts: $hosts"
    echo -e "   Interval: ${interval}s"
    echo -e "   Duration: ${duration}min"
    echo -e "   Report: $report_file"
    echo ""

    # Run Java LoadSimulator
    java -cp "$CLASSPATH" \
        com.cmict.metricshub.perf.LoadSimulator \
        -n "$hosts" \
        -i "$interval" \
        -d "$duration" \
        -e "$ENDPOINT" \
        | tee "$report_file"

    echo ""
    echo -e "${GREEN}✓ Test completed: $name${NC}"
    echo -e "   Report saved to: $report_file"
    echo ""
}

# Parse command line argument
SCENARIO="${1:-all}"

case "$SCENARIO" in
    baseline)
        echo -e "${YELLOW}Running BASELINE test only${NC}"
        echo ""
        run_test "baseline" 100 15 30
        ;;
    
    scale)
        echo -e "${YELLOW}Running SCALE test only${NC}"
        echo ""
        run_test "scale" 1000 10 30
        ;;
    
    spike)
        echo -e "${YELLOW}Running SPIKE test only${NC}"
        echo ""
        run_test "spike" 2000 5 10
        ;;
    
    endurance)
        echo -e "${YELLOW}Running ENDURANCE test only${NC}"
        echo ""
        run_test "endurance" 500 15 1440  # 24 hours
        ;;
    
    all)
        echo -e "${YELLOW}Running ALL test scenarios${NC}"
        echo ""
        
        # Scenario 1: Baseline
        run_test "baseline" 100 15 30
        
        # Wait 5 minutes between tests
        echo -e "${YELLOW}⏳ Waiting 5 minutes before next test...${NC}"
        sleep 300
        
        # Scenario 2: Scale
        run_test "scale" 1000 10 30
        
        # Wait 5 minutes between tests
        echo -e "${YELLOW}⏳ Waiting 5 minutes before next test...${NC}"
        sleep 300
        
        # Scenario 3: Spike
        run_test "spike" 2000 5 10
        
        # Endurance test is separate (24h)
        echo ""
        echo -e "${YELLOW}ℹ  Endurance test (24h) not included in 'all'.${NC}"
        echo -e "${YELLOW}   Run it separately: ./run-perf-tests.sh endurance${NC}"
        ;;
    
    *)
        echo -e "${RED}✗ Invalid scenario: $SCENARIO${NC}"
        echo ""
        echo "Usage: $0 [scenario]"
        echo ""
        echo "Scenarios:"
        echo "  baseline   - 100 hosts, 15s interval, 30min (normal load)"
        echo "  scale      - 1000 hosts, 10s interval, 30min (high scale)"
        echo "  spike      - 2000 hosts, 5s interval, 10min (traffic spike)"
        echo "  endurance  - 500 hosts, 15s interval, 24h (long-running)"
        echo "  all        - Run baseline, scale, and spike (default)"
        exit 1
        ;;
esac

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ Performance testing completed!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo ""
echo -e "Reports saved to: ${YELLOW}$REPORT_DIR${NC}"
echo ""
echo -e "Next steps:"
echo -e "  1. Review performance reports in $REPORT_DIR"
echo -e "  2. Check P95 latency (target: < 300ms)"
echo -e "  3. Verify success rate (target: >= 99%)"
echo -e "  4. Monitor system resources during tests"
echo ""
