package com.example.soilmetrics;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

// check how cow walks
public class GaitActivity extends AppCompatActivity {

    static final int RECORD_CODE = 1;
    static final int PICK_CODE = 2;
    // frames to look at
    static final int FRAMES = 48;
    // longest video part
    static final long MAX_TIME = 20000;
    // check box every
    static final int FIND_EVERY = 3;

    CowPose pose;
    DatabaseHelper db;
    GaitResult lastResult;

    // frames with green dots
    ArrayList<Bitmap> replay = new ArrayList<>();
    int replaySpot = 0;
    long replayDelay = 120;
    Handler replayTimer = new Handler(Looper.getMainLooper());

    ImageView frameView;
    LinearLayout videoHint;
    ProgressBar progressBar;
    TextView progressText;
    Button recordButton;
    Button pickButton;
    GaugeView scoreGauge;
    TextView adviceText;
    TextView balanceChip;
    TextView bobChip;
    TextView rhythmChip;
    MotionView motionView;
    TextView motionText;
    EditText animalBox;
    Button saveButton;
    TextView historyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gait);
        db = new DatabaseHelper(this);

        frameView = findViewById(R.id.frameView);
        videoHint = findViewById(R.id.videoHint);
        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        recordButton = findViewById(R.id.recordButton);
        pickButton = findViewById(R.id.pickButton);
        scoreGauge = findViewById(R.id.scoreGauge);
        adviceText = findViewById(R.id.adviceText);
        balanceChip = findViewById(R.id.balanceChip);
        bobChip = findViewById(R.id.bobChip);
        rhythmChip = findViewById(R.id.rhythmChip);
        motionView = findViewById(R.id.motionView);
        motionText = findViewById(R.id.motionText);
        animalBox = findViewById(R.id.animalBox);
        saveButton = findViewById(R.id.saveButton);
        historyText = findViewById(R.id.historyText);

        // score meter 1 to 5
        scoreGauge.setRange(1, 5, "");
        scoreGauge.setColors(Color.parseColor("#A2C1BB"), Color.parseColor("#E07A64"));
        scoreGauge.clear("No video yet");

        // load the pose AI
        try {
            pose = new CowPose(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not load the pose AI", Toast.LENGTH_LONG).show();
        }
        showHistory();
    }

    public void recordVideo(View view) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 20);
        try {
            startActivityForResult(intent, RECORD_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    public void pickVideo(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        checkVideo(data.getData());
    }

    // run AI on video
    private void checkVideo(final Uri video) {
        if (pose == null) {
            adviceText.setText("The pose AI did not load.");
            return;
        }
        replayTimer.removeCallbacks(replayStep);
        replay.clear();
        recordButton.setEnabled(false);
        pickButton.setEnabled(false);
        saveButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        progressText.setText("Opening video...");
        scoreGauge.clear("Checking...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                ArrayList<float[][]> allPoints = new ArrayList<>();
                String error = "";

                try {
                    MediaMetadataRetriever reader = new MediaMetadataRetriever();
                    reader.setDataSource(GaitActivity.this, video);
                    long length = Long.parseLong(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    long useTime = Math.min(length, MAX_TIME);
                    replayDelay = Math.max(40, useTime / FRAMES);

                    float[] box = null;
                    for (int i = 0; i < FRAMES; i++) {
                        // grab one frame
                        long time = useTime * i / FRAMES;
                        Bitmap frame = reader.getFrameAtTime(time * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
                        if (frame == null) {
                            allPoints.add(null);
                            continue;
                        }
                        frame = shrink(frame);

                        // find cow then points
                        if (box == null || i % FIND_EVERY == 0) {
                            float[] found = pose.findAnimal(frame);
                            if (found != null && found[4] > 0.25f) {
                                box = found;
                            }
                        }
                        float[][] points = null;
                        if (box != null) {
                            points = pose.findPoints(frame, box);
                            box = CowPose.boxFromPoints(points);
                        }
                        allPoints.add(points);

                        // show green dots now
                        final Bitmap shown = drawSkeleton(frame, points);
                        replay.add(shown);
                        final int done = i + 1;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                frameView.setImageBitmap(shown);
                                frameView.setVisibility(View.VISIBLE);
                                videoHint.setVisibility(View.GONE);
                                progressBar.setProgress(done * 100 / FRAMES);
                                progressText.setText("Finding body points in frame " + done + " of " + FRAMES + "...");
                            }
                        });
                    }
                    reader.release();
                } catch (Exception e) {
                    error = "Could not read that video.";
                }

                final GaitResult result = GaitAnalyzer.analyze(allPoints);
                final long seconds = (System.currentTimeMillis() - start) / 1000;
                final String finalError = error;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showResult(result, seconds, finalError);
                    }
                });
            }
        }).start();
    }

    // make frame smaller
    private Bitmap shrink(Bitmap frame) {
        int width = 640;
        if (frame.getWidth() <= width) {
            return frame;
        }
        int height = Math.round(frame.getHeight() * width / (float) frame.getWidth());
        return Bitmap.createScaledBitmap(frame, width, height, true);
    }

    // green dots on joints
    private Bitmap drawSkeleton(Bitmap frame, float[][] points) {
        Bitmap copy = frame.copy(Bitmap.Config.RGB_565, true);
        if (points == null) {
            return copy;
        }
        Canvas canvas = new Canvas(copy);
        float size = copy.getWidth() / 160f;

        // thin lines between joints
        Paint bone = new Paint();
        bone.setColor(Color.argb(170, 255, 255, 255));
        bone.setStrokeWidth(size * 0.7f);
        bone.setAntiAlias(true);
        for (int i = 0; i < CowPose.BONES.length; i++) {
            float[] a = points[CowPose.BONES[i][0]];
            float[] b = points[CowPose.BONES[i][1]];
            if (a[2] > 0.3f && b[2] > 0.3f) {
                canvas.drawLine(a[0], a[1], b[0], b[1], bone);
            }
        }

        // green dot each joint
        Paint dot = new Paint();
        dot.setAntiAlias(true);
        for (int k = 0; k < CowPose.POINTS; k++) {
            if (points[k][2] < 0.3f) {
                continue;
            }
            dot.setColor(Color.WHITE);
            canvas.drawCircle(points[k][0], points[k][1], size * 1.9f, dot);
            dot.setColor(Color.parseColor("#22C55E"));
            canvas.drawCircle(points[k][0], points[k][1], size * 1.4f, dot);
        }
        return copy;
    }

    // play dot video loop
    Runnable replayStep = new Runnable() {
        @Override
        public void run() {
            if (replay.size() == 0) {
                return;
            }
            frameView.setImageBitmap(replay.get(replaySpot));
            replaySpot = (replaySpot + 1) % replay.size();
            replayTimer.postDelayed(this, replayDelay);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        replayTimer.removeCallbacks(replayStep);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (replay.size() > 0 && recordButton.isEnabled()) {
            replayTimer.removeCallbacks(replayStep);
            replayTimer.post(replayStep);
        }
    }

    // show the answer
    private void showResult(GaitResult result, long seconds, String error) {
        recordButton.setEnabled(true);
        pickButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);

        // replay video with dots
        if (replay.size() > 0) {
            replaySpot = 0;
            replayTimer.post(replayStep);
        }

        if (!error.equals("") || !result.ok) {
            String words = error;
            if (words.equals("")) {
                words = result.problem;
            }
            progressText.setText(words);
            adviceText.setText(words);
            scoreGauge.clear("No score");
            return;
        }

        lastResult = result;
        progressText.setText("Checked " + result.framesUsed + " frames and about " + Math.round(result.strides)
                + " strides in " + seconds + " seconds, all on your phone. Green dots are the joints the AI tracks.");
        scoreGauge.setValue(result.score, result.words);

        String advice = result.advice;
        if (!result.shortLeg.equals("") && result.score >= 2) {
            advice = advice + "\nShortest steps: " + result.shortLeg + " leg.";
        }
        adviceText.setText(advice);
        balanceChip.setText(Math.round(100 * (1 - result.asymmetry)) + "%\nstep balance");
        bobChip.setText(String.format("%.1f", 100 * result.headBob) + "%\nhead bob");
        rhythmChip.setText(Math.round(100 * Math.max(result.rhythm, 0)) + "%\nstep rhythm");
        motionView.setData(result.leftBack, result.rightBack);
        saveButton.setEnabled(true);
    }

    public void saveCheck(View view) {
        if (lastResult == null) {
            return;
        }
        String animal = animalBox.getText().toString().trim();
        if (animal.equals("")) {
            animal = "Cow";
        }
        db.addGaitCheck(animal, System.currentTimeMillis(), lastResult.score, lastResult.words);
        Toast.makeText(this, "Saved " + animal, Toast.LENGTH_SHORT).show();
        animalBox.setText("");
        showHistory();
    }

    // list recent checks
    private void showHistory() {
        ArrayList<GaitCheck> checks = db.getGaitChecks();
        if (checks.size() == 0) {
            historyText.setText("No cows checked yet.");
            return;
        }
        SimpleDateFormat niceDate = new SimpleDateFormat("MMM d", Locale.US);
        String text = "";
        for (int i = 0; i < checks.size(); i++) {
            GaitCheck g = checks.get(i);
            if (i > 0) {
                text = text + "\n";
            }
            text = text + g.animal + "   " + String.format("%.1f", g.score) + " of 5   " + g.note
                    + "   " + niceDate.format(new Date(g.date));
        }
        historyText.setText(text);
    }

    // top back arrow
    public void goBack(View view) {
        finish();
    }
}
