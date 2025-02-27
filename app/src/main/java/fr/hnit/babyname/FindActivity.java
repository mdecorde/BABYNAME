package fr.hnit.babyname;
/*
The babyname app is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public
License as published by the Free Software Foundation,
either version 2 of the License, or (at your option) any
later version.

The babyname app is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
PURPOSE. See the GNU General Public License for more
details.

You should have received a copy of the GNU General
Public License along with the TXM platform. If not, see
http://www.gnu.org/licenses
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

public class FindActivity extends AppCompatActivity
{
    public static final String PROJECT_EXTRA = "project_position";

    BabyNameProject project;
    BabyName currentBabyName;

    ImageView backgroundImage;
    Button nextButton;
    RatingBar rateBar;
    TextView nameText;
    TextView extraText;
    TextView remainingText;
    boolean goToNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(FindActivity.this);
        goToNext = sharedPref.getBoolean("pref_next_ontouch", false);

        backgroundImage = findViewById(R.id.imageView);
        if (Math.random() > 0.5d) {
            backgroundImage.setImageResource(R.drawable.tuxbaby);
        } else {
            backgroundImage.setImageResource(R.drawable.tuxbaby2);
        }
        nextButton = findViewById(R.id.next_button);
        rateBar = findViewById(R.id.rate_bar);
        nameText = findViewById(R.id.name_text);
        extraText = findViewById(R.id.extra_text);
        remainingText = findViewById(R.id.remaining_text);

        nextButton.setOnClickListener(view -> nextName());

        nextButton.setEnabled(!goToNext);

        rateBar.setOnTouchListener((view, motionEvent) -> {
            if (goToNext && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                nextName();
            }
            return false;
        });

        Intent intent = getIntent();

        if (intent != null) {
            int index = intent.getIntExtra(PROJECT_EXTRA, 0);
            if (index >= 0 && MainActivity.projects.size() > index) {
                BabyNameProject project = MainActivity.projects.get(index);
                if (project.nexts.isEmpty()) {
                    Toast.makeText(FindActivity.this, R.string.starting_new_loop, Toast.LENGTH_LONG).show();
                    project.rebuildNexts();
                    project.setNeedToBeSaved(true);
                }
                setProject(project);
            }
        }
    }

    private static ArrayList<String> genresToLocale(Context context, ArrayList<String> origins) {
        ArrayList<String> ret = new ArrayList<>();
        for (int i = 0; i < origins.size(); i +=1 ) {
            switch (origins.get(i)) {
                case BabyNameDatabase.GENDER_FEMALE:
                    ret.add(context.getString(R.string.girl));
                    break;
                case BabyNameDatabase.GENDER_MALE:
                    ret.add(context.getString(R.string.boy));
                    break;
                default:
                    ret.add(origins.get(i));
                    break;
            }
        }
        return ret;
    }

    // Make every origin begin with an upper case letter.
    // It would be nice to have proper localisation in the future.
    private static ArrayList<String> originsToLocale(Context context, ArrayList<String> origins) {
        ArrayList<String> ret = new ArrayList<>();
        for (int i = 0; i < origins.size(); i +=1 ) {
            String origin = origins.get(i);
            ret.add(origin.substring(0, 1).toUpperCase() + origin.substring(1));
        }
        return ret;
    }

    private void updateName() {
        if (currentBabyName == null) {
            AppLogger.info("No current baby name found: "+project);
            Toast.makeText(FindActivity.this, getString(R.string.all_names_reviewed), Toast.LENGTH_LONG).show();
            FindActivity.this.finish();
        } else {
            nameText.setText(currentBabyName.name);

            if (!project.genders.isEmpty() || project.origins.size() > 1) {
                Context context = getApplicationContext();
                ArrayList<String> genres = new ArrayList<>(currentBabyName.genres);
                ArrayList<String> origins = new ArrayList<>(currentBabyName.origins);
                Collections.sort(genres);
                Collections.sort(origins);
                String extra = "";
                if (!genres.isEmpty()) {
                    extra += genresToLocale(context, genres).toString();
                }
                extra += " ";
                if (!origins.isEmpty()) {
                    extra += originsToLocale(context, origins).toString();
                }
                extraText.setText(extra);
            } else {
                extraText.setText("");
            }

            remainingText.setText(String.format(getString(R.string.n_name_left), project.nexts.size()));
            rateBar.setRating(0);
        }
    }

    private void nextName() {
        if (project == null) {
            return;
        }

        saveRate();

        currentBabyName = project.nextName();
        updateName();
    }

    protected void saveRate() {
        int rate = (int)rateBar.getRating();
        int score = project.evaluate(currentBabyName, rate);
        if (rate > 0) {
            Toast.makeText(FindActivity.this, String.format(getString(R.string.name_rated_score), currentBabyName.name, score, rate), Toast.LENGTH_SHORT).show();
        }
        project.setNeedToBeSaved(true);
    }

    public void setProject(BabyNameProject project) {
        //AppLogger.info("Set project preferences: "+project);
        this.project = project;

        if (project.currentBabyNameIndex == -1) {
            currentBabyName = project.nextName();
        } else {
            currentBabyName = MainActivity.database.get(project.currentBabyNameIndex);
        }

        if (currentBabyName == null) {
            AppLogger.error("No current baby name found: "+project);
            return;
        }

        updateName();
    }

    public void onStop () {
        super.onStop();
        if (project != null) {
            project.setNeedToBeSaved(true);
        }
    }
}
