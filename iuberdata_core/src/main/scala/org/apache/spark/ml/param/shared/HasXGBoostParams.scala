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

package org.apache.spark.ml.param.shared

import org.apache.spark.ml.param.{Param, Params}

/**
  * Created by dirceu on 07/07/16.
  */
trait HasXGBoostParams extends Params {

  final val xGBoostParams: Param[Map[String, Any]] =
    new Param[Map[String, Any]](
      this,
      "xgboostparams",
      "XGBoost algorithm parameters"
    )
  final val xGBoostRounds: Param[Int] = new Param[Int](
    this,
    "xgboostRounds",
    "the number of rounds used by XGBoost algorithm"
  )

  setDefault(xGBoostRounds, 2000)

  setDefault(
    xGBoostParams,
    Map[String, Any](
      "silent" -> 1,
      "objective" -> "reg:linear",
      "booster" -> "gbtree",
      "eta" -> 0.0225,
      "max_depth" -> 26,
      "subsample" -> 0.63,
      "colsample_btree" -> 0.63,
      "min_child_weight" -> 9,
      "gamma" -> 0,
      "eval_metric" -> "rmse",
      "tree_method" -> "exact"
    ).map(f => (f._1, f._2.asInstanceOf[AnyRef]))
  )

  /** @group getParam */
  final def getXGBoostParams: Map[String, Any] = $(xGBoostParams)
  final def getXGBoostRounds: Int = $(xGBoostRounds)
}
