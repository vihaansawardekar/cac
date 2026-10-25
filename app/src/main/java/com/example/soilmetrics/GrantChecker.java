package com.example.soilmetrics;

import java.util.ArrayList;

// checks grants and carbon
public class GrantChecker {

    // state cover crop programs
    public static String[] stateNames = {"Iowa", "Illinois", "Wisconsin", "Maryland", "California"};
    public static String[] statePrograms = {
            "Iowa Cover Crop Insurance Discount",
            "Illinois Fall Covers for Spring Savings",
            "Wisconsin Cover Crop Insurance Rebate",
            "Maryland Cover Crop Program",
            "California Healthy Soils Program"
    };
    public static double[] stateRates = {5, 5, 5, 45, 60};
    public static String[] stateNotes = {
            "Crop insurance discount for acres with cover crops.",
            "Crop insurance discount for acres with cover crops.",
            "Crop insurance rebate for acres with cover crops.",
            "Pays per acre of fall cover crops planted.",
            "Grants for cover crops, compost and less tillage."
    };

    // check every program
    public static ArrayList<Program> check(double om, double acres, String state, String tillage,
                                           boolean cover, boolean nutrientPlan, boolean willingNew, boolean beginner) {
        ArrayList<Program> list = new ArrayList<>();
        boolean noTill = tillage.equals("No-Till");

        // USDA EQIP program
        Program eqip = new Program();
        eqip.name = "USDA EQIP (Environmental Quality Incentives Program)";
        eqip.kind = "USDA";
        double perAcre = 0;
        String pays = "";
        if (!cover) {
            perAcre = perAcre + 55;
            pays = pays + "cover crops ($55/acre), ";
        }
        if (!noTill) {
            perAcre = perAcre + 20;
            pays = pays + "no-till ($20/acre), ";
        }
        if (!nutrientPlan) {
            perAcre = perAcre + 10;
            pays = pays + "nutrient plan ($10/acre), ";
        }
        if (perAcre > 0 && willingNew) {
            eqip.eligible = true;
            if (beginner) {
                perAcre = perAcre * 1.2;
            }
            eqip.payout = perAcre * acres;
            // cut the last comma
            pays = pays.substring(0, pays.length() - 2);
            eqip.reason = "For about 3 years it pays for new " + pays + ".";
            if (om < 3) {
                eqip.reason = eqip.reason + " Your low organic matter makes this field a higher priority.";
            }
            if (beginner) {
                eqip.reason = eqip.reason + " Beginning farmers get higher rates.";
            }
        } else if (perAcre == 0) {
            eqip.reason = "You already use all the practices EQIP pays for. Look at CSP instead.";
        } else {
            eqip.reason = "EQIP pays for new practices. Check the box if you are willing to start one.";
        }
        list.add(eqip);

        // USDA CSP program
        Program csp = new Program();
        csp.name = "USDA CSP (Conservation Stewardship Program)";
        csp.kind = "USDA";
        if ((cover || noTill) && willingNew) {
            csp.eligible = true;
            csp.payout = Math.max(4000, acres * 18);
            csp.reason = "You already protect your soil. CSP pays you to keep it up and add one more practice. 5 year contract, at least $4,000 per year.";
        } else if (!cover && !noTill) {
            csp.reason = "Needs no-till or cover crops already in place.";
        } else {
            csp.reason = "Needs you to add one new practice.";
        }
        list.add(csp);

        // USDA CRP program
        Program crp = new Program();
        crp.name = "USDA CRP (Conservation Reserve Program)";
        crp.kind = "USDA";
        crp.countInTotal = false;
        if (om < 1.5) {
            crp.eligible = true;
            crp.payout = acres * 100;
            crp.reason = "Your soil is very worn out. CRP pays rent (about $100/acre) to plant grass for 10 to 15 years instead of crops. Not added to the total.";
        } else {
            crp.reason = "Only for very worn out ground (under 1.5% organic matter).";
        }
        list.add(crp);

        // free NRCS help
        Program help = new Program();
        help.name = "USDA NRCS Free Soil Health Plan";
        help.kind = "USDA";
        help.eligible = true;
        help.payout = 0;
        help.reason = "Every farmer can get a free conservation plan from the local NRCS office.";
        list.add(help);

        // check state programs
        boolean foundState = false;
        for (int i = 0; i < stateNames.length; i++) {
            if (stateNames[i].equals(state)) {
                foundState = true;
                Program sp = new Program();
                sp.name = statePrograms[i];
                sp.kind = "State";
                if (cover || willingNew) {
                    sp.eligible = true;
                    sp.payout = stateRates[i] * acres;
                    sp.reason = stateNotes[i] + " About $" + Math.round(stateRates[i]) + " per acre.";
                } else {
                    sp.reason = "Needs cover crops. Check the box if you are willing to start them.";
                }
                list.add(sp);
            }
        }
        if (!foundState) {
            Program none = new Program();
            none.name = "State programs for " + state;
            none.kind = "State";
            none.reason = "None in our list yet. Ask your county Soil and Water Conservation District.";
            list.add(none);
        }

        // carbon tons per acre
        double tons = 0;
        if (willingNew) {
            if (!cover) {
                tons = tons + 0.35;
            }
            if (tillage.equals("Conventional Till")) {
                tons = tons + 0.25;
            } else if (tillage.equals("Reduced Till")) {
                tons = tons + 0.15;
            }
            if (!nutrientPlan) {
                tons = tons + 0.05;
            }
        }
        // low om stores more
        if (om < 2) {
            tons = tons * 1.2;
        } else if (om > 5) {
            tons = tons * 0.7;
        }
        String tonsText = String.format("%.2f", tons) + " tons of CO2 per acre per year";
        String noTons = "Carbon programs only pay for NEW practices like cover crops or less tillage.";

        // Indigo Ag carbon
        Program indigo = new Program();
        indigo.name = "Indigo Ag (Carbon by Indigo)";
        indigo.kind = "Carbon";
        if (tons > 0) {
            indigo.eligible = true;
            indigo.payout = tons * acres * 60 * 0.8;
            indigo.reason = "About " + tonsText + " at about $60 per credit, minus 20% held back. Paid over 5 years.";
        } else {
            indigo.reason = noTons;
        }
        list.add(indigo);

        // Truterra carbon program
        Program truterra = new Program();
        truterra.name = "Truterra (Land O'Lakes)";
        truterra.kind = "Carbon";
        if (tons > 0) {
            truterra.eligible = true;
            truterra.payout = tons * acres * 20;
            truterra.reason = "Pays for carbon actually stored. About " + tonsText + " at about $20 per ton.";
        } else {
            truterra.reason = noTons;
        }
        list.add(truterra);

        // Agoro carbon program
        Program agoro = new Program();
        agoro.name = "Agoro Carbon Alliance";
        agoro.kind = "Carbon";
        if (tons > 0 && acres >= 500) {
            agoro.eligible = true;
            agoro.payout = tons * acres * 20;
            agoro.reason = "About " + tonsText + " at about $20 per ton.";
        } else if (tons > 0) {
            agoro.reason = "Needs at least 500 acres.";
        } else {
            agoro.reason = noTons;
        }
        list.add(agoro);

        // Nori is closed
        Program nori = new Program();
        nori.name = "Nori";
        nori.kind = "Carbon";
        nori.reason = "Nori shut down in September 2024 and does not pay farmers anymore.";
        list.add(nori);

        return list;
    }
}
