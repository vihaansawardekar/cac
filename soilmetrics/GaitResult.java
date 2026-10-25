package com.example.soilmetrics;

// one cow walk check
public class GaitResult {
    public boolean ok = false;
    public String problem = "";
    public int framesUsed = 0;
    public double strides = 0;

    // the three gait signs
    public double asymmetry = 0;
    public double headBob = 0;
    public double rhythm = 0;

    // final lameness score
    public double score = 1;
    public String words = "";
    public String advice = "";
    public String shortLeg = "";

    // back hoof paths
    public double[] leftBack = new double[0];
    public double[] rightBack = new double[0];
}
