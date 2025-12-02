#!/bin/bash
echo "=== New Relic Setup Verification ==="
echo ""
echo "1. License Key in config:"
grep -A 1 "license_key" newrelic.yml | head -2
echo ""
echo "2. Agent JAR file:"
if [ -f newrelic-agent-8.10.0.jar ]; then
    ls -lh newrelic-agent-8.10.0.jar
    echo "   ✅ Agent JAR found"
else
    echo "   ❌ Agent JAR NOT found"
    echo "   Run: curl -L -o newrelic-agent-8.10.0.jar https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.10.0/newrelic-agent-8.10.0.jar"
fi
echo ""
echo "3. Application status:"
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "   ✅ Application is running"
else
    echo "   ❌ Application is NOT running"
    echo "   Start with: ./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar"
fi
echo ""
echo "4. New Relic agent logs:"
if [ -f logs/newrelic_agent.log ]; then
    echo "   Last 5 lines:"
    tail -5 logs/newrelic_agent.log | sed 's/^/   /'
else
    echo "   ⚠️  No log file found (logs will be created when agent starts)"
fi
echo ""
echo "5. Process check:"
if pgrep -f "GoCometDawApplication" > /dev/null; then
    echo "   ✅ Application process is running"
    if pgrep -f "newrelic" > /dev/null; then
        echo "   ✅ New Relic agent process detected"
    else
        echo "   ⚠️  New Relic agent process not detected (may be embedded)"
    fi
else
    echo "   ❌ Application process NOT running"
fi
echo ""
echo "=== Recommendations ==="
echo ""
if [ ! -f newrelic-agent-8.10.0.jar ]; then
    echo "❌ Download the agent JAR file first"
fi
if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "❌ Start the application with New Relic agent"
    echo "   Command: ./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar"
fi
echo ""
echo "📊 After starting, wait 2-3 minutes, then check:"
echo "   https://one.newrelic.com → APM & Services → GoComet DAW"
