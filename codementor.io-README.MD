## Client
```
val s3client: AmazonS3 = new AmazonS3Client(new ProfileCredentialsProvider())
s3client.setRegion(Region.getRegion(Regions.US_EAST_1))
```

## Create Bucket
Versioning is enabled to show that file was indeed created and then got a *Delete Marker*
```
  Try {
      println(s"S3 create bucket: ${bucketName}")
      s3client.createBucket(new CreateBucketRequest(bucketName))
      s3client.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
        bucketName, new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)))
    } recover {
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

## Upload Log File
```
  Try {
    println(s"S3 upload log file: ${logFile.getName}")
    s3client.putObject(bucketName, logFile.getName, logFile)
  }
```

## Delete Log File
```
  Try {
    println(s"S3 delete log file: ${logFile.getName}")
    s3client.deleteObject(bucketName, logFile.getName)
  }
```

## Delete Bucket
Bucket can be deleted only if it's empty (if versioning is enabled then all file versions should be deleted as well).
```
tbd
```

## Test

## App README
On [Bitbucket](https://bitbucket.org/yuriy_susuk/s3/overview)