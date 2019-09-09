CONTEXT_NAME=progression

mvn -f ${CONTEXT_NAME}-viewstore/${CONTEXT_NAME}-viewstore-liquibase/pom.xml -Dliquibase.url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}viewstore -Dliquibase.username=${CONTEXT_NAME} -Dliquibase.password=${CONTEXT_NAME} -Dliquibase.logLevel=info resources:resources liquibase:update
