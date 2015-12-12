package com.koadr

import com.koadr.tables.SeriesTable
import scala.concurrent.ExecutionContext.Implicits.global
import CommonPostgresProfile.api._


trait CommonSeeder extends DBProfile {

  val db = DbHolder.db

  def allTables =
    SeriesTable.SeriesWithComposite.schema ++
      SeriesTable.SeriesWithArrays.schema ++
      SeriesTable.Series.schema ++
      SeriesTable.SeriesPoint.schema

  def createTables() = {
    db run {
      for {
        _ <- sqlu"create type tuple2point as (date TIMESTAMP, value FLOAT)"
        _ <- allTables.create
      } yield ()
    }
  }
  def dropTables() = {
    db run {
      for {
        _ <- allTables.drop
        _ <- sqlu"drop type tuple2point "
      } yield ()
    }
  }

}
