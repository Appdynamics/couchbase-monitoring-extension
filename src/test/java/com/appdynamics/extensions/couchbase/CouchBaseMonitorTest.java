package com.appdynamics.extensions.couchbase;


import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CouchBaseMonitorTest {

    @Test
    public void runSuccessfullyWhenGivenEncryptedPassword() throws TaskExecutionException {
        Map<String, String> taskArgs = getMap();
        CouchBaseMonitor monitor = new CouchBaseMonitor();
        monitor.execute(taskArgs,new TaskExecutionContext());
    }

    private Map<String, String> getMap() {
        Map<String,String> taskArgs = new HashMap<String,String>();
        taskArgs.put("host","192.168.57.102");
        taskArgs.put("port","8091");
        taskArgs.put("username","admin1");
        taskArgs.put("password-encrypted","WU0P1KiqbLerTUBzRLepfw==");
        taskArgs.put("encryption-key","myKey");
        taskArgs.put("disabled-metrics-path","src/test/resources/conf/DisabledMetrics.xml");
        taskArgs.put("metric-prefix","Custom Metrics|Couchbase|");
        return taskArgs;
    }

}
