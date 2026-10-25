package com.example.soilmetrics;

// what the farmer typed
public class FarmPlan {
    public String fieldName = "this field";
    public double om = 3.0;
    public double acres = 100;
    public String tillage = "Conventional Till";
    public String climate = "Cool and Wet";
    public double nRate = 150;
    public boolean manure = false;
    public String pest = "Low";
    public String currentCrop = "Corn";

    // past trend from samples
    public boolean useTrend = false;
    public double trend = 0;

    // prices for each crop
    public double[] prices = {4.20, 10.50, 5.60, 3.50, 180, 0};

    // choices for optimizer
    public boolean[] useCrop = {true, true, true, false, false, true};
    public boolean planter = true;
    public boolean drill = true;
    public boolean combine = true;
    public boolean hayTools = false;
    public boolean oatsBuyer = false;
    public boolean hayBuyer = false;
    public int rotationYears = 3;
    public int soilWeight = 50;
}
