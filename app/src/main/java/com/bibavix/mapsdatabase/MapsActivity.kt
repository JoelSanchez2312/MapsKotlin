package com.bibavix.mapsdatabase

import android.graphics.Color
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_maps.*
import okhttp3.OkHttpClient
import okhttp3.Request

class MapsActivity : AppCompatActivity(){
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    lateinit var mapFragment : SupportMapFragment
    private lateinit var mMap: GoogleMap
    private val markerList:MutableList<LatLng> = mutableListOf()
    lateinit var googleMap: GoogleMap
    private val markerOptions =  MarkerOptions()
    private var cont:Int = 1
    private var dbHelper = DatabaseHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(OnMapReadyCallback {
            googleMap = it
            googleMap.setOnMapClickListener(
                object: GoogleMap.OnMapClickListener{
                    override fun onMapClick(p0: LatLng) {
                        if(cont <= 2){
                            markerOptions.position(p0)
                            markerList.add(p0)
                            if(cont == 2){
                                name_txt.isEnabled = true
                                btn_create.isClickable = true
                                btn_save.isClickable = true
                                generateRoute(markerList)
                            }
                        }
                        cont += 1
                        //   googleMap.clear()
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(p0))
                        googleMap.addMarker(markerOptions)
                    }
                }
            )
        })


        handleInserts()
        handleViewing()
        btn_create.isClickable = false
        btn_save.isClickable = false
        name_txt.isEnabled = false
    }


    private fun generateRoute(markers: MutableList<LatLng>){
        btn_create.setOnClickListener {
            Log.d("generateRoute", "${markers[0]} ${markers[1]}")
            showToast("Generating...")
            val url = getDirectionUrl(markers[0], markers[1])
            GetDirection(url).execute()
        }
    }

    private fun getDirectionUrl(from: LatLng, to: LatLng): String{
        val key= getString(R.string.google_maps_key)
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${from.latitude},${from.longitude}&destination=${to.latitude},${to.longitude}&sensor=false&mode=driving&key=$key"
    }


    private inner class GetDirection(val url : String) : AsyncTask<Void, Void, List<List<LatLng>>>(){
        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body()!!.string()
            Log.d("GoogleMap" , " data : $data")
            val result =  ArrayList<List<LatLng>>()
            try{
                val respObj = Gson().fromJson(data,GoogleMapDTO::class.java)

                val path =  ArrayList<LatLng>()

                for (i in 0..(respObj.routes[0].legs[0].steps.size-1)){
//                    val startLatLng = LatLng(respObj.routes[0].legs[0].steps[i].start_location.lat.toDouble()
//                            ,respObj.routes[0].legs[0].steps[i].start_location.lng.toDouble())
//                    path.add(startLatLng)
//                    val endLatLng = LatLng(respObj.routes[0].legs[0].steps[i].end_location.lat.toDouble()
//                            ,respObj.routes[0].legs[0].steps[i].end_location.lng.toDouble())
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
            }catch (e:Exception){
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineOption = PolylineOptions()
            for (i in result.indices){
                lineOption.addAll(result[i])
                lineOption.width(10f)
                lineOption.color(Color.BLUE)
                lineOption.geodesic(true)
            }
            googleMap.addPolyline(lineOption)
        }
    }

    public fun decodePolyline(encoded: String): List<LatLng> {

        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
            poly.add(latLng)
        }

        return poly
    }

    private fun showToast(text: String){
        Toast.makeText(this@MapsActivity, text, Toast.LENGTH_LONG).show()
    }

    private fun showDialog(title: String, Message: String){
        var builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setTitle(title)
        builder.setMessage(Message)
        builder.show()
    }

    private fun clearMap(){
        googleMap.clear()
        name_txt.setText("")
        btn_save.isClickable = false
        btn_create.isClickable = false
        name_txt.isEnabled = false
        cont = 0
    }

    private fun handleInserts(){
        btn_save.setOnClickListener {
            try {
                if(name_txt.isEnabled && name_txt.text.toString()!=""){
                    dbHelper.insertData(name_txt.text.toString(),markerList[0].latitude,markerList[0].longitude,  markerList[1].latitude,markerList[1].longitude)
                    clearMap()
                }else{
                    showDialog("Warning!","Name is required.")
                }

            }catch (e: Exception){
                e.printStackTrace()
                showToast(e.message.toString())
            }
        }
    }

    private fun handleViewing(){
        btn_view.setOnClickListener {

            val res = dbHelper.allData
            if(res.count == 0){
                showDialog("Warning", "No Data Found")
            }

            val buffer = StringBuffer()
            while (res.moveToNext()){
                buffer.append("ID: " + res.getString(0) +"\n")
                buffer.append("NAME: "+ res.getString(1) +"\n")
                buffer.append("FROM: " + res.getString(2) + " " + res.getString(3)+"\n")
                buffer.append("TO: "+res.getString(3) + " " + res.getString(4)+"\n")
            }
            showDialog("Routes: ", buffer.toString())
        }
    }



}