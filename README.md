# SoilMetrics

An Android app that helps farmers check their soil and their cows.

## What it does

- **Animal walk check (lameness screener)** - record or pick a video of a cow (or horse) walking from the side. A pose AI on the phone finds 17 body points in each frame, then the app measures step balance, head bob and step rhythm and gives a lameness score from 1 (sound) to 5 (very lame). The result replays the video with green dots on every joint the AI tracks. Checks can be saved to a herd log.
- **Soil photo AI** - take or pick a photo of soil and a small CNN on the phone guesses the organic matter %. Works with no internet.
- **Dashboard** - organic matter graph over time, season by season changes, and if the field is improving or degrading.
- **Weather** - current weather, rain, and topsoil moisture from Open-Meteo (free, no key).
- **Satellite** - a satellite photo of the field (Esri World Imagery, free, no key) and plant health (NDVI) from NASA MODIS (ORNL DAAC web service, free, no key).
- **Compaction risk zones** - a field map colored by compaction risk using soil moisture, rain, tillage and organic matter.
- **Grants and carbon credits** - checks USDA EQIP, CSP, CRP, some state programs, and carbon programs (Indigo Ag, Truterra, Agoro, Nori) with rough payouts.
- **Best crop plan (rotation optimizer)** - enter your soil, prices, tillage, the crops you would grow, your equipment and nearby buyers. The app tries every crop order for a 3, 4 or 5 year rotation, skips orders that break farming rules, runs each one through the soil model year by year, and ranks the top 3 by your mix of profit and soil health. It compares each with continuous corn and shows plan #1 year by year.
- **Offline** - the soil AI, the animal walk check and the crop optimizer run fully on the phone. Saved weather, NDVI and satellite photos show when there is no signal.

## Sign in (Firebase)

The app uses Firebase project **SoilMetrics** (`soilmetrics-d24b4`). Settings are in `app/google-services.json`.

- **Email and password** - new accounts must tap a link in an email before they can get in. "Forgot password?" emails a reset link.
- **Google** - "Continue with Google" button.
- **Phone number** - sign in with a code sent by text message.
- **Guest** - try the app without an account.
- **2-step verification** - in the Account page, turn on 2-step. After that, signing in needs your password AND a code texted to your phone.

### Steps to finish in the Firebase console

Text messages cost money, so Firebase only sends them on the pay-as-you-go **Blaze** plan. Phone sign-in and 2-step need these steps:

1. Upgrade the project to the Blaze plan (Firebase console, bottom left, "Upgrade"). The first 10 texts a day are free, then about $0.01 per text in the US.
2. Authentication, Sign-in method, Add new provider, **Phone**, Enable, Save.
3. Authentication, Sign-in method, Advanced, **Upgrade to enable** (Identity Platform). Then turn on **SMS Multi-factor Authentication**.
4. Optional: under Phone, add "Phone numbers for testing" so you can test without real texts.

Email/password, Google and Guest sign-in already work with no extra steps.

### Testing sign in without real accounts

Set `USE_EMULATOR = true` in `AuthHelper.java`, then run the Firebase Auth emulator on your computer:

```
firebase emulators:start --only auth --project soilmetrics-d24b4
```

Emails and text codes show up in the emulator instead of being sent. Set it back to `false` before sharing the app.

## Run the app

1. Open this folder in Android Studio.
2. Pick a phone or emulator and press Run.

## Demo cow videos

`cow_walk_side_1.mp4`, `cow_walk_side_2.mp4` and `cow_walk_side_3.mp4` are in the emulator's Movies folder. Open Animal Walk Check, tap Pick Video, and choose one. They are free Pexels videos (Pexels license), sped up to real walking speed.

## The equations

`equations/SoilEquations.java` is NOT part of the app. It shows the two equations from our video as plain Java so we can explain them:

1. Soil organic matter next year (the same math the optimizer runs for every year of every plan).
2. One convolution filter of the soil photo CNN: h = relu(b + sum of K times X). The file runs the whole CNN with it (16 + 32 + 32 filters) using the real trained weights in `equations/soil_model_weights.txt`.

Run it with `java SoilEquations.java` from the `equations` folder. It prints 1.37%, 1.41%, 1.45% (same as the app's soil model for 3 years of no-till corn) and 6.28% for the 6.5% test photo (the app shows 6.3%).

## Retrain the soil AI

```
pip install tensorflow pillow
python ml/train_model.py
```

This saves a new `app/src/main/assets/soil_model.tflite`.

To train on real lab results, put photos in `ml/real_photos/` and make `ml/real_photos/labels.csv` with one line per photo:

```
photo1.jpg,3.2
photo2.jpg,4.1
```

## Things to know

- **The AI was trained on computer-made practice soil photos, not real lab samples.** The practice photos use the known link between darker soil color and more organic matter. Its average error is about 0.36% on practice photos. It will not be that good on real fields until it is retrained with real photos that have lab test results.
- The crop optimizer uses simple yearly soil and profit math (not a trained model) mixed with a straight-line trend from the field's own past tests. It checks every crop order, so it always finds the best plan for its own math.
- The animal walk check is a screener, not a diagnosis. The pose AI is RTMPose-m trained on the AP-10K animal dataset (OpenMMLab, Apache 2.0) and the animal finder is YOLOX-Nano (Megvii, Apache 2.0). The lameness score rules were set by us, tested on sound cows, and have not been checked against vet scores yet.
- All grant, carbon and profit numbers are rough estimates. Profit does not include land rent. Check with the USDA office or the company before signing up.
- Nori shut down in September 2024, so the app shows it as closed.
