package lv.peisenieks

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartUtilities
import org.jfree.chart.JFreeChart
import org.jfree.data.time.Day
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.data.xy.XYDataset
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset


class DataGrapher
(dest: File, langJson: String?) {

    init {

        val dataset = parseLanguageStats(langJson)
        val chart = createChart(dataset)
        val width=2000
        val height=800

        ChartUtilities.saveChartAsJPEG(dest, chart, width, height)

    }

    /**
     * Creates a sample dataset.

     * @return a time-based set {@link TimeSeriesCollection} of series .
     */
    private fun parseLanguageStats(langJson: String?): XYDataset {
        val coll = TimeSeriesCollection()
        if (langJson != null) {
            val datesWithData = Gson().fromJson<Map<String, Map<String, String>>>(langJson).toList().reversed()
            val series: MutableMap<String, TimeSeries> = mutableMapOf()

            var lastValueUsed: Pair<String, Map<String, String>>? = null
            var currentIndex: Int = 0
            val startingDate = LocalDateTime.parse(datesWithData.get(0).first)
            val requiredNumOfIterations = (LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - startingDate.toEpochSecond(ZoneOffset.UTC)) / 86400
            var currentDay = Day(startingDate.dayOfMonth, startingDate.monthValue, startingDate.year)

            for (i in 0..requiredNumOfIterations - 1) {

                // JFreeCharts allows us to just iterate from one day to the next. This is cool, but if we don't have
                // a day with data for each of those iterations, we want to use data we used for the previous data point
                val currentParsedDate = LocalDateTime.parse(datesWithData.get(currentIndex).first)
                val convertedDate = Day(currentParsedDate.dayOfMonth, currentParsedDate.monthValue, currentParsedDate.year)
                if (currentDay == convertedDate) {
                    currentIndex = Math.min(currentIndex + 1, datesWithData.size - 1)
                    lastValueUsed = datesWithData.get(currentIndex)
                }

                val dataToGraph = lastValueUsed
                // We need to make sure that every language has a series attached to it. If not - create one
                dataToGraph?.second?.entries?.forEach {
                    if (!series.containsKey(it.key)) {
                        series.put(it.key, TimeSeries(it.key))
                    }
                    series.get(it.key)?.add(currentDay, it.value.toDouble())
                }
                currentDay = currentDay.next() as Day

            }

            series.forEach {
                coll.addSeries(it.value)
            }
        }
        return coll
    }

    /**
     * Creates a chart.

     * @param dataset  the data for the chart.
     * *
     * *
     * @return a chart.
     */
    private fun createChart(dataset: XYDataset): JFreeChart {
        return ChartFactory.createTimeSeriesChart(
                "Language Distribution",
                "Date",
                "Bytes of code in language",
                dataset,
                true,
                false,
                false)
    }
}
