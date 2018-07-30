# Instructions for running the jar

1. create datasource file from sample (standalone-ds) provided. Make sure to change with correct host, database user and password.

2. Run with the following command
        1. touch processFile
        2. java -jar -Dorg.wildfly.swarm.mainProcessFile=/path/to/processFile -Devent.transformation.jar=progression-domain-transformations-cleanup-and-archive-1.0.30-R2-SNAPSHOT.jar event-tool-0.0.1-SNAPSHOT-swarm.jar -c standalone-ds.xml

3. Run with debug option
     java -jar -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -Dorg.wildfly.swarm.mainProcessFile=/path/to/processFile -Devent.transformation.jar=progression-domain-transformations-cleanup-and-archive-1.0.30-R2-SNAPSHOT.jar event-tool-0.0.1-SNAPSHOT-swarm.jar -c standalone-ds.xml
