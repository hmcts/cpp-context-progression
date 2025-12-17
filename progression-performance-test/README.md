# Instructions for running the performance tests

1. Performance test is written using Jmeter and DBUnit maven plugin. Following command can be used  to run the test.

    mvn clean verify -pl progression-performance-test -Pprogression-performance-test -Dtarget.host=localhost -Dtarget.port=8080 -Djmeter.get.progression.read.test.threads=1

	
	where target host and port should be given with appropriate values wildfly server is running on. 
	jmeter.get.progression.read.test.threads is the number of thread to be spawned.

	If run with mvn clean verify -pl progression-performance-test -Pprogression-performance-test, it will pick the default values mentioned in JMX file.

	The above command will first create the records in postgres DB in  table using DBUnit Maven plugin. The database details are configured in pom itself.

2. If you want to see and edit the jmeter tests you can launch Jmeter gui with:
    
	mvn jmeter:gui -pl progression-performance-test -Pprogression-performance-test
