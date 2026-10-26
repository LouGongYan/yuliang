package com.yuliang.ledger.ui.view;

import android.content.Context;
import android.widget.ScrollView;

/** ScrollView that wraps its content but never grows beyond maxHeight. */
public class MaxHeightScrollView extends ScrollView {

    private int maxHeight;

    public MaxHeightScrollView(Context context) {
        super(context);
        setFillViewport(false);
        setClipToPadding(false);
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (maxHeight > 0) {
            int mode = MeasureSpec.getMode(heightMeasureSpec);
            int size = MeasureSpec.getSize(heightMeasureSpec);
            int height = mode == MeasureSpec.UNSPECIFIED ? maxHeight : Math.min(size, maxHeight);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
