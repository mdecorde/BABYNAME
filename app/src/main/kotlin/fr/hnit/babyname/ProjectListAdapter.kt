package fr.hnit.babyname

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView

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

class ProjectListAdapter(private val main: MainActivity, private val itemsArrayList: ArrayList<BabyNameProject>) :
    ArrayAdapter<BabyNameProject>(main, R.layout.item_project, itemsArrayList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val project = itemsArrayList[position]

        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val rowView = inflater.inflate(R.layout.item_project, parent, false)

        val text = rowView.findViewById<TextView>(R.id.list_text)
        text.text = main.projectToString(project)

        rowView.findViewById<ImageButton>(R.id.list_scroll_find)
            .setOnClickListener { main.doScrollSearch(project) }

        rowView.findViewById<ImageButton>(R.id.list_flip_find)
            .setOnClickListener { main.doFlipSearch(project) }

        rowView.findViewById<ImageButton>(R.id.list_delete)
            .setOnClickListener { main.doDeleteProject(project) }

        rowView.findViewById<ImageButton>(R.id.list_top)
            .setOnClickListener { main.doShowTop10(project) }

        rowView.isLongClickable = true

        return rowView
    }
}
