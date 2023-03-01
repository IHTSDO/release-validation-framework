Using Docker
===================

These instructions will provide guidance in getting the RVF running on your local machine for development and testing using docker.


Prerequisites
-------------

You will need:
- [Git Client](https://git-scm.com/) to clone the project
- [Docker](https://www.docker.com/get-started) for docker(!)

Starting the application
------------------------

Clone the project then start using docker-compose:

```bash
git clone https://github.com/IHTSDO/release-validation-framework.git
```
```bash
cd release-validation-framework
```
```bash
docker-compose up -d
```

Go to <http://localhost:8081/api/swagger-ui.html> and you should see the Swagger API documentation page.

For more information on what to do next, go to the [getting started page](using-the-api.md).

You can use the same to restart the application by using `docker-compose up -d` as specific docker volumes are used in to ensure that the data survives across docker image changes. 

If you want to recreate a fresh install, look for the `release-validation-framework_mysql` docker volume on your docker server, delete it and then restart the docker containers

Stopping the application
------------------------

```bash
docker-compose stop
```
