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
        // Note: Draw the line based on the top of the child (skipping the first child) so that
        // dividers follow views during remove animation
        int numChildren = parent.getChildCount();
        for (int i = 1; i < numChildren; i++) {
            View child = parent.getChildAt(i);

            // Adjust y by the offset to account for animations
            int y = child.getTop() + (int)child.getTranslationY();
            c.drawLine(0, y, child.getRight(), y, mPaint);
        }
    }
}
