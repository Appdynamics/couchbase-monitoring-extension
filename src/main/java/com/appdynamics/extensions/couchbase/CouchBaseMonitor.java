package com.appdynamics.extensions.couchbase;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.ATaskExecutor;
import com.appdynamics.extensions.util.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.couchbase.utils.Constants.DEFAULT_METRIC_PREFIX;

public class CouchBaseMonitor extends ABaseMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CouchBaseMonitor.class);

    @Override
    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    @Override
    protected String getMonitorName() {
        return "CouchBase Monitor";
    }

    @Override
    protected void doRun(ATaskExecutor taskExecutor) {
        List<Map<String,String>> servers = (List<Map<String,String>>)configuration.getConfigYml().get("servers");
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");
        for (Map<String, String> server : servers) {
            CouchBaseMonitorTask task = new CouchBaseMonitorTask(configuration, server);
            taskExecutor.submit(server.get("name"),task);
        }
    }

    @Override
    protected int getTaskCount() {
        List<Map<String,String>> servers = (List<Map<String,String>>)configuration.getConfigYml().get("servers");
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");
        return servers.size();
    }
}
