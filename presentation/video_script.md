# SoilMetrics: Congressional App Challenge Video (2:53)

The Canva project "Congressional App Challenge - Video" has 14 pages. Each page's **Notes** has the same lines as this file.

## Script

| # | Time | Who | Say |
|---|------|-----|-----|
| 1 | 0:00–0:09 | Both on camera ([Name 1] speaks) | "Every farm runs on two things: healthy animals and healthy soil. But the warning signs stay hidden until it's too late." |
| 2 | 0:09–0:23 | [Name 2] voice-over | "Nearly one in four dairy cows is lame, and farmers miss most of them. And one percent more organic matter helps an acre hold twenty thousand more gallons of water, but soil tests take weeks." |
| 3 | 0:23–0:34 | Both on camera | [Name 1]: "Hi, I'm [Name 1]." [Name 2]: "And I'm [Name 2]. We built SoilMetrics, an Android app for farmers and ranchers that works even with no signal." |
| 4 | 0:34–0:50 | [Name 1] voice-over + recording | "First, livestock. A farmer films a cow walking past, and our pose AI finds seventeen body points in every frame, right on the phone. It scores lameness from one to five and saves it to a herd log." |
| 5 | 0:50–1:04 | [Name 2] voice-over | "YOLOX finds the cow, then RTMPose, trained on ten thousand animal photos, marks seventeen joints. Using body length as a ruler, we measure three signs of a limp: uneven steps, head bobbing, and broken rhythm." |
| 6 | 1:04–1:15 | [Name 1] voice-over + code | "Each sign gets a weight, and together they make a score from one to five, the same scale vets use. A sound cow scores near one." |
| 7 | 1:15–1:28 | [Name 2] voice-over + recording | "Next, satellites. SoilMetrics shows a real satellite photo of each field with NASA plant-health data, then combines soil moisture and organic matter to map where tractors will pack the soil." |
| 8 | 1:28–1:41 | [Name 1] voice-over + recording | "For organic carbon, snap a photo of your soil. Our neural network estimates organic matter in about eleven milliseconds with no internet, and converts it to tons of carbon per acre." |
| 9 | 1:41–1:55 | [Name 2] voice-over + code | "This is the core of our soil AI. A three-by-three filter slides across the photo, multiplies and adds up the pixels, and keeps the positive results. Our network uses eighty of these filters." |
| 10 | 1:55–2:03 | [Name 1] voice-over + recording | "Our optimizer tests every crop rotation through our soil model and ranks the best plans by profit and soil health." |
| 11 | 2:03–2:17 | Both on camera ([Name 2] speaks) | "Each year, crops and manure add organic matter while tillage and heat break it down. Repeating a crop cuts the gain by twenty percent, and lambda blends in the field's own measured trend." |
| 12 | 2:17–2:29 | [Name 1] voice-over + recording | "Finally, grants. SoilMetrics checks USDA, state, and carbon-credit programs and estimates the payout. For this field, that's about twenty thousand dollars a year." |
| 13 | 2:29–2:43 | [Name 2] voice-over | "We built it in Java and XML in Android Studio, trained the soil AI in Python with TensorFlow, run both AI models on the phone, and use Firebase for sign-in and SQLite for offline storage." |
| 14 | 2:43–2:53 | Both on camera | [Name 1]: "Next, we'll test SoilMetrics on local farms and check our gait scores against vets." [Name 2]: "Thanks for watching!" |

## Screen recordings (the tall box on the right of pages 4, 7, 8, 10 and 12)

- **Page 4, animal walk:**
  1. Tap **Check a Cow's Walk** (on the Animal Walk Check card).
  2. Tap **Pick Video**, then choose `cow_walk_side_1` (5 seconds).
  3. Green dots appear on the cow in each frame. Speed this part up when editing.
  4. When it finishes, the video replays with green dots on every joint.
  5. Scroll down to show:
     - the score, 1.0 "Sound"
     - step balance, head bob and rhythm
     - the Back Hoof Motion graph
  6. Type "Bessie 214" and tap **Save Check**.
- **Page 7, satellite:**
  1. Pick the field North.
  2. Tap the refresh button on Weather.
  3. Show the satellite photo and NDVI 0.78.
  4. Show the Compaction Risk Map and the advice under it.
- **Page 8, soil photo:**
  1. Tap **Scan Soil Now**, then **Pick Photo**, and choose the darkest soil photo.
  2. Show 6.3% High, about 37 tons of carbon per acre, and the time (about 11 ms).
  3. Tap **Save to Field**.
- **Page 10, crop optimizer:**
  1. Tap **Best Crop Plan**.
  2. Leave steps 1 to 3 as they are.
  3. In Find Your Best Plan, keep Corn, Soybeans, Wheat and Cover crop year picked.
  4. Tap **Find My Best Plan**.
  5. Show the top 3 plans. #1 is Soybeans → Cover Crop → Corn (+$16,027 vs. continuous corn).
  6. Scroll to the Plan #1 graph and the year cards.
- **Page 12, grants:**
  1. Tap **Grants & Carbon**, then **Check What I Qualify For**.
  2. Show about $20,422 per year from 7 programs.
  3. Scroll through the cards.

Drop each recording on top of its box, then delete the box text.

## Code screenshots (the dark blue boxes)

| Page | File | Lines | What it shows |
|------|------|-------|---------------|
| 4 | `GaitActivity.java` | 174–190 | Finds the cow, gets its 17 body points, shows the green dots |
| 6 | `GaitAnalyzer.java` | 147–186 | Step balance, rhythm, head bob, and the 1 to 5 score |
| 7 | `WeatherHelper.java` | 150–163 | Asks NASA MODIS for the field's plant health (NDVI) |
| 8 | `SoilModel.java` | 51–67 | Photo to 64 by 64 numbers, then runs the CNN |
| 9 | `equations/SoilEquations.java` | 29–48 | Equation 2 line for line: h = relu(b + sum of K times X) |
| 10 | `SoilSimulator.java` | 366–387 | Tries every crop order, checks the farming rules, runs each through the soil model |
| 11 | `equations/SoilEquations.java` | 15–27 | Equation 1 in plain Java (the optimizer runs it for every year of every plan) |
| 12 | `GrantChecker.java` | 160–171 | Carbon credit payout |

App code is in `app/src/main/java/com/example/soilmetrics/`, and the CNN training code is in `ml/train_model.py`. The `equations` folder is not part of the app; `java SoilEquations.java` prints 1.37%, 1.41%, 1.45% and 6.28%, the same answers as the app.

## Check before you submit

- **Stats:**
  - 22.8% of dairy cows are lame (Thomsen et al., *Veterinary Journal*, 2023).
  - Farmers guessed 9% when the real rate was 22% (*Journal of Dairy Science*, 2017).
  - 20,000 gallons per acre per 1% organic matter (USDA NRCS). Keep the links.
- **Placeholders:** fill in names, grade, school, state and district on pages 1, 3, 11 and 14.
- **Firebase:** phone sign-in and text-message 2-step need the Firebase Blaze plan (see README).
- **Length:** the whole video must stay under 3:00.

## Sources

- Congressional App Challenge rules: https://www.congressionalappchallenge.us
- Cow videos: Pexels (free license), https://www.pexels.com/video/a-cow-walking-in-a-picturesque-location-9734068/
- Pose model: RTMPose-m AP-10K (OpenMMLab), via https://github.com/Tau-J/rtmlib
