package com.example.soilmetrics;

import java.util.ArrayList;

// soil and rotation math
public class SoilSimulator {

    // numbers for crops
    public static final int CORN = 0;
    public static final int SOYBEANS = 1;
    public static final int WHEAT = 2;
    public static final int OATS = 3;
    public static final int ALFALFA = 4;
    public static final int COVER = 5;

    public static String[] crops = {"Corn", "Soybeans", "Wheat", "Oats", "Alfalfa", "Cover Crop"};
    // organic matter added yearly
    public static double[] omGain = {0.07, 0.035, 0.06, 0.055, 0.11, 0.15};
    // normal yield per acre
    public static double[] yield = {180, 55, 70, 80, 3.8, 0};
    // cost per acre
    public static double[] cost = {440, 340, 250, 190, 450, 45};
    // nitrogen crop needs
    public static double[] nNeed = {150, 0, 90, 60, 0, 0};
    // nitrogen left behind
    public static double[] nCredit = {0, 30, 0, 0, 100, 40};
    // compaction from crop
    public static double[] rootEffect = {8, 2, 0, 0, -12, -10};

    // crop name to number
    public static int cropNumber(String name) {
        for (int i = 0; i < crops.length; i++) {
            if (crops[i].equals(name)) {
                return i;
            }
        }
        return CORN;
    }

    // keep number in range
    public static double limit(double value, double low, double high) {
        if (value < low) {
            return low;
        }
        if (value > high) {
            return high;
        }
        return value;
    }

    // tillage breaks down soil
    public static double tillFactor(String tillage) {
        if (tillage.equals("No-Till")) {
            return 0.75;
        } else if (tillage.equals("Reduced Till")) {
            return 0.9;
        }
        return 1.2;
    }

    // warm weather breaks down
    public static double climateFactor(String climate) {
        if (climate.equals("Cool and Wet")) {
            return 0.85;
        } else if (climate.equals("Cool and Dry")) {
            return 0.8;
        } else if (climate.equals("Warm and Wet")) {
            return 1.15;
        }
        return 1.05;
    }

    // compaction from tillage
    public static double tillCompaction(String tillage) {
        if (tillage.equals("No-Till")) {
            return 30;
        } else if (tillage.equals("Reduced Till")) {
            return 38;
        }
        return 50;
    }

    // wet places pack more
    public static double wetExtra(String climate) {
        if (climate.contains("Wet")) {
            return 10;
        }
        return 0;
    }

    // compaction number to words
    public static String compactionWords(double risk) {
        if (risk < 35) {
            return "Low";
        } else if (risk < 60) {
            return "Medium";
        }
        return "High";
    }

    // run the years
    public static ArrayList<YearResult> simulate(FarmPlan plan, int[] cropList) {
        ArrayList<YearResult> results = new ArrayList<>();
        double om = plan.om;
        int lastCrop = cropNumber(plan.currentCrop);
        double compaction = limit(tillCompaction(plan.tillage) + wetExtra(plan.climate) - (om - 3) * 8, 0, 100);

        for (int i = 0; i < cropList.length; i++) {
            int crop = cropList[i];

            // nitrogen from soil
            double nitrogen = om * 20 + nCredit[lastCrop];
            if (plan.manure) {
                nitrogen = nitrogen + 40;
            }
            double fertilizer = Math.min(plan.nRate, nNeed[crop]);

            // figure crop yield
            double y = yield[crop] * limit(1 + 0.04 * (om - 3), 0.85, 1.15);
            boolean sameAgain = crop == lastCrop && crop != ALFALFA;
            if (sameAgain) {
                y = y * 0.88;
            }
            if (crop == CORN && (lastCrop == SOYBEANS || lastCrop == ALFALFA || lastCrop == COVER)) {
                y = y * 1.08;
            }
            if (plan.pest.equals("Medium")) {
                y = y * 0.95;
                if (sameAgain) {
                    y = y * 0.95;
                }
            }
            if (plan.pest.equals("High")) {
                y = y * 0.88;
                if (sameAgain) {
                    y = y * 0.9;
                }
            }
            if (nNeed[crop] > 0) {
                double have = nitrogen + fertilizer;
                if (have < nNeed[crop]) {
                    y = y * (0.6 + 0.4 * have / nNeed[crop]);
                }
            }

            // money per acre
            double profit = y * plan.prices[crop] - cost[crop] - fertilizer * 0.55;
            if (plan.manure) {
                profit = profit - 30;
            }
            if (crop != COVER) {
                if (plan.pest.equals("Medium")) {
                    profit = profit - 10;
                }
                if (plan.pest.equals("High")) {
                    profit = profit - 25;
                }
            }

            // organic matter change
            double decay = 0.02 * tillFactor(plan.tillage) * climateFactor(plan.climate);
            double gain = omGain[crop];
            // same crop hurts soil
            if (sameAgain) {
                gain = gain * 0.8;
            }
            if (plan.manure) {
                gain = gain + 0.05;
            }
            double change = gain - om * decay;
            if (plan.useTrend) {
                change = change * 0.7 + plan.trend * 0.3;
            }
            om = om + change;
            if (om < 0.1) {
                om = 0.1;
            }

            // compaction this year
            double score = tillCompaction(plan.tillage) + wetExtra(plan.climate) + rootEffect[crop] - (om - 3) * 8;
            if (sameAgain) {
                score = score + 5;
            }
            compaction = limit(compaction * 0.5 + score * 0.5, 0, 100);

            YearResult r = new YearResult();
            r.crop = crop;
            r.om = om;
            r.nitrogen = nitrogen;
            r.compaction = compaction;
            r.profit = profit;
            results.add(r);
            lastCrop = crop;
        }
        return results;
    }

    // dollars with commas
    public static String money(double amount) {
        if (amount < 0) {
            return "-$" + String.format("%,.0f", -amount);
        }
        return "$" + String.format("%,.0f", amount);
    }

    // dollars with plus sign
    public static String moneyChange(double amount) {
        if (amount < 0) {
            return "-$" + String.format("%,.0f", -amount);
        }
        return "+$" + String.format("%,.0f", amount);
    }

    // why crop cant work
    public static String cropProblem(FarmPlan plan, int crop) {
        if (crop == CORN && !plan.planter) {
            return "needs a row crop planter";
        }
        if (crop == SOYBEANS && !plan.planter && !plan.drill) {
            return "needs a planter or grain drill";
        }
        if ((crop == WHEAT || crop == OATS || crop == ALFALFA) && !plan.drill) {
            return "needs a grain drill";
        }
        if (crop != ALFALFA && crop != COVER && !plan.combine) {
            return "needs a combine";
        }
        if (crop == ALFALFA && !plan.hayTools) {
            return "needs hay equipment";
        }
        if (crop == OATS && !plan.oatsBuyer) {
            return "no oats buyer nearby";
        }
        if (crop == ALFALFA && !plan.hayBuyer) {
            return "no hay buyer nearby";
        }
        return "";
    }

    // can crop follow crop
    public static boolean okAfter(int before, int now) {
        // no same crop twice
        if (before == now && now != ALFALFA) {
            return false;
        }
        // no wheat after corn
        if (before == CORN && now == WHEAT) {
            return false;
        }
        return true;
    }

    // check farming rules
    public static boolean followsRules(FarmPlan plan, int[] list) {
        int years = list.length;

        // need two different crops
        boolean allSame = true;
        for (int i = 0; i < years; i++) {
            if (list[i] != list[0]) {
                allSame = false;
            }
        }
        if (allSame) {
            return false;
        }

        // need one cash crop
        boolean hasCash = false;
        for (int i = 0; i < years; i++) {
            if (list[i] != COVER) {
                hasCash = true;
            }
        }
        if (!hasCash) {
            return false;
        }

        // check each year pair
        int before = cropNumber(plan.currentCrop);
        for (int i = 0; i < years; i++) {
            if (!okAfter(before, list[i])) {
                return false;
            }
            before = list[i];
        }

        // rotation starts over again
        if (!okAfter(list[years - 1], list[0])) {
            return false;
        }

        // alfalfa max 3 years
        for (int i = 0; i < years; i++) {
            if (list[i] == ALFALFA && list[(i + 1) % years] == ALFALFA
                    && list[(i + 2) % years] == ALFALFA && list[(i + 3) % years] == ALFALFA) {
                return false;
            }
        }
        return true;
    }

    // same rotation different start
    public static boolean sameLoop(int[] a, int[] b) {
        int years = a.length;
        for (int shift = 0; shift < years; shift++) {
            boolean match = true;
            for (int i = 0; i < years; i++) {
                if (a[(i + shift) % years] != b[i]) {
                    match = false;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    // crop list to words
    public static String rotationName(int[] list) {
        String text = "";
        for (int i = 0; i < list.length; i++) {
            if (i > 0) {
                text = text + " → ";
            }
            text = text + crops[list[i]];
        }
        return text;
    }

    // winner of last search
    public static int[] bestRotation = null;

    // find the best rotations
    public static ArrayList<String> findBestRotations(FarmPlan plan) {
        ArrayList<String> lines = new ArrayList<>();
        ArrayList<String> notes = new ArrayList<>();

        // crops we can use
        ArrayList<Integer> allowed = new ArrayList<>();
        for (int i = 0; i < crops.length; i++) {
            if (plan.useCrop[i]) {
                String problem = cropProblem(plan, i);
                if (problem.equals("")) {
                    allowed.add(i);
                } else {
                    notes.add("Left out " + crops[i] + ": " + problem + ".");
                }
            }
        }

        // try every crop order
        int years = plan.rotationYears;
        int n = allowed.size();
        int total = 1;
        for (int i = 0; i < years; i++) {
            total = total * n;
        }

        ArrayList<int[]> rotations = new ArrayList<>();
        ArrayList<Double> omChanges = new ArrayList<>();
        ArrayList<Double> profits = new ArrayList<>();
        ArrayList<Double> endCompaction = new ArrayList<>();

        for (int number = 0; number < total; number++) {
            // number to crop list
            int[] list = new int[years];
            int temp = number;
            for (int y = 0; y < years; y++) {
                list[y] = allowed.get(temp % n);
                temp = temp / n;
            }
            if (!followsRules(plan, list)) {
                continue;
            }

            ArrayList<YearResult> results = simulate(plan, list);
            double money = 0;
            for (int y = 0; y < years; y++) {
                money = money + results.get(y).profit;
            }
            rotations.add(list);
            omChanges.add(results.get(years - 1).om - plan.om);
            profits.add(money * plan.acres);
            endCompaction.add(results.get(years - 1).compaction);
        }

        if (rotations.size() == 0) {
            lines.add("No rotation fits your rules. Try picking more crops or equipment.");
            lines.addAll(notes);
            return lines;
        }

        // continuous corn to compare
        int[] cornList = new int[years];
        ArrayList<YearResult> cornResults = simulate(plan, cornList);
        double cornMoney = 0;
        for (int y = 0; y < years; y++) {
            cornMoney = cornMoney + cornResults.get(y).profit;
        }
        cornMoney = cornMoney * plan.acres;
        double cornOm = cornResults.get(years - 1).om - plan.om;

        // find min and max
        double minOm = omChanges.get(0);
        double maxOm = omChanges.get(0);
        double minMoney = profits.get(0);
        double maxMoney = profits.get(0);
        for (int i = 0; i < rotations.size(); i++) {
            minOm = Math.min(minOm, omChanges.get(i));
            maxOm = Math.max(maxOm, omChanges.get(i));
            minMoney = Math.min(minMoney, profits.get(i));
            maxMoney = Math.max(maxMoney, profits.get(i));
        }

        // tiny gaps dont count
        double omRange = Math.max(maxOm - minOm, 0.1);
        double moneyRange = Math.max(maxMoney - minMoney, 1000);

        // score soil and money
        double weight = plan.soilWeight / 100.0;
        double[] scores = new double[rotations.size()];
        for (int i = 0; i < rotations.size(); i++) {
            double soilPart = (omChanges.get(i) - minOm) / omRange;
            double moneyPart = (profits.get(i) - minMoney) / moneyRange;
            scores[i] = weight * soilPart + (1 - weight) * moneyPart;
        }

        // pick top three
        bestRotation = null;
        boolean[] used = new boolean[rotations.size()];
        ArrayList<int[]> picked = new ArrayList<>();
        int place = 1;
        while (place <= 3) {
            int best = -1;
            for (int i = 0; i < rotations.size(); i++) {
                if (!used[i] && (best == -1 || scores[i] > scores[best])) {
                    best = i;
                }
            }
            if (best == -1) {
                break;
            }
            used[best] = true;

            // skip same loop shifted
            boolean repeat = false;
            for (int i = 0; i < picked.size(); i++) {
                if (sameLoop(picked.get(i), rotations.get(best))) {
                    repeat = true;
                }
            }
            if (repeat) {
                continue;
            }
            picked.add(rotations.get(best));
            if (place == 1) {
                bestRotation = rotations.get(best);
            }

            String name = rotationName(rotations.get(best));
            double change = omChanges.get(best);
            String omWords;
            if (change >= 0) {
                omWords = "rises " + String.format("%.2f", change) + "%";
            } else {
                omWords = "drops " + String.format("%.2f", -change) + "%";
            }
            String text = "#" + place + ": " + name + "\n";
            text = text + "If you rotate " + name + " over " + years + " years on " + plan.fieldName
                    + ", your organic matter " + omWords + " and you net "
                    + moneyChange(profits.get(best) - cornMoney) + " vs. continuous corn.\n";
            text = text + "Total profit before land rent: " + money(profits.get(best))
                    + ". Compaction risk at end: " + Math.round(endCompaction.get(best))
                    + " (" + compactionWords(endCompaction.get(best)) + ").";
            lines.add(text);
            place++;
        }

        String cornText = "Continuous corn would change organic matter by " + String.format("%+.2f", cornOm)
                + "% and make " + money(cornMoney) + ".";
        lines.add(cornText);
        lines.add("Checked " + total + " crop orders. " + rotations.size() + " followed the farming rules.");
        lines.addAll(notes);
        return lines;
    }

    // yearly organic matter change
    public static double trendPerYear(ArrayList<Sample> samples) {
        int n = samples.size();
        if (n < 2) {
            return 0;
        }
        long first = samples.get(0).date;
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;
        for (int i = 0; i < n; i++) {
            // years since first test
            double x = (samples.get(i).date - first) / (1000.0 * 60 * 60 * 24 * 365);
            double y = samples.get(i).om;
            sumX = sumX + x;
            sumY = sumY + y;
            sumXY = sumXY + x * y;
            sumXX = sumXX + x * x;
        }
        double bottom = n * sumXX - sumX * sumX;
        if (bottom == 0) {
            return 0;
        }
        return (n * sumXY - sumX * sumY) / bottom;
    }

    // days between first last
    public static double daysCovered(ArrayList<Sample> samples) {
        if (samples.size() < 2) {
            return 0;
        }
        long time = samples.get(samples.size() - 1).date - samples.get(0).date;
        return time / (1000.0 * 60 * 60 * 24);
    }

    // risk for today
    public static double compactionToday(double om, String tillage, double moisture, double rain) {
        double risk = 20;
        if (moisture >= 0.35) {
            risk = risk + 35;
        } else if (moisture >= 0.32) {
            risk = risk + 20;
        } else if (moisture >= 0.25) {
            risk = risk + 10;
        }
        if (rain > 1) {
            risk = risk + 10;
        }
        if (tillage.equals("Conventional Till")) {
            risk = risk + 15;
        } else if (tillage.equals("Reduced Till")) {
            risk = risk + 8;
        }
        risk = risk - (om - 3) * 6;
        return limit(risk, 0, 100);
    }
}
