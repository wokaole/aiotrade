/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: Iterable.scala 19219 2009-10-22 09:43:14Z moors $


package org.aiotrade.lib.collection
package immutable

import generic._
import mutable.Builder

/** A subtrait of scala.collection.Iterable which represents iterables
 *  that cannot be mutated.
 *        
 *  @author  Matthias Zenger
 *  @author   Martin Odersky
 *  @version 2.8
 *  @since   2.8
 */
trait Iterable[+A] extends Traversable[A] 
                      with scala.collection.Iterable[A] 
                      with GenericTraversableTemplate[A, Iterable]
                      with IterableLike[A, Iterable[A]] { 
  override def companion: GenericCompanion[Iterable] = Iterable
}	

/** A factory object for the trait <code>Iterable</code>.
 *
 *  @author   Martin Odersky
 *  @version 2.8
 *  @since   2.8
 */
object Iterable extends TraversableFactory[Iterable] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, Iterable[A]] = new GenericCanBuildFrom[A]
  def newBuilder[A]: Builder[A, Iterable[A]] = new mutable.ListBuffer
}
