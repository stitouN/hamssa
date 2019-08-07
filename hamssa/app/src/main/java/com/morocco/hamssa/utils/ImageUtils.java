package com.morocco.hamssa.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore.Images;

import java.io.IOException;

public class ImageUtils {
	public static Bitmap correctImageOrientation(Bitmap bitmap, String filename){
		Bitmap newBitmap = bitmap;
		try{
			ExifInterface exif = new ExifInterface(filename);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			int degrees = getDegrees(orientation);
			newBitmap = rotate(bitmap, degrees);
		}catch(IOException e){}
        return newBitmap;
    }
	
	public static Bitmap correctImageOrientation(Context context, Bitmap bitmap, Uri uri){
		Bitmap newBitmap = bitmap;
		try{
			int rotation = 0;
			if (uri.getScheme().equals("content")) {
				//From the media gallery
				String[] projection = { Images.ImageColumns.ORIENTATION };
				Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
				if (c.moveToFirst()) {
					rotation = c.getInt(0);
				}               
			} else if (uri.getScheme().equals("file")) {
				//From a file saved by the camera
				ExifInterface exif = new ExifInterface(uri.getPath());
				int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
				rotation = getDegrees(orientation);
			}

			newBitmap = rotate(bitmap, rotation);
		} catch (IOException e) {}
		return newBitmap;
	}

	private static int getDegrees(int orientation){
		if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
	}
	
	private static Bitmap rotate(Bitmap bitmap, int degree) {
		if(degree == 0){
			return bitmap;
		}
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

	static public Bitmap getRoundImage(Bitmap bitmap, boolean withBorder){
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(output);
		canvas.drawARGB(0, 0, 0, 0);

		final Paint paint = new Paint();


		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);

		canvas.drawCircle(bitmap.getWidth() / 2+0.7f, bitmap.getHeight() / 2+0.7f,
				bitmap.getWidth() / 2+0.1f, paint);

		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

		final Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		canvas.drawBitmap(bitmap, srcRect, srcRect, paint);

		if(withBorder){
			paint.setStyle(Paint.Style.STROKE);
			paint.setColor(Color.WHITE);
			paint.setStrokeWidth((int)(bitmap.getWidth()*0.07));
			canvas.drawCircle(bitmap.getWidth() / 2+0.7f, bitmap.getHeight() / 2+0.7f,
					bitmap.getWidth() / 2+0.1f, paint);
		}
		return output;
	}
}
