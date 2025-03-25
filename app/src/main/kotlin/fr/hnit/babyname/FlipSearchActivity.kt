package fr.hnit.babyname

import android.content.DialogInterface
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

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

open class FlipSearchActivity : AppCompatActivity() {
    private lateinit var project: BabyNameProject
    private var currentBabyName: BabyName? = null

    private lateinit var backgroundImage: ImageView
    private lateinit var nextButton: Button
    private lateinit var removeButton: Button
    private lateinit var previousButton: Button
    private lateinit var rateBar: RatingBar
    private lateinit var nameText: TextView
    private lateinit var extraText: TextView
    private lateinit var progressCounterText: TextView
    private lateinit var progressPercentText: TextView
    private var goToNext: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flip_search)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this@FlipSearchActivity)
        goToNext = sharedPref.getBoolean("pref_next_ontouch", false)

        backgroundImage = findViewById(R.id.imageView)
        if (Math.random() > 0.5) {
            backgroundImage.setImageResource(R.drawable.tuxbaby)
        } else {
            backgroundImage.setImageResource(R.drawable.tuxbaby2)
        }

        nextButton = findViewById(R.id.next_button)
        removeButton = findViewById(R.id.remove_button)
        previousButton = findViewById(R.id.previous_button)
        rateBar = findViewById(R.id.rate_bar)
        nameText = findViewById(R.id.name_text)
        extraText = findViewById(R.id.extra_text)
        progressCounterText = findViewById(R.id.progress_counter)
        progressPercentText = findViewById(R.id.progress_percent)

        nextButton.setOnClickListener { nextName() }
        removeButton.setOnClickListener { removeName() }
        previousButton.setOnClickListener { previousName() }

        rateBar.setOnTouchListener { view: View?, motionEvent: MotionEvent ->
            if (goToNext && motionEvent.action == MotionEvent.ACTION_UP) {
                nextName()
            }
            false
        }

        val index = intent.getIntExtra(MainActivity.PROJECT_EXTRA, 0)
        if (index >= 0 && MainActivity.projects.size > index) {
            project = MainActivity.projects[index]
            currentBabyName = project.currentName()
        }

        if (currentBabyName == null) {
            Toast.makeText(this@FlipSearchActivity, R.string.message_no_name_to_review, Toast.LENGTH_LONG).show()
            finish()
        } else {
            updateName()
        }

        rateBar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener {
                ratingBar: RatingBar, rating: Float, fromUser: Boolean ->
            val babyName = currentBabyName
            if (fromUser && babyName != null) {
                currentBabyName

                val score = (rating * 2.0F).toInt()
                project.scores[babyName.id] = score

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

    private fun updateName() {
        val babyName = currentBabyName
        if (babyName == null) {
            // last or first name reached
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.finish_round_title)
            builder.setMessage(String.format(getString(R.string.finish_round_message), BabyNameProject.DROP_RATE_PERCENT))
            builder.setPositiveButton(R.string.yes) { dialog: DialogInterface, id: Int ->
                project.nextRound()
                dialog.dismiss()
                finish()
            }
            builder.setNegativeButton(R.string.no) { dialog: DialogInterface, id: Int ->
                dialog.dismiss()
            }
            builder.show()
        } else {
            nameText.text = babyName.name

            if (project.genders.isNotEmpty() || project.origins.size > 1) {
                extraText.text = babyName.getMetaString(applicationContext)
            } else {
                extraText.text = ""
            }

            progressCounterText.text = String.format("(%d/%d)", project.nextsIndex + 1, project.nexts.size)
            progressPercentText.text = String.format("%d%%", (100 * project.nextsIndex) / project.nexts.size)

            // set existing score or default to 0
            rateBar.rating = (project.scores[babyName.id] ?: 0).toFloat() / 2.0F
        }
    }

    private fun nextName() {
        currentBabyName = project.nextName()
        updateName()
    }

    private fun removeName() {
        project.removeCurrent()
        currentBabyName = project.currentName()
        updateName()
    }

    private fun previousName() {
        currentBabyName = project.previousName()
        updateName()
    }

    public override fun onStop() {
        super.onStop()
        project.setNeedToBeSaved(true)
    }
}
