package com.koadr.tables

import com.koadr.CommonPostgresProfile.api._
import com.koadr.PgCompositeSupportSuite.Tuple2tStruct
import org.joda.time.DateTime


object SeriesTable extends SeriesTable

private[tables] trait SeriesTable {

  case class SeriesWithCompositeRow(id: Int,code: String,ident1Id: Int,ident2Id: Int, series: List[Tuple2tStruct])
  case class SeriesWithCompositeInsertRow(code: String,ident1Id: Int,ident2Id: Int, series: List[Tuple2tStruct])
  class SeriesWithComposite(tag: Tag) extends Table[SeriesWithCompositeRow](tag, "series") {
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    def code = column[String]("code")
    def ident1Id = column[Int]("ident1-id")
    def ident2Id = column[Int]("ident2-id")
    def series = column[List[Tuple2tStruct]]("series")

    def columnsForInsert = (code,ident1Id, ident2Id,series) <> (SeriesWithCompositeInsertRow.tupled, SeriesWithCompositeInsertRow.unapply)
    def * = (id,code,ident1Id, ident2Id,series) <> (SeriesWithCompositeRow.tupled, SeriesWithCompositeRow.unapply)
  }

  case class SeriesWithArraysRow(id: Int,code: String,accuracyId: Int,frequencyId: Int, dates: List[DateTime], values: List[Double])
  case class SeriesWithArraysInsertRow(code: String,accuracyId: Int,frequencyId: Int, dates: List[DateTime], values: List[Double])
  class SeriesWithArrays(tag: Tag) extends Table[SeriesWithArraysRow](tag, "series_with_array") {
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    def code = column[String]("code")
    def ident1Id = column[Int]("ident1-id")
    def ident2Id = column[Int]("ident2-id")
    def dates = column[List[DateTime]]("dates")
    def values = column[List[Double]]("values")

    def columnsForInsert = (code,ident1Id, ident2Id,dates,values) <> (SeriesWithArraysInsertRow.tupled, SeriesWithArraysInsertRow.unapply)
    def * = (id,code,ident1Id, ident2Id,dates,values) <> (SeriesWithArraysRow.tupled, SeriesWithArraysRow.unapply)
  }

  case class SeriesRow(id: Int,code: String,accuracyId: Int,frequencyId: Int)
  case class SeriesInsertRow(code: String,accuracyId: Int,frequencyId: Int)
  class Series(tag: Tag) extends Table[SeriesRow](tag, "series-reg") {
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    def code = column[String]("code")
    def ident1Id = column[Int]("ident1-id")
    def ident2Id = column[Int]("ident2-id")

    def columnsForInsert = (code,ident1Id, ident2Id) <> (SeriesInsertRow.tupled, SeriesInsertRow.unapply)
    def * = (id,code,ident1Id, ident2Id) <> (SeriesRow.tupled, SeriesRow.unapply)
  }

  case class SeriesPointRow(id: Int,date: DateTime,value:Double,seriesId: Int)
  case class SeriesPointInsertRow(date: DateTime,value:Double,seriesId: Int)
  class SeriesPoint(tag: Tag) extends Table[SeriesPointRow](tag, "series-point") {
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    def date = column[DateTime]("date")
    def value = column[Double]("value")
    def seriesId = column[Int]("time_series_id")

    def seriesFk = foreignKey("series-point-series-fk", seriesId, SeriesTable.Series)(_.id)

    def columnsForInsert = (date,value, seriesId) <> (SeriesPointInsertRow.tupled, SeriesPointInsertRow.unapply)
    def * = (id,date,value, seriesId) <> (SeriesPointRow.tupled, SeriesPointRow.unapply)
  }

  val SeriesWithComposite = TableQuery[SeriesWithComposite]
  val SeriesWithArrays= TableQuery[SeriesWithArrays]
  val Series= TableQuery[Series]
  val SeriesPoint= TableQuery[SeriesPoint]
}
