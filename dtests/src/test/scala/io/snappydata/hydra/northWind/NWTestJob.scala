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

import com.typesafe.config.Config
import io.snappydata.hydra.northWind
import org.apache.spark.sql._

import scala.util.{Failure, Success, Try}

object NWTestJob extends SnappySQLJob {
  var regions, categories, shippers, employees, customers, orders, order_details, products, suppliers, territories, employee_territories: DataFrame = null

  def getCurrentDirectory = new java.io.File(".").getCanonicalPath

  override def runSnappyJob(snc: SnappyContext, jobConfig: Config): Any = {
    val pw = new PrintWriter(new FileOutputStream(new File("NWTestSnappyJob.out"), true));
    Try {
      snc.sql("set spark.sql.shuffle.partitions=6")
      northWind.NWQueries.snc = snc
      val dataLocation = jobConfig.getString("dataFilesLocation")
      println(s"SS - dataLocation is : ${dataLocation}")
      regions = snc.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .load(s"$dataLocation/regions.csv")
      categories = snc.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .load(s"$dataLocation/categories.csv")
      shippers = snc.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .load(s"$dataLocation/shippers.csv")
      employees = snc.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .load(s"$dataLocation/employees.csv")
      customers = snc.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .load(s"$dataLocation/customers.csv")
      orders = snc.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .load(s"$dataLocation/orders.csv")
      order_details = snc.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .load(s"$dataLocation/order-details.csv")
      products = snc.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .load(s"$dataLocation/products.csv")
      suppliers = snc.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .load(s"$dataLocation/suppliers.csv")
      territories = snc.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .load(s"$dataLocation/territories.csv")
      employee_territories = snc.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .load(s"$dataLocation/employee-territories.csv")
      dropTables(snc)
      println("Test replicated row tables queries started")
      createAndLoadReplicatedTables(snc)
      validateQueries(snc, "Replicated Row Table", pw)
      println("Test replicated row tables queries completed successfully")
      dropTables(snc)
      println("Test partitioned row tables queries started")
      createAndLoadPartitionedTables(snc)
      validateQueries(snc, "Partitioned Row Table", pw)
      println("Test partitioned row tables queries completed successfully")
      dropTables(snc)
      println("Test column tables queries started")
      createAndLoadColumnTables(snc)
      validateQueries(snc, "Column Table", pw)
      println("Test column tables queries completed successfully")
      dropTables(snc)
      createAndLoadColocatedTables(snc)
      validateQueries(snc, "Colocated Table", pw)
      pw.close()
    } match {
      case Success(v) => pw.close()
        s"See ${getCurrentDirectory}/NWTestSnappyJob.out"
      case Failure(e) => pw.close();
        throw e;
    }
  }

  private def assertJoin(snc: SnappyContext, sqlString: String, numRows: Int, queryNum: String, tableType: String, pw: PrintWriter): Any = {
    snc.sql("set spark.sql.crossJoin.enabled = true")
    val df = snc.sql(sqlString)
    pw.println(s"Query ${queryNum} \n df.count for join query is : ${df.count} \n Expected numRows : ${numRows} \n Table Type : ${tableType}")
    assert(df.count() == numRows,
      s"Mismatch got for query ${queryNum} : df.count ->" + df.count() + " but expected numRows ->" + numRows
        + " for query =" + sqlString + " Table Type : " + tableType)
  }

  private def assertQuery(snc: SnappyContext, sqlString: String, numRows: Int, queryNum: String, tableType: String, pw: PrintWriter): Any = {
    val df = snc.sql(sqlString)
    pw.println(s"Query ${queryNum} \n df.count is : ${df.count} \n Expected numRows : ${numRows} \n Table Type : ${tableType}")
    assert(df.count() == numRows,
      s"Mismatch got for query ${queryNum} : df.count ->" + df.count() + " but expected numRows ->" + numRows
        + " for query =" + sqlString + " Table Type : " + tableType)
  }

  private def createAndLoadReplicatedTables(snc: SnappyContext): Unit = {
    snc.sql(northWind.NWQueries.regions_table)
    regions.write.insertInto("regions")

    snc.sql(northWind.NWQueries.categories_table)
    categories.write.insertInto("categories")

    snc.sql(northWind.NWQueries.shippers_table)
    shippers.write.insertInto("shippers")

    snc.sql(northWind.NWQueries.employees_table)
    employees.write.insertInto("employees")

    snc.sql(northWind.NWQueries.customers_table)
    customers.write.insertInto("customers")

    snc.sql(northWind.NWQueries.orders_table)
    orders.write.insertInto("orders")

    snc.sql(northWind.NWQueries.order_details_table)
    order_details.write.insertInto("order_details")

    snc.sql(northWind.NWQueries.products_table)
    products.write.insertInto("products")

    snc.sql(northWind.NWQueries.suppliers_table)
    suppliers.write.insertInto("suppliers")

    snc.sql(northWind.NWQueries.territories_table)
    territories.write.insertInto("territories")

    snc.sql(northWind.NWQueries.employee_territories_table)
    employee_territories.write.insertInto("employee_territories")
  }

  private def validateQueries(snc: SnappyContext, tableType: String, pw: PrintWriter): Unit = {
    for (q <- northWind.NWQueries.queries) {
      q._1 match {
        case "Q1" => assertQuery(snc, northWind.NWQueries.Q1, 8, "Q1", tableType, pw)
        case "Q2" => assertQuery(snc, northWind.NWQueries.Q2, 91, "Q2", tableType, pw)
        case "Q3" => assertQuery(snc, northWind.NWQueries.Q3, 830, "Q3", tableType, pw)
        case "Q4" => assertQuery(snc, northWind.NWQueries.Q4, 9, "Q4", tableType, pw)
        case "Q5" => assertQuery(snc, northWind.NWQueries.Q5, 9, "Q5", tableType, pw)
        case "Q6" => assertQuery(snc, northWind.NWQueries.Q6, 9, "Q6", tableType, pw)
        case "Q7" => assertQuery(snc, northWind.NWQueries.Q7, 9, "Q7", tableType, pw)
        case "Q8" => assertQuery(snc, northWind.NWQueries.Q8, 6, "Q8", tableType, pw)
        case "Q9" => assertQuery(snc, northWind.NWQueries.Q9, 3, "Q9", tableType, pw)
        case "Q10" => assertQuery(snc, northWind.NWQueries.Q10, 2, "Q10", tableType, pw)
        case "Q11" => assertQuery(snc, northWind.NWQueries.Q11, 0, "Q11", tableType, pw)
        case "Q12" => assertQuery(snc, northWind.NWQueries.Q12, 2, "Q12", tableType, pw)
        case "Q13" => //assertQuery(snc, NWQueries.Q13, 0, "Q13", tableType, pw)
        case "Q14" => assertQuery(snc, northWind.NWQueries.Q14, 91, "Q14", tableType, pw)
        case "Q15" => assertQuery(snc, northWind.NWQueries.Q15, 5, "Q15", tableType, pw)
        case "Q16" => assertQuery(snc, northWind.NWQueries.Q16, 8, "Q16", tableType, pw)
        case "Q17" => //assertQuery(snc, NWQueries.Q17, 3, "Q17", tableType, pw)
        case "Q18" => assertQuery(snc, northWind.NWQueries.Q18, 9, "Q18", tableType, pw)
        case "Q19" => assertQuery(snc, northWind.NWQueries.Q19, 13, "Q19", tableType, pw)
        case "Q20" => assertQuery(snc, northWind.NWQueries.Q20, 1, "Q20", tableType, pw)
        case "Q21" => assertQuery(snc, northWind.NWQueries.Q21, 1, "Q21", tableType, pw)
        case "Q22" => assertQuery(snc, northWind.NWQueries.Q22, 1, "Q22", tableType, pw)
        case "Q23" => assertQuery(snc, northWind.NWQueries.Q23, 1, "Q23", tableType, pw)
        case "Q24" => assertQuery(snc, northWind.NWQueries.Q24, 4, "Q24", tableType, pw)
        case "Q25" => assertJoin(snc, northWind.NWQueries.Q25, 1, "Q25", tableType, pw)
        case "Q26" => //assertJoin(snc, NWQueries.Q26, 86, "Q26", tableType, pw)
        case "Q27" => assertJoin(snc, northWind.NWQueries.Q27, 9, "Q27", tableType, pw)
        case "Q28" => assertJoin(snc, northWind.NWQueries.Q28, 12, "Q28", tableType, pw)
        case "Q29" => assertJoin(snc, northWind.NWQueries.Q29, 8, "Q29", tableType, pw)
        case "Q30" => assertJoin(snc, northWind.NWQueries.Q30, 8, "Q30", tableType, pw)
        case "Q31" => assertJoin(snc, northWind.NWQueries.Q31, 830, "Q31", tableType, pw)
        case "Q32" => //assertJoin(snc, NWQueries.Q32, 29, "Q32", tableType, pw)
        case "Q33" => //assertJoin(snc, NWQueries.Q33, 51, "Q33")
        case "Q34" => assertJoin(snc, northWind.NWQueries.Q34, 5, "Q34", tableType, pw)
        case "Q35" => assertJoin(snc, northWind.NWQueries.Q35, 3, "Q35", tableType, pw)
        case "Q36" => //assertJoin(snc, northWind.NWQueries.Q36, 5, "Q36", tableType, pw)
        case "Q37" => //assertJoin(snc, northWind.NWQueries.Q37, 69, "Q37", tableType, pw)
        case "Q38" => //assertJoin(snc, northWind.NWQueries.Q38, 71, "Q38", tableType, pw)
        case "Q39" => assertJoin(snc, northWind.NWQueries.Q39, 9, "Q39", tableType, pw)
        case "Q40" => assertJoin(snc, northWind.NWQueries.Q40, 830, "Q40", tableType, pw)
        case "Q41" => assertJoin(snc, northWind.NWQueries.Q41, 2155, "Q41", tableType, pw)
        case "Q42" => assertJoin(snc, northWind.NWQueries.Q42, 22, "Q42", tableType, pw)
        case "Q43" => assertJoin(snc, northWind.NWQueries.Q43, 830, "Q43", tableType, pw)
        case "Q44" => assertJoin(snc, northWind.NWQueries.Q44, 830, "Q44", tableType, pw) //LeftSemiJoinHash
        case "Q45" => assertJoin(snc, northWind.NWQueries.Q45, 1788650, "Q45", tableType, pw)
        case "Q46" => assertJoin(snc, northWind.NWQueries.Q46, 1788650, "Q46", tableType, pw)
        case "Q47" => assertJoin(snc, northWind.NWQueries.Q47, 1788650, "Q47", tableType, pw)
        case "Q48" => assertJoin(snc, northWind.NWQueries.Q48, 1788650, "Q48", tableType, pw)
        case "Q49" => assertJoin(snc, northWind.NWQueries.Q49, 1788650, "Q49", tableType, pw)
        case "Q50" => assertJoin(snc, northWind.NWQueries.Q50, 2155, "Q50", tableType, pw)
        case "Q51" => assertJoin(snc, northWind.NWQueries.Q51, 2155, "Q51", tableType, pw)
        case "Q52" => assertJoin(snc, northWind.NWQueries.Q52, 2155, "Q52", tableType, pw)
        case "Q53" => assertJoin(snc, northWind.NWQueries.Q53, 2155, "Q53", tableType, pw)
        case "Q54" => assertJoin(snc, northWind.NWQueries.Q54, 2155, "Q54", tableType, pw)
        case "Q55" => assertJoin(snc, NWQueries.Q55, 21, "Q55", tableType, pw)
        case "Q56" => //assertJoin(snc, NWQueries.Q56, 8, "Q56", tableType, pw)
        case _ => println("ok")
      }
    }
  }

  private def createAndLoadPartitionedTables(snc: SnappyContext): Unit = {

    snc.sql(northWind.NWQueries.regions_table)
    regions.write.insertInto("regions")

    snc.sql(northWind.NWQueries.categories_table)
    categories.write.insertInto("categories")

    snc.sql(northWind.NWQueries.shippers_table)
    shippers.write.insertInto("shippers")

    snc.sql(northWind.NWQueries.employees_table)
    employees.write.insertInto("employees")

    snc.sql(northWind.NWQueries.customers_table)
    customers.write.insertInto("customers")

    snc.sql(northWind.NWQueries.orders_table + " using row options (partition_by 'OrderId', buckets '13')")
    orders.write.insertInto("orders")

    snc.sql(northWind.NWQueries.order_details_table +
      " using row options (partition_by 'OrderId', buckets '13', COLOCATE_WITH 'orders')")
    order_details.write.insertInto("order_details")

    snc.sql(northWind.NWQueries.products_table +
      " using row options ( partition_by 'ProductID', buckets '17')")
    products.write.insertInto("products")

    snc.sql(northWind.NWQueries.suppliers_table +
      " USING row options (PARTITION_BY 'SupplierID', buckets '123' )")
    suppliers.write.insertInto("suppliers")

    snc.sql(northWind.NWQueries.territories_table +
      " using row options (partition_by 'TerritoryID', buckets '3')")
    territories.write.insertInto("territories")

    snc.sql(northWind.NWQueries.employee_territories_table +
      " using row options(partition_by 'EmployeeID', buckets '1')")
    employee_territories.write.insertInto("employee_territories")

  }

  private def createAndLoadColumnTables(snc: SnappyContext): Unit = {
    snc.sql(northWind.NWQueries.regions_table)
    regions.write.insertInto("regions")

    snc.sql(northWind.NWQueries.categories_table)
    categories.write.insertInto("categories")

    snc.sql(northWind.NWQueries.shippers_table)
    shippers.write.insertInto("shippers")

    snc.sql(northWind.NWQueries.employees_table + " using column options()")
    employees.write.insertInto("employees")

    snc.sql(northWind.NWQueries.customers_table)
    customers.write.insertInto("customers")

    snc.sql(northWind.NWQueries.orders_table + " using column options (partition_by 'OrderId', buckets '13')")
    orders.write.insertInto("orders")

    snc.sql(northWind.NWQueries.order_details_table +
      " using column options (partition_by 'OrderId', buckets '13', COLOCATE_WITH 'orders')")
    order_details.write.insertInto("order_details")

    snc.sql(northWind.NWQueries.products_table +
      " USING column options ( partition_by 'ProductID,SupplierID', buckets '17')")
    products.write.insertInto("products")

    snc.sql(northWind.NWQueries.suppliers_table +
      " USING column options (PARTITION_BY 'SupplierID', buckets '123' )")
    suppliers.write.insertInto("suppliers")

    snc.sql(northWind.NWQueries.territories_table +
      " using column options (partition_by 'TerritoryID', buckets '3')")
    territories.write.insertInto("territories")

    snc.sql(northWind.NWQueries.employee_territories_table +
      " using row options(partition_by 'EmployeeID', buckets '1')")
    employee_territories.write.insertInto("employee_territories")
  }

  private def createAndLoadColocatedTables(snc: SnappyContext): Unit = {
    snc.sql(northWind.NWQueries.regions_table)
    regions.write.insertInto("regions")

    snc.sql(northWind.NWQueries.categories_table)
    categories.write.insertInto("categories")

    snc.sql(northWind.NWQueries.shippers_table)
    shippers.write.insertInto("shippers")

    snc.sql(northWind.NWQueries.employees_table +
      " using row options( partition_by 'EmployeeID', buckets '3')")
    employees.write.insertInto("employees")

    snc.sql(northWind.NWQueries.customers_table +
      " using column options( partition_by 'CustomerID', buckets '19')")
    customers.write.insertInto("customers")

    snc.sql(northWind.NWQueries.orders_table +
      " using row options (partition_by 'CustomerID', buckets '19', colocate_with 'customers')")
    orders.write.insertInto("orders")

    snc.sql(northWind.NWQueries.order_details_table +
      " using row options ( partition_by 'ProductID', buckets '329')")
    order_details.write.insertInto("order_details")

    snc.sql(northWind.NWQueries.products_table +
      " USING column options ( partition_by 'ProductID', buckets '329'," +
      " colocate_with 'order_details')")
    products.write.insertInto("products")

    snc.sql(northWind.NWQueries.suppliers_table +
      " USING column options (PARTITION_BY 'SupplierID', buckets '123')")
    suppliers.write.insertInto("suppliers")

    snc.sql(northWind.NWQueries.territories_table +
      " using column options (partition_by 'TerritoryID', buckets '3')")
    territories.write.insertInto("territories")

    snc.sql(northWind.NWQueries.employee_territories_table +
      " using row options(partition_by 'TerritoryID', buckets '3', colocate_with 'territories') ")
    employee_territories.write.insertInto("employee_territories")

  }

  private def dropTables(snc: SnappyContext): Unit = {
    snc.sql("drop table if exists regions")
    println("regions table dropped successfully.");
    snc.sql("drop table if exists categories")
    println("categories table dropped successfully.");
    snc.sql("drop table if exists products")
    println("products table dropped successfully.");
    snc.sql("drop table if exists order_details")
    println("order_details table dropped successfully.");
    snc.sql("drop table if exists orders")
    println("orders table dropped successfully.");
    snc.sql("drop table if exists customers")
    println("customers table dropped successfully.");
    snc.sql("drop table if exists employees")
    println("employees table dropped successfully.");
    snc.sql("drop table if exists employee_territories")
    println("employee_territories table dropped successfully.");
    snc.sql("drop table if exists shippers")
    println("shippers table dropped successfully.");
    snc.sql("drop table if exists suppliers")
    println("suppliers table dropped successfully.");
    snc.sql("drop table if exists territories")
    println("territories table dropped successfully.");
  }

  override def isValidJob(sc: SnappyContext, config: Config): SnappyJobValidation = SnappyJobValid()
}