package com.example.coviderttracking

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.spark.SparkView
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {


    private lateinit var currentShowData: List<CovidData>
    private lateinit var perStatesDailyData: Map<String, List<CovidData>>
    private lateinit var natoinalDataDaliy: List<CovidData>
    private val BASE_URL = "https://api.covidtracking.com/"
    private var adapter: CovidSparkAdapter? = null
    private val ALl_STATES = "ALL STATES"

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:s").create()
        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        val covidServer = retrofit.create(CovidServer::class.java)


        covidServer.getNationalData().enqueue(object: Callback<List<CovidData>> {

            override fun onFailure(call: retrofit2.Call<List<CovidData>>?, t: Throwable?) {
                Log.e("failure getNationalData","error",t)
            }

            override fun onResponse(call: retrofit2.Call<List<CovidData>>?, response: Response<List<CovidData>>?) {
                val natoinalData = response?.body()

                if (natoinalData != null) {
                     setupEventListern()
                     natoinalDataDaliy = natoinalData.reversed()
                     updateUIwithData(natoinalDataDaliy)
                     Log.v("api 2 CHECK","how taking time")
                    Toast.makeText(this@MainActivity, "api 1" +natoinalDataDaliy.size,Toast.LENGTH_LONG).show()
                }else{
                    Toast.makeText(this@MainActivity,response.toString(),Toast.LENGTH_SHORT).show()
                    return
                }
            }
        })

        covidServer.getStatesData().enqueue(object: Callback<List<CovidData>>{
            override fun onFailure(call: Call<List<CovidData>>?, t: Throwable?) {
                Log.e("getStatesData failure","error",t)
            }

            override fun onResponse(call: Call<List<CovidData>>?, response: Response<List<CovidData>>?) {
                val statesData = response?.body()

                if (statesData != null){
                    Log.v("api 2 CHECK","how taking time")
                    perStatesDailyData = statesData.reversed().groupBy { it.state }
                    updateSpinnerWithData(perStatesDailyData.keys)
                }
            }

        })
    }

    private fun updateSpinnerWithData(statesNames: Set<String>) {
        val statesNamesList = statesNames.toMutableList()
        statesNamesList.sort()
        statesNamesList.add(0, ALl_STATES )
        Toast.makeText(this@MainActivity, "api 2" +statesNamesList,Toast.LENGTH_LONG).show()
        selectSpinner.attachDataSource(statesNamesList)
        selectSpinner.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedStates = parent.getItemAtPosition(position) as String
            val selectedData =  perStatesDailyData[selectedStates] ?: natoinalDataDaliy
            updateUIwithData(selectedData)
        }


    }

    private fun setupEventListern() {
        spark_view.isScrubEnabled = true
        spark_view.setScrubListener { itemData ->
            if (itemData is CovidData){
                updateTimeAndDate(itemData)
            }
        }
        radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId: Int ->
                adapter?.daysAgo = when(checkedId){
                    R.id.btnWeek -> TimeScale.WEEK
                    R.id.btnMonth -> TimeScale.MONTH
                    else -> TimeScale.MAX
                }
                adapter?.notifyDataSetChanged()
        }

        radioGroupMetricSelection.setOnCheckedChangeListener{_, checkid ->
            when(checkid){
                R.id.btnPostive ->  updateDispalyWithMetric(Metric.POSITIVE)
                R.id.btnNegtaive ->  updateDispalyWithMetric(Metric.NEGATIVE)
                R.id.btnDeath ->  updateDispalyWithMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDispalyWithMetric(data: Metric) {
        val color = when(data){
            Metric.NEGATIVE -> R.color.negetive
            Metric.POSITIVE -> R.color.positive
            Metric.DEATH -> R.color.death
        }
        spark_view.lineColor = color
        tvMetricLabel.setTextColor(color)


        adapter?.metric = data
        adapter?.notifyDataSetChanged()

        updateTimeAndDate(currentShowData.last())
    }

    private fun updateUIwithData(dataDaliy: List<CovidData>) {
         currentShowData = dataDaliy
        adapter = CovidSparkAdapter(dataDaliy)
        spark_view?.adapter = adapter
        btnPostive.isChecked = true;
        btnMax.isChecked = true;
        updateTimeAndDate(dataDaliy.last())
    }

    private fun updateTimeAndDate(covidData: CovidData) {
        val numCases = when (adapter?.metric){
            Metric.POSITIVE -> covidData.negativeIncrease
            Metric.NEGATIVE -> covidData.positiveIncrease
            Metric.DEATH    -> covidData.deathIncrease
            else -> covidData.positiveIncrease
        }
        tvMetricLabel.text = NumberFormat.getInstance().format(numCases)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.UK)
        tvDateLabel.text = dateFormat.format(covidData.dateChecked)
    }
}








