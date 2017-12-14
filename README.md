# Akka HTTP microservice example
Based off of this project [~[here](https://github.com/theiterators/akka-http-microservice)]


This project demonstrates the [Akka HTTP](http://doc.akka.io/docs/akka-http/current/scala/http/) library and Scala to write a simple REST (micro)service. The project shows the following tasks that are typical for most Akka HTTP-based projects:

* starting standalone HTTP server,
* handling file-based configuration,
* logging,
* routing,
* deconstructing requests,
* unmarshalling JSON entities to Scala's case classes,
* marshaling Scala's case classes to JSON responses,
* error handling,
* persisting to mongo database

## Usage

Start services with sbt:

```
$ sbt
> ~re-start
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
