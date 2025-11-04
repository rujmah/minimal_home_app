package com.minimal.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class CalendarEventAdapter(private var events: List<CalendarEvent>) :
    RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder>() {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventTitle: TextView = view.findViewById(R.id.eventTitle)
        val eventTime: TextView = view.findViewById(R.id.eventTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_event_item, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.eventTitle.text = event.title

        if (event.isAllDay) {
            holder.eventTime.text = "All day"
        } else if (event.startTime != null) {
            val timeStr = timeFormat.format(event.startTime)
            val dateStr = dateFormat.format(event.startTime)
            holder.eventTime.text = "$dateStr at $timeStr"
        } else {
            holder.eventTime.text = "No time"
        }
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<CalendarEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
