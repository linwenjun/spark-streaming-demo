package workshop.station

import java.time.Clock

import javassist.bytecode.SignatureAttribute.ArrayType
import org.apache.log4j.{Level, LogManager, Logger}
import org.apache.spark
import org.apache.spark.sql.{SparkSession, types}
import org.apache.spark.sql.types.{ArrayType, DoubleType, IntegerType, StringType, StructType}
import org.apache.spark.sql.functions._
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.Time


object Station {
  val log: Logger = LogManager.getRootLogger
  implicit val clock: Clock = Clock.systemDefaultZone()

  def main(args: Array[String]): Unit = {
    log.setLevel(Level.WARN)

    val spark = SparkSession
      .builder
      .master("local")
      .config("spark.hadoop.dfs.client.use.datanode.hostname", "true")
      .appName("Spark Workshop Station").getOrCreate()

    run(spark)

    spark.stop()
  }

  def run(spark: SparkSession): Unit = {

    val schema = types.ArrayType(new StructType()
      .add("id", StringType)
      .add("count", IntegerType)
    )
//      .add("location", new StructType()
//        .add("longtitude", DoubleType)
//        .add("latitude", DoubleType)
//        .add("timestamp", StringType)
//      ))
    import spark.implicits._
    val stream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "ec2-52-82-65-35.cn-northwest-1.compute.amazonaws.com.cn:9092")
      .option("subscribe", "test")
      .option("startingOffsets", "latest")
      .load()
//      .groupBy(window($"timestamp", "5 seconds") as 'window)




    stream


      .selectExpr("CAST(value AS STRING) as raw_payload")
      .select(explode(from_json($"raw_payload", schema)) as "data")
      .select($"data.id" as "id",
        $"data.count" as "count"
      )
//      .withWatermark("timestamp", "10 minutes")
//      .groupBy(
//        window($"timestamp", "10 minutes", "5 minutes"),
//        $"word")
//      .count()

      .writeStream

      .format("console")

      .start()
      .awaitTermination()

  }
}
