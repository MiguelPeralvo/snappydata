package org.apache.spark.examples.snappydata

import java.sql.{Blob, Connection, DriverManager}

import scala.util.Try

import com.pivotal.gemfirexd.snappy.ComplexTypeSerializer

import org.apache.spark.sql.types.Decimal

/**
 * An example that shows JDBC operations on SnappyData system
 *
 * Before running this example, ensure that SnappyData cluster is started and
 * running. To start the cluster execute the following command:
 * sbin/snappy-start-all.sh
 */
object JDBCWithComplexTypes {

  val tableName = "TABLE_WITH_COMPLEX_TYPES"

  def createTableWithComplexType(conn: Connection): Unit = {
    val stmt = conn.createStatement()
    println(s"Creating a table $tableName using JDBC connection")

    stmt.execute(s"DROP TABLE IF EXISTS $tableName")
    stmt.execute(
      s"""
        CREATE TABLE $tableName (
          col1 Int,
          col2 Array<Decimal>
        ) USING column options()""")

  }

  def doInsertComplexType(conn: Connection): Unit = {
    println(s"Inserting a single row having a a complex type ...")
    val arrDecimal = Array(Decimal("4.92"), Decimal("51.98"))
    val pstmt = conn.prepareStatement(
      s"insert into $tableName values (?, ?)")
    val serializer1 = ComplexTypeSerializer.create(tableName, "col2", conn)

    pstmt.setInt(1, 1)
    pstmt.setBytes(2, serializer1.serialize(arrDecimal))
    pstmt.execute
    pstmt.close
  }

  def readComplexType(conn: Connection): Unit = {
    println(s"Reading results as  BLOB and Bytes ...")
    val stmt = conn.createStatement()
    val serializer = ComplexTypeSerializer.create(tableName, "col2", conn)
    val rs = stmt.executeQuery(s"SELECT * FROM $tableName")
    while (rs.next()) {
      val res1 = serializer.deserialize(rs.getBytes(2))
      println(s"res1 = $res1")
      val res2 = serializer.deserialize(rs.getBytes("col2"))
      println(s"res2 = $res2")
      val res3 = serializer.deserialize(rs.getObject("col2")
          .asInstanceOf[Blob])
      println(s"res3 = $res3")
      val res4 = serializer.deserialize(rs.getBlob("col2"))
      println(s"res4 = $res4")
    }
  }


  private def doOperationsUsingJDBC(clientPort: String) {
    // JDBC url string to connect to SnappyData cluster
    val url: String = s"jdbc:snappydata://localhost:$clientPort/"
    val conn = DriverManager.getConnection(url)
    try {
      //Create table with one complex type column
      createTableWithComplexType(conn)
      //Insert into a table with complex type
      doInsertComplexType(conn)
      //Read complex type
      readComplexType(conn)
      //readComplexTypeAsJson(conn)

    } finally {
      conn.close()
    }
  }

  def main(args: Array[String]): Unit = {
    if (args.length > 1) {
      printUsage()
    } else if (args.length == 0) {
      println("Using default client port 1527 for JDBC connection")
      doOperationsUsingJDBC("1527")
    } else {
      if (Try(args(0).toInt).isFailure) {
        printUsage()
      } else {
        doOperationsUsingJDBC(args(0))
      }
    }
  }

  def printUsage(): Unit = {
    val usage: String =
      "Usage: bin/run-example JDBCWithComplexTypes <clientPort>\n" +
          "\n" +
          "clientPort - client port number for SnappyData on which JDBC connections are accepted \n"
    println(usage)
  }
}
