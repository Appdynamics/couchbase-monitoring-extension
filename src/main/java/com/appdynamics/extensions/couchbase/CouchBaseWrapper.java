/** 
* Copyright 2013 AppDynamics 
* 
* Licensed under the Apache License, Version 2.0 (the License);
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an AS IS BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.appdynamics.extensions.couchbase;

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
     * Connects to the couchbase cluster host and retrieves metrics using the CouchBase REST API
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
            logger.error("Invalid URL used to connect to CouchBase: " + statsUrl);
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

    /**
     * Populates the cluster metrics hashmap
     * @param   clusterStats     A JsonObject containing metrics for the entire cluster
     * @param   clusterMetrics   An initially empty map that is populated based on values retrieved from traversing the clusterStats JsonObject
     */
    private void populateClusterMetrics(JsonObject clusterStats, HashMap clusterMetrics) throws Exception{
        JsonObject ramStats = clusterStats.getAsJsonObject("ram");
        Iterator iterator = ramStats.entrySet().iterator();
        populateMetricsMapHelper(iterator, clusterMetrics, "ram_");

        JsonObject hddStats = clusterStats.getAsJsonObject("hdd");
        iterator = hddStats.entrySet().iterator();
        populateMetricsMapHelper(iterator, clusterMetrics, "hdd_");
    }

    /**
     * Populates the node metrics hashmap
     * @param   nodes           A JsonArray containing metrics for all nodes
     * @param   nodeMetrics     An initially empty map that is populated based on values retrieved from traversing the nodes JsonArray
     */
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

    /**
     * Populates the bucket metrics hashmap
     * @param   buckets        A JsonArray containing metrics for all buckets
     * @param   bucketMetrics  An initially empty map that is populated based on values retrieved from traversing the buckets JsonArray
     */
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

    /**
     * Populates an empty map with values retrieved from the entry set of a Json Object
     * @param   iterator    An entry set iterator for the json object
     * @param   metricsMap  Initially empty map that is populated based on the values retrieved from entry set
     * @param   prefix      Optional prefix for the metric name to distinguish duplicate metric names
     */
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
     * Construct the REST URL for CoucheBase cluster/node statistics
     * @return	The CoucheBase cluster/node statistics url string
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
     * Construct the REST URL for CoucheBase buckets associated with a node
     * @return	The CoucheBase Bucket REST url string
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