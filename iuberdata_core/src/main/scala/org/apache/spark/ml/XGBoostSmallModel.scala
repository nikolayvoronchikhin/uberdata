/*
 * Copyright 2015 eleflow.com.br.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.ml

import eleflow.uberdata.IUberdataForecastUtil
import eleflow.uberdata.enums.SupportedAlgorithm
import eleflow.uberdata.models.UberXGBOOSTModel
import ml.dmlc.xgboost4j.scala.DMatrix
import ml.dmlc.xgboost4j.{LabeledPoint => XGBLabeledPoint}
import org.apache.hadoop.fs.Path
import org.apache.spark.ml.XGBoostSmallModel.XGBoostRegressionModelWriter
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.param.shared.{HasFeaturesCol, HasGroupByCol, HasIdCol, HasLabelCol}
import org.apache.spark.ml.util.{DefaultParamsReader, _}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{DoubleType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.Logging

import scala.reflect.ClassTag

/**
  * Created by dirceu on 30/06/16.
  */
class XGBoostSmallModel[G](
  override val uid: String,
  val models: RDD[(G, (UberXGBOOSTModel, Seq[(ModelParamEvaluation[G])]))])(
  implicit gt: ClassTag[G],
  ord: Ordering[G] = null)
    extends ForecastBaseModel[XGBoostSmallModel[G]]
    with HasGroupByCol
    with HasIdCol
    with HasFeaturesCol
    with HasLabelCol
    with MLWritable
    with ForecastPipelineStage {

  private var trainingSummary: Option[XGBoostTrainingSummary[G]] = None

  def setGroupByCol(value: String): this.type = set(groupByCol, value)

  def setIdCol(value: String): this.type = set(idCol, value)

  def setLabelCol(value: String): this.type = set(labelCol, value)

  def setSummary(summary: XGBoostTrainingSummary[G]) = {
    trainingSummary = Some(summary)
    this
  }

  override def write: MLWriter = new XGBoostRegressionModelWriter(this)

  override def transform(dataSet: DataFrame): DataFrame = {
    val schema = dataSet.schema
    val predSchema = transformSchema(schema)

    val joined = models.join(dataSet.map(r => (r.getAs[G]($(groupByCol)), r)))

    val predictions = joined.map {
      case (id, ((bestModel, metrics), row)) =>
        val features =
          row.getAs[org.apache.spark.mllib.linalg.Vector](IUberdataForecastUtil.FEATURES_COL_NAME)
        val idColumnIndex = row.fieldIndex($(idCol))
        val featuresIndex = row.fieldIndex(IUberdataForecastUtil.FEATURES_COL_NAME)
        val groupByColumnIndex = row.fieldIndex($(groupByCol))
        val rowValues = row.toSeq.zipWithIndex.filter {
          case (_, index) =>
            index == idColumnIndex ||
              index == featuresIndex || index == groupByColumnIndex
        }.map(_._1)
        val featuresAsFloat = features.toArray.map(_.toFloat)
        val labeledPoints = Iterator(XGBLabeledPoint.fromDenseVector(0, featuresAsFloat))
        val forecast = bestModel.boosterInstance
          .predict(new DMatrix(labeledPoints, null))
          .flatMap(_.map(_.toDouble))
        Row(
          rowValues :+ SupportedAlgorithm.XGBoostAlgorithm.toString :+
            bestModel.params.map(f => f._1 -> f._2.toString) :+ forecast.head: _*)
    }
    dataSet.sqlContext.createDataFrame(predictions, predSchema).cache
  }

  override def transformSchema(schema: StructType): StructType =
    StructType(
      super
        .transformSchema(schema)
        .filter(
          f =>
            Seq(
              $(idCol),
              IUberdataForecastUtil.FEATURES_COL_NAME,
              $(featuresCol),
              $(groupByCol),
              IUberdataForecastUtil.ALGORITHM,
              IUberdataForecastUtil.PARAMS).contains(f.name)))
      .add(StructField(IUberdataForecastUtil.FEATURES_PREDICTION_COL_NAME, DoubleType))

  override def copy(extra: ParamMap): XGBoostSmallModel[G] = {
    val newModel = copyValues(new XGBoostSmallModel[G](uid, models), extra)
    trainingSummary.map(summary => newModel.setSummary(summary))
    newModel
      .setGroupByCol($(groupByCol))
      .setIdCol($(idCol))
      .setValidationCol($(validationCol))
      .asInstanceOf[XGBoostSmallModel[G]]
  }
}

object XGBoostSmallModel extends MLReadable[XGBoostSmallModel[_]] {

  override def read: MLReader[XGBoostSmallModel[_]] = null

  private[XGBoostSmallModel] class XGBoostRegressionModelWriter(instance: XGBoostSmallModel[_])
      extends MLWriter
      with Logging {

    override protected def saveImpl(path: String): Unit = {
      // Save metadata and Params
      DefaultParamsWriter.saveMetadata(instance, path, sc)
      val dataPath = new Path(path, "data").toString
      instance.models.saveAsObjectFile(dataPath)
    }
  }

  private class XGBoostRegressionModelReader[G](implicit kt: ClassTag[G], ord: Ordering[G] = null)
      extends MLReader[XGBoostSmallModel[G]] {

    /** Checked against metadata when loading model */
    private val className = classOf[XGBoostSmallModel[G]].getName

    override def load(path: String): XGBoostSmallModel[G] = {
      val metadata = DefaultParamsReader.loadMetadata(path, sc, className)

      val dataPath = new Path(path, "data").toString
      val models = sc.objectFile[(G, (UberXGBOOSTModel, Seq[ModelParamEvaluation[G]]))](dataPath)

      val arimaModel = new XGBoostSmallModel[G](metadata.uid, models)

      DefaultParamsReader.getAndSetParams(arimaModel, metadata)
      arimaModel
    }
  }

}
