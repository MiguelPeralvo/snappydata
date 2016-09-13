/*
 * Copyright (c) 2016 SnappyData, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */
package io.snappydata.hydra.northWind

import java.io.{File, FileOutputStream, PrintWriter}

import org.apache.spark.sql.SnappyContext
import org.apache.spark.{SparkContext, SparkConf}

object CreateAndLoadReplicatedRowTablesSparkApp {
  val conf = new SparkConf().
    setAppName("NWTestUtil Application")
  val sc = new SparkContext(conf)
  val snc = SnappyContext(sc)

  def main(args: Array[String]) {
    snc.sql("set spark.sql.shuffle.partitions=6")
    NWQueries.snc = snc
    val tableType = args(0)
    val pw = new PrintWriter(new FileOutputStream(new File("CreateAndLoadReplicatedRowTablesSparkApp.out"), true));
    NWTestUtil.dropTables(snc)
    println(s"Create and load ${tableType} tables Test started")
    tableType match {
      case "Replicated Row" => NWTestUtil.createAndLoadReplicatedTables(snc)
      case "Partitioned Row" => NWTestUtil.createAndLoadPartitionedTables(snc)
      case "Column" => NWTestUtil.createAndLoadColumnTables(snc)
      case "Colocated" => NWTestUtil.createAndLoadColocatedTables(snc)
      case _ => // the default, catch-all
    }
    NWTestUtil.createAndLoadReplicatedTables(snc)
    NWTestUtil.validateQueries(snc, tableType + "  Table", pw)
    println(s"Create and load ${tableType} tables Test completed successfully")
    pw.close()
  }
}