# Configuration Guide

This document will detail how to configure a MySQL database.

### Create a new database using Docker
```bash
export MYSQL_ROOT_PASSWORD=root
export MYSQL_DATABASE=rvf_master
export MYSQL_USER=rvf_user
export MYSQL_PASSWORD=password
export MYSQL_VERSION=8.0
export MYSQL_NETWORK=mysql-rvf-network

docker network create $MYSQL_NETWORK || true

docker run -d \
  --name mysql-rvf \
  -e MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD \
  -e MYSQL_DATABASE=$MYSQL_DATABASE \
  -e MYSQL_USER=$MYSQL_USER \
  -e MYSQL_PASSWORD=$MYSQL_PASSWORD \
  -e MYSQL_SSL='false' \
  -p 3306:3306 \
  -v mysql-rvf-data-$MYSQL_VERSION:/var/lib/mysql \
  --network $MYSQL_NETWORK \
  mysql:$MYSQL_VERSION \
  --local-infile=1

sleep 30

docker exec -i mysql-rvf mysql -u root -p$MYSQL_ROOT_PASSWORD -e "GRANT ALL PRIVILEGES ON *.* TO '$MYSQL_USER'@'%' WITH GRANT OPTION; FLUSH PRIVILEGES;"

```

### Create a new database using source
Alternatively, follow the steps described on [this page](https://dev.mysql.com/doc/refman/8.0/en/installing.html) to install and configure MySQL 8.

If the MySQL database has been set up outside of Docker, the command to update the user's privileges will still need
run:

``` bash
mysql -u root

CREATE USER 'rvf_user'@'%' IDENTIFIED BY 'password';

GRANT ALL PRIVILEGES ON *.* TO 'rvf_user'@'%' WITH GRANT OPTION; FLUSH PRIVILEGES;
```

### Create new property file
```bash 
touch ../src/main/resources/application-local.properties

echo spring.datasource.username=$MYSQL_USER >> ../src/main/resources/application-local.properties
echo spring.datasource.password=$MYSQL_PASSWORD >> ../src/main/resources/application-local.properties
echo "spring.datasource.url=jdbc:mysql://localhost:3306/?useSSL=false&allowLoadLocalInfile=true&sessionVariables=sql_mode='STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION'&allowPublicKeyRetrieval=true" >> ../src/main/resources/application-local.properties 
```

### Use new property file
Use the new property file with either `spring.config.additional-location` or `spring.profiles.active`.
