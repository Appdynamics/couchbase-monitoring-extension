package com.appdynamics.monitors.couchbase;

import com.google.gson.*;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CouchBaseWrapper {

    private static final Logger logger = Logger.getLogger(CouchBaseWrapper.class.getSimpleName());
    private String host;
    private String port;
    private String username;
    private String password;

    public CouchBaseWrapper(Map<String, String> taskArguments) {
        this.host = taskArguments.get("host");
        this.port = taskArguments.get("port");
        this.username = taskArguments.get("username");
        this.password = taskArguments.get("password");
    }

    /**
     * Connects to the couchbase host and retrieves metrics using the CouchBase REST API
     * @return 	HashMap     Map containing metrics retrieved from using the CouchBase REST API
     */
    public HashMap gatherMetrics() throws Exception{
        HttpURLConnection connection = null;
        InputStream is = null;
        String statsUrl = constructClusterURL();
        HashMap<String, HashMap> couchbaseMetrics = new HashMap<String, HashMap>();

        try {
            String currentLine;
            URL u = new URL(statsUrl);
            connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            logger.info("Connecting to database for host: " + host + ":" + port);
            connection.connect();
            is = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonString = new StringBuilder();
            while ((currentLine = bufferedReader.readLine()) != null) {
                jsonString.append(currentLine);
            }

            JsonObject clusterStats = new JsonParser().parse(jsonString.toString()).getAsJsonObject();
            JsonArray nodes = (JsonArray) clusterStats.get("nodes");
            JsonObject storageTotals = clusterStats.getAsJsonObject("storageTotals");

            HashMap clusterMetrics = new HashMap();
            populateClusterMetrics(storageTotals, clusterMetrics);

            HashMap nodeMetrics = new HashMap();
            populateNodeMetrics(nodes, nodeMetrics);

            couchbaseMetrics.put("ClusterStats", clusterMetrics);
            couchbaseMetrics.put("NodeStats", nodeMetrics);

            // Clean up before starting new connection
            is.close();
            connection.disconnect();
            is = null;
            connection = null;
            u = null;
            bufferedReader = null;
            jsonString = null;

            // Open new connection
            statsUrl = constructBucketURL();
            u = new URL(statsUrl);
            connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            is = connection.getInputStream();

            bufferedReader = new BufferedReader(new InputStreamReader(is));
            jsonString = new StringBuilder();
            while ((currentLine = bufferedReader.readLine()) != null) {
                jsonString.append(currentLine);
            }
            JsonArray bucketStats = new JsonParser().parse(jsonString.toString()).getAsJsonArray();
            HashMap bucketMetrics = new HashMap();
            populateBucketMetrics(bucketStats, bucketMetrics);
            couchbaseMetrics.put("BucketStats", bucketMetrics);

            return couchbaseMetrics;
        } catch(MalformedURLException e) {
            logger.error("Invalid URL used to connect to CouchDB: " + statsUrl);
            throw e;
        } catch(JsonSyntaxException e) {
            logger.error("Error parsing the Json response");
            throw e;
        } catch(IOException e) {
            throw e;
        } finally {
            try {
                if (is != null && connection != null) {
                    is.close();
                    connection.disconnect();
                }
            }catch(Exception e) {
                logger.error("Exception", e);
            }
        }
    }

    private void populateClusterMetrics(JsonObject clusterStats, HashMap clusterMetrics) throws Exception{
        JsonObject ramStats = clusterStats.getAsJsonObject("ram");
        Iterator iterator = ramStats.entrySet().iterator();
        populateMetricsMapHelper(iterator, clusterMetrics, "ram_");

        JsonObject hddStats = clusterStats.getAsJsonObject("hdd");
        iterator = hddStats.entrySet().iterator();
        populateMetricsMapHelper(iterator, clusterMetrics, "hdd_");
    }

    private void populateNodeMetrics(JsonArray nodes, HashMap<String, HashMap<String, Number>> nodeMetrics) {
        for (JsonElement node : nodes) {
            JsonObject nodeObject = node.getAsJsonObject();
            HashMap<String, Number> metrics = new HashMap<String, Number>();
            nodeMetrics.put(nodeObject.get("hostname").getAsString(), metrics);

            JsonObject interestingStats = nodeObject.getAsJsonObject("interestingStats");
            Iterator iterator = interestingStats.entrySet().iterator();
            populateMetricsMapHelper(iterator, metrics, "");

            JsonObject systemStats = nodeObject.getAsJsonObject("systemStats");
            iterator = systemStats.entrySet().iterator();
            populateMetricsMapHelper(iterator, metrics, "");

            iterator = nodeObject.entrySet().iterator();
            populateMetricsMapHelper(iterator, metrics, "");
        }
    }

    private void populateBucketMetrics(JsonArray buckets, HashMap<String, HashMap<String, Number>> bucketMetrics) {
        for (JsonElement bucket : buckets) {
            JsonObject bucketObject = bucket.getAsJsonObject();
            HashMap<String, Number> metrics = new HashMap<String, Number>();
            bucketMetrics.put(bucketObject.get("name").getAsString(), metrics);

            JsonObject interestingStats = bucketObject.getAsJsonObject("quota");
            Iterator iterator = interestingStats.entrySet().iterator();
            populateMetricsMapHelper(iterator, metrics, "");

            JsonObject systemStats = bucketObject.getAsJsonObject("basicStats");
            iterator = systemStats.entrySet().iterator();
            populateMetricsMapHelper(iterator, metrics, "");
        }
    }

    private void populateMetricsMapHelper(Iterator iterator, HashMap metricsMap, String prefix) {
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            String metricName = (String)entry.getKey();
            JsonElement value = (JsonElement)entry.getValue();
            if (value instanceof JsonPrimitive && NumberUtils.isNumber(value.getAsString())) {
                Number val = value.getAsNumber();
                metricsMap.put(prefix + metricName, val);
            }
        }
    }

    /**
     * Construct the REST URL for the CoucheDB host
     * @return	The CoucheDB host REST URL string
     */
    private String constructClusterURL() {
        return new StringBuilder()
                .append("http://")
                .append(username)
                .append(":")
                .append(password)
                .append("@")
                .append(host)
                .append(":")
                .append(port)
                .append("/pools/stats")
                .toString();
    }

    /**
     * Construct the REST URL for the CoucheDB host
     * @return	The CoucheDB host REST URL string
     */
    private String constructBucketURL() {
        return new StringBuilder()
                .append("http://")
                .append(username)
                .append(":")
                .append(password)
                .append("@")
                .append(host)
                .append(":")
                .append(port)
                .append("/pools/buckets/buckets")
                .toString();
    }
}