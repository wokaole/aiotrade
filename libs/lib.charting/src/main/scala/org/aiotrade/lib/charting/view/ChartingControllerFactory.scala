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
package org.aiotrade.lib.charting.view

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseWheelListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JComponent
import javax.swing.JFrame
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents
import org.aiotrade.lib.math.timeseries.MasterTSer
import org.aiotrade.lib.math.timeseries.TSerEvent
import javax.swing.WindowConstants
import org.aiotrade.lib.util.ChangeObserver
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.swing.Reactions
import scala.swing.Reactor


/**
 * Only DefaultChartingController will be provided in this factory
 *
 * @author Caoyuan Deng
 */
object ChartingControllerFactory {
  /** a static map to know how many controllers are bound with each MasterSer */
  private val sersTocontrollers = new HashMap[MasterTSer, HashSet[ChartingController]]

  def createInstance(masterSer: MasterTSer, contents: AnalysisContents): ChartingController = {
    val controllers = sersTocontrollers.get(masterSer) getOrElse {
      val controllersx = new HashSet[ChartingController]
      sersTocontrollers += (masterSer -> controllersx)
      controllersx
    }

    val controller = new DefaultChartingController(masterSer, contents)
    controllers.add(controller)

    controller
  }

  object DefaultChartingController {
    /**
     * min spacing between rightRow and left / right edge, if want more, such as:
     *     minSpacing = (nBars * 0.168).intValue
     */
    private val MIN_RIGHT_SPACING = 2
    private val MIN_LEFT_SPACING = 0

    /** BASIC_BAR_WIDTH = 6 */
    private val BAR_WIDTHS_ARRAY = Array(
      0.00025f, 0.0005f, 0.001f, 0.025f, 0.05f, 0.1f, 0.25f, 0.5f, 1f, 2f, 4f, 6f, 10f, 20f
    )

    private var _isCursorAccelerated = false

    def isCursorAccelerated = _isCursorAccelerated
    def isCursorAccelerated_=(b: Boolean) {
      _isCursorAccelerated = b
    }
  }

  /**
   * DefaultChartingController that implements ChartingController
   * 
   * ChangeSubject cases for ChartValidityObserver:
   *   rightSideRow
   *   referCursorRow
   *   wBar
   *   onCalendarMode
   * ChangeSubject cases for MouseCursorObserver:
   *   mosueCursor
   *   mouseEnteredAnyChartPane
   */
  import DefaultChartingController._
  private class DefaultChartingController($masterSer: MasterTSer, $contents: AnalysisContents) extends ChartingController
                                                                                                  with Reactor {

    val masterSer = $masterSer
    val contents  = $contents

    private val popupViews = new HashSet[ChartView]
    private var viewContainer: ChartViewContainer = _
    private var _wBarIdx = 11
    /** pixels per bar (bar width in pixels) */
    private var _wBar = BAR_WIDTHS_ARRAY(_wBarIdx)
    private var _referCursorRow: Int = _
    private var _mouseCursorRow: Int = _
    private var _rightSideRow: Int = _
    private var _lastOccurredRowOfMasterSer: Int = _
    private var _isAutoScrollToNewData = true
    private var _isMouseEnteredAnyChartPane: Boolean = _
    private var _isCursorCrossLineVisible = true
    /** listen to masterSer and process loading, update events to check if need to update cursor */
    private val masterSerReaction: Reactions.Reaction = {
      /** this reaction only process loading, update events to check if need to update cursor */
      case TSerEvent.FinishedLoading(_, _, fromTime, toTime, _, _)  => updateView(toTime)
      case TSerEvent.RefreshInLoading(_, _, fromTime, toTime, _, _) => updateView(toTime)
      case TSerEvent.Updated(_, _, fromTime, toTime, _, _)          => updateView(toTime)
      case _ =>
    }

    private def internal_setChartViewContainer(viewContainer: ChartViewContainer) {
      this.viewContainer = viewContainer

      internal_initCursorRow

      reactions += masterSerReaction
      listenTo(masterSer)

      addKeyMouseListenersTo(viewContainer)
    }

    private def internal_initCursorRow {
      /**
       * masterSer may have finished computing at this time, to adjust
       * the cursor to proper row, update it here.
       * @NOTICE
       * don't set row directly, instead, use setCursorByRow(row, row);
       */
      val row = masterSer.lastOccurredRow
      setCursorByRow(row, row, true)

      _mouseCursorRow = referCursorRow
    }

    private def addKeyMouseListenersTo(component: JComponent) {
      component.setFocusable(true)
      component.addKeyListener(new ChartViewKeyAdapter)
      component.addMouseWheelListener(new ChartViewMouseWheelListener)
    }

    private def removeKeyMouseListenersFrom(component: JComponent) {
      /** copy to a list to avoid concurrent issue */
      component.getKeyListeners foreach {x => component.removeKeyListener(x)}
      component.getMouseWheelListeners foreach {x => component.removeMouseWheelListener(x)}
    }

    def isCursorCrossLineVisible = _isCursorCrossLineVisible
    def isCursorCrossLineVisible_=(b: Boolean) {
      this._isCursorCrossLineVisible = b
    }

    def isMouseEnteredAnyChartPane = _isMouseEnteredAnyChartPane
    def isMouseEnteredAnyChartPane_=(b: Boolean) {
      val oldValue = this._isMouseEnteredAnyChartPane
      this._isMouseEnteredAnyChartPane = b

      if (!_isMouseEnteredAnyChartPane) {
        /** this cleanups mouse cursor */
        if (this._isMouseEnteredAnyChartPane != oldValue) {
          notifyChanged(classOf[MouseCursorObserver])
          updateViews
        }
      }

    }

    def isAutoScrollToNewData = _isAutoScrollToNewData
    def isAutoScrollToNewData_=(autoScrollToNewData: Boolean) {
      this._isAutoScrollToNewData = autoScrollToNewData
    }

    def wBar: Float = {
      _wBar
    }

    def growWBar(increment: Int) {
      _wBarIdx += increment
      if (_wBarIdx < 0) {
        _wBarIdx = 0
      } else if (_wBarIdx > BAR_WIDTHS_ARRAY.length - 1) {
        _wBarIdx = BAR_WIDTHS_ARRAY.length - 1
      }

      internal_setWBar(BAR_WIDTHS_ARRAY(_wBarIdx))
      updateViews
    }

    def setWBarByNBars(wViewPort: Int, nBars: Int) {
      if (nBars < 0) {
        return
      }

      /** decide wBar according to wViewPort. Do not use integer divide here */
      var newWBar = wViewPort.toFloat / nBars.toFloat

      /** adjust xfactorIdx to nearest */
      if (newWBar < BAR_WIDTHS_ARRAY(0)) {
        /** avoid too small xfactor */
        newWBar = BAR_WIDTHS_ARRAY(0)

        _wBarIdx = 0
      } else if (newWBar > BAR_WIDTHS_ARRAY(BAR_WIDTHS_ARRAY.length - 1)) {
        _wBarIdx = BAR_WIDTHS_ARRAY.length - 1
      } else {
        var i = 0
        val n = BAR_WIDTHS_ARRAY.length - 1
        var break = false
        while (i < n && !break) {
          if (newWBar > BAR_WIDTHS_ARRAY(i) && newWBar < BAR_WIDTHS_ARRAY(i + 1)) {
            /** which one is the nearest ? */
            _wBarIdx = if (Math.abs(BAR_WIDTHS_ARRAY(i) - newWBar) < Math.abs(BAR_WIDTHS_ARRAY(i + 1) - newWBar)) i else i + 1
            break = true
          }
          i += 1
        }
      }

      internal_setWBar(newWBar)
      updateViews
    }

    def isOnCalendarMode = masterSer.isOnCalendarMode
    def isOnCalendarMode_=(b: Boolean) {
      if (isOnCalendarMode != b) {
        val referCursorTime1 = referCursorTime
        val rightCursorTime1 = rightSideTime

        if (b == true) {
          masterSer.toOnCalendarMode
        } else {
          masterSer.toOnOccurredMode
        }

        internal_setReferCursorByTime(referCursorTime1)
        internal_setRightCursorByTime(rightCursorTime1)

        notifyChanged(classOf[ChartValidityObserver])
        updateViews
      }
    }

    def setCursorByRow(referRow: Int, rightRow: Int, willUpdateViews: Boolean) {
      /** set right cursor row first and directly */
      internal_setRightSideRow(rightRow)

      val oldValue = referCursorRow
      scrollReferCursor(referRow - oldValue, willUpdateViews)
    }

    def setReferCursorByRow(row: Int, willUpdateViews: Boolean) {
      val increment = row - referCursorRow
      scrollReferCursor(increment, willUpdateViews)
    }

    def scrollReferCursor(increment: Int, willUpdateViews: Boolean) {
      val referRow = referCursorRow + increment
      val rightRow = rightSideRow

      val rightSpacing = rightRow - referRow
      if (rightSpacing >= MIN_RIGHT_SPACING) {
        /** right spacing is enough, check left spacing: */
        val nBars = viewContainer.masterView.nBars
        val leftRow = rightRow - nBars + 1
        val leftSpacing = referRow - leftRow
        if (leftSpacing < MIN_LEFT_SPACING) {
          internal_setRightSideRow(rightRow + leftSpacing - MIN_LEFT_SPACING)
        }
      } else {
        internal_setRightSideRow(rightRow + MIN_RIGHT_SPACING - rightSpacing)

      }

      internal_setReferCursorRow(referRow)
      if (willUpdateViews) {
        updateViews
      }
    }

    /** keep refer cursor stay on same x of screen, and scroll charts left or right by bar */
    def scrollChartsHorizontallyByBar(increment: Int) {
      val rightRow = rightSideRow
      internal_setRightSideRow(rightRow + increment)

      scrollReferCursor(increment, true)
    }

    def scrollReferCursorToLeftSide {
      val rightRow = rightSideRow
      val nBars = viewContainer.masterView.nBars

      val leftRow = rightRow - nBars + MIN_LEFT_SPACING
      setReferCursorByRow(leftRow, true)
    }

    def setMouseCursorByRow(row: Int) {
      internal_setMouseCursorRow(row)
    }

    def updateViews {
      if (viewContainer != null) {
        viewContainer.repaint()
      }

      /**
       * as repaint() may be called by awt in instance's initialization, before
       * popupViewSet is created, so, check null.
       */
      popupViews foreach {view => view.repaint()}
    }

    final def referCursorRow: Int = _referCursorRow
    final def referCursorTime: Long = masterSer.timeOfRow(_referCursorRow)

    final def rightSideRow: Int = _rightSideRow
    final def rightSideTime: Long = masterSer.timeOfRow(_rightSideRow)

    final def leftSideTime: Long = masterSer.timeOfRow(leftSideRow)
    final def leftSideRow: Int = {
      val rightRow = rightSideRow
      val nBars = viewContainer.masterView.nBars

      rightRow - nBars + MIN_LEFT_SPACING
    }


    final def mouseCursorRow: Int = _mouseCursorRow
    final def mouseCursorTime: Long = masterSer.timeOfRow(_mouseCursorRow)

    /**
     * @NOTICE
     * =======================================================================
     * as we don't like referCursor and rightCursor being set directly by others,
     * the following setter methods are named internal_setXXX, and are private.
     */
    private def internal_setWBar(wBar: Float) {
      val oldValue = _wBar
      _wBar = wBar
      if (_wBar != oldValue) {
        notifyChanged(classOf[ChartValidityObserver])
      }
    }

    private def internal_setReferCursorRow(row: Int) {
      val oldValue = this._referCursorRow
      this._referCursorRow = row
      /** remember the lastRow for decision if need update cursor, see changeCursorByRow() */
      this._lastOccurredRowOfMasterSer = masterSer.lastOccurredRow
      if (this._referCursorRow != oldValue) {
        notifyChanged(classOf[ReferCursorObserver])
        notifyChanged(classOf[ChartValidityObserver])
      }
    }

    private def internal_setRightSideRow(row: Int) {
      val oldValue = this._rightSideRow
      this._rightSideRow = row
      if (this._rightSideRow != oldValue) {
        notifyChanged(classOf[ChartValidityObserver])
      }
    }

    private def internal_setReferCursorByTime(time: Long) {
      internal_setReferCursorRow(masterSer.rowOfTime(time))
    }

    private def internal_setRightCursorByTime(time: Long) {
      internal_setRightSideRow(masterSer.rowOfTime(time))
    }

    private def internal_setMouseCursorRow(row: Int) {
      val oldValue = this._mouseCursorRow
      this._mouseCursorRow = row

      /**
       * even mouseCursor row not changed, the mouse's y may has been changed,
       * so, notify observers without comparing the oldValue and newValue.
       */
      notifyChanged(classOf[MouseCursorObserver])
    }

    def isCursorAccelerated = _isCursorAccelerated
    def isCursorAccelerated_=(b: Boolean) {
      _isCursorAccelerated = b
    }

    def popupViewToDesktop(view: ChartView, dim: Dimension, alwaysOnTop: Boolean, joint: Boolean) {
      val popupView = view

      popupViews.add(popupView)
      addKeyMouseListenersTo(popupView)

      val w = dim.width
      val h = dim.height
      val frame = new JFrame//new JDialog (), true);
      frame.setAlwaysOnTop(alwaysOnTop)
      frame.setTitle(popupView.mainSer.shortDescription)
      frame.add(popupView, BorderLayout.CENTER)
      val screenSize = Toolkit.getDefaultToolkit.getScreenSize
      frame.setBounds((screenSize.width - w) / 2, (screenSize.height - h) / 2, w, h)
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
      frame.addWindowListener(new WindowAdapter {

          override def windowClosed(e: WindowEvent) {
            removeKeyMouseListenersFrom(popupView)
            popupViews.remove(popupView)
          }
        })

      frame.setVisible(true)
    }

    /**
     * Factory method to create ChartViewContainer instance, got the relations
     * between ChartViewContainer and Controller ready.
     */
    def createChartViewContainer[T <: ChartViewContainer](clazz: Class[T], focusableParent: Component): T = {
      try {
        val instance = clazz.newInstance
        instance.init(focusableParent, this)
        /**
         * @Note
         * Always call internal_setChartViewContainer(instance) next to
         * instance.init(focusableParent, this), since the internal_initCursorRow()
         * procedure needs the children of chartViewContainer ready.
         */
        internal_setChartViewContainer(instance)
        instance
      } catch {
        case ex: InstantiationException => ex.printStackTrace; null.asInstanceOf[T]
        case ex: IllegalAccessException => ex.printStackTrace; null.asInstanceOf[T]
      }
    }

    @throws(classOf[Throwable])
    override protected def finalize {
      deafTo(masterSer)
      if (masterSerReaction != null) {
        reactions -= masterSerReaction
      }

      super.finalize
    }

    private def updateView(toTime: Long): Unit = {
      viewContainer.masterView match {
        case masterView: WithDrawingPane =>
          val drawing = masterView.selectedDrawing
          if (drawing != null && drawing.isInDrawing) {
            return
          }
        case _ =>
      }

      val oldReferRow = referCursorRow
      if (oldReferRow == _lastOccurredRowOfMasterSer || _lastOccurredRowOfMasterSer <= 0) {
        /** refresh only when the old lastRow is extratly oldReferRow, or prev lastRow <= 0 */
        val lastTime = Math.max(toTime, masterSer.lastOccurredTime)
        val rightRow = masterSer.rowOfTime(lastTime)
        val referRow = rightRow

        setCursorByRow(referRow, rightRow, true)
      }

      notifyChanged(classOf[ChartValidityObserver])
    }

    private def internal_getCorrespondingChartView(e: InputEvent): ChartView = {
      var correspondingChartView: ChartView = null

      e.getSource match {
        case source: ChartViewContainer =>
          correspondingChartView = source.masterView
        case source: ChartView =>
          correspondingChartView = source
        case _ => null
      }

      correspondingChartView
    }

    /**
     * =============================================================
     * Bellow is the private listener classes for key and mouse:
     */
    private class ChartViewKeyAdapter extends KeyAdapter {

      private val LEFT  = -1
      private val RIGHT = 1

      override def keyPressed(e: KeyEvent) {
        val view = internal_getCorrespondingChartView(e)
        if (view == null || !view.isInteractive) {
          return
        }

        val fastSteps = (view.nBars * 0.168f).toInt

        e.getKeyCode match {
          case KeyEvent.VK_LEFT =>
            if (e.isControlDown) {
              moveCursorInDirection(fastSteps, LEFT)
            } else {
              moveChartsInDirection(fastSteps, LEFT)
            }
          case KeyEvent.VK_RIGHT =>
            if (e.isControlDown) {
              moveCursorInDirection(fastSteps, RIGHT)
            } else {
              moveChartsInDirection(fastSteps, RIGHT)
            }
          case KeyEvent.VK_UP   if !e.isControlDown =>
            growWBar(1)
          case KeyEvent.VK_DOWN if !e.isControlDown =>
            growWBar(-1)
          case _ =>
        }

      }

      override def keyReleased(e: KeyEvent) {
        if (e.getKeyCode == KeyEvent.VK_SPACE) {
          _isCursorAccelerated = !_isCursorAccelerated
        }
      }

      override def keyTyped(e: KeyEvent) {
      }

      private def moveCursorInDirection(fastSteps: Int, DIRECTION: Int) {
        val steps = (if (isCursorAccelerated) fastSteps else 1) * DIRECTION

        scrollReferCursor(steps, true)
      }

      private def moveChartsInDirection(fastSteps: Int, DIRECTION: Int) {
        val steps = (if (isCursorAccelerated) fastSteps else 1) * DIRECTION

        scrollChartsHorizontallyByBar(steps)
      }
    }

    private class ChartViewMouseWheelListener extends MouseWheelListener {

      def mouseWheelMoved(e: java.awt.event.MouseWheelEvent) {
        val view = internal_getCorrespondingChartView(e)
        if (view == null || !view.isInteractive) {
          return
        }

        val fastSteps = (view.nBars * 0.168f).toInt

        if (e.isShiftDown) {
          /** zoom in / zoom out */
          growWBar(e.getWheelRotation)
        } else if (e.isControlDown) {
          if (!view.isInteractive) {
            return
          }

          val unitsToScroll = if (isCursorAccelerated) e.getWheelRotation * fastSteps else e.getWheelRotation
          /** move refer cursor left / right */
          scrollReferCursor(unitsToScroll, true)
        } else {
          if (!view.isInteractive) {
            return
          }

          val unitsToScroll = if (isCursorAccelerated) e.getWheelRotation * fastSteps else e.getWheelRotation
          /** keep referCursor stay same x in screen, and move */
          scrollChartsHorizontallyByBar(unitsToScroll)
        }
      }
    }
  }
}