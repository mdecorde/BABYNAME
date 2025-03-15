package fr.hnit.babyname

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
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
    private lateinit var project: BabyNameProject
    private var currentBabyName: BabyName? = null

    private lateinit var backgroundImage: ImageView
    private lateinit var nextButton: Button
    private lateinit var previousButton: Button
    private lateinit var rateBar: RatingBar
    private lateinit var nameText: TextView
    private lateinit var extraText: TextView
    private lateinit var progressCounterText: TextView
    private lateinit var progressPercentText: TextView
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
        previousButton = findViewById(R.id.previous_button)
        rateBar = findViewById(R.id.rate_bar)
        nameText = findViewById(R.id.name_text)
        extraText = findViewById(R.id.extra_text)
        progressCounterText = findViewById(R.id.progress_counter)
        progressPercentText = findViewById(R.id.progress_percent)

        nextButton.setOnClickListener { view: View? -> nextName() }
        nextButton.isEnabled = !goToNext

        previousButton.setOnClickListener { view: View? -> previousName() }

        rateBar.setOnTouchListener { view: View?, motionEvent: MotionEvent ->
            if (goToNext && motionEvent.action == MotionEvent.ACTION_UP) {
                nextName()
            }
            false
        }

        val index = intent.getIntExtra(PROJECT_EXTRA, 0)
        if (index >= 0 && MainActivity.projects.size > index) {
            project = MainActivity.projects[index]
            currentBabyName = project.currentName()
        }

        if (currentBabyName == null) {
            Toast.makeText(this@FindActivity, "No names to review!", Toast.LENGTH_LONG).show()
            finish()
        } else {
            updateName()
        }
    }

    private fun updateName() {
        val babyName = currentBabyName
        if (babyName == null) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.finish_loop_tile)
            builder.setMessage(R.string.finish_review_question)
            builder.setPositiveButton(R.string.yes) { dialog: DialogInterface, id: Int ->
                project.nextLoop()
                dialog.dismiss()
                finish()
            }
            builder.setNegativeButton(R.string.no) { dialog: DialogInterface, id: Int ->
                dialog.dismiss()
                finish()
            }
            builder.show()
        } else {
            nameText.text = babyName.name

            if (project.genders.isNotEmpty() || project.origins.size > 1) {
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

            progressCounterText.text = String.format("(%d/%d)", project.nextsIndex, project.nexts.size)
            progressPercentText.text = String.format("%d%%", 100 * (project.nextsIndex) / project.nexts.size)

            // set existing score or default to 0
            rateBar.rating = (project.scores[babyName.id] ?: 0).toFloat()
        }
    }

    private fun nextName() {
        saveRate()
        currentBabyName = project.nextName()
        updateName()
    }

    private fun previousName() {
        saveRate()
        currentBabyName = project.previousName()
        updateName()
    }

    private fun saveRate() {
        val babyName = currentBabyName
        if (babyName != null) {
            val score = rateBar.rating.toInt()
            val changed = project.evaluateScore(babyName, score)
            if (changed) {
                Toast.makeText(
                    this,
                    String.format(
                        getString(R.string.name_rated_score),
                        babyName.name,
                        score
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                project.setNeedToBeSaved(true)
            }
        }
    }

    public override fun onStop() {
        super.onStop()
        project.setNeedToBeSaved(true)
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
