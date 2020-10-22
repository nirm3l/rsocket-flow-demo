# Project Flow Demo   
Welcome to the flow demo source code repository.      
      
## Getting started   
Project is based on microservice architecture by using [Spring Cloud stack](https://spring.io/projects/spring-cloud).    

## List of available microservices.     

### Task Service    
Manage queue of tasks for cleaning songs    
    
### User Service    
Manage users and permissions    
    
### Gateway    
Single entry point for all Frontend apps under one user-facing app. Gateway is based on Spring Cloud Gateway. It aims to provide a simple, yet effective way to route to APIs and provide cross cutting concerns to them such as: security, monitoring/metrics, and resiliency. It's available on **8080** port.    
  
### Configuration server (config-server)    
Provides server-side and client-side support for externalized configuration in a distributed system. With the Config Server, you have a central place to manage external properties for applications across all environments.    
    
Configurations for all services (and all environments) are available inside configuration folder. Global configurations are available inside config/application.yaml file. Specific configurations should go to service-name.yaml file. Configurations specific to environment should to to application-env.yaml (e.g. application-dev.yaml, gateway-prod.yaml, song-search-service-local.yaml). Configuration server is available on **8888** port.    

### Admin    
Administration panel for all registered services. Used for managing and monitoring registered services. Each services is considered as a client and registers to the admin server. Behind the scenes, the magic is given by the Spring Boot Actuator endpoints. It's available on **8085** port.    
    
### Shared dependencies

It contains shared dependencies which are build in way that they can be reused in other projects.

List of dependencies:
 - business-service-starter
 
### Pre-requisites

Java JDK 11

Docker compose:
https://hub.docker.com/editions/community/docker-ce-desktop-mac


### Build services and run using docker-compose

You can build all microservices by running (inside root folder):

    mvn -T 1C clean package

Start environment by running:

    docker-compose up -d

### Fast building on local environment

To skip docker files generation and tests execution you can build application by using specific profile:

    mvn -T 1C clean package -PskipDockerAndTests
    
    mvn -T 1C clean package -PskipDocker

### Running on local environment

To get running services on local environment (e.g. inside Intellij) outside docker, you will need to add spring profile "local".

### Tips and tricks

To get faster Spring Boot applications startup on macos try next tip.

Execute:

    hostname

Use result to edit hosts file:

    sudo vi /etc/hosts
    
Update localhost line to:
    
    127.0.0.1       localhost       {result-of-hostname}.local 

Spring Boot startup should be much faster now.

To get faster service change redeployment, do not restart service, inside Intellij just go to Build -> Recompile (class) or Build -> Build (specific module) and service Spring Boot application will restart automatically in fast mode.

