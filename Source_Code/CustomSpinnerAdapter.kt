package com.example.pro_impact_hub

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

class CustomSpinnerAdapter(context: Context, resource: Int, objects: List<String>) :
    ArrayAdapter<String>(context, resource, objects) {

    override fun isEnabled(position: Int): Boolean {
        // Disable the first item from Spinner
        // First item will be used for hint
        return position != 0
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val tv = view as TextView

        // Set the hint text color gray
        if (position == 0) {
            tv.setTextColor(ContextCompat.getColor(context, R.color.gray))
        } else {
            tv.setTextColor(ContextCompat.getColor(context, R.color.black))
        }
        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val tv = view as TextView

        // Set the hint text color gray
        if (position == 0) {
            tv.setTextColor(ContextCompat.getColor(context, R.color.gray))
        } else {
            tv.setTextColor(ContextCompat.getColor(context, R.color.black))
        }
        return view
    }
}
