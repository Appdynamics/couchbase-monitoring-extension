CouchBase Monitoring Extension
============================

This eXtension works only with the Java agent.

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
|conf            | Contains the monitor.xml |
|lib             | Contains third-party project references |
|src             | Contains source code of the CouchBase monitoring extension |
|dist            | Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file |
|build.xml       | Ant build script to package the project (required only if changing Java code) |

## Metrics

### Metric Category: Cluster Metrics

|Metric Name            	|
|------------------------------	|
|ram_total        		|
|ram_quotaUsed        		|
|ram_usedByData         	|
|ram_quotaTotal         	|
|ram_used	        	|
|hdd_total       		|
|hdd_used       		|
|hdd_usedByData       		|
|hdd_quotaTotal       		|
|hdd_free       		|

### Metric Category: Node Metrics

|Metric Name            	|
|------------------------------	|
|memoryFree        		|
|vb_replica_curr_items  	|
|clusterCompatibility         	|
|couch_docs_data_size         	|
|mem_total	        	|
|mcdMemoryAllocated       	|
|mcdMemoryReserved       	|
|uptime       			|
|ep_bg_fetched       		|
|mem_used       		|
|memoryTotal        		|
|get_hits  			|
|curr_items         		|
|mem_free         		|
|cmd_get	        	|
|couch_views_actual_disk_size   |
|swap_used       		|
|cpu_utilization_rate       	|
|couch_views_data_size       	|
|curr_items_tot       		|
|couch_docs_actual_disk_size    |
|swap_total       		|
|ops       			|

### Metric Category: Bucket Metrics

|Metric Name            	|
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

![](https://raw.github.com/Appdynamics/couchbase-monitoring-extension/master/CouchBase%20Dashboard.png?token=2880440__eyJzY29wZSI6IlJhd0Jsb2I6QXBwZHluYW1pY3MvY291Y2hiYXNlLW1vbml0b3JpbmctZXh0ZW5zaW9uL21hc3Rlci9Db3VjaEJhc2UgRGFzaGJvYXJkLnBuZyIsImV4cGlyZXMiOjEzODU1OTc2NTB9--6bbd2905a54e9debae5404fc6ba249c7a752b13b)

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/couchbase-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).

