#!/bin/bash
echo "=== Testing Server Groups Management APIs ==="
echo ""

echo "1. Testing /admin/server-groups/api/list"
curl -s http://localhost:9090/admin/server-groups/api/list | head -100
echo ""
echo ""

echo "2. Testing /admin/server-groups/api/health-trend?hours=24"
curl -s "http://localhost:9090/admin/server-groups/api/health-trend?hours=24" | head -100
echo ""
echo ""

echo "3. Testing /admin/server-groups/api/load-distribution"
curl -s http://localhost:9090/admin/server-groups/api/load-distribution | head -100
echo ""
echo ""

echo "4. Testing /admin/server-groups/api/app-distribution"
curl -s http://localhost:9090/admin/server-groups/api/app-distribution | head -100
echo ""
