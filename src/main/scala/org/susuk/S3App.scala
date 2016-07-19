package org.susuk

import java.io.{PrintWriter, File}
import java.util.UUID

import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.model.CreateBucketRequest
import com.amazonaws.services.s3.{AmazonS3Client, AmazonS3}

import scala.util.Try

object S3App extends App {
  println("test S3 start")

  val s3client: AmazonS3 = new AmazonS3Client(new ProfileCredentialsProvider())
  s3client.setRegion(Region.getRegion(Regions.US_EAST_1))

  val bucketName = "logs-" + UUID.randomUUID
  Try {
    println(s"S3 create bucket: ${bucketName}")
    s3client.createBucket(new CreateBucketRequest(bucketName))
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

  val logFile = {
    val logFilePrefix = "tmp-"
    val logFileSuffix = ".log"
    val logFile: File = File.createTempFile(logFilePrefix, logFileSuffix)

    val writer = new PrintWriter(logFile)
    writer.write("Hello AWS S3!")
    writer.close()

    logFile
  }

  Try {
    println(s"S3 upload log file: ${logFile.getName}")
    s3client.putObject(bucketName, logFile.getName, logFile)
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

  println("test S3 stop")
}