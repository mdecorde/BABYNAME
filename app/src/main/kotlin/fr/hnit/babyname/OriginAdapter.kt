package fr.hnit.babyname

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView
import java.util.Collections

class OriginAdapter(
    val origins: ArrayList<String?>,
    mContext: Context,
    private val onSelectionChange: () -> Unit
) : ArrayAdapter<Any?>(mContext, R.layout.item_origin, listOf<Any>(origins)) {

    val checked: ArrayList<Boolean> = ArrayList(
        Collections.nCopies(
            origins.size, java.lang.Boolean.FALSE
        )
    )

    override fun getPosition(origin: Any?): Int {
        for (i in origins.indices) {
            if (origins[i] == origin) {
                return i
            }
        }
        return -1
    }

    override fun getCount(): Int {
        return origins.size
    }

    override fun getItem(position: Int): String? {
        return origins[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val newConvertView = if (convertView == null) {
            LayoutInflater.from(context).inflate(R.layout.item_origin, parent, false)
        } else {
            convertView
        }

        val txtName = newConvertView.findViewById<TextView>(R.id.txtName)
        val checkBox = newConvertView.findViewById<CheckBox>(R.id.checkBox)

        txtName.text = origins[position]
        checkBox.isChecked = checked[position]

        newConvertView.setOnClickListener { v: View? ->
            val newChecked = !checkBox.isChecked
            checkBox.isChecked = newChecked
            checked[position] = newChecked
            onSelectionChange()
        }

        return newConvertView
    }
}
