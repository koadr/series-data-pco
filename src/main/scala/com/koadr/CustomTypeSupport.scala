package com.koadr

import org.joda.time.{LocalDate, DateTime}
import CommonPostgresProfile.api._

object CommonTypeSupport extends CommonTypeSupport
trait CommonTypeSupport {
  //FIXME: It should be in MyPgJodaDateSupport but for some reason doesn't work there
  implicit lazy val jodaDateTimeMapper = MappedColumnType.base[DateTime, java.sql.Timestamp](
    { d ⇒ new java.sql.Timestamp(d.getMillis) }, { t ⇒ new DateTime(t.getTime) })

  implicit lazy val jodaDateMapper = MappedColumnType.base[LocalDate, java.sql.Date](
    { l ⇒ new java.sql.Date(l.toDate.getTime) }, { d ⇒ LocalDate.fromDateFields(d) })


}

