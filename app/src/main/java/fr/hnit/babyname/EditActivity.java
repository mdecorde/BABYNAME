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
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class EditActivity extends AppCompatActivity
{
    public static final String PROJECT_EXTRA = "project_position";
    BabyNameProject project;

    OriginAdapter adapter;
    ListView originsListView;
    RadioGroup genderRadio;
    EditText patternText;
    TextView counterText;

    Boolean loadFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        originsListView = findViewById(R.id.origins_list);
        patternText = findViewById(R.id.pattern_text);
        counterText = findViewById(R.id.counter_text);
        genderRadio = findViewById(R.id.gender_radio);

        int defaultBackgroundColor = patternText.getDrawingCacheBackgroundColor();
        patternText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    // check if the pattern is valid
                    Pattern.compile(s.toString().trim());
                    patternText.setBackgroundColor(defaultBackgroundColor);
                    if (loadFinished) {
                        updateNameCounter();
                    }
                } catch (PatternSyntaxException e) {
                    // set background to orange
                    patternText.setBackgroundColor(Color.rgb(255, 165, 0));
                }
            }
        });

        genderRadio.setOnCheckedChangeListener((group, checkedId) -> {
            if (loadFinished) {
                updateNameCounter();
            }
        });

        HashSet<String> allOrigins = MainActivity.database.getAllOrigins();
        ArrayList<String> origins = new ArrayList<>(allOrigins);
        Collections.sort(origins);

        adapter = new OriginAdapter(origins, getApplicationContext(), () -> {
            if (loadFinished) {
                updateNameCounter();
            }
        });
        originsListView.setAdapter(adapter);

        Intent intent = getIntent();

        int projectIndex = intent.getIntExtra(PROJECT_EXTRA, -1);
        if (projectIndex == -1) {
            //AppLogger.info("new baby");
            project = new BabyNameProject();
        } else {
            project = MainActivity.projects.get(projectIndex);
        }

        applyFromProject(project);

        loadFinished = true;
        updateNameCounter();
    }

    public void applyFromProject(BabyNameProject project) {
        //AppLogger.info("Set project preferences: "+project);

        HashSet<String> genders = project.getGenders();
        if (genders.contains(BabyNameDatabase.GENDER_FEMALE) && genders.contains(BabyNameDatabase.GENDER_MALE)) {
            genderRadio.check(R.id.all_radio);
        } else if (genders.contains(BabyNameDatabase.GENDER_MALE)) {
            genderRadio.check(R.id.boy_radio);
        } else {
            genderRadio.check(R.id.girl_radio);
        }

        // set pattern
        patternText.setText(project.pattern.toString());

        // clear origin selection
        for (int i = 0 ; i < originsListView.getCount() ; i += 1) {
            originsListView.setItemChecked(i, false);
        }

        // select project origins
        for (String origin : project.getOrigins()) {
            int position = adapter.getPosition(origin);
            if (position >= 0) {
                originsListView.setItemChecked(position, true);
            }
        }
    }

    public boolean storeToProject(BabyNameProject project) {
        //AppLogger.info("storeToProject()");

        // update origins
        project.getOrigins().clear();
        for (int i = 0; i < adapter.origins.size(); i += 1) {
            String origin = adapter.origins.get(i);
            Boolean checked = adapter.checked.get(i);
            if (checked) {
                project.getOrigins().add(origin);
            }
        }

        // update genders
        project.getGenders().clear();
        switch (genderRadio.getCheckedRadioButtonId()) {
            case R.id.boy_radio -> {
                project.getGenders().add(BabyNameDatabase.GENDER_MALE);
            }
            case R.id.girl_radio -> {
                project.getGenders().add(BabyNameDatabase.GENDER_FEMALE);
            }
            case R.id.all_radio -> {
                project.getGenders().add(BabyNameDatabase.GENDER_FEMALE);
                project.getGenders().add(BabyNameDatabase.GENDER_MALE);
            }
        }

        // update name pattern
        try {
            Pattern newPattern = Pattern.compile(patternText.getText().toString().trim());
            project.setPattern(newPattern);
        } catch (PatternSyntaxException e) {
            Toast.makeText(this, String.format(getString(R.string.name_pattern_malformed), e.getMessage()), Toast.LENGTH_LONG).show();
            return false;
        }

        //Toast.makeText(EditActivity.this, "Project set, "+project.nexts.size()+" names to review !", Toast.LENGTH_SHORT).show();

        project.setNeedToBeSaved(true);

        return true;
    }

    void updateNameCounter() {
        //AppLogger.info("updateNameCounter()");
        int count = 0;

        if (project != null) {
            BabyNameProject tmp = new BabyNameProject();
            if (storeToProject(tmp)) {
                int n = MainActivity.database.size();
                for (int i = 0; i < n; i++) {
                    if (tmp.isNameValid(MainActivity.database.get(i))) {
                        count += 1;
                    }
                }
            }
        }

        counterText.setText(
            String.format(getString(R.string.names_counter), count)
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_cancelsave_babyproject:
                //AppLogger.info("Cancel changes");
                project.setNeedToBeSaved(false);
                MainActivity.projects.remove(project);
                this.finish();
                return true;
            case R.id.action_save_babyproject:
                //AppLogger.info("Save project");
                if (storeToProject(project)) {
                    if (!project.rebuildNexts()) {
                        Toast.makeText(EditActivity.this, R.string.too_much_constraint, Toast.LENGTH_SHORT).show();
                    }

                    if (MainActivity.projects.indexOf(project) == -1) {
                        MainActivity.projects.add(project);
                    }

                    this.finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
