package com.example.coviderttracking

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(val dataDaily: List<CovidData>) : SparkAdapter() {

    var metric = Metric.POSITIVE
    var daysAgo = TimeScale.MAX

    override fun getCount() = dataDaily.size

    override fun getItem(index: Int) = dataDaily[index]

    override fun getY(index: Int): Float {
        val chosenDayData = dataDaily[index]
        return when (metric){
            Metric.NEGATIVE ->  chosenDayData.negativeIncrease.toFloat()
            Metric.POSITIVE -> chosenDayData.positiveIncrease.toFloat()
            Metric.DEATH -> chosenDayData.deathIncrease.toFloat()
        }
    }

    override fun getDataBounds(): RectF {
        val bounds =  super.getDataBounds()
        if (daysAgo != TimeScale.MAX)
            bounds.left = count - daysAgo.numDays.toFloat()
        return bounds
    }


}
