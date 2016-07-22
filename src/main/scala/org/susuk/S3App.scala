package org.susuk

import java.io._
import java.util.UUID

import cats._
import cats.data.{Xor, Kleisli}
import cats.std.all._

import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{AmazonS3Client, AmazonS3}

import scala.util.{Failure, Success, Try}

object S3App extends App {
  println("test S3 start")

  val logFile = {
    val logFilePrefix = "tmp-"
    val logFileSuffix = ".log"
    val logFile: File = File.createTempFile(logFilePrefix, logFileSuffix)

    val writer = new PrintWriter(logFile)
    writer.write("Hello AWS S3!")
    writer.close()

    logFile
  }

  val s3client: AmazonS3 = new AmazonS3Client(new ProfileCredentialsProvider())
  s3client.setRegion(Region.getRegion(Regions.US_EAST_1))

  type S3OperationResult[T] = AmazonClientException Xor T

  implicit def toXor[T](tr: Try[T]): S3OperationResult[T] = {
    tr match {
      case Success(bucket) => Xor.Right(bucket)
      case Failure(exc: AmazonClientException) => Xor.Left(exc)
    }
  }

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

  val uploadObject = { logFile: File =>
    Kleisli[S3OperationResult, String, PutObjectResult] { bucketName: String =>
      Try {
        println(s"S3 upload log file: ${logFile.getName}")
        s3client.putObject(
          new PutObjectRequest(bucketName, logFile.getName, logFile))
      }
    }
  }

  val deleteObject = { logFileName: String =>
    Kleisli[S3OperationResult, String, Unit] { bucketName: String =>
      Try {
        println(s"S3 delete log file: ${logFileName}")
        s3client.deleteObject(
          new DeleteObjectRequest(bucketName, logFileName))
      }
    }
  }

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


  val test = for {
    bucket <- createBucket
    putObjectResult <- uploadObject(logFile)
    _ <- deleteObject(logFile.getName)
    s3Obj <- downloadObject(putObjectResult.getVersionId)
  //_ <- emptyAndDeleteBucket
  } yield ()

  val bucketName = "logs-" + UUID.randomUUID
  val res = test(bucketName)

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

  println("test S3 stop")
}