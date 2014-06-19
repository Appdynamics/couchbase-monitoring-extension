CouchBase Monitoring Extension
============================

This extension works only with the standalone machine agent.

## Use Case

Couchbase Server is an open source, distributed (shared-nothing architecture) NoSQL document-oriented database that is optimized for interactive applications. This extension allows the user to connect to a specific cluster host and retrieve metrics about the cluster, all the nodes within the cluster and any buckets associated with the nodes.  

## Installation
<ol>
	<li>Run 'mvn clean install' in the command line from the couchbase-monitoring-extension directory.
	</li>
	<li>Deploy the file CouchBaseMonitor.zip found in the 'target' directory into `<MACHINE_AGENT_HOME>/monitors/` directory.
	</li>
	<li>Unzip the deployed file.
	</li>
	<li>Open `<MACHINE_AGENT_HOME>/monitors/CouchBaseMonitor/monitor.xml` and configure the CouchBase parameters.
<p></p>
<pre>
	     &lt;argument name="host" is-required="true" default-value="localhost"/&gt;
         &lt;argument name="port" is-required="true" default-value="8091"/&gt;
         &lt;argument name="username" is-required="true" default-value="username"/&gt;
         &lt;argument name="password" is-required="true" default-value="password"/&gt;
         &lt;argument name="disabled-metrics-path" is-required="false" default-value="monitors/CouchBaseMonitor/conf/DisabledMetrics.xml"/&gt;
		 &lt;argument name="metric-prefix" is-required="false" default-value="Custom Metrics|Couchbase|"/&gt;
</pre>
	</li>
	<li>Open `<MACHINE_AGENT_HOME>/monitors/CouchBaseMonitor/conf/DisabledMetrics.xml` and configure the list of disabled metrics. Here is a sample configuration of the disabled metrics:
<p></p>
<pre>
	 &lt;Metric name="mem_free"/&gt;
	 &lt;Metric name="mem_total"/&gt;
</pre>
	</li>	
	<li> Restart the machine agent.
	</li>
	<li>In the AppDynamics Metric Browser, look for: Application Infrastructure Performance | &lt;Tier&gt; | Custom Metrics | CouchBase
	</li>
</ol>

## Directory Structure

| Directory/File | Description |
|----------------|-------------|
|src/main/resources/conf            | Contains the monitor.xml, DisabledMetrics.xml |
|src/main/java             | Contains source code of the CouchBase monitoring extension |
|target            | Only obtained when using maven. Run 'mvn clean install' to get the distributable .zip file |
|pom.xml       | Maven build script to package the project (required only if changing Java code) |

## Metrics

### Metric Category: Cluster Metrics

|Metric Name            	|Description|
|------------------------------	|------------|
|ram_total        		|Total ram available to cluster (bytes)
|ram_quotaUsed        		|Ram quota used by the cluster (bytes)
|ram_usedByData         	|Ram used by the data in the cluster (bytes)
|ram_quotaTotal         	|Ram quota total for the cluster (bytes)
|ram_used	        	|Ram used by the cluster (bytes)
|hdd_total       		|Total harddrive space available to cluster (bytes)
|hdd_used       		|Harddrive space used by the cluster (bytes)
|hdd_usedByData       		|Harddrive use by the data in the cluster(bytes)
|hdd_quotaTotal       		|Harddrive quota total for the cluster (bytes)
|hdd_free       		|Free harddrive space in the cluster (bytes)

### Metric Category: Node Metrics

|Metric Name            	|Description|
|------------------------------	|-----------|
|memoryFree        		|Amount of memory free for the node (bytes)
|vb_replica_curr_items  	|Number of items/documents that are replicas 
|couch_docs_data_size         	|Data size of couch documents associated with a node (bytes)
|mem_total	        	|Total memory available to the node (bytes)
|mcdMemoryAllocated       	|Amount of memcached memory allocated (bytes)
|mcdMemoryReserved       	|Amount of memcached memory reserved (bytes)
|uptime       			|Time during which the node was in operation (sec)
|ep_bg_fetched       		|Number of disk fetches performed since server was started
|mem_used       		|Memory used by the node (bytes)
|memoryTotal        		|Total memory available to the node (bytes)
|get_hits  			|Number of get hits
|curr_items         		|Number of current items
|cmd_get	        	|Number of get commands
|couch_views_actual_disk_size   |Amount of disk space occupied by Couch views (bytes)
|swap_used       		|Amount of swap space used.(bytes)
|cpu_utilization_rate       	|The CPU utilization rate (%)
|couch_views_data_size       	|Size of object data for Couch views (bytes)
|curr_items_tot       		|Total number of items associated with node
|couch_docs_actual_disk_size    |Amount of disk space used by Couch docs.(bytes)
|swap_total       		|Total swap size allocated (bytes)
|ops       			|Number of operations performed on Couchbase

### Metric Category: Bucket Metrics

|Metric Name            	|Description|
|------------------------------	|-----------|
|opsPerSec        		|Number of operations per second
|rawRAM  			|Amount of raw RAM used by the bucket (bytes)
|diskFetches         		|Number of disk fetches
|ram         			|Amount of RAM used by the bucket (bytes)
|dataUsed	        	|Size of user data within buckets of the specified state that are resident in RAM.(%)
|memUsed       			|Amount of memory used by the bucket (bytes)
|itemCount       		|Number of items associated with the bucket
|diskUsed       		|Amount of disk used (bytes)
|quotaPercentUsed       	|Percentage of RAM used (for active objects) against the configure bucket size.(%)


## Custom Dashboard

![](https://raw.github.com/Appdynamics/couchbase-monitoring-extension/master/CouchBase%20Dashboard.png)

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/eXchange/CouchBase---Monitoring-Extension/idi-p/5567) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).

