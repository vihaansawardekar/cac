package com.example.soilmetrics;

import java.util.ArrayList;
import java.util.Arrays;

// turns body points into lameness
public class GaitAnalyzer {

    // need this many frames
    public static final int MIN_FRAMES = 20;

    // keep number in range
    static double limit(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }

    // middle number of list
    static double median(double[] list) {
        double[] copy = list.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    // number at a percent
    static double percentile(double[] list, double percent) {
        double[] copy = list.clone();
        Arrays.sort(copy);
        int spot = (int) Math.round((copy.length - 1) * percent / 100.0);
        return copy[spot];
    }

    // smooth out one bad frame
    static double[] medianFilter(double[] list) {
        double[] out = list.clone();
        for (int i = 1; i < list.length - 1; i++) {
            out[i] = median(new double[]{list[i - 1], list[i], list[i + 1]});
        }
        return out;
    }

    // how well steps repeat
    // answer: match amount, step length
    static double[] repeatCheck(double[] signal) {
        int n = signal.length;
        double mean = 0;
        for (int i = 0; i < n; i++) {
            mean = mean + signal[i];
        }
        mean = mean / n;

        double best = -1;
        int bestLag = 4;
        for (int lag = 4; lag < n / 2; lag++) {
            double top = 0;
            double sumA = 0;
            double sumB = 0;
            for (int i = 0; i < n - lag; i++) {
                double a = signal[i] - mean;
                double b = signal[i + lag] - mean;
                top = top + a * b;
                sumA = sumA + a * a;
                sumB = sumB + b * b;
            }
            double match = top / Math.sqrt(sumA * sumB + 0.000001);
            if (match > best) {
                best = match;
                bestLag = lag;
            }
        }
        return new double[]{best, bestLag};
    }

    // main gait math
    public static GaitResult analyze(ArrayList<float[][]> frames) {
        GaitResult result = new GaitResult();

        // hoof spots per frame
        ArrayList<double[]> rows = new ArrayList<>();
        ArrayList<Double> heads = new ArrayList<>();
        ArrayList<Double> lengths = new ArrayList<>();
        int[] hooves = {CowPose.LEFT_FRONT, CowPose.RIGHT_FRONT, CowPose.LEFT_BACK, CowPose.RIGHT_BACK};
        for (int f = 0; f < frames.size(); f++) {
            float[][] p = frames.get(f);
            if (p == null || p[CowPose.NECK][2] < 0.3f || p[CowPose.TAIL][2] < 0.3f) {
                continue;
            }
            // body length is ruler
            double dx = p[CowPose.NECK][0] - p[CowPose.TAIL][0];
            double dy = p[CowPose.NECK][1] - p[CowPose.TAIL][1];
            double body = Math.sqrt(dx * dx + dy * dy);
            if (body < 20) {
                continue;
            }
            // which way cow faces
            double facing = 1;
            if (dx < 0) {
                facing = -1;
            }
            double midX = (p[CowPose.NECK][0] + p[CowPose.TAIL][0]) / 2.0;
            double[] row = new double[4];
            for (int h = 0; h < 4; h++) {
                row[h] = facing * (p[hooves[h]][0] - midX) / body;
            }
            rows.add(row);
            heads.add((p[CowPose.NOSE][1] - p[CowPose.NECK][1]) / body);
            lengths.add(body);
        }

        // skip turned frames
        double[] lengthList = new double[lengths.size()];
        for (int i = 0; i < lengths.size(); i++) {
            lengthList[i] = lengths.get(i);
        }
        if (lengthList.length < MIN_FRAMES) {
            result.problem = "Could not see the whole cow in enough frames. Film her from the side as she walks past.";
            return result;
        }
        double normalLength = median(lengthList);
        ArrayList<double[]> goodRows = new ArrayList<>();
        ArrayList<Double> goodHeads = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (lengthList[i] > 0.6 * normalLength) {
                goodRows.add(rows.get(i));
                goodHeads.add(heads.get(i));
            }
        }
        int n = goodRows.size();
        if (n < MIN_FRAMES) {
            result.problem = "The cow turned too much. Film her walking straight past you.";
            return result;
        }

        // one list per hoof
        double[][] hoof = new double[4][n];
        double[] head = new double[n];
        for (int i = 0; i < n; i++) {
            for (int h = 0; h < 4; h++) {
                hoof[h][i] = goodRows.get(i)[h];
            }
            head[i] = goodHeads.get(i);
        }
        for (int h = 0; h < 4; h++) {
            hoof[h] = medianFilter(hoof[h]);
        }
        head = medianFilter(head);

        // 1. step length balance
        double[] reach = new double[4];
        for (int h = 0; h < 4; h++) {
            reach[h] = percentile(hoof[h], 90) - percentile(hoof[h], 10);
        }
        double frontGap = Math.abs(reach[0] - reach[1]) / Math.max(reach[0], reach[1]);
        double backGap = Math.abs(reach[2] - reach[3]) / Math.max(reach[2], reach[3]);
        result.asymmetry = Math.max(frontGap, backGap);

        // 2. steady step rhythm
        double[] matches = new double[4];
        double[] lags = new double[4];
        for (int h = 0; h < 4; h++) {
            double[] check = repeatCheck(hoof[h]);
            matches[h] = check[0];
            lags[h] = check[1];
        }
        result.rhythm = median(matches);
        double stepLength = median(lags);

        // 3. head bob each stride
        double headMean = 0;
        for (int i = 0; i < n; i++) {
            headMean = headMean + head[i];
        }
        headMean = headMean / n;
        double cosSum = 0;
        double sinSum = 0;
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / stepLength;
            cosSum = cosSum + (head[i] - headMean) * Math.cos(angle);
            sinSum = sinSum + (head[i] - headMean) * Math.sin(angle);
        }
        result.headBob = 2 * Math.sqrt(cosSum * cosSum + sinSum * sinSum) / n;

        // lameness score 1 to 5
        double a = limit((result.asymmetry - 0.15) / 0.30, 0, 1);
        double b = limit((result.headBob - 0.02) / 0.06, 0, 1);
        double c = limit((0.60 - result.rhythm) / 0.40, 0, 1);
        result.score = 1 + 4 * (0.5 * a + 0.25 * b + 0.25 * c);

        // words for the score
        if (result.score < 2) {
            result.words = "Sound";
            result.advice = "Walks sound. No sign of lameness.";
        } else if (result.score < 3) {
            result.words = "Mild";
            result.advice = "Mild warning. Watch this cow and check her again in a few days.";
        } else if (result.score < 4) {
            result.words = "Lame";
            result.advice = "Likely lame. Check her hooves soon.";
        } else {
            result.words = "Very lame";
            result.advice = "Very likely lame. Call your vet or hoof trimmer.";
        }

        // which leg steps short
        if (result.asymmetry > 0.15) {
            String[] names = {"left front", "right front", "left back", "right back"};
            int pair = 0;
            if (backGap > frontGap) {
                pair = 2;
            }
            int shortOne = pair;
            if (reach[pair + 1] < reach[pair]) {
                shortOne = pair + 1;
            }
            result.shortLeg = names[shortOne];
        }

        result.framesUsed = n;
        result.strides = n / stepLength;
        result.leftBack = hoof[2];
        result.rightBack = hoof[3];
        result.ok = true;
        return result;
    }
}
