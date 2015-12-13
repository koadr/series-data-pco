package com.koadr

import org.joda.time.LocalDate

import scala.language.{ higherKinds, implicitConversions }
import slick.ast.Library.{ JdbcFunction, SqlAggregateFunction, SqlFunction }
import slick.ast._
import slick.lifted.FunctionSymbolExtensionMethods._
import slick.lifted._
import CustomLibrary._


object CustomLibrary {
  val ArrayAgg = new SqlAggregateFunction("array_agg")
  val StringToArray = new SqlAggregateFunction("string_to_array")
  val Distinct = new SqlAggregateFunction("distinct")
}

final class CustomSingleColumnQueryExtensionMethods[B1, P1, C[_]](val q: Query[Rep[P1], _, C]) extends AnyVal {
  type OptionListTM = TypedType[Option[List[B1]]]
  type ListTM = TypedType[List[B1]]
  type OptionTM = TypedType[Option[B1]]
  type TM = TypedType[B1]

  //FIXME: Find a way to be able to always safely return List, otherwise ALWAYS use optArrayAgg
  def optArrayAgg(distinct: Boolean = false, ensureType: Boolean = false)(implicit oltm: OptionListTM, tm: TM, otm: OptionTM): Rep[Option[List[B1]]] = {
    val inner = innerArrayAgg(distinct, ensureType)
    ArrayAgg.column[Option[List[B1]]](inner.toNode)
  }
  def arrayAgg(distinct: Boolean = false, ensureType: Boolean = false)(implicit ltm: ListTM, tm: TM, otm: OptionTM) = {
    val inner = innerArrayAgg(distinct, ensureType)
    ArrayAgg.column[List[B1]](inner.toNode)
  }

  private def innerArrayAgg(distinct: Boolean, ensureType: Boolean)(implicit tm: TM, otm: OptionTM) = {
    val afterCast = if (ensureType) {
      Library.Cast.column[B1](q.toNode)
    } else {
      q
    }
    if (distinct) {
      Distinct.column[Option[B1]](afterCast.toNode)
    } else {
      afterCast
    }
  }
}

trait CustomExtensionMethodConversions {
  implicit def customSingleColumnQueryExtensionMethods[B1: BaseTypedType, C[_]](q: Query[Rep[B1], _, C]): CustomSingleColumnQueryExtensionMethods[B1, B1, C] = new CustomSingleColumnQueryExtensionMethods[B1, B1, C](q)
  implicit def customSingleOptionColumnQueryExtensionMethods[B1, C[_]](q: Query[Rep[Option[B1]], _, C]): CustomSingleColumnQueryExtensionMethods[B1, Option[B1], C] = new CustomSingleColumnQueryExtensionMethods[B1, Option[B1], C](q)
}

trait GenericExtensions extends CustomExtensionMethodConversions {
  type CF[A, B, C] = CompiledFunction[(Rep[A]) ⇒ Query[B, C, Seq], Rep[A], A, Query[B, C, Seq], Seq[C]]
}
