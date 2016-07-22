## Introduction
In this tutorial we will see basic s3 operations: create/delete bucket, upload/download/delete object and versioning.
We will use Kleisli monad to sequence operations and Try monad to do basic failover.

## AWS S3 API

### Client
```
val s3client: AmazonS3 = new AmazonS3Client(new ProfileCredentialsProvider())
s3client.setRegion(Region.getRegion(Regions.US_EAST_1))
```

### Create Bucket
 
```
  val createBucket = Try {
    println(s"S3 create bucket: ${bucketName}")
    val bucket = s3client.createBucket(
      new CreateBucketRequest(bucketName))
    s3client.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
      bucketName, new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)))
    bucket
  }
```

### Upload Log File

```
  val uploadObject = Try {
    println(s"S3 upload log file: ${logFile.getName}")
    s3client.putObject(
      new PutObjectRequest(bucketName, logFile.getName, logFile))
  }
```

### Delete Log File

If versioning is enabled then delete will put a *Delete Marker* on the last version of the file.
```
  val deleteObject = Try {
    println(s"S3 delete log file: ${logFile.getName}")
    s3client.deleteObject(
      new DeleteObjectRequest(bucketName, logFile.getName))
  }
```

### Download Log File

If versioning is enabled then specific version of the file can be downloaded (even after deletion).
```
  val downloadObject = { versionId: String =>
    Try {
      println(s"S3 get object with version: ${versionId}")
      s3client.getObject(
        new GetObjectRequest(bucketName, logFile.getName)
          .withVersionId(versionId))
    }
  }
```

### Delete Bucket

Bucket can be deleted only if it's empty (if versioning is enabled then all file versions should be deleted as well).
```
a
```

### Sequence of Operations
Since Kleisli is a Monad, we can put our operations in a sequence.
```
  val test = for {
    bucket <- createBucket
    putObjectResult <- uploadObject
    _ <- deleteObject
    s3Obj <- downloadObject(putObjectResult.getVersionId)
  } yield ()
```

### Sequence of Operations Application

```
  val bucketName = "logs-" + UUID.randomUUID
  val res = test(bucketName)
```

### Exceptions and Recovery
AWS Api can generally throws two types of Exceptions: AmazonClientException and AmazonServiceException.
Test sequence will exit whenever one of the operations breaks and print detailed issue description.

```
  res recover {
    case exc: AmazonServiceException =>
      println(exc.getMessage)
      println(exc.getStatusCode)
      println(exc.getErrorCode)
      println(exc.getErrorType)
      println(exc.getRequestId)
      exc.getMessage
    case exc: AmazonClientException =>
      println(exc.getMessage)
      exc.getMessage
    case exc: Exception =>
      println(exc.getMessage)
      exc.getMessage
  }
```

## App

On [Bitbucket](https://bitbucket.org/yuriy_susuk/s3/overview) you can find a src code and complete description on how to build and run test application.