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
package org.aiotrade.lib.charting.chart

import org.aiotrade.lib.charting.widget.HeavyPathWidget
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.widget.LineSegment
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.charting.laf.LookFeel

/**
 *
 * @author Caoyuan Deng
 */
class PolyLineChart extends AbstractChart {
  final class Model extends WidgetModel {
    var v: TVar[_] = _
        
    def set(v: TVar[_]) {
      this.v = v
    }
  }

  type M = Model
  
  protected def createModel = new Model
    
  protected def plotChart {
    val m = model
    val color = LookFeel.getCurrent.getChartColor(getDepth)
    setForeground(color)
        
    val heavyPathWidget = addChild(new HeavyPathWidget)
    val template = new LineSegment
    var y1 = Null.Float   // for prev
    var y2 = Null.Float   // for curr
    var bar = 1
    while (bar <= nBars) {
      var value = Null.Float
      var max = -Math.MAX_FLOAT
      var min = +Math.MAX_FLOAT
      var i = 0
      while (i < nBarsCompressed) {
        val  time = tb(bar + i)
        val item = ser.getItem(time)
        if (item != null) {
          value = item.getFloat(m.v)
          max = Math.max(max, value)
          min = Math.min(min, value)
        }

        i += 1
      }
            
      if (Null.not(value)) {
        template.setForeground(color)
                
        y2 = yv(value)
        if (nBarsCompressed > 1) {
          /** draw a vertical line to cover the min to max */
          val x = xb(bar)
          template.model.set(x, yv(min), x, yv(max))
        } else {
          if (Null.not(y1)) {
            /**
             * x1 shoud be decided here, it may not equal prev x2:
             * think about the case of on calendar day mode
             */
            val x1 = xb(bar - nBarsCompressed)
            val x2 = xb(bar)
            template.model.set(x1, y1, x2, y2)
                        
            if (x2 % AbstractChart.MARK_INTERVAL == 0) {
              addMarkPoint(x2.toInt, y2.toInt)
            }
                        
          }
        }
        y1 = y2
                
        template.plot
        heavyPathWidget.appendFrom(template)
      }

      bar += nBarsCompressed
    }
        
  }
    
}