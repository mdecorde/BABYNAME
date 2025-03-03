package fr.hnit.babyname

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import java.util.Collections
import java.util.Locale

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

open class FindActivity : AppCompatActivity() {
    private var project: BabyNameProject? = null
    private var currentBabyName: BabyName? = null

    private lateinit var backgroundImage: ImageView
    private lateinit var nextButton: Button
    private lateinit var rateBar: RatingBar
    private lateinit var nameText: TextView
    private lateinit var extraText: TextView
    private lateinit var remainingText: TextView
    private var goToNext: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this@FindActivity)
        goToNext = sharedPref.getBoolean("pref_next_ontouch", false)

        backgroundImage = findViewById(R.id.imageView)
        if (Math.random() > 0.5) {
            backgroundImage.setImageResource(R.drawable.tuxbaby)
        } else {
            backgroundImage.setImageResource(R.drawable.tuxbaby2)
        }
        nextButton = findViewById(R.id.next_button)
        rateBar = findViewById(R.id.rate_bar)
        nameText = findViewById(R.id.name_text)
        extraText = findViewById(R.id.extra_text)
        remainingText = findViewById(R.id.remaining_text)

        nextButton.setOnClickListener { view: View? -> nextName() }

        nextButton.isEnabled = !goToNext

        rateBar.setOnTouchListener { view: View?, motionEvent: MotionEvent ->
            if (goToNext && motionEvent.action == MotionEvent.ACTION_UP) {
                nextName()
            }
            false
        }

        val intent = intent

        if (intent != null) {
            val index = intent.getIntExtra(PROJECT_EXTRA, 0)
            if (index >= 0 && MainActivity.projects.size > index) {
                val project = MainActivity.projects[index]
                if (project.nexts.isEmpty()) {
                    Toast.makeText(this@FindActivity, R.string.starting_new_loop, Toast.LENGTH_LONG).show()
                    project.rebuildNexts()
                    project.setNeedToBeSaved(true)
                }
                setProject(project)
            }
        }
    }

    private fun updateName() {
        val babyName = currentBabyName
        if (babyName == null) {
            AppLogger.d(this, "No current baby name found: $project")
            Toast.makeText(
                this@FindActivity,
                getString(R.string.all_names_reviewed),
                Toast.LENGTH_LONG
            ).show()
            this@FindActivity.finish()
        } else {
            nameText.text = babyName.name

            if (project!!.genders.isNotEmpty() || project!!.origins.size > 1) {
                val context = applicationContext
                val genres = ArrayList(babyName.genres)
                val origins = ArrayList(babyName.origins)
                genres.sort()
                origins.sort()
                var extra = ""
                if (genres.isNotEmpty()) {
                    extra += genresToLocale(context, genres).toString()
                }
                extra += " "
                if (origins.isNotEmpty()) {
                    extra += originsToLocale(context, origins).toString()
                }
                extraText.text = extra
            } else {
                extraText.text = ""
            }

            remainingText.text =
                String.format(getString(R.string.n_name_left), project!!.nexts.size)
            rateBar.rating = 0f
        }
    }

    private fun nextName() {
        if (project == null) {
            return
        }

        saveRate()

        currentBabyName = project!!.nextName()
        updateName()
    }

    private fun saveRate() {
        val rate = rateBar.rating.toInt()
        val score = project!!.evaluate(currentBabyName!!, rate)
        if (rate > 0) {
            Toast.makeText(
                this@FindActivity,
                String.format(
                    getString(R.string.name_rated_score),
                    currentBabyName!!.name,
                    score,
                    rate
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
        project!!.setNeedToBeSaved(true)
    }

    private fun setProject(project: BabyNameProject) {
        //AppLogger.d("Set project preferences: "+project);
        this.project = project

        currentBabyName = if (project.currentBabyNameIndex == -1) {
            project.nextName()
        } else {
            MainActivity.database.get(project.currentBabyNameIndex)
        }

        if (currentBabyName == null) {
            AppLogger.e(this, "No current baby name found: $project")
            return
        }

        updateName()
    }

    public override fun onStop() {
        super.onStop()
        if (project != null) {
            project!!.setNeedToBeSaved(true)
        }
    }

    companion object {
        const val PROJECT_EXTRA: String = "project_position"

        private fun genresToLocale(
            context: Context,
            origins: ArrayList<String>
        ): ArrayList<String> {
            val ret = ArrayList<String>()
            var i = 0
            while (i < origins.size) {
                when (origins[i]) {
                    BabyNameDatabase.GENDER_FEMALE -> ret.add(context.getString(R.string.girl))
                    BabyNameDatabase.GENDER_MALE -> ret.add(context.getString(R.string.boy))
                    else -> ret.add(origins[i])
                }
                i += 1
            }
            return ret
        }

        // Make every origin begin with an upper case letter.
        // It would be nice to have proper localisation in the future.
        private fun originsToLocale(
            context: Context,
            origins: ArrayList<String>
        ): ArrayList<String> {
            val ret = ArrayList<String>()
            var i = 0
            while (i < origins.size) {
                val origin = origins[i]
                ret.add(
                    origin.substring(0, 1).uppercase(Locale.getDefault()) + origin.substring(1)
                )
                i += 1
            }
            return ret
        }
    }
}
