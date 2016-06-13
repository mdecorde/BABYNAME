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
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

public class FindActivity extends AppCompatActivity {

    public static final String PROJECT_EXTRA = "project_position";

    BabyNameProject project;
    BabyName currentBabyName;

    ImageView backgroundImage;
    Button nextButton;
    RatingBar rateBar;
    TextView nameText;
    TextView remainingText;
    boolean goToNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(FindActivity.this);
        goToNext = sharedPref.getBoolean("pref_next_ontouch", false);

        backgroundImage = (ImageView) findViewById(R.id.imageView);
        if (Math.random() > 0.5d) {
            backgroundImage.setImageResource(R.drawable.tuxbaby);
        } else {
            backgroundImage.setImageResource(R.drawable.tuxbaby2);
        }
        nextButton = (Button) findViewById(R.id.next_button);
        rateBar = (RatingBar) findViewById(R.id.rate_bar);
        nameText = (TextView) findViewById(R.id.name_text);
        remainingText = (TextView) findViewById(R.id.remaining_text);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               nextName();
            }
        });

        nextButton.setEnabled(!goToNext);

        rateBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (goToNext && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    nextName();
                }
                return false;
            }
        });

        Intent intent = getIntent();

        if (intent != null) {
            int index = intent.getIntExtra(PROJECT_EXTRA, 0);
            if (index >= 0 && MainActivity.projects.size() > index) {
                BabyNameProject project = MainActivity.projects.get(index);
                if (project.nexts.size() == 0) {
                    Toast.makeText(FindActivity.this, "Starting a new review loop!", Toast.LENGTH_LONG).show();
                    project.rebuildNexts();
                    project.setNeedToBeSaved(true);
                }
                setProject(project);
            }
        }
    }

    private void nextName() {
        if (project == null) return;
        saveRate();
        currentBabyName = project.nextName();
        if (currentBabyName == null) {
            AppLogger.error("No current baby name found: "+project);
            Toast.makeText(FindActivity.this, "All names have been reviewed !", Toast.LENGTH_LONG).show();
            FindActivity.this.finish();
        } else {
            String newName = currentBabyName.name;
            nameText.setText(newName);
            remainingText.setText(""+project.nexts.size()+" left.");
            rateBar.setRating(0);
        }
    }

    protected void saveRate() {
        int rate = (int)rateBar.getRating();
        int score = project.evaluate(currentBabyName, rate);
        Toast.makeText(FindActivity.this, currentBabyName.name+ " rated: "+score+ " (+"+rate+")", Toast.LENGTH_SHORT).show();
        project.setNeedToBeSaved(true);
    }

    public void tryGetNextBabyName() {

    }

    public void setProject(BabyNameProject project) {
        //AppLogger.info("Set project preferences: "+project);
        this.project = project;

        if (project.currentBabyNameIndex == -1)
            currentBabyName = project.nextName();
        else
            currentBabyName = MainActivity.database.get(project.currentBabyNameIndex);

        if (currentBabyName == null) {
            AppLogger.error("No current baby name found: "+project);
            return;
        }

        String newName = currentBabyName.name;
        nameText.setText(newName);
        remainingText.setText(""+project.nexts.size()+" left.");
        rateBar.setRating(0);
    }

    public void onStop () {
        super.onStop();
        if (project != null) project.setNeedToBeSaved(true);
    }
}
