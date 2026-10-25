package com.example.soilmetrics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.HashMap;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

// finds cow body points
public class CowPose {

    // finder picture size
    public static final int FINDER_SIZE = 416;
    // pose picture size
    public static final int POSE_SIZE = 256;
    // body points we get
    public static final int POINTS = 17;

    // body point numbers
    public static final int NOSE = 2;
    public static final int NECK = 3;
    public static final int TAIL = 4;
    public static final int LEFT_FRONT = 7;
    public static final int RIGHT_FRONT = 10;
    public static final int LEFT_BACK = 13;
    public static final int RIGHT_BACK = 16;

    // lines to draw skeleton
    public static final int[][] BONES = {{2, 3}, {3, 4}, {3, 5}, {5, 6}, {6, 7}, {3, 8}, {8, 9}, {9, 10},
            {4, 11}, {11, 12}, {12, 13}, {4, 14}, {14, 15}, {15, 16}};

    // animal types to find
    static final int HORSE = 17;
    static final int SHEEP = 18;
    static final int COW = 19;

    private OrtEnvironment env;
    private OrtSession finder;
    private OrtSession pose;

    public CowPose(Context context) throws Exception {
        env = OrtEnvironment.getEnvironment();
        finder = env.createSession(readAsset(context, "cow_finder.onnx"), new OrtSession.SessionOptions());
        pose = env.createSession(readAsset(context, "cow_pose.onnx"), new OrtSession.SessionOptions());
    }

    // read model file bytes
    private byte[] readAsset(Context context, String name) throws Exception {
        InputStream input = context.getAssets().open(name);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int count = input.read(chunk);
        while (count != -1) {
            output.write(chunk, 0, count);
            count = input.read(chunk);
        }
        input.close();
        return output.toByteArray();
    }

    // find the animal box
    // answer: left, top, right, bottom, score
    public float[] findAnimal(Bitmap frame) throws Exception {
        // shrink to fit square
        float scale = Math.min(FINDER_SIZE / (float) frame.getWidth(), FINDER_SIZE / (float) frame.getHeight());
        int newW = Math.round(frame.getWidth() * scale);
        int newH = Math.round(frame.getHeight() * scale);
        Bitmap small = Bitmap.createScaledBitmap(frame, newW, newH, true);
        Bitmap square = Bitmap.createBitmap(FINDER_SIZE, FINDER_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(square);
        canvas.drawColor(Color.rgb(114, 114, 114));
        canvas.drawBitmap(small, 0, 0, null);

        // pixels to numbers
        int area = FINDER_SIZE * FINDER_SIZE;
        int[] pixels = new int[area];
        square.getPixels(pixels, 0, FINDER_SIZE, 0, 0, FINDER_SIZE, FINDER_SIZE);
        float[] input = new float[3 * area];
        for (int i = 0; i < area; i++) {
            input[i] = Color.blue(pixels[i]);
            input[area + i] = Color.green(pixels[i]);
            input[2 * area + i] = Color.red(pixels[i]);
        }

        OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), new long[]{1, 3, FINDER_SIZE, FINDER_SIZE});
        HashMap<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("images", tensor);
        OrtSession.Result result = finder.run(inputs);
        float[][] rows = ((float[][][]) result.get(0).getValue())[0];
        result.close();
        tensor.close();

        // check every guess box
        float[] best = null;
        int row = 0;
        int[] steps = {8, 16, 32};
        for (int s = 0; s < steps.length; s++) {
            int stride = steps[s];
            int cells = FINDER_SIZE / stride;
            for (int gy = 0; gy < cells; gy++) {
                for (int gx = 0; gx < cells; gx++) {
                    float[] r = rows[row];
                    row++;
                    float animal = Math.max(r[5 + COW], Math.max(r[5 + HORSE], r[5 + SHEEP]));
                    float score = r[4] * animal;
                    if (best == null || score > best[4]) {
                        float centerX = (r[0] + gx) * stride / scale;
                        float centerY = (r[1] + gy) * stride / scale;
                        float w = (float) Math.exp(r[2]) * stride / scale;
                        float h = (float) Math.exp(r[3]) * stride / scale;
                        best = new float[]{centerX - w / 2, centerY - h / 2, centerX + w / 2, centerY + h / 2, score};
                    }
                }
            }
        }
        return best;
    }

    // find 17 body points
    // each point: x, y, sure
    public float[][] findPoints(Bitmap frame, float[] box) throws Exception {
        // square around the animal
        float centerX = (box[0] + box[2]) / 2;
        float centerY = (box[1] + box[3]) / 2;
        float side = Math.max(box[2] - box[0], box[3] - box[1]) * 1.25f;
        float left = centerX - side / 2;
        float top = centerY - side / 2;

        Bitmap crop = Bitmap.createBitmap(POSE_SIZE, POSE_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(crop);
        Matrix move = new Matrix();
        move.postTranslate(-left, -top);
        move.postScale(POSE_SIZE / side, POSE_SIZE / side);
        canvas.drawBitmap(frame, move, null);

        // pixels to numbers
        int area = POSE_SIZE * POSE_SIZE;
        int[] pixels = new int[area];
        crop.getPixels(pixels, 0, POSE_SIZE, 0, 0, POSE_SIZE, POSE_SIZE);
        float[] input = new float[3 * area];
        for (int i = 0; i < area; i++) {
            input[i] = (Color.red(pixels[i]) - 123.675f) / 58.395f;
            input[area + i] = (Color.green(pixels[i]) - 116.28f) / 57.12f;
            input[2 * area + i] = (Color.blue(pixels[i]) - 103.53f) / 57.375f;
        }

        OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), new long[]{1, 3, POSE_SIZE, POSE_SIZE});
        HashMap<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input", tensor);
        OrtSession.Result result = pose.run(inputs);
        float[][] xGuess = ((float[][][]) result.get(0).getValue())[0];
        float[][] yGuess = ((float[][][]) result.get(1).getValue())[0];
        result.close();
        tensor.close();

        // best spot for each
        float[][] points = new float[POINTS][3];
        for (int k = 0; k < POINTS; k++) {
            int bestX = 0;
            for (int i = 1; i < xGuess[k].length; i++) {
                if (xGuess[k][i] > xGuess[k][bestX]) {
                    bestX = i;
                }
            }
            int bestY = 0;
            for (int i = 1; i < yGuess[k].length; i++) {
                if (yGuess[k][i] > yGuess[k][bestY]) {
                    bestY = i;
                }
            }
            // half steps to pixels
            points[k][0] = left + (bestX / 2f) * side / POSE_SIZE;
            points[k][1] = top + (bestY / 2f) * side / POSE_SIZE;
            points[k][2] = Math.min(xGuess[k][bestX], yGuess[k][bestY]);
        }
        return points;
    }

    // box around body points
    public static float[] boxFromPoints(float[][] points) {
        float left = Float.MAX_VALUE;
        float top = Float.MAX_VALUE;
        float right = -Float.MAX_VALUE;
        float bottom = -Float.MAX_VALUE;
        int count = 0;
        for (int k = 0; k < POINTS; k++) {
            if (points[k][2] < 0.3f) {
                continue;
            }
            count++;
            left = Math.min(left, points[k][0]);
            top = Math.min(top, points[k][1]);
            right = Math.max(right, points[k][0]);
            bottom = Math.max(bottom, points[k][1]);
        }
        // too few to trust
        if (count < 5) {
            return null;
        }
        // add room for back
        float extra = (bottom - top) * 0.25f;
        return new float[]{left, top - extra, right, bottom, 1};
    }
}
