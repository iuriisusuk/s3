package org.susuk

import java.util.UUID

import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.model.CreateBucketRequest
import com.amazonaws.services.s3.{AmazonS3Client, AmazonS3}

import scala.util.Try

object S3Main extends App {
  println("S3 start")

  val s3client: AmazonS3 = new AmazonS3Client(new ProfileCredentialsProvider())
  s3client.setRegion(Region.getRegion(Regions.US_EAST_1))

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

  println("S3 stop")
}