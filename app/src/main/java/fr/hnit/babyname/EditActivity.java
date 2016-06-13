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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class EditActivity extends AppCompatActivity {

    public static final String PROJECT_EXTRA = "project_position";
public static final HashSet<String> allOrigins = new HashSet<String>();;
    BabyNameProject project;

    ArrayAdapter<String> adapter;
    ListView originsListView;
    RadioButton boyRadioButton;
    RadioButton girlRadioButton;
    RadioButton bothRadioButton;
    EditText patternText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        originsListView = (ListView) findViewById(R.id.origins_list);
        boyRadioButton = (RadioButton) findViewById(R.id.boy_radio);
        girlRadioButton = (RadioButton) findViewById(R.id.girl_radio);
        bothRadioButton = (RadioButton) findViewById(R.id.both_radio);
        patternText = (EditText) findViewById(R.id.pattern_text);

        // initialize
        allOrigins.clear();
        int n = MainActivity.database.size();
        for (int i = 0 ; i < n ; i++) {
            BabyName name = MainActivity.database.get(i);
            if (name != null)
                allOrigins.addAll(name.origins);
        }
        ArrayList<String> originsList = new ArrayList<>(allOrigins);
        Collections.sort(originsList);
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, originsList);
        originsListView.setAdapter(adapter);

        Intent intent = getIntent();

        if (intent != null) {
            BabyNameProject project = MainActivity.projects.get(intent.getIntExtra(PROJECT_EXTRA, 0));
            setProject(project);
        }
    }

    public void setProject(BabyNameProject project) {
        //AppLogger.info("Set project preferences: "+project);
        this.project = project;

        if (project.getGenders().contains(NameData.F) && project.getGenders().contains(NameData.M)) {
            boyRadioButton.setChecked(false);
            girlRadioButton.setChecked(false);
            bothRadioButton.setChecked(true);
        } else if (project.getGenders().contains(NameData.M)) {
            boyRadioButton.setChecked(true);
            girlRadioButton.setChecked(false);
            bothRadioButton.setChecked(false);
        } else {
            boyRadioButton.setChecked(false);
            girlRadioButton.setChecked(true);
            bothRadioButton.setChecked(false);
        }

        patternText.setText(project.getPattern().toString());

        // clear selection
        for (int i = 0 ; i < originsListView.getCount() ; i++) {
            originsListView.setItemChecked(i, false);
        }

        // select project origins
        for (String origin : project.getOrigins()) {
            int position = adapter.getPosition(origin);
            if (position >= 0)
                originsListView.setItemChecked(position, true);
        }
    }

    public boolean registerProject() {

        //update origins
        project.getOrigins().clear();
        SparseBooleanArray itemChecked = originsListView.getCheckedItemPositions();
        for (int i = 0 ; i < itemChecked.size() ; i++) {
            boolean checked = itemChecked.get(i);
            if (checked) {
                String v = adapter.getItem(i);
                project.getOrigins().add(v);
            }
        }

        // update genders
        project.getGenders().clear();
        if (bothRadioButton.isChecked()) {
            project.getGenders().add(NameData.F);
            project.getGenders().add(NameData.M);
        } else if (boyRadioButton.isChecked()) {
            project.getGenders().add(NameData.M);
        } else {
            project.getGenders().add(NameData.F);
        }

        // update name pattern
        try {
            Pattern newPattern = Pattern.compile(patternText.getText().toString().trim());
            project.setPattern(newPattern);
        } catch (PatternSyntaxException e) {
            Toast.makeText(this, "Name pattern is malformed : \n"+e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }

        if (!project.rebuildNexts()) {
            Toast.makeText(EditActivity.this, "Too much name constraint, no name found", Toast.LENGTH_SHORT).show();
            return false;
        }

        Toast.makeText(EditActivity.this, "Project set, "+project.nexts.size()+" names to review !", Toast.LENGTH_SHORT).show();

        project.setNeedToBeSaved(true);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.action_cancelsave_babyproject:
                //AppLogger.info("Cancel changes");
                project.setNeedToBeSaved(false);
                MainActivity.projects.remove(project);
                this.finish();
                return true;
            case R.id.action_save_babyproject:
                //AppLogger.info("Save project");
                if (registerProject()) {
                    this.finish();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
