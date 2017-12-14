# Akka HTTP CRUD microservice connected to mongodb example
Based off of this project [here](https://github.com/theiterators/akka-http-microservice)


This project uses [Akka HTTP](http://doc.akka.io/docs/akka-http/current/scala/http/) library and Scala to write a simple REST service connected to a mongo database.
I've also included a Dockerfile for packaging this service in a container.

## Usage

Start services with sbt:

```
$ sbt run
```

### Testing

Execute tests using `test` command:

```
$ sbt
> test
```

** Note
You'll need a mongo instance running to point this service at.

MIT license.


### Building the container for local minikube
Setup docker env
```
eval $(minikube-docker-env)
```

Do a docker build of the container
```
build -t item-service:v1 .
```

Create a kube deployment
```
kubectl run item-service --image=item-service:v1 --port=9000
```

Check deployment status
```
kubectl get pods
```

Expose the pod to external requests
```
kubectl expose deployment item-service —type=LoadBalancer
```

Check the service is up
```
kubectl get services
```

Open the service in the browser
```
minikube service item-service
```

Updating the deployed image
1. rebuild docker image -> ```build -t item-service:v2 .```
2. set image for kube node ->``` kubectl set image deployment/item-service item-service=item-service:v2```
