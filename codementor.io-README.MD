## Introduction



## AWS S3 API

### Client
```
val s3client: AmazonS3 = new AmazonS3Client(new ProfileCredentialsProvider())
s3client.setRegion(Region.getRegion(Regions.US_EAST_1))
```

### Create Bucket
 
```
  Try {
      println(s"S3 create bucket: ${bucketName}")
      s3client.createBucket(new CreateBucketRequest(bucketName))
      s3client.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
        bucketName, new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)))
    }
```

### Upload Log File

```
  Try {
    println(s"S3 upload log file: ${logFile.getName}")
    s3client.putObject(bucketName, logFile.getName, logFile)
  }
```

### Delete Log File

If versioning is enabled then delete will put a *Delete Marker* on the last version of the file.
```
  Try {
    println(s"S3 delete log file: ${logFile.getName}")
    s3client.deleteObject(bucketName, logFile.getName)
  }
```

### Download Log File

If versioning is enabled then specific version of the file can be downloaded (even after deletion).
```
Try {
    println(s"S3 get object with specified version")
    s3client.getObject(
      new GetObjectRequest(bucketName, logFile.getName)
        .withVersionId(uploadObject.get.getVersionId))
  }
```

### Delete Bucket

Bucket can be deleted only if it's empty (if versioning is enabled then all file versions should be deleted as well).
```
tbd
```

### Sequence

Since Try is a Monad, we can put our operations in a sequence.
```
  val test = for {
    bucket <- createBucket
    putObjectResult <- uploadObject
    _ <- deleteObject
    specificVersionOfS3Object <- downloadObjectWithSpecifiedVersion
  } yield specificVersionOfS3Object.getKey
```

### Exceptions and Recovery
AWS Api can generally throws two types of Exceptions: AmazonClientException and AmazonServiceException.
Test sequence will exit whenever one of the operations breaks and print detailed issue description.

```
  test recover {
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

## Test App

[Bitbucket](https://bitbucket.org/yuriy_susuk/s3/overview)