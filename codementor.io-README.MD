## Client
```
val s3client: AmazonS3 = new AmazonS3Client(new ProfileCredentialsProvider())
s3client.setRegion(Region.getRegion(Regions.US_EAST_1))
```

## Create Bucket
```
val bucketName = "logs-" + UUID.randomUUID
Try {
  println("S3 create bucket: " + bucketName)
  s3client.createBucket(new CreateBucketRequest(bucketName))
} recover {
  case exc: AmazonServiceException =>
    println(exc.getMessage)
    println(exc.getStatusCode)
    println(exc.getErrorCode)
    println(exc.getErrorType)
    println(exc.getRequestId)
  case exc: AmazonClientException =>
    println(exc.getMessage)
  case exc: Exception =>
    println(exc.getMessage)
}

```

## Upload


## Delete

## App README


## App Prerequisites

1. [AWS credentials file](http://http://docs.aws.amazon.com/java-sdk/latest/developer-guide/credentials.html) with permissions to access S3.
2. [Docker](http://www.docker.com/)
3. [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
4. [sbt](http://www.scala-sbt.org/)
