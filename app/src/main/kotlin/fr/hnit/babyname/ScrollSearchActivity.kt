package fr.hnit.babyname

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

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

open class ScrollSearchActivity : AppCompatActivity() {
    private lateinit var project: BabyNameProject
    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollAdapter: ScrollSearchAdapter
    private lateinit var dropButton: Button
    private lateinit var sortButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_search)

        dropButton = findViewById(R.id.dropButton)
        sortButton = findViewById(R.id.sortButton)
        recyclerView = findViewById(R.id.recyclerview)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val index = intent.getIntExtra(MainActivity.PROJECT_EXTRA, 0)
        if (index >= 0 && MainActivity.projects.size > index) {
            project = MainActivity.projects[index]
        } else {
            finish()
            return
        }

        sortNames()

        scrollAdapter = ScrollSearchAdapter(this, project)

        recyclerView.adapter = scrollAdapter

        dropButton.text = String.format(getString(R.string.button_drop_percent), BabyNameProject.DROP_RATE_PERCENT)
        dropButton.setOnClickListener {
            dropDialog()
        }

        sortButton.setOnClickListener {
            sortNames()
            scrollAdapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(0)
        }

        if (project.nexts.size > 10) {
            dropButton.visibility = View.VISIBLE
        } else {
            dropButton.visibility = View.GONE
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                    val position = viewHolder.adapterPosition
                    val nameId = project.nexts.removeAt(position)
                    val scoreBackup = project.scores.remove(nameId)

                    val needSavingBackup =  project.needSaving
                    project.needSaving = true

                    scrollAdapter.notifyItemRemoved(position)

                    val snackbar = Snackbar.make(
                        recyclerView,
                        R.string.name_was_removed,
                        Snackbar.LENGTH_LONG
                    )
                    snackbar.setAction(R.string.undo) {
                        project.nexts.add(position, nameId)
                        project.needSaving = needSavingBackup
                        if (scoreBackup != null) {
                            project.scores[nameId] = scoreBackup
                        }
                        recyclerView.scrollToPosition(position)
                        scrollAdapter.notifyItemInserted(position)
                    }

                    snackbar.setActionTextColor(Color.YELLOW)
                    snackbar.show()
                }
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun dropDialog() {
        val amountToRemove = ((BabyNameProject.DROP_RATE_PERCENT * project.nexts.size) / 100)

        val builder = AlertDialog.Builder(this)

        builder.setTitle(R.string.dialog_drop_title)
        builder.setMessage(String.format(getString(R.string.dialog_drop_message), amountToRemove, project.nexts.size))

        builder.setPositiveButton(R.string.yes) { dialog, _ ->
            project.dropLast()
            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.no) { dialog, which ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    fun onRatingChangeListener(babyName: BabyName, rating: Float) {
        val newScore = (rating * 2.0F).toInt()
        val oldScore = project.scores[babyName.id] ?: 0
        if (newScore != oldScore) {
            //Log.d(this, "rating changed for ${babyName.name}: $oldScore => $newScore")
            project.scores[babyName.id] = newScore
            project.setNeedToBeSaved(true)
        }
    }

    private fun getOriginScore(target: BabyName): Int {
        var maxScore = 0
        for (id in project.scores.keys) {
            var score = 0
            val source = MainActivity.database.get(id)
            for (targetOrigin in target.origins) {
                if (source.origins.contains(targetOrigin)) {
                    score += project.scores[id] ?: 0
                }
            }

            if (score > maxScore) {
                maxScore = score
            }
        }
        return maxScore
    }

    private fun getSoundexScore(target: BabyName): Int {
        var maxScore = 0
        for (id in project.scores.keys) {
            val source = MainActivity.database.get(id)
            if (target.soundex == source.soundex) {
                val score = project.scores[id] ?: 0
                if (score > maxScore) {
                    maxScore = score
                }
            }
        }
        return maxScore
    }

    private fun sortNames() {
        project.nexts.sortWith { i: Int, j: Int ->
            val a = MainActivity.database.get(i)
            val b = MainActivity.database.get(j)

            val aScore = project.scores[a.id] ?: 0
            val bScore = project.scores[b.id] ?: 0

            if (aScore != bScore) {
                return@sortWith bScore - aScore;
            } else {
                val aSoundexScore = getSoundexScore(a)
                val bSoundexScore = getSoundexScore(b)

                if (aSoundexScore != bSoundexScore) {
                    return@sortWith bSoundexScore - aSoundexScore
                } else {
                    val aOriginScore = getOriginScore(a)
                    val bOriginScore = getOriginScore(b)
                    return@sortWith bOriginScore - aOriginScore
                }
            }
        }
    }

    public override fun onStop() {
        super.onStop()
        project.setNeedToBeSaved(true)
    }
}
