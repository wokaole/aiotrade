/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: ImmutableSortedSetFactory.scala 18799 2009-09-26 15:19:40Z stepancheg $


package org.aiotrade.lib.collection
package generic

import mutable.{Builder, SetBuilder}

/** A template for companion objects of mutable.Map and subclasses thereof.
 *
 *  @since 2.8
 */
abstract class ImmutableSortedSetFactory[CC[A] <: immutable.SortedSet[A] with SortedSetLike[A, CC[A]]] extends SortedSetFactory[CC]{
  def newBuilder[A](implicit ord: Ordering[A]): Builder[A, CC[A]] = new SetBuilder[A, CC[A]](empty)
}