package org.susuk

import java.io._
import java.util.UUID

import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{AmazonS3Client, AmazonS3}

import scala.util.Try

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

  val bucketName = "logs-" + UUID.randomUUID

  val createBucket = Try {
    println(s"S3 create bucket: ${bucketName}")
    val bucket = s3client.createBucket(
      new CreateBucketRequest(bucketName))
    s3client.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
      bucketName, new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)))
    bucket
  }

  val uploadObject = Try {
    println(s"S3 upload log file: ${logFile.getName}")
    s3client.putObject(
      new PutObjectRequest(bucketName, logFile.getName, logFile))
  }

  val deleteObject = Try {
    println(s"S3 delete log file: ${logFile.getName}")
    s3client.deleteObject(
      new DeleteObjectRequest(bucketName, logFile.getName))
  }

  val downloadObject = { versionId: String =>
    Try {
      println(s"S3 get object with version: ${versionId}")
      s3client.getObject(
        new GetObjectRequest(bucketName, logFile.getName)
          .withVersionId(versionId))
    }
  }

  val test = for {
    bucket <- createBucket
    putObjectResult <- uploadObject
    _ <- deleteObject
    s3Obj <- downloadObject(putObjectResult.getVersionId)
  } yield ()

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

  println("test S3 stop")
}