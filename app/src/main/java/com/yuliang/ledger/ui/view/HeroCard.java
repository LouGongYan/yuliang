package com.yuliang.ledger.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.FrameLayout;

import com.yuliang.ledger.ui.AppTheme;
import com.yuliang.ledger.ui.Ui;

/** A gradient hero card with two soft decorative circles. */
public class HeroCard extends FrameLayout {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HeroCard(Context context, AppTheme theme) {
        super(context);
        setBackground(theme.heroDrawable());
        setClipToOutline(true);
        setWillNotDraw(false);
        setPadding(Ui.dp(context, 22), Ui.dp(context, 20),
                Ui.dp(context, 22), Ui.dp(context, 20));
        paint.setColor(0x14FFFFFF);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        canvas.drawCircle(w * 0.94f, -h * 0.18f, w * 0.34f, paint);
        paint.setColor(0x0CFFFFFF);
        canvas.drawCircle(w * 0.70f, h * 1.08f, w * 0.44f, paint);
        paint.setColor(0x14FFFFFF);
    }
}
