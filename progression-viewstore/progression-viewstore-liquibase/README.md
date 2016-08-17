# Instructions for creating the database

1. Install Postgres 9.4 or later
2. Create a user called postgres
3. Create a database called progression-view-store-db
4. Run with the following command:

    mvn resources:resources liquibase:update -Dliquibase.url=jdbc:postgresql://localhost:5432/progressionviewstore -Dliquibase.username=postgres -Dliquibase.password=postgres -Dliquibase.logLevel=info
    
   Or
   
    java -jar progression-view-db-liquibase-1.0-SNAPSHOT.jar --url=jdbc:postgresql://localhost:5432/progression-view-store-db --username=postgres --password=postgres --logLevel=info update
    
5. for generating change log 
	mvn resources:resources liquibase:generateChangeLog

