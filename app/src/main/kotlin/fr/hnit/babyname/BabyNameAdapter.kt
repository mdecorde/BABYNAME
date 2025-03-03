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

class BabyNameAdapter(private val main: MainActivity, private val itemsArrayList: ArrayList<BabyNameProject>) :
    ArrayAdapter<BabyNameProject>(main, R.layout.item_project, itemsArrayList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val project = itemsArrayList[position]

        // 1. Create inflater
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // 2. Get rowView from inflater
        val rowView = inflater.inflate(R.layout.item_project, parent, false)

        // 3. Get the two text view from the rowView
        val text = rowView.findViewById<TextView>(R.id.list_text)
        val goButton = rowView.findViewById<ImageButton>(R.id.list_go)
        goButton.setOnClickListener { view: View? -> main.doFindName(project) }
        val resetButton = rowView.findViewById<ImageButton>(R.id.list_reset)
        resetButton.setOnClickListener { view: View? -> main.doResetBaby(project) }
        val topButton = rowView.findViewById<ImageButton>(R.id.list_top)
        topButton.setOnClickListener { view: View? -> main.doShowTop10(project) }
        val deleteButton = rowView.findViewById<ImageButton>(R.id.list_delete)
        deleteButton.setOnClickListener { view: View? -> main.doDeleteBaby(project) }

        // 4. Set the text for textView
        text.text = main.projectToString(project)

        // 5. return rowView
        return rowView
    }
}
