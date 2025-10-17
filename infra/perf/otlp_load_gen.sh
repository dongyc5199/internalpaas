#!/bin/bash
################################################################################
# OTLP Load Generator Script (T8: Performance Hardening)
# 
# Simulates N hosts sending metrics every 10-15 seconds to Metrics Hub
#
# Usage:
#   ./otlp_load_gen.sh [OPTIONS]
#
# Options:
#   -n, --hosts NUM        Number of simulated hosts (default: 100)
#   -i, --interval SECS    Reporting interval in seconds (default: 15)
#   -d, --duration MINS    Test duration in minutes (default: 10)
#   -e, --endpoint URL     OTLP HTTP endpoint (default: http://localhost:4318)
#   -m, --metrics NUM      Number of metrics per host (default: 20)
#   -h, --help             Show this help message
#
# Examples:
#   # Simulate 100 hosts for 10 minutes
#   ./otlp_load_gen.sh
#
#   # Simulate 1000 hosts for 30 minutes
#   ./otlp_load_gen.sh -n 1000 -d 30
#
#   # High-frequency test (10 second interval)
#   ./otlp_load_gen.sh -n 500 -i 10 -d 15
#
################################################################################

set -e

# Default configuration
HOSTS=100
INTERVAL=15
DURATION=10
ENDPOINT="http://localhost:4318/v1/metrics"
METRICS_PER_HOST=20
VERBOSE=false

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -n|--hosts)
            HOSTS="$2"
            shift 2
            ;;
        -i|--interval)
            INTERVAL="$2"
            shift 2
            ;;
        -d|--duration)
            DURATION="$2"
            shift 2
            ;;
        -e|--endpoint)
            ENDPOINT="$2"
            shift 2
            ;;
        -m|--metrics)
            METRICS_PER_HOST="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            sed -n '2,/^$/p' "$0" | tail -n +2 | head -n -1 | sed 's/^# //'
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}╔═══════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         OTLP Load Generator (T8 Perf Test)           ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Configuration:${NC}"
echo -e "  Simulated Hosts: ${YELLOW}$HOSTS${NC}"
echo -e "  Interval: ${YELLOW}${INTERVAL}s${NC}"
echo -e "  Duration: ${YELLOW}${DURATION}min${NC}"
echo -e "  Endpoint: ${YELLOW}$ENDPOINT${NC}"
echo -e "  Metrics/Host: ${YELLOW}$METRICS_PER_HOST${NC}"
echo ""

# Calculate test parameters
TOTAL_ITERATIONS=$((DURATION * 60 / INTERVAL))
TOTAL_REQUESTS=$((HOSTS * TOTAL_ITERATIONS))
EXPECTED_QPS=$((HOSTS / INTERVAL))

echo -e "${GREEN}Expected Load:${NC}"
echo -e "  Total Iterations: ${YELLOW}$TOTAL_ITERATIONS${NC}"
echo -e "  Total Requests: ${YELLOW}$TOTAL_REQUESTS${NC}"
echo -e "  Expected QPS: ${YELLOW}~$EXPECTED_QPS req/s${NC}"
echo ""

read -p "Press Enter to start test (Ctrl+C to cancel)..."
echo ""

# Counters
SUCCESS_COUNT=0
FAILURE_COUNT=0
START_TIME=$(date +%s)

# Cleanup on exit
cleanup() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}Test Summary:${NC}"
    echo -e "  Duration: ${YELLOW}$(($(date +%s) - START_TIME))s${NC}"
    echo -e "  Success: ${GREEN}$SUCCESS_COUNT${NC}"
    echo -e "  Failure: ${RED}$FAILURE_COUNT${NC}"
    echo -e "  Success Rate: ${YELLOW}$(awk "BEGIN {printf \"%.2f\", ($SUCCESS_COUNT/($SUCCESS_COUNT+$FAILURE_COUNT)*100)}")%${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
    exit 0
}

trap cleanup SIGINT SIGTERM

# Generate OTLP payload for one host
generate_payload() {
    local server_id=$1
    local timestamp=$(($(date +%s) * 1000000000)) # nanoseconds
    
    cat <<EOF
{
  "resourceMetrics": [{
    "resource": {
      "attributes": [
        {"key": "server.id", "value": {"stringValue": "server-$server_id"}},
        {"key": "region", "value": {"stringValue": "us-east-1"}},
        {"key": "env", "value": {"stringValue": "prod"}}
      ]
    },
    "scopeMetrics": [{
      "metrics": [
        {"name": "system.cpu.utilization", "gauge": {"dataPoints": [{"asDouble": $(awk "BEGIN {printf \"%.2f\", rand()*100}"), "timeUnixNano": "$timestamp"}]}},
        {"name": "system.memory.utilization", "gauge": {"dataPoints": [{"asDouble": $(awk "BEGIN {printf \"%.2f\", rand()*100}"), "timeUnixNano": "$timestamp"}]}},
        {"name": "system.disk.io.read", "gauge": {"dataPoints": [{"asDouble": $(awk "BEGIN {printf \"%.2f\", rand()*1000000}"), "timeUnixNano": "$timestamp"}]}},
        {"name": "system.disk.io.write", "gauge": {"dataPoints": [{"asDouble": $(awk "BEGIN {printf \"%.2f\", rand()*1000000}"), "timeUnixNano": "$timestamp"}]}},
        {"name": "system.network.io.receive", "gauge": {"dataPoints": [{"asDouble": $(awk "BEGIN {printf \"%.2f\", rand()*10000000}"), "timeUnixNano": "$timestamp"}]}},
        {"name": "system.network.io.transmit", "gauge": {"dataPoints": [{"asDouble": $(awk "BEGIN {printf \"%.2f\", rand()*10000000}"), "timeUnixNano": "$timestamp"}]}}
      ]
    }]
  }]
}
EOF
}

# Send metrics for one host
send_metrics() {
    local server_id=$1
    local payload=$(generate_payload "$server_id")
    
    if [ "$VERBOSE" = true ]; then
        echo -e "${BLUE}[$(date +%H:%M:%S)]${NC} Sending metrics for server-$server_id"
    fi
    
    if curl -s -X POST "$ENDPOINT" \
        -H "Content-Type: application/json" \
        -d "$payload" \
        -o /dev/null \
        -w "%{http_code}" | grep -q "200"; then
        ((SUCCESS_COUNT++))
        return 0
    else
        ((FAILURE_COUNT++))
        return 1
    fi
}

# Main test loop
echo -e "${GREEN}Starting load test...${NC}"
for iteration in $(seq 1 $TOTAL_ITERATIONS); do
    ITERATION_START=$(date +%s)
    
    echo -e "${BLUE}[Iteration $iteration/$TOTAL_ITERATIONS]${NC} Sending metrics from $HOSTS hosts..."
    
    # Send metrics from all hosts in parallel
    for host_id in $(seq 1 $HOSTS); do
        send_metrics "$host_id" &
        
        # Throttle parallel requests (max 50 at a time)
        if [ $((host_id % 50)) -eq 0 ]; then
            wait
        fi
    done
    
    # Wait for all background jobs
    wait
    
    ITERATION_END=$(date +%s)
    ITERATION_DURATION=$((ITERATION_END - ITERATION_START))
    
    echo -e "  ${GREEN}✓${NC} Completed in ${YELLOW}${ITERATION_DURATION}s${NC}"
    echo -e "  Progress: ${GREEN}$SUCCESS_COUNT${NC} success, ${RED}$FAILURE_COUNT${NC} failure"
    
    # Wait until next interval
    if [ $iteration -lt $TOTAL_ITERATIONS ]; then
        SLEEP_TIME=$((INTERVAL - ITERATION_DURATION))
        if [ $SLEEP_TIME -gt 0 ]; then
            echo -e "  Waiting ${YELLOW}${SLEEP_TIME}s${NC} until next iteration..."
            sleep $SLEEP_TIME
        fi
    fi
    echo ""
done

cleanup
