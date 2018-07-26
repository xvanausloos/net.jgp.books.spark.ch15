package net.jgp.books.sparkWithJava.ch12.lab900.lowLevelTransformationAndActionApp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.ForeachFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.KeyValueGroupedDataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Low level transformations.
 * 
 * @author jgp
 */
public class LowLevelTransformationAndActionApp implements Serializable {

  private static final long serialVersionUID = -17568L;

  /**
   * main() is your entry point to the application.
   * 
   * @param args
   */
  public static void main(String[] args) {
    LowLevelTransformationAndActionApp app =
        new LowLevelTransformationAndActionApp();
    app.start();
  }

  private final class CountyFipsExtractorUsingMap
      implements MapFunction<Row, String> {
    private static final long serialVersionUID = 26547L;

    @Override
    public String call(Row r) throws Exception {
      String s = r.getAs("id2").toString().substring(2);
      return s;
    }
  }

  private final class SmallCountiesUsingFilter implements FilterFunction<Row> {
    private static final long serialVersionUID = 17392L;

    @Override
    public boolean call(Row r) throws Exception {
      if (r.getInt(4) < 30000) {
        return true;
      }
      return false;
    }
  }

  public class CountyStateExtractorUsingFlatMap
      implements FlatMapFunction<Row, String> {
    private static final long serialVersionUID = 63784L;

    @Override
    public Iterator<String> call(Row r) throws Exception {
      String[] s = r.getAs("Geography").toString().split(", ");
      return Arrays.stream(s).iterator();
    }
  }

  public class FirstCountyAndStateOfPartitionUsingMapPartitions
      implements MapPartitionsFunction<Row, String> {
    private static final long serialVersionUID = -62694L;

    @Override
    public Iterator<String> call(Iterator<Row> input) throws Exception {
      Row r = input.next();
      String[] s = r.getAs("Geography").toString().split(", ");
      return Arrays.stream(s).iterator();
    }
  }

  private final class ForeachFunctionExample
      implements ForeachFunction<Row> {
    private static final long serialVersionUID = 14738L;
    private int count = 0;

    @Override
    public void call(Row r) throws Exception {
      if (count < 10) {
        System.out.println(r.getAs("Geography").toString() + " had "
            + r.getAs("real2010").toString() + " inhabitants in 2010.");
      }
      count++;
    }
  }

  /**
   * The processing code.
   */
  private void start() {

    SparkSession spark = SparkSession.builder()
        .appName("Low level transofrmation and actions")
        .master("local")
        .getOrCreate();

    Dataset<Row> df = spark.read().format("csv")
        .option("header", "true")
        .option("inferSchema", "true")
        .load(
            "data/PEP_2017_PEPANNRES.csv");
    df = df.withColumnRenamed("GEO.id", "id")
        .withColumnRenamed("GEO.id2", "id2")
        .withColumnRenamed("GEO.display-label", "Geography")
        .withColumnRenamed("rescen42010", "real2010")
        .drop("resbase42010")
        .withColumnRenamed("respop72010", "estimate2010")
        .withColumnRenamed("respop72011", "estimate2011")
        .withColumnRenamed("respop72012", "estimate2012")
        .withColumnRenamed("respop72013", "estimate2013")
        .withColumnRenamed("respop72014", "estimate2014")
        .withColumnRenamed("respop72015", "estimate2015")
        .withColumnRenamed("respop72016", "estimate2016")
        .withColumnRenamed("respop72017", "estimate2017");
    df.printSchema();
    df.show(5);

    // Transformation
    System.out.println("map()");
    Dataset<String> dfMap = df.map(new CountyFipsExtractorUsingMap(),
        Encoders.STRING());
    dfMap.show(5);

    System.out.println("filter()");
    Dataset<Row> dfFilter = df.filter(new SmallCountiesUsingFilter());
    dfFilter.show(5);

    System.out.println("flatMap()");
    Dataset<String> dfFlatMap = df.flatMap(
        new CountyStateExtractorUsingFlatMap(),
        Encoders.STRING());
    dfFlatMap.show(5);

    System.out.println("mapPartitions()");
    Dataset<Row> dfPartitioned = df.repartition(10);
    Dataset<String> dfMapPartitions = dfPartitioned.mapPartitions(
        new FirstCountyAndStateOfPartitionUsingMapPartitions(),
        Encoders.STRING());
    System.out.println("Input dataframe has " + df.count() + " records");
    System.out.println("Result dataframe has " + dfMapPartitions.count()
        + " records");
    dfMapPartitions.show(5);

    System.out.println("groupByKey()");
    KeyValueGroupedDataset<String, Row> dfGroupByKey = df.groupByKey(
        new CountyFipsExtractorUsingMap(), Encoders.STRING());
    dfGroupByKey.count().show(5);

    // Action
    // df.foreach(new ForeachFunctionExample());
  }
}