/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: MapBuilder.scala 18801 2009-09-26 16:33:54Z stepancheg $

package org.aiotrade.lib.collection
package mutable

/** The canonical builder for immutable maps, working with the map's `+` method
 *  to add new elements. 
 *  Collections are built from their `empty` element using this + method.
 *  @param empty   The empty element of the collection.
 *
 *  @since 2.8
 */
class MapBuilder[A, B, Coll <: scala.collection.Map[A, B] with scala.collection.MapLike[A, B, Coll]](empty: Coll) 
extends Builder[(A, B), Coll] {
  protected var elems: Coll = empty
  def +=(x: (A, B)): this.type = { 
    elems = (elems + x).asInstanceOf[Coll]
      // the cast is necessary because right now we cannot enforce statically that
      // for every map of type Coll, `+` yields again a Coll. With better support
      // for hk-types we might be able to enforce this in the future, though.
    this 
  }
  def clear() { elems = empty }
  def result: Coll = elems
}