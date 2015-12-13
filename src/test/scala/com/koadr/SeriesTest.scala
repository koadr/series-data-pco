package com.koadr

import com.koadr.PgCompositeSupportSuite.Tuple2tStruct
import com.koadr.tables.SeriesTable
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minutes, Milliseconds,Span}
import org.scalatest.{BeforeAndAfter, FreeSpec}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


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
  var currDateTime = new DateTime()
  var currValue = 0.1
  val arbTuple2Structs = List.fill(1200){
    currDateTime = currDateTime.plusMonths(1)
    currValue += 0.1
    Tuple2tStruct(currDateTime,currValue)
  }

  "Series" - {
    "with composite for realistic series data" ignore {
      val sProj = SeriesTable.SeriesWithComposite.map(_.columnsForInsert)
      val rows = for {
        i <- 1 to 277
      } yield {
        SeriesTable.SeriesWithCompositeInsertRow(arbCode,arbId,arbId,arbTuple2Structs)
      }

      whenReady(
        db.run((for {
        _ <- sProj ++= rows
          series <- SeriesTable.SeriesWithComposite.result
      } yield series))) {identity}

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
        lazy val createseries =
          db.run{
            for {
              _ <- sProj ++= rows
            } yield ()
          }

        lazy val allseries =
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
            // Ran ~1083.845838ms
            println("*****" * 30)
            println("WITH ARRAY")
            println(time)
            println("*****" * 30)
        }

    }

    "with series data in separate table" in {
        var currDateTime = new DateTime()
        var currValue = 0.1
        val sProj = SeriesTable.Series.map(_.columnsForInsert).returning(SeriesTable.Series.map(_.id))
        val sPointProj = SeriesTable.SeriesPoint.map(_.columnsForInsert)
        val sRows = for {
          _ <- 1 to 277
        } yield {
            SeriesTable.SeriesInsertRow(arbCode,arbId,arbId)
          }
        val sPointRows = for {
          sId <- 1 to 277
          _ <- 1 to 1200
        } yield {
            currDateTime = currDateTime.plusMonths(1)
            currValue += 0.1
            SeriesTable.SeriesPointInsertRow(currDateTime,currValue,sId)
          }

        import org.scalameter._
        lazy val createTS =
          db.run{
            for {
              sIds <- sProj ++= sRows
              _ <- sPointProj ++= sPointRows
            } yield ()
          }

        lazy val allseries: Future[Seq[(SeriesTable.SeriesRow, List[Double], List[DateTime])]] =
          db run {
            (for {
              sT <- SeriesTable.SeriesPoint
              s <- sT.seriesFk
              if s.id === 1
            } yield (s,sT))
              .sortBy(_._2.date)
              .groupBy(_._1)
              .map {
                case (series, rows) =>
                  (series, rows.map(_._2.value).arrayAgg(), rows.map(_._2.date).arrayAgg())
            }.result
          }

        whenReady(createTS) {
          _ =>
            val time = measure {
              whenReady(allseries){
                series =>
                  def isSorted(l:List[Double]) = (l, l.tail).zipped.forall(_ <= _)
                  def isDateSorted(l:List[DateTime]) = (l, l.tail).zipped.forall(_ isBefore _)
                  case class TsPoint(date: DateTime,value: Double)
                  val tsPoints = series.head._3.zip(series.head._2).map{case (time,value) => TsPoint(time,value)}
                  assert(tsPoints.sortBy(_.value) == tsPoints)
                  assert(tsPoints.sortBy(_.date.getMillis) == tsPoints)
                  assert(tsPoints.size == 1200)
                  assert(isSorted(tsPoints.map(_.value)))
                  assert(isDateSorted(tsPoints.map(_.date)))
              }
            }
            // Ran around ~107.303741 ms
            println("*****" * 30)
            println("WITH TWO TABLES")
            println(time)
            println("*****" * 30)
        }

    }

    "migrates series with composite table over to series-reg and series-point table" ignore {
      val seriesIds = db.run(SeriesTable.SeriesWithComposite.map(_.id).result)

      whenReady(seriesIds) {
        ids =>
          val insertedRows = for {
            idSeries <- ids
          } yield {
              val seriesById = SeriesTable.SeriesWithComposite.filter(_.id === idSeries).map(sC => (sC.id,sC.series)).result.head
              def insertSeriesPointTable(seriesWithId: (Int,List[Tuple2tStruct])) = {
                val (seriesId,series) = seriesWithId
                val sPointProj = SeriesTable.SeriesPoint.map(_.columnsForInsert)
                val insertRows = for {
                  s <- series
                } yield SeriesTable.SeriesPointInsertRow(s.date,s.value, seriesId)
                sPointProj ++= insertRows
              }
              lazy val createdSeriesPoints = db run {
                for {
                  seriesTup <- seriesById
                  (id,series) = seriesTup
                  _ <- insertSeriesPointTable(seriesTup)
                } yield series.size
              }
              whenReady(createdSeriesPoints) {
                size =>
                  println(s"$size series points for id $idSeries have been created")
                  size
              }
            }

          println(s"Total of ${insertedRows.sum} were created")
      }
    }
  }
}
