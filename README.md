# CPP progression context
# Wildfly 10 configuration 

* Use `standalone-progression.xml`:

    `bin/standalone.sh -c standalone-progression.xml 
    
* Download the ActiveMQ RAR file to your `standalone/deployments` directory for the version of
ActiveMQ you are running locally, and update `standalone-progression.xml` to be consistent as shown below. The RAR files
are available from Maven Central here: [http://repo1.maven.org/maven2/org/apache/activemq/activemq-rar/].
For example, you can find the 5.11.1 RAR file at 
    [http://repo1.maven.org/maven2/org/apache/activemq/activemq-rar/5.11.1/activemq-rar-5.11.1.rar].

    The corresponding entry in standalone.xml should have the correct file name:
        <resource-adapter id="activemq">
            <archive>activemq-rar-5.11.1.rar</archive>
            ...
         </resource-adapter>

* Install the PostgreSQL driver for the version of PostgreSQL you are running locally:
    * Download the correct driver Jar from [https://jdbc.postgresql.org/download.html]
    * Place in a correctly named directory within your Wildfly home, eg: `modules/org/postgresql/main/postgresql-9.4-1201-jdbc42.jar`
    * Create a `module.xml` file in the same directory containing:
    
        <?xml version="1.0" ?>
    
        <module xmlns="urn:jboss:module:1.1" name="org.postgresql" slot="main">
        
            <resources>
                <resource-root path="postgresql-9.4-1201-jdbc42.jar"/>
            </resources>
        
            <dependencies>
                <module name="javax.api"/>
                <module name="javax.transaction.api"/>
            </dependencies>
        </module>

    
 # Misc