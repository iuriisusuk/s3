## Prerequisites

1. [AWS credentials file](http://http://docs.aws.amazon.com/java-sdk/latest/developer-guide/credentials.html) with permissions to access S3.
2. [Docker](http://www.docker.com/)
3. [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
4. [sbt](http://www.scala-sbt.org/)

## S3 App

Since S3 App is running in Docker Container you need to build a Docker Image.

To create a Dockerfile (creds system property defines path to your AWS credentials file):

```
sbt -Dcreds="path/to/credentials" clean docker

```

To build a Docker Image from the Dockerfile (this will take some time to download java image from Docker Hub):

```
$ cd target/docker
$ docker build .
```

To check that image is created (lists images with the label aws.s3 that has a value s3):

```
docker images --filter "label=aws.api=s3"

```

To run S3App in Docker Container:

```
docker run default/s3

```

## Q & A

q. Why Docker?
a. Main reason is to have an isolated env to run examples (e.g. you most probably already have default creds saved some where in
the root path, that can interfere with the tests we are running)


 