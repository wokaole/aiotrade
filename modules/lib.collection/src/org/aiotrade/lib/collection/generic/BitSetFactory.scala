/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: BitSetFactory.scala 19190 2009-10-21 13:24:41Z moors $


package org.aiotrade.lib.collection
package generic

import scala.collection._
import mutable.{Builder, AddingBuilder}

/**
 * @since 2.8
 */
trait BitSetFactory[Coll <: BitSet with BitSetLike[Coll]] { 
  def newBuilder: Builder[Int, Coll] = new AddingBuilder[Int, Coll](empty)
  def empty: Coll
  def apply(elems: Int*): Coll = (empty /: elems) (_ + _)
  def bitsetCanBuildFrom = new CanBuildFrom[Coll, Int, Coll] {
    def apply(from: Coll) = newBuilder
    def apply() = newBuilder
  }
}
