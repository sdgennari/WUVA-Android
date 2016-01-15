package com.poofstudios.android.wuvaradio.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.ColorFilter;
import android.graphics.Canvas;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.squareup.picasso.Transformation;

/*
    Mostly from this gist: https://gist.github.com/ryanbateman/6667995
 */

public class BlurTransform implements Transformation {

    RenderScript rs;
    public BlurTransform(Context context) {
        super();
        rs = RenderScript.create(context);
    }

    @Override
    public Bitmap transform(Bitmap bitmap) {

        Bitmap source = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        Bitmap blurredBitmap = Bitmap.createBitmap(source);

        // Allocate memory for Renderscript to work with
        Allocation input = Allocation.createFromBitmap(rs, source, Allocation.MipmapControl.MIPMAP_FULL, Allocation.USAGE_SHARED);
        Allocation output = Allocation.createTyped(rs, input.getType());

        // Load up an instance of the specific script that we want to use.
        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setInput(input);

        // Set the blur radius
        script.setRadius(25);

        // Start the ScriptIntrinisicBlur
        script.forEach(output);

        // Copy the output to the blurred source
        output.copyTo(blurredBitmap);

        // Darken image
        Paint paint = new Paint();
        ColorFilter filter = new LightingColorFilter(0xffaaaaaa, 0x00000000);
        paint.setColorFilter(filter);
        Canvas canvas = new Canvas(blurredBitmap);
        canvas.drawBitmap(blurredBitmap, 0, 0, paint);

        bitmap.recycle();

        return blurredBitmap;
    }

    @Override
    public String key() {
        return "blur";
    }

}