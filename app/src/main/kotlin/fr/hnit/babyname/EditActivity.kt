package fr.hnit.babyname

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

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

class EditActivity : AppCompatActivity() {
    lateinit var project: BabyNameProject
    lateinit var adapter: OriginAdapter
    lateinit var originsListView: ListView
    lateinit var genderRadio: RadioGroup
    lateinit var patternText: EditText
    lateinit var counterText: TextView

    var loadFinished: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        originsListView = findViewById(R.id.origins_list)
        patternText = findViewById(R.id.pattern_text)
        counterText = findViewById(R.id.counter_text)
        genderRadio = findViewById(R.id.gender_radio)

        val defaultBackgroundColor = patternText.getDrawingCacheBackgroundColor()
        patternText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                try {
                    // check if the pattern is valid
                    Pattern.compile(s.toString().trim { it <= ' ' })
                    patternText.setBackgroundColor(defaultBackgroundColor)
                    if (loadFinished) {
                        updateNameCounter()
                    }
                } catch (e: PatternSyntaxException) {
                    // set background to orange
                    patternText.setBackgroundColor(Color.rgb(255, 165, 0))
                }
            }
        })

        genderRadio.setOnCheckedChangeListener { _: RadioGroup?, _: Int ->
            if (loadFinished) {
                updateNameCounter()
            }
        }

        val allOrigins = MainActivity.database.getAllOrigins()
        val origins = ArrayList(allOrigins)
        origins.sort()

        adapter = OriginAdapter(origins, applicationContext) {
            if (loadFinished) {
                updateNameCounter()
            }
        }
        originsListView.adapter = adapter

        val projectIndex = intent.getIntExtra(MainActivity.PROJECT_EXTRA, -1)
        project = if (projectIndex == -1) {
            BabyNameProject()
        } else {
            MainActivity.projects[projectIndex]
        }

        applyFromProject(project)

        updateNameCounter()
        loadFinished = true
    }

    fun applyFromProject(project: BabyNameProject) {
        val genders = project.genders
        if (genders.contains(BabyNameDatabase.GENDER_FEMALE)
                && genders.contains(BabyNameDatabase.GENDER_MALE)) {
            genderRadio.check(R.id.all_radio)
        } else if (genders.contains(BabyNameDatabase.GENDER_MALE)) {
            genderRadio.check(R.id.boy_radio)
        } else {
            genderRadio.check(R.id.girl_radio)
        }

        // set pattern
        patternText.setText(project.pattern.toString())

        // clear origin selection
        for (i in adapter.checked.indices) {
            adapter.checked[i] = false
        }

        // select project origins
        for (origin in project.origins) {
            val i = adapter.getPosition(origin)
            if (i > -1) {
                adapter.checked[i] = true
            }
        }

        adapter.notifyDataSetChanged()
    }

    fun storeToProject(project: BabyNameProject): Boolean {
        // update origins
        project.origins.clear()
        var i = 0
        while (i < adapter.origins.size) {
            val origin = adapter.origins[i]
            val checked = adapter.checked[i]
            if (checked && origin != null) {
                project.origins.add(origin)
            }
            i += 1
        }

        // update genders
        project.genders.clear()
        when (genderRadio.checkedRadioButtonId) {
            R.id.boy_radio -> {
                project.genders.add(BabyNameDatabase.GENDER_MALE)
            }

            R.id.girl_radio -> {
                project.genders.add(BabyNameDatabase.GENDER_FEMALE)
            }

            R.id.all_radio -> {
                project.genders.add(BabyNameDatabase.GENDER_FEMALE)
                project.genders.add(BabyNameDatabase.GENDER_MALE)
            }
        }

        // update name pattern
        try {
            project.pattern = Pattern.compile(patternText.text.toString().trim { it <= ' ' })
        } catch (e: PatternSyntaxException) {
            Toast.makeText(
                this,
                String.format(getString(R.string.name_pattern_malformed), e.message),
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        project.rebuildNexts()
        project.setNeedToBeSaved(true)

        return true
    }

    fun updateNameCounter() {
        //Log.d("updateNameCounter()");
        var count = 0

        val tmp = BabyNameProject()
        if (storeToProject(tmp)) {
            val n: Int = MainActivity.database.size()
            for (i in 0 until n) {
                if (tmp.isNameValid(MainActivity.database.get(i))) {
                    count += 1
                }
            }
        }

        counterText.text = String.format(getString(R.string.names_counter), count)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.edit, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save_project -> {
                if (storeToProject(project)) {
                    if (project.nexts.isEmpty()) {
                        Toast.makeText(this, R.string.too_much_constraint, Toast.LENGTH_SHORT).show()
                        return false
                    }

                    // add if not added yet
                    if (MainActivity.projects.indexOf(project) == -1) {
                        MainActivity.projects.add(project)
                    }

                    finish()
                }
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }
}
