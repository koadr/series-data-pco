package com.koadr


import com.github.tminglei.slickpg.{PgArraySupport, utils, PgCompositeSupport}
import com.github.tminglei.slickpg.composite.Struct
import com.koadr.PgCompositeSupportSuite.Tuple2tStruct
import org.joda.time.{DateTimeZone, DateTime}
import slick.driver.PostgresDriver
import slick.jdbc.PositionedResult
import slick.lifted.RepShapeImplicits
import scala.reflect.runtime.{universe => u}
import java.sql.Timestamp
import java.text.SimpleDateFormat
import slick.jdbc.JdbcBackend.Database


object PgCompositeSupportSuite {
  case class Tuple2tStruct(date: DateTime,value: Double) extends Struct
}

object CommonPostgresProfile extends DBProfile

trait DBProfile extends PostgresDriver with PgCompositeSupport with PgArraySupport with utils.PgCommonJdbcTypes {
  val tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def ts(str: String) = new Timestamp(tsFormat.parse(str).getTime)

  override val api =
    new API
      with ArrayImplicits
      with CompositeImplicits
      with MyArrayImplicitsPlus
      with GenericExtensions
      with CommonTypeSupport {}

  trait CompositeImplicits {
    utils.TypeConverters.register[DateTime,String](d => tsFormat.format(new Timestamp(d.getMillis)))
    utils.TypeConverters.register[String, DateTime](s => new DateTime(ts(s), DateTimeZone.UTC) )
    implicit val tuple2pointTypeMapper = createCompositeJdbcType[Tuple2tStruct]("tuple2point")
    implicit val tuple2pointArrayTypeMapper = createCompositeArrayJdbcType[Tuple2tStruct]("tuple2point").to(_.toList)
  }

  trait MyArrayImplicitsPlus {
    implicit val simpleDateTimeListTypeMapper = new SimpleArrayJdbcType[DateTime]("timestamp").basedOn[Timestamp](
    d => new Timestamp(d.getMillis),
    ts => new DateTime(ts.getTime)
    ).to(_.toList)
  }
}


object DbHolder {
  val db = Database.forConfig("seriesDb")
}

