package fr.hnit.babyname

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import android.widget.Toast
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

class MainActivity : AppCompatActivity() {
    private lateinit var namesListView: ListView
    private lateinit var adapter: ProjectListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (database.size() == 0) {
            database.initialize(applicationContext)
        }

        namesListView = findViewById(R.id.listView)
        registerForContextMenu(namesListView)

        adapter = ProjectListAdapter(this, projects)
        namesListView.adapter = adapter

        if (projects.isEmpty()) {
            initializeProjects()
        }
    }

    private fun storeProjects() {
        for (project in projects) {
            if (!project.needSaving) {
                continue
            }

            if (!BabyNameProject.storeProject(project, this)) {
                Toast.makeText(
                    this,
                    "Error: could not save changes to babyname project: $project",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    public override fun onResume() {
       super.onResume()
       adapter.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        storeProjects()
    }

    override fun onDestroy() {
        super.onDestroy()
        storeProjects()
    }

    private fun initializeProjects() {
        for (filename in this.fileList()) {
            if (filename.endsWith(".baby")) {
                //Log.d("Loading $filename");
                try {
                    val project: BabyNameProject? = BabyNameProject.readProject(filename, this)
                    if (project != null) {
                        projects.add(project)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Error: could not read baby name project from $filename",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.menu_list, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo

        if (adapter.count <= info.position) return false
        val project = adapter.getItem(info.position) ?: return false

        return when (item.itemId) {
            R.id.action_edit_project -> {
                doEditProject(project)
                true
            }

            R.id.action_reset_scores -> {
                doResetScores(project)
                true
            }

            R.id.action_delete_project -> {
                doDeleteProject(project)
                true
            }

            R.id.action_clone_project -> {
                doCloneProject(project)
                true
            }

            R.id.action_top_names -> {
                doShowTop10(project)
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    private fun doResetScores(project: BabyNameProject) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(R.string.reset_question_title)
        builder.setMessage(R.string.reset_question_content)

        builder.setPositiveButton(R.string.yes) { dialog, _ ->
            project.scores.clear()
            project.setNeedToBeSaved(true)
            adapter.notifyDataSetChanged()
            if (!BabyNameProject.storeProject(project, this@MainActivity)) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: could not save reset changes to babyname project: $project",
                    Toast.LENGTH_LONG
                ).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    private fun doCloneProject(project: BabyNameProject) {
        val cloned = project.cloneProject()
        cloned.setNeedToBeSaved(true)
        projects.add(cloned)
        adapter.notifyDataSetChanged()
    }

    fun doDeleteProject(project: BabyNameProject) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(R.string.delete_question_title)
        builder.setMessage(R.string.delete_question_content)

        builder.setPositiveButton(R.string.yes) { dialog, _ ->
            projects.remove(project)
            this@MainActivity.deleteFile(project.iD + ".baby")
            adapter.notifyDataSetChanged()
            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    fun projectToString(p: BabyNameProject): String {
        var text = ""
        text += if (p.genders.contains(BabyNameDatabase.GENDER_FEMALE)
                && p.genders.contains(BabyNameDatabase.GENDER_MALE)) {
            getString(R.string.boy_or_girl_name)
        } else if (p.genders.contains(BabyNameDatabase.GENDER_MALE)) {
            getString(R.string.boy_name)
        } else {
            getString(R.string.girl_name)
        }

        // sort origins for display
        val origins = ArrayList(p.origins)
        origins.sort()

        text += " "
        text += if (origins.size == 1) {
            String.format(getString(R.string.origin_is), origins[0])
        } else if (p.origins.size > 1) {
            String.format(getString(R.string.origin_are), origins)
        } else {
            getString(R.string.no_origin)
        }

        if (p.pattern != null) {
            text += ", "
            text += if (".*" == p.pattern.toString()) {
                getString(R.string.no_pattern)
            } else {
                String.format(getString(R.string.matches_with), p.pattern)
            }
            text += " "
        }

        //Log.d(this, "p.nexts.size: ${p.nexts.size}, p.scores.size: ${p.scores.size} p.nextsIndex: ${p.nextsIndex}, remainingNames: ${p.nexts.size - p.nextsIndex}")

        if (p.nexts.size == 1) {
            text += getString(R.string.one_remaining_name)
        } else {
            val remainingNames = p.nexts.size - p.nextsIndex
            text += String.format(getString(R.string.remaining_names), remainingNames)
        }

        val bestName = p.getBest()
        if (p.scores.isNotEmpty() && bestName != null) {
            text += " " + String.format(getString(R.string.best_match_is), bestName.name)
        }

        return text
    }

    fun doShowTop10(project: BabyNameProject) {
        val names = project.getTop10()

        val buffer = StringBuffer()
        for (name in names) {
            buffer.append("${database[name]!!.name}: ${project.scores[name]}\n")
        }

        if (names.isEmpty()) {
            buffer.append(getString(R.string.no_name_rated))
        }

        val builder = AlertDialog.Builder(this)

        builder.setTitle(R.string.top_title)
        builder.setMessage(buffer.toString())

        builder.setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }

        if (names.isNotEmpty()) {
            builder.setNegativeButton(R.string.copy) { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("baby top10", buffer.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, R.string.text_copied, Toast.LENGTH_LONG).show()
            }
        }

        val alert = builder.create()
        alert.show()

        //Toast.makeText(this, buffer.toString(), Toast.LENGTH_LONG).show();
    }

    fun doFlipSearch(project: BabyNameProject) {
        val intent = Intent(this, FlipSearchActivity::class.java)
        intent.putExtra(PROJECT_EXTRA, projects.indexOf(project))
        startActivityForResult(intent, 0)
    }

    fun doScrollSearch(project: BabyNameProject) {
        val intent = Intent(this, ScrollSearchActivity::class.java)
        intent.putExtra(PROJECT_EXTRA, projects.indexOf(project))
        startActivityForResult(intent, 0)
    }

    fun doEditProject(project: BabyNameProject?) {
        val intent = Intent(this, EditActivity::class.java)
        if (project != null) {
            intent.putExtra(PROJECT_EXTRA, projects.indexOf(project))
        }
        startActivityForResult(intent, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                this.startActivityForResult(intent, 0)
                true
            }

            R.id.action_about -> {
                val intent = Intent(this@MainActivity, AboutActivity::class.java)
                this.startActivityForResult(intent, 0)
                true
            }

            R.id.action_new_baby -> {
                doNewBaby()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun doNewBaby() {
        Toast.makeText(this, R.string.new_baby, Toast.LENGTH_LONG).show()
        doEditProject(null)
    }

    companion object {
        const val PROJECT_EXTRA: String = "project_position"
        var database = BabyNameDatabase()
        var projects = ArrayList<BabyNameProject>()
    }
}
