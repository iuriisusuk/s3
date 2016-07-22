## Introduction
In this tutorial we will see basic s3 operations: create/delete bucket, upload/download/delete object and versioning.
We will try to isolate our impure code.
We will use [Kleisli](http://typelevel.org/cats/tut/kleisli.html) monad to sequence operations and [Xor](http://typelevel.org/cats/tut/xor.html) monad to do a basic failover. Xor as well shows in the signature that the result of the computation
can be ```AmazonServiceException``` or a child of it. We will get Xor from Try using implicit conversion. Try is used to try/catch exceptions.

```
  type S3OperationResult[T] = AmazonClientException Xor T

  implicit def toXor[T](tr: Try[T]): S3OperationResult[T] = {
    tr match {
      case Success(bucket) => Xor.Right(bucket)
      case Failure(exc: AmazonClientException) => Xor.Left(exc)
    }
  }
```

### Other Details

*Log File* is a temporary created file with "Hello AWS S3!" text.

```
  val logFile = {
    val logFilePrefix = "tmp-"
    val logFileSuffix = ".log"
    val logFile: File = File.createTempFile(logFilePrefix, logFileSuffix)

    val writer = new PrintWriter(logFile)
    writer.write("Hello AWS S3!")
    writer.close()

    logFile
  } 
```

## AWS S3 API

### Client
```
val s3client: AmazonS3 = new AmazonS3Client(new ProfileCredentialsProvider())
s3client.setRegion(Region.getRegion(Regions.US_EAST_1))
```

### Create Bucket

Create bucket and enable versioning.
```
  val createBucket = Kleisli[S3OperationResult, String, Bucket] { bucketName: String =>
    Try {
      println(s"S3 create bucket: ${bucketName}")
      val bucket = s3client.createBucket(
        new CreateBucketRequest(bucketName))
      s3client.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
        bucketName, new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)))
      bucket
    }
  }
```

### Upload Log File

```
  val uploadObject = { logFile: File =>
    Kleisli[S3OperationResult, String, PutObjectResult] { bucketName: String =>
      Try {
        println(s"S3 upload log file: ${logFile.getName}")
        s3client.putObject(
          new PutObjectRequest(bucketName, logFile.getName, logFile))
      }
    }
  }
```

### Delete Log File

If versioning is enabled then delete will put a *Delete Marker* on the last version of the file.
```
  val deleteObject = { logFileName: String =>
    Kleisli[S3OperationResult, String, Unit] { bucketName: String =>
      Try {
        println(s"S3 delete log file: ${logFileName}")
        s3client.deleteObject(
          new DeleteObjectRequest(bucketName, logFileName))
      }
    }
  }
```

### Download Log File

If versioning is enabled then specific version of the file can be downloaded (even after deletion).
```
  val downloadObject = { versionId: String =>
    Kleisli[S3OperationResult, String, S3Object] { bucketName: String =>
      Try {
        println(s"S3 get object with version: ${versionId}")
        s3client.getObject(
          new GetObjectRequest(bucketName, logFile.getName)
            .withVersionId(versionId))
      }
    }
  }
```

### Empty And Delete Bucket

Bucket can be deleted only if it's empty (if versioning is enabled then all file versions should be deleted as well).
```
  val emptyAndDeleteBucket = Kleisli[S3OperationResult, String, Unit] { bucketName: String =>
    Try {
      println(s"S3 delete bucket: ${bucketName}")
      val versionListing = s3client.listVersions(
        new ListVersionsRequest()
          .withBucketName(bucketName))

      import scala.collection.JavaConverters._

      for (s3VersionSummary <- versionListing.getVersionSummaries.iterator().asScala) {
        s3client.deleteVersion(bucketName, s3VersionSummary.getKey(), s3VersionSummary.getVersionId())
      }
      s3client.deleteBucket(bucketName)
    }
  }

```

### Sequence of Operations

Since Kleisli is a Monad, we can put our operations in a sequence.
```
  val test = for {
    bucket <- createBucket
    putObjectResult <- uploadObject(logFile)
    _ <- deleteObject(logFile.getName)
    s3Obj <- downloadObject(putObjectResult.getVersionId)
    _ <- emptyAndDeleteBucket
  } yield ()
```

### Sequence of Operations Application

All operations need a bucket name, that's why our Kleisli arrows take it as input (aligned on it).
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
  }
```

## App

On [Bitbucket](https://bitbucket.org/yuriy_susuk/s3/overview) you can find a src code and complete description on how to build and run test application.