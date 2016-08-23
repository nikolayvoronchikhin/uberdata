package org.apache.spark.ml

import org.apache.spark.annotation.Since
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util.{DefaultParamsReadable, Identifiable}

import org.apache.spark.sql.types.{ StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row}

import scala.reflect.ClassTag

/**
  * Created by dirceu on 04/07/16.
  */
class AllColumnsTimeSeriesGenerator[T, U](override val uid: String)(implicit ct: ClassTag[T])
  extends BaseTimeSeriesGenerator {

  def this()(implicit ct: ClassTag[T]) =
    this(Identifiable.randomUID("AllColumnsTimeSeriesGenerator"))

  def setLabelCol(value: String) = set(labelCol, value)

  def setTimeCol(colName: String) = set(timeCol, colName)

  def setFeaturesCol(value: String) = set(featuresCol, value)

  /** @group setParam */
  def setInputCol(value: String): this.type = set(inputCol, value)

  /** @group setParam */
  def setOutputCol(value: String): this.type = set(outputCol, value)

  override def transform(dataSet: DataFrame): DataFrame = {
    val rdd = dataSet.rdd
    val sparkContext = dataSet.sqlContext.sparkContext
    val labelColIndex = sparkContext.broadcast(dataSet.schema.fieldIndex($(labelCol)))

    val grouped = rdd.map {
      row =>
        Row(row.getAs[T](labelColIndex.value), row.getAs[org.apache.spark.mllib.linalg.Vector]($(featuresCol)))
    } //erro aqui

    val trainSchema = transformSchema(dataSet.schema)
    dataSet.sqlContext.createDataFrame(grouped, trainSchema)
  }

  override def transformSchema(schema: StructType): StructType = {
    StructType(schema.filter(_.name == $(labelCol)).head +: Seq(StructField($(outputCol),
      new org.apache.spark.mllib.linalg.VectorUDT)))
  }

  override def copy(extra: ParamMap): AllColumnsTimeSeriesGenerator[T, U] = defaultCopy(extra)
}

@Since("1.6.0")
object AllColumnsTimeSeriesGenerator extends DefaultParamsReadable[AllColumnsTimeSeriesGenerator[_, _]] {

  @Since("1.6.0")
  override def load(path: String): AllColumnsTimeSeriesGenerator[_, _] = super.load(path)
}
