package com.wind.printer_lib;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

/**
 * Created by wind on 2018/6/10.
 */

public class Utils {
    private static int[][] Floyd16x16 = new int[][]{{0, 128, 32, 160, 8, 136, 40, 168, 2, 130, 34, 162, 10, 138, 42, 170}, {192, 64, 224, 96, 200, 72, 232, 104, 194, 66, 226, 98, 202, 74, 234, 106}, {48, 176, 16, 144, 56, 184, 24, 152, 50, 178, 18, 146, 58, 186, 26, 154}, {240, 112, 208, 80, 248, 120, 216, 88, 242, 114, 210, 82, 250, 122, 218, 90}, {12, 140, 44, 172, 4, 132, 36, 164, 14, 142, 46, 174, 6, 134, 38, 166}, {204, 76, 236, 108, 196, 68, 228, 100, 206, 78, 238, 110, 198, 70, 230, 102}, {60, 188, 28, 156, 52, 180, 20, 148, 62, 190, 30, 158, 54, 182, 22, 150}, {252, 124, 220, 92, 244, 116, 212, 84, 254, 126, 222, 94, 246, 118, 214, 86}, {3, 131, 35, 163, 11, 139, 43, 171, 1, 129, 33, 161, 9, 137, 41, 169}, {195, 67, 227, 99, 203, 75, 235, 107, 193, 65, 225, 97, 201, 73, 233, 105}, {51, 179, 19, 147, 59, 187, 27, 155, 49, 177, 17, 145, 57, 185, 25, 153}, {243, 115, 211, 83, 251, 123, 219, 91, 241, 113, 209, 81, 249, 121, 217, 89}, {15, 143, 47, 175, 7, 135, 39, 167, 13, 141, 45, 173, 5, 133, 37, 165}, {207, 79, 239, 111, 199, 71, 231, 103, 205, 77, 237, 109, 197, 69, 229, 101}, {63, 191, 31, 159, 55, 183, 23, 151, 61, 189, 29, 157, 53, 181, 21, 149}, {254, 127, 223, 95, 247, 119, 215, 87, 253, 125, 221, 93, 245, 117, 213, 85}};
    private static int[] p0 = new int[]{0, 128};
    private static int[] p1 = new int[]{0, 64};
    private static int[] p2 = new int[]{0, 32};
    private static int[] p3 = new int[]{0, 16};
    private static int[] p4 = new int[]{0, 8};
    private static int[] p5 = new int[]{0, 4};
    private static int[] p6 = new int[]{0, 2};
    /**
     * 灰度图
     * @param origin
     * @return
     */
    public static Bitmap grayBitmap(Bitmap origin){
        int width=origin.getWidth();
        int height=origin.getHeight();
        Bitmap bitmap=Bitmap.createBitmap(width,height, Bitmap.Config.RGB_565);
        Canvas canvas=new Canvas(bitmap);
        Paint paint=new Paint();
        ColorMatrix matrix=new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter=new ColorMatrixColorFilter(matrix);
        paint.setColorFilter(filter);
        canvas.drawBitmap(origin,0,0,paint);
        return bitmap;
    }

    public static Bitmap resizeImage(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = (float)w / (float)width;
        float scaleHeight = (float)h / (float)height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return resizedBitmap;
    }

    public static byte[] bitmapToBWPix(Bitmap mBitmap) {
        int[] pixels = new int[mBitmap.getWidth() * mBitmap.getHeight()];
        byte[] data = new byte[mBitmap.getWidth() * mBitmap.getHeight()];
        Bitmap grayBitmap = grayBitmap(mBitmap);
        grayBitmap.getPixels(pixels, 0, mBitmap.getWidth(), 0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        format_K_dither16x16(pixels, grayBitmap.getWidth(), grayBitmap.getHeight(), data);
        return data;
    }

    private static void format_K_dither16x16_int(int[] orgpixels, int xsize, int ysize, int[] despixels) {
        int k = 0;

        for(int y = 0; y < ysize; ++y) {
            for(int x = 0; x < xsize; ++x) {
                if((orgpixels[k] & 255) > Floyd16x16[x & 15][y & 15]) {
                    despixels[k] = -1;
                } else {
                    despixels[k] = -16777216;
                }

                ++k;
            }
        }

    }

    private static void format_K_dither16x16(int[] orgpixels, int xsize, int ysize, byte[] despixels) {
        int k = 0;

        for(int y = 0; y < ysize; ++y) {
            for(int x = 0; x < xsize; ++x) {
                if((orgpixels[k] & 255) > Floyd16x16[x & 15][y & 15]) {
                    despixels[k] = 0;
                } else {
                    despixels[k] = 1;
                }

                ++k;
            }
        }

    }

    public static byte[] pixToEscRastBitImageCmd(byte[] src) {
        byte[] data = new byte[src.length / 8];
        int i = 0;

        for(int k = 0; i < data.length; ++i) {
            data[i] = (byte)(p0[src[k]] + p1[src[k + 1]] + p2[src[k + 2]] + p3[src[k + 3]]
                    + p4[src[k + 4]] + p5[src[k + 5]] + p6[src[k + 6]] + src[k + 7]);
            k += 8;
        }

        return data;
    }
}
