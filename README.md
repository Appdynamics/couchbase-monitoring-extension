CouchBase Monitoring Extension
============================

This extension works only with the standalone machine agent.

## Use Case

Couchbase Server is an open source, distributed (shared-nothing architecture) NoSQL document-oriented database that is optimized for interactive applications. This extension allows the user to connect to a specific cluster host and retrieve metrics about the cluster, all the nodes within the cluster and any buckets associated with the nodes.  

## Installation
<ol>
	<li>Type 'ant package' in the command line from the couchbase-monitoring-extension directory.
	</li>
	<li>Deploy the file CouchBaseMonitor.zip found in the 'dist' directory into the &lt;machineagent install dir&gt;/monitors/ directory.
	</li>
	<li>Unzip the deployed file.
	</li>
	<li>Open &lt;machineagent install dir&gt;/monitors/CouchBaseMonitor/monitor.xml and configure the CouchBase parameters.
<p></p>
<pre>
	 &lt;argument name="host" is-required="false" default-value="localhost"/&gt;
         &lt;argument name="port" is-required="false" default-value="8091"/&gt;
         &lt;argument name="username" is-required="false" default-value="username"/&gt;
         &lt;argument name="password" is-required="false" default-value="password"/&gt;
         &lt;argument name="disabled-metrics-path" is-required="false" default-value="monitors/CouchBaseMonitor/conf/DisabledMetrics.xml"/&gt;
</pre>
	</li>
	<li>Open &lt;machineagent install dir&gt;/monitors/CouchBaseMonitor/conf/DisabledMetrics.xml and configure the list of disabled metrics. Here is a sample configuration of the disabled metrics:
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
|conf            | Contains the monitor.xml, DisabledMetrics.xml |
|lib             | Contains third-party project references |
|src             | Contains source code of the CouchBase monitoring extension |
|dist            | Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file |
|build.xml       | Ant build script to package the project (required only if changing Java code) |

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
|mcdMemoryAllocated       	|N/A
|mcdMemoryReserved       	|N/A
|uptime       			|Time during which the node was in operation (sec)
|ep_bg_fetched       		|Number of disk fetches performed since server was started
|mem_used       		|Memory used by the node (bytes)
|memoryTotal        		|Total memory available to the node (bytes)
|get_hits  			|Number of get hits
|curr_items         		|Number of current items
|cmd_get	        	|Number of get commands
|couch_views_actual_disk_size   |N/A
|swap_used       		|N/A
|cpu_utilization_rate       	|The CPU utilization rate (%)
|couch_views_data_size       	|N/A
|curr_items_tot       		|Total number of items associated with node
|couch_docs_actual_disk_size    |N/A
|swap_total       		|N/A
|ops       			|N/A

### Metric Category: Bucket Metrics

|Metric Name            	|Descri
|------------------------------	|
|opsPerSec        		|
|rawRAM  			|
|diskFetches         		|
|ram         			|
|dataUsed	        	|
|memUsed       			|
|itemCount       		|
|diskUsed       		|
|quotaPercentUsed       	|


## Custom Dashboard

![](https://raw.github.com/Appdynamics/couchbase-monitoring-extension/master/CouchBase%20Dashboard.png)

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/couchbase-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).

