package com.koadr

import com.koadr.PgCompositeSupportSuite.Tuple2tStruct
import com.koadr.tables.SeriesTable
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minutes, Milliseconds,Span}
import org.scalatest.{BeforeAndAfter, FreeSpec}
import scala.concurrent.ExecutionContext.Implicits.global


class SeriesTest extends FreeSpec with CommonSeeder with BeforeAndAfter with ScalaFutures {
  import api._

  implicit val defaultPatience =
    PatienceConfig(Span(15, Minutes),Span(10, Milliseconds))

  before {
    whenReady(createTables()) { identity }
  }

  after {
    whenReady(dropTables()) { identity }
  }

  val arbCode = "arbCode"
  val arbId = 1
  val arbDouble = 0.3
  val arbTuple2Structs = List.fill(1200)(Tuple2tStruct(DateTime.now(),arbDouble))

  "Series" - {
    "with composite for realistic series data" ignore {
      val sProj = SeriesTable.SeriesWithComposite.map(_.columnsForInsert)
      val rows = for {
        i <- 1 to 277
      } yield {
        SeriesTable.SeriesWithCompositeInsertRow(arbCode,arbId,arbId,arbTuple2Structs)
      }
      val createdseries = db run {
        for {
          _ <- sProj ++= rows
          series <- SeriesTable.SeriesWithComposite.result
        } yield series
      }
      whenReady(createdseries) {println}
    }

    "with array times and array values for realistic series data" in {
        val sProj = SeriesTable.SeriesWithArrays.map(_.columnsForInsert)
        val rows = for {
          i <- 1 to 227
        } yield {
            val (dates, values) = arbTuple2Structs.map(t => (t.date, t.value)).unzip
            SeriesTable.SeriesWithArraysInsertRow(arbCode, arbId, arbId, dates, values)
          }
      import org.scalameter._
        val createseries =
          db.run{
            for {
              _ <- sProj ++= rows
            } yield ()
          }

        val allseries =
          db run {
            for {
              series <- SeriesTable.SeriesWithArrays.result
            } yield series
          }

        whenReady(createseries) {
          _ =>
            val time = measure {
              whenReady(allseries){identity}
            }
            // Ran ~0.334188 ms
            println("*****" * 30)
            println("WITH ARRAY")
            println(time)
            println("*****" * 30)
        }

    }

    "with series data in separate table" in {
        val sProj = SeriesTable.Series.map(_.columnsForInsert).returning(SeriesTable.Series.map(_.id))
        val sPointProj = SeriesTable.SeriesPoint.map(_.columnsForInsert)
        val sRows = for {
          i <- 1 to 277
        } yield {
            SeriesTable.SeriesInsertRow(arbCode,arbId,arbId)
          }
        val sPointRows = for {
          sId <- 1 to 277
          _ <- 1 to 1200
        } yield {
            SeriesTable.SeriesPointInsertRow(DateTime.now(),0.2,sId)
          }

        import org.scalameter._
        val createTS =
          db.run{
            for {
              sIds <- sProj ++= sRows
              _ <- sPointProj ++= sPointRows
            } yield ()
          }

        val allseries =
          db run {
            (for {
              sP <- SeriesTable.SeriesPoint
              s <- sP.seriesFk
            } yield (s,sP)).result
          }

        whenReady(createTS) {
          _ =>
            val time = measure {
              whenReady(allseries){identity}
            }
            // Ran around ~0.60455 ms
            println("*****" * 30)
            println("WITH TWO TABLES")
            println(time)
            println("*****" * 30)
        }

    }
  }
}
