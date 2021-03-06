package com.stratio.spark.structured.streaming.help

import akka.event.slf4j.SLF4JLogging
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.expressions.Literal
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.OutputMode


object BasicMain extends App with SLF4JLogging {

  /** Creating context **/

  val sparkConf = new SparkConf()
    .setAppName("structured-streaming-basic-tests")
    .setMaster("local[*]")
  val sparkSession = SparkSession.builder()
    .config(sparkConf)
    .getOrCreate()

  import sparkSession.implicits._


  /** Initializing source streams **/

  // Reading from fixed data
  val fixedData = sparkSession.readStream
    .format("rate")
    .load()

  fixedData.printSchema()

  // Reading from Socket nc -lk 9999
  val lines = sparkSession.readStream
    .format("socket")
    .option("host", "127.0.0.1")
    .option("port", 9999)
    .load()
  lines.printSchema()


  /** Creating queries to execute **/

  val fixedQuery = fixedData.writeStream
    .outputMode(OutputMode.Append())
    //.outputMode(OutputMode.Update()) //No aggregations! Same as Append
    //.outputMode(OutputMode.Complete()) //No aggregations!
    .format("console")
    //.trigger(Trigger.Once())
    //.trigger(Trigger.ProcessingTime(6, TimeUnit.SECONDS))
    //.trigger(Trigger.ProcessingTime(10, TimeUnit.SECONDS))
    .queryName("fixedQuery")

  // First query from socket (only one query supported with this source)
  val linesQuery = lines.writeStream
    .outputMode(OutputMode.Append())
    .format("console")
    .queryName("linesQuery")

  // Split the lines into words
  val words = lines.as[String]
    .flatMap(lineString => lineString.split(" "))
    .withColumn("company", lit(Literal("stratio")))
    .withColumn("employees", lit(Literal(300)))
  words.printSchema()

  // Second query over modified data
  val wordsQuery = words.writeStream
    .outputMode(OutputMode.Append())
    .format("console")
    .queryName("wordsQuery")


  /** Start queries **/

  val fixedQueryExecution = fixedQuery.start()
  val linesQueryExecution = linesQuery.start()
  //val wordsQueryExecution = wordsQuery.start()


  /** Manage execution **/

  fixedQueryExecution.awaitTermination()
  linesQueryExecution.awaitTermination()
  //wordsQueryExecution.awaitTermination()

  //sparkSession.streams.awaitAnyTermination()

}

