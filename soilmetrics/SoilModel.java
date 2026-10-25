package com.example.soilmetrics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// runs the soil AI
public class SoilModel {

    // model picture size
    public static final int SIZE = 64;

    private Interpreter tflite;

    public SoilModel(Context context) throws IOException {
        // read model file
        InputStream input = context.getAssets().open("soil_model.tflite");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int count = input.read(chunk);
        while (count != -1) {
            output.write(chunk, 0, count);
            count = input.read(chunk);
        }
        input.close();

        byte[] bytes = output.toByteArray();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.order(ByteOrder.nativeOrder());
        buffer.put(bytes);
        tflite = new Interpreter(buffer);
    }

    // cut out middle square
    private Bitmap getSquare(Bitmap photo) {
        int w = photo.getWidth();
        int h = photo.getHeight();
        int side = Math.min(w, h);
        Bitmap square = Bitmap.createBitmap(photo, (w - side) / 2, (h - side) / 2, side, side);
        return Bitmap.createScaledBitmap(square, SIZE, SIZE, true);
    }

    // guess organic matter percent
    public double predict(Bitmap photo) {
        Bitmap small = getSquare(photo);

        // pixels to numbers
        float[][][][] input = new float[1][SIZE][SIZE][3];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int pixel = small.getPixel(x, y);
                input[0][y][x][0] = Color.red(pixel) / 255.0f;
                input[0][y][x][1] = Color.green(pixel) / 255.0f;
                input[0][y][x][2] = Color.blue(pixel) / 255.0f;
            }
        }

        float[][] output = new float[1][1];
        tflite.run(input, output);

        double om = output[0][0];
        if (om < 0.1) {
            om = 0.1;
        }
        if (om > 12) {
            om = 12;
        }
        return om;
    }

    // check colors look soil
    public boolean looksLikeSoil(Bitmap photo) {
        Bitmap small = getSquare(photo);
        long red = 0;
        long green = 0;
        long blue = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int pixel = small.getPixel(x, y);
                red = red + Color.red(pixel);
                green = green + Color.green(pixel);
                blue = blue + Color.blue(pixel);
            }
        }
        int total = SIZE * SIZE;
        double r = red / (double) total;
        double g = green / (double) total;
        double b = blue / (double) total;

        // too green or blue
        if (g > r + 15) {
            return false;
        }
        if (b > r + 10) {
            return false;
        }
        return true;
    }

    // words for the number
    public static String omRating(double om) {
        if (om < 2) {
            return "Low";
        } else if (om < 3.5) {
            return "Medium";
        } else if (om < 5) {
            return "Good";
        } else {
            return "High";
        }
    }
}
