echo "Running Liquibase"
dbServerName=$1
dbUserName=$2
dbPassword=$3
dbPort=${5:-5432}
java -jar event-repository-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/progressioneventstore?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar aggregate-snapshot-repository-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/progressioneventstore?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar event-buffer-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/progressionviewstore?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar event-tracking-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/progressionviewstore?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar progression-viewstore-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/progressionviewstore?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar framework-system-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/progressionsystem?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar activiti-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/progressionactiviti?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi