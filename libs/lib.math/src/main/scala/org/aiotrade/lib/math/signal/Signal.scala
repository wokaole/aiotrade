/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.math.signal

import org.aiotrade.lib.math.indicator.SignalIndicator
import org.aiotrade.lib.util.actors.Publisher
import java.awt.Color

/**
 *
 * @author Caoyuan Deng
 */
case class SignalEvent(source: SignalIndicator, signal: Signal)
case class SignalsEvent(source: SignalIndicator, signals: Array[Signal])

/** Helper classes to carray full information of signal/signals */
case class SignalX(symbol: String, name: String, freq: String, signal: Signal) {
  def this() = this(null, null, null, null) /* for serializable */
}
case class SignalsX(symbol: String, name: String, freq: String, signals: Array[Signal]) {
  def this() = this(null, null, null, null) /* for serializable */
}

/**
 * @note We have to write Signal and Sign/Mark as here for avro serializer:
 *   Signal coundn't be abstract class 
 *   Don't write Sign/Mark as case class
 */
class Sign private (time: => Long, 
                    kind: => Direction, 
                    id: => Int, 
                    text: => String, 
                    color: => Color
) extends Signal(time, kind, id, text, color) {
  def this() = this(0L, Direction.EnterLong, 0, null, null) /* for serializable */
  override def kind = super.kind.asInstanceOf[Direction]
}

object Sign {
  def apply(time: Long, kind: Direction, id: Int = 0, text: String = null, color: Color = null) = new Sign(time, kind, id, text, color)
  def unapply(v: Signal): Option[(Long, Direction, Int, String, Color)] = v.kind match {
    case dire: Direction => Some((v.time, dire, v.id, v.text, v.color))
    case _ => None
  }
}

class Mark private (time: => Long, 
                    kind: => Position, 
                    id: => Int, 
                    text: => String, 
                    color: => Color
) extends Signal(time, kind, id, text, color) {
  def this() = this(0L, Position.Lower, 0, null, null) /* for serializable */
  override def kind = super.kind.asInstanceOf[Position]
}

object Mark {
  def apply(time: Long, kind: Position, id: Int = 0, text: String = null, color: Color = null) = new Mark(time, kind, id, text, color)
  def unapply(v: Signal): Option[(Long, Position, Int, String, Color)] = v.kind match {
    case posi: Position => Some((v.time, posi, v.id, v.text, v.color))
    case _ => None
  }
}

class Signal(val time: Long, _kind: Kind, val id: Int = 0, val text: String = null, @transient val color: Color = null) {
  def this() = this(0L, null) /* for serializable */

  def kind: Kind = _kind
  
  def isSign = kind.isDirection
  def isMark = kind.isPosition
    
  def hasText = text != null

  override def hashCode: Int = {
    var h = 17
    h = 37 * h + (time ^ (time >>> 32)).toInt
    h = 37 * h + kind.hashCode
    h = 37 * h + id
    if (text  != null) h = 37 * h + text.hashCode
    if (color != null) h = 37 * h + color.hashCode
    h
  }
  
  override def equals(a: Any): Boolean = {
    a match {
      case x: Signal => x.time == time && x.kind == kind && x.id == id && x.text == text && x.color == color
      case _ => false
    }
  }
}

object Signal extends Publisher {
  
  // --- simple test
  def main(args: Array[String]) {
    try {
      val posi = new Position(-1)
      val matchPosi = posi match {
        case Position.Lower => false
        case Position.Upper => true
      }
      assert(matchPosi)
      println(matchPosi)

      val dire = new Direction(4)
      val matchDire = dire match {
        case Direction.EnterLong => false
        case Direction.ExitShort => true
      }
      assert(matchDire)
      println(matchDire)
    
      val sign = new Signal(0, Direction.EnterLong)
      val matchSign = sign match {
        case Mark(time, kind, _, _, _) => println(kind); false
        case Sign(time, kind, _, _, _) => println(kind); true
        case _ => false
      }
      assert(matchSign)
      println(matchSign)
    
      val mark = new Signal(0, Position.Lower)
      val matchMark = mark match {
        case Sign(time, kind, _, _, _) => println(kind); false
        case Mark(time, kind, _, _, _) => println(kind); true
        case _ => false
      }
      assert(matchMark)
      println(matchMark)

      System.exit(0)
    } catch {
      case ex => ex.printStackTrace; System.exit(1)
    }
  }

}
