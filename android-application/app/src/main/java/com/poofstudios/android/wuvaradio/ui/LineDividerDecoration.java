package com.poofstudios.android.wuvaradio.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.poofstudios.android.wuvaradio.R;

public class LineDividerDecoration extends RecyclerView.ItemDecoration {

    private Paint mPaint;

    public LineDividerDecoration(Context context, Resources resources) {
        int dividerColor = ContextCompat.getColor(context, R.color.dividerColor);
        mPaint = new Paint();
        mPaint.setColor(dividerColor);
        mPaint.setStrokeWidth(resources.getDimensionPixelOffset(R.dimen.divider_height));
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int numChildren = parent.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildLayoutPosition(child);

            // Skip last position
            if (position == state.getItemCount()-1) {
                continue;
            }
            int y = child.getBottom();
            c.drawLine(0, y, child.getRight(), y, mPaint);
        }
    }
}
