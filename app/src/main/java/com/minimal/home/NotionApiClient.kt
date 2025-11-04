package com.minimal.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NotionApiClient(
    private val apiKey: String,
    private val databaseId: String
) {

    suspend fun fetchUpcomingEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.notion.com/v1/databases/$databaseId/query")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Notion-Version", "2022-06-28")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Query for events starting from today
            val requestBody = JSONObject().apply {
                put("page_size", 10)
                put("sorts", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("property", "Date")
                        put("direction", "ascending")
                    })
                })
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseNotionResponse(response)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseNotionResponse(jsonResponse: String): List<CalendarEvent> {
        try {
            val json = JSONObject(jsonResponse)
            val results = json.getJSONArray("results")
            val events = mutableListOf<CalendarEvent>()

            for (i in 0 until results.length()) {
                val page = results.getJSONObject(i)
                val properties = page.getJSONObject("properties")

                // Extract title (assuming there's a "Name" or "Title" property)
                val title = extractTitle(properties)

                // Extract date (assuming there's a "Date" property)
                val (startTime, endTime, isAllDay) = extractDate(properties)

                if (title.isNotEmpty()) {
                    events.add(CalendarEvent(title, startTime, endTime, isAllDay))
                }
            }

            return events
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun extractTitle(properties: JSONObject): String {
        // Try common title property names
        val titleKeys = listOf("Name", "Title", "Event", "Task")
        for (key in titleKeys) {
            if (properties.has(key)) {
                val titleProp = properties.getJSONObject(key)
                val type = titleProp.getString("type")

                return when (type) {
                    "title" -> {
                        val titleArray = titleProp.getJSONArray("title")
                        if (titleArray.length() > 0) {
                            titleArray.getJSONObject(0).getString("plain_text")
                        } else ""
                    }
                    "rich_text" -> {
                        val richTextArray = titleProp.getJSONArray("rich_text")
                        if (richTextArray.length() > 0) {
                            richTextArray.getJSONObject(0).getString("plain_text")
                        } else ""
                    }
                    else -> ""
                }
            }
        }
        return ""
    }

    private fun extractDate(properties: JSONObject): Triple<Date?, Date?, Boolean> {
        try {
            if (properties.has("Date")) {
                val dateProp = properties.getJSONObject("Date")
                if (!dateProp.isNull("date")) {
                    val dateObj = dateProp.getJSONObject("date")
                    val startStr = dateObj.optString("start", "")
                    val endStr = dateObj.optString("end", "")

                    if (startStr.isNotEmpty()) {
                        val isAllDay = !startStr.contains("T")
                        val dateFormat = if (isAllDay) {
                            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        } else {
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                        }

                        val startDate = dateFormat.parse(startStr)
                        val endDate = if (endStr.isNotEmpty()) dateFormat.parse(endStr) else null

                        return Triple(startDate, endDate, isAllDay)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Triple(null, null, false)
    }
}
