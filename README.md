CouchBase Monitoring Extension
============================

This eXtension works only with the Java agent.

## Use Case

Couchbase Server is an open source, distributed (shared-nothing architecture) NoSQL document-oriented database that is optimized for interactive applications. This extension allows the user to connect to specific cluster host and retrieve metrics about the cluster, all the nodes within the cluster and any buckts associated with the nodes.  

## Installation
<ol>
	<li>Type 'ant package' in the command line from the couchbase-monitoring-extension directory.
	</li>
	<li>Deploy the file CouchBaseMonitor.zip found in the 'dist' directory into the &lt;machineagent install dir&gt;/monitors/ directory.
	</li>
	<li>Unzip the deployed file.
	</li>
	<li>Open &lt;machineagent install dir&gt;/monitors/CouchBaseMonitor/conf/monitor.xml and configure the CouchBase parameters.
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

### Metric Category: CouchBase

|Metric Name           | Description     |
|----------------------|-----------------|
|database_writes       | Number of times a database was changed |
|database_reads        | Number of times a document was read from a database |
|open_databases        | Number of open databases |
|open_os_files         | Number of file descriptors CouchBase has open |
|request_time          | Length of a request inside CouchBase without MochiWeb |

### Metric Category: httpd

|Metric Name           | Description     |
|----------------------|-----------------|
|bulk_requests         | Number of bulk requests |
|requests              | Number of HTTP requests |
|temporary_view_reads  | Number of temporary view reads |
|view_reads            | Number of view reads |

### Metric Category: httpd_request_methods

|Metric Name           | Description     |
|----------------------|-----------------|
|COPY       		   | Number of HTTP COPY requests |
|DELETE                | Number of HTTP DELETE requests |
|GET                   | Number of HTTP GET requests |
|HEAD                  | Number of HTTP HEAD requests |
|MOVE                  | Number of HTTP MOVE requests |
|POST                  | Number of HTTP POST requests |
|PUT                   | Number of HTTP PUT requests |

### Metric Category: httpd_status_codes

|Metric Name           | Description     |
|----------------------|-----------------|
|201       			   | Number of HTTP 200 OK responses |
|201        		   | Number of HTTP 201 Created responses |
|202        		   | Number of HTTP 202 Accepted responses |
|301         		   | Number of HTTP 301 Moved Permanently responses |
|304          		   | Number of HTTP 304 Not Modified responses |
|400         		   | Number of HTTP 400 Bad Request responses |
|401                   | Number of HTTP 401 Unauthorized responses|
|403  				   | Number of HTTP 403 Forbidden responses |
|404            	   | Number of HTTP 404 Not Found responses |
|405       		   	   | Number of HTTP 405 Method Not Allowed responses |
|409                   | Number of HTTP 409 Conflict responses |
|412                   | Number of HTTP 412 Precondition Failed responses |
|500                   | Number of  HTTP 500 Internal Server Error responses} |

## Custom Dashboard

![](https://raw.github.com/Appdynamics/couchedb-monitoring-extension/master/couchedb%20Dashboard.png?token=2880440__eyJzY29wZSI6IlJhd0Jsb2I6QXBwZHluYW1pY3MvZWhjYWNoZS1tb25pdG9yaW5nLWV4dGVuc2lvbi9tYXN0ZXIvRWhjYWNoZSBEYXNoYm9hcmQucG5nIiwiZXhwaXJlcyI6MTM4NDM2NzI4Mn0%3D--a6f98fa60151f8b5c0823c39fb52770d147e55bf)

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/couchbase-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).

