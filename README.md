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
