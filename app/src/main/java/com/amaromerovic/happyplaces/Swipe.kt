package com.amaromerovic.happyplaces

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

abstract class Swipe(context: Context) : ItemTouchHelper.Callback() {

    var mContext: Context? = null
    private var mClearPaint: Paint? = null
    private var drawable: Drawable? = null
    private var intrinsicWidth = 0
    private var intrinsicHeight = 0

    init {
        mContext = context
        mClearPaint = Paint()
        mClearPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
    }


    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        viewHolder1: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        val itemView = viewHolder.itemView
        val itemHeight = itemView.height
        val isCancelled = dX == 0f && !isCurrentlyActive
        if (isCancelled) {
            clearCanvas(
                c,
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }
        val p = Paint()
        if (dX < 0) {
            p.color = ContextCompat.getColor(mContext!!, R.color.black)
            c.drawRoundRect(
                itemView.left.toFloat() + 10f,
                itemView.top.toFloat() + 13f,
                itemView.right.toFloat() - 10f,
                itemView.bottom.toFloat() - 13f,
                5f,
                5f,
                p
            )
            drawable = ContextCompat.getDrawable(mContext!!, android.R.drawable.ic_menu_delete)
            assert(drawable != null)
            intrinsicWidth = drawable!!.intrinsicWidth
            intrinsicHeight = drawable!!.intrinsicHeight
            val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
            val deleteIconLeft = itemView.right - deleteIconMargin - intrinsicWidth
            val deleteIconRight = itemView.right - deleteIconMargin
            val deleteIconBottom = deleteIconTop + intrinsicHeight
            drawable!!.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
        } else {
            p.color = ContextCompat.getColor(mContext!!, R.color.black)
            c.drawRoundRect(
                itemView.left.toFloat() + 10f,
                itemView.top.toFloat() + 13f,
                itemView.right.toFloat() - 10f,
                itemView.bottom.toFloat() - 13f,
                5f,
                5f,
                p
            )
            drawable = ContextCompat.getDrawable(mContext!!, android.R.drawable.ic_menu_edit)
            assert(drawable != null)
            intrinsicWidth = drawable!!.intrinsicWidth
            intrinsicHeight = drawable!!.intrinsicHeight
            val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
            val deleteIconLeft = itemView.right - deleteIconMargin - intrinsicWidth - 800
            val deleteIconRight = itemView.right - deleteIconMargin - 800
            val deleteIconBottom = deleteIconTop + intrinsicHeight
            drawable!!.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
        }
        val alpha = 1 - abs(dX) / viewHolder.itemView.width.toFloat()
        viewHolder.itemView.alpha = alpha
        viewHolder.itemView.translationX = dX
        drawable!!.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    open fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        c.drawRect(left, top, right, bottom, mClearPaint!!)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.7f
    }
}