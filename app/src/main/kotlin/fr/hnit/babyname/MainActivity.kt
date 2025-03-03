package fr.hnit.babyname

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.Toast
import java.util.Collections

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
    lateinit var namesListView: ListView
    lateinit var adapter: BabyNameAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (database.size() == 0) {
            database.initialize(applicationContext)
        }

        namesListView = findViewById(R.id.listView)
        registerForContextMenu(namesListView)

        namesListView.setOnItemClickListener { adapterView: AdapterView<*>?, view: View?, i: Int, l: Long ->
            doFindName(projects[i])
        }

        adapter = BabyNameAdapter(this, projects)
        namesListView.adapter = adapter

        if (projects.isEmpty()) {
            initializeProjects()
        }
    }

    public override fun onResume() {
        super.onResume() // Always call the superclass method first
        adapter.notifyDataSetChanged()
        for (project in projects) {
            if (project.needSaving) {
                //Toast.makeText(this, "Saving changes of "+project+"... "+project, Toast.LENGTH_SHORT).show();
                if (!BabyNameProject.Companion.storeProject(project, this)) {
                    Toast.makeText(
                        this,
                        "Error: could not save changes to babyname project: $project",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun initializeProjects() {
        //AppLogger.d("Initializing projects...");
        for (filename in this.fileList()) {
            if (filename.endsWith(".baby")) {
                //AppLogger.d("Restoring... "+filename);
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

        val inflater = menuInflater
        inflater.inflate(R.menu.menu_list, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo

        if (adapter.count <= info.position) return false
        val project = adapter.getItem(info.position) ?: return false

        return when (item.itemId) {
            R.id.action_reset_baby -> {
                doResetBaby(project)
                true
            }

            R.id.action_top_baby -> {
                doShowTop10(project)
                true
            }

            R.id.action_delete_baby -> {
                doDeleteBaby(project)
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    fun doResetBaby(project: BabyNameProject) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(R.string.reset_question_title)
        builder.setMessage(R.string.reset_question_content)

        builder.setPositiveButton(
            R.string.yes
        ) { dialog, _ ->
            project.reset()
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

        builder.setNegativeButton(
            R.string.no
        ) { dialog, which -> // I do not need any action here you might
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    fun doDeleteBaby(project: BabyNameProject) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(R.string.delete_question_title)
        builder.setMessage(R.string.delete_question_content)

        builder.setPositiveButton(
            R.string.yes
        ) { dialog, _ ->
            projects.remove(project)
            this@MainActivity.deleteFile(project.iD + ".baby")
            adapter.notifyDataSetChanged()
            dialog.dismiss()
        }

        builder.setNegativeButton(
            R.string.no
        ) { dialog, _ -> dialog.dismiss() }

        val alert = builder.create()
        alert.show()
    }


    fun projectToString(p: BabyNameProject): String {
        var text = ""
        text += if (p.genders.contains(BabyNameDatabase.GENDER_FEMALE) && p.genders.contains(
                BabyNameDatabase.GENDER_MALE
            )
        ) {
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

        if (p.nexts.size == 1) {
            text += getString(R.string.one_remaining_name)
        } else if (p.nexts.isEmpty()) {
            var n = p.scores.size
            if (n > 11) {
                n -= 10
            }
            text += String.format(getString(R.string.no_remaining_loop), p.loop, n)
        } else {
            text += String.format(getString(R.string.remaining_names), p.nexts.size)
        }

        val bestName= p.getBest()
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

        builder.setPositiveButton(
            R.string.ok
        ) { dialog, _ -> dialog.dismiss() }

        if (names.isNotEmpty()) {
            builder.setNegativeButton(
                R.string.copy
            ) { _, _ ->
                val clipboard =
                    getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("baby top10", buffer.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    this@MainActivity,
                    R.string.text_copied,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val alert = builder.create()
        alert.show()

        //Toast.makeText(this, buffer.toString(), Toast.LENGTH_LONG).show();
    }

    fun doFindName(project: BabyNameProject) {
        //AppLogger.d("Open FindActivity with "+project+" index="+projects.indexOf(project));
        val intent = Intent(this@MainActivity, FindActivity::class.java)
        intent.putExtra(FindActivity.PROJECT_EXTRA, projects.indexOf(project))
        this.startActivityForResult(intent, 0)
    }

    private fun openEditActivity(project: BabyNameProject?) {
        //AppLogger.d("Open EditActivity with "+project+" index="+projects.indexOf(project));
        val intent = Intent(this@MainActivity, EditActivity::class.java)
        if (project != null) {
            intent.putExtra(EditActivity.PROJECT_EXTRA, projects.indexOf(project))
        }
        this.startActivityForResult(intent, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
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
        openEditActivity(null)
    }

    companion object {
        var database = BabyNameDatabase()
        var projects = ArrayList<BabyNameProject>()
    }
}
