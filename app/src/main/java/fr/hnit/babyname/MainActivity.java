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

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    ListView namesListView;
    BabyNameAdapter adapter;

    public static BabyNameDatabase database = new BabyNameDatabase();
    public static ArrayList<BabyNameProject> projects = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (database.size() == 0) {
            database.initialize(getApplicationContext());
        }

        namesListView = findViewById(R.id.listView);
        registerForContextMenu(namesListView);

        namesListView.setOnItemClickListener((adapterView, view, i, l) -> {
            BabyNameProject project = projects.get(i);
            if (project != null) {
                doFindName(project);
            }
        });

        adapter = new BabyNameAdapter(this, projects);
        namesListView.setAdapter(adapter);

        if (projects.isEmpty()) {
            initializeProjects();
        }
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        adapter.notifyDataSetChanged();
        for (BabyNameProject project : projects) {
            if (project.needSaving) {
                //Toast.makeText(this, "Saving changes of "+project+"... "+project, Toast.LENGTH_SHORT).show();
                if (!BabyNameProject.storeProject(project, this)) {
                    Toast.makeText(this, "Error: could not save changes to babyname project: "+project, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void initializeProjects() {
        //AppLogger.info("Initializing projects...");
        for (String filename : this.fileList()) {
            if (filename.endsWith(".baby")) {
                //AppLogger.info("Restoring... "+filename);
                try {
                    BabyNameProject project = BabyNameProject.readProject(filename, this);
                    if (project != null) {
                        projects.add(project);
                    } else {
                        Toast.makeText(MainActivity.this, "Error: could not read baby name project from "+filename, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (adapter.getCount() <= info.position) return false;
        BabyNameProject project = adapter.getItem(info.position);

        if (project == null) return false;

        return switch (item.getItemId()) {
            case R.id.action_reset_baby -> {
                doResetBaby(project);
                yield true;
            }
            case R.id.action_top_baby -> {
                doShowTop10(project);
                yield true;
            }
            case R.id.action_delete_baby -> {
                doDeleteBaby(project);
                yield true;
            }
            default -> super.onContextItemSelected(item);
        };
    }

    public void doResetBaby(final BabyNameProject project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.reset_question_title);
        builder.setMessage(R.string.reset_question_content);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                project.reset();
                adapter.notifyDataSetChanged();
                if (!BabyNameProject.storeProject(project, MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "Error: could not save reset changes to babyname project: "+project, Toast.LENGTH_LONG).show();
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // I do not need any action here you might
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public void doDeleteBaby(final BabyNameProject project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.delete_question_title);
        builder.setMessage(R.string.delete_question_content);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                projects.remove(project);
                MainActivity.this.deleteFile(project.getID()+".baby");
                adapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }


    public String projectToString(BabyNameProject p) {
        String text = "";
        if (p.genders.contains(BabyNameDatabase.GENDER_FEMALE) && p.genders.contains(BabyNameDatabase.GENDER_MALE)) {
            text += getString(R.string.boy_or_girl_name);
        } else if (p.genders.contains(BabyNameDatabase.GENDER_MALE)) {
            text += getString(R.string.boy_name);
        } else {
            text += getString( R.string.girl_name);
        }

        // sort origins for display
        ArrayList<String> origins = new ArrayList<>(p.origins);
        Collections.sort(origins);

        text += " ";
        if (origins.size() == 1) {
            text += String.format(getString(R.string.origin_is), origins.get(0));
        } else if (p.origins.size() > 1) {
            text += String.format(getString(R.string.origin_are), origins);
        } else {
            text += getString(R.string.no_origin);
        }

        if (p.pattern != null) {
            text += ", ";
            if (".*".equals(p.pattern.toString())) {
                text += getString(R.string.no_pattern);
            } else {
                text += String.format(getString(R.string.matches_with), p.pattern);
            }
            text += " ";
        }

        if (p.nexts.size() == 1) {
            text += getString(R.string.one_remaining_name);
        } else if (p.nexts.isEmpty()) {
            int n = p.scores.size();
            if (n > 11) {
                n = n - 10;
            }
            text += String.format(getString(R.string.no_remaining_loop), p.loop, n);
        } else {
            text += String.format(getString(R.string.remaining_names), p.nexts.size());
        }

        if (!p.scores.isEmpty() && p.getBest() != null) {
            text += " " + String.format(getString(R.string.best_match_is), p.getBest().name);
        }

        return text;
    }

    public void doShowTop10(final BabyNameProject project) {
        List<Integer> names = project.getTop10();

        final StringBuffer buffer = new StringBuffer();

        int n = 0;
        for (Integer name : names) {
            buffer.append("\n" + MainActivity.database.get(name).name + ": " + project.scores.get(name));
        }

        if (names.isEmpty()) {
            buffer.append(getString(R.string.no_name_rated));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.top_title);
        builder.setMessage(buffer.toString());

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        if (!names.isEmpty()) {
            builder.setNegativeButton(R.string.copy, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("baby top10", buffer.toString());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, R.string.text_copied, Toast.LENGTH_LONG).show();
                }
            });
        }

        AlertDialog alert = builder.create();
        alert.show();

        //Toast.makeText(this, buffer.toString(), Toast.LENGTH_LONG).show();
    }

    public void doFindName(BabyNameProject project) {
        //AppLogger.info("Open FindActivity with "+project+" index="+projects.indexOf(project));
        Intent intent = new Intent(MainActivity.this, FindActivity.class);
        intent.putExtra(FindActivity.PROJECT_EXTRA, projects.indexOf(project));
        this.startActivityForResult(intent, 0);
    }

    private void openEditActivity(BabyNameProject project) {
        //AppLogger.info("Open EditActivity with "+project+" index="+projects.indexOf(project));
        Intent intent = new Intent(MainActivity.this, EditActivity.class);
        if (project != null) {
            intent.putExtra(EditActivity.PROJECT_EXTRA, projects.indexOf(project));
        }
        this.startActivityForResult(intent, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return switch (item.getItemId()) {
            case R.id.action_settings -> {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                this.startActivityForResult(intent, 0);
                yield true;
            }
            case R.id.action_about -> {
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                this.startActivityForResult(intent, 0);
                yield true;
            }
            case R.id.action_new_baby -> {
                doNewBaby();
                yield true;
            }
            default -> super.onOptionsItemSelected(item);
        };
    }

    public void doNewBaby() {
        Toast.makeText(this, R.string.new_baby, Toast.LENGTH_LONG).show();
        openEditActivity(null);
    }
}
