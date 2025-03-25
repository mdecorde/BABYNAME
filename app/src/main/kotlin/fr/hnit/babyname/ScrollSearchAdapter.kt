package fr.hnit.babyname

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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

class ScrollSearchAdapter(private val scrollActivity: ScrollSearchActivity, private val project: BabyNameProject)
    : RecyclerView.Adapter<ScrollSearchAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scroll, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val index = project.nexts[position]
        val babyName = MainActivity.database.get(index)

        holder.rankView.text = "${position + 1}."
        holder.textView.text = babyName.name
        holder.rateBar.rating = (project.scores[index] ?: 0).toFloat() / 2.0F
        holder.extraView.text = babyName.getMetaString(scrollActivity.applicationContext)

        holder.rateBar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener {
                ratingBar: RatingBar, rating: Float, fromUser: Boolean ->
            if (fromUser) {
                scrollActivity.onRatingChangeListener(babyName, rating)
            }
        }
    }

    override fun getItemCount(): Int {
        return project.nexts.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankView: TextView = itemView.findViewById(R.id.rankView)
        val rateBar: RatingBar = itemView.findViewById(R.id.rateBar)
        val textView: TextView = itemView.findViewById(R.id.nameView)
        val extraView: TextView = itemView.findViewById(R.id.extraView)
    }
}
