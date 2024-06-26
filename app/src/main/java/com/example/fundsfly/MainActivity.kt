package com.example.fundsfly

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity";

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bt_send = findViewById<Button>(R.id.send_main)
        val balance = findViewById<TextView>(R.id.Balance)
        val bt_receive = findViewById<Button>(R.id.receive_main)
        val refresh = findViewById<ImageView>(R.id.refresh)


        checkOnline()
        refresh.setOnClickListener {
            checkOnline()
        }
        bt_send.setOnClickListener {
            val isOnline = checkOnline()
            // Use the value of isOnline as needed
            if (isOnline) {
                val sharedPreferences = getSharedPreferences("online_status", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putBoolean("is_online", isOnline)
                editor.apply()

                val intent = Intent(this, overseasTransfer::class.java)
                intent.putExtra("isOnline", isOnline)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                val sharedPreferences = getSharedPreferences("online_status", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putBoolean("is_online", false)
                editor.apply()
                Onlocation()
            }

        }
        val buttonClick2 = findViewById<Button>(R.id.buy_token_main)
        bt_receive.setOnClickListener {
            val intent = Intent(this, RecieveActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        val sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor= sharedPreferences.edit()
        val accountID = sharedPreferences.getString("accountID", "")
        Log.d(TAG,"accountID ${accountID}")

        if(hasNetworkConnection(this)){
            val queue = Volley.newRequestQueue(this)
            val url = "http://ec2-54-89-83-185.compute-1.amazonaws.com/balance"
            val stringRequest = object : StringRequest(Method.POST, url,
                Response.Listener { response ->
                    run {
                        val jsonObject = JSONObject(response)
                        val bal = jsonObject.getString("message")
                        Log.d(TAG, "onCreate: Main $bal")
                        editor.putString("balance", bal)
                        balance.text = sharedPreferences.getString("balance", "0")
                    }
                },
                Response.ErrorListener { error ->
                    run {
//                        Toast.makeText(applicationContext, "$error", Toast.LENGTH_SHORT).show()
//                        Log.d(TAG, "$error")
                    }
                }) {
                override fun getParams(): MutableMap<String, String>? {
                    val params = HashMap<String, String>()
                    params["address"] = accountID.toString()
                    Log.d(TAG,"params:${params}")
                    return params
//

                    }



                override fun getHeaders(): Map<String, String>? {
                    val headers: MutableMap<String, String> = HashMap()
                    headers["Content-Type"] = "application/json"

                    return headers
                }

            }
            queue.add(stringRequest)
        }

        buttonClick2.setOnClickListener {
            if(!hasNetworkConnection(this)){
                Toast.makeText(this, "No Internet Connection, Please try again.", Toast.LENGTH_SHORT).show()

            }
            else{
                val intent = Intent(this, buy_navi::class.java)
                startActivity(intent)
            }



        }





        if (!sharedPreferences.getBoolean("updateb_once", false)) {  //test
            editor.putBoolean("updateb_once", true)
            editor.putString("balance", "0")
            editor.commit()
        }
        balance.text = sharedPreferences.getString("balance", "")
        CheckPermission()





    }

    private fun CheckPermission() {
        Dexter.withContext(applicationContext)
            .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener, MultiplePermissionsListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {

                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {

                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {

                }

            })
            .check();
    }

    private fun Onlocation(){

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {

            val intent = Intent(this, quantity_entry::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Log.d("MainActivity","task called")

        }

        task.addOnFailureListener { exception ->

            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    val REQUEST_CHECK_SETTINGS =1
                    exception.startResolutionForResult(this@MainActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }

    }

    private fun checkOnline(): Boolean {
        val toggleOnlineStatus = findViewById<ToggleButton>(R.id.toggleOnlineStatus)
        val isConnected = hasNetworkConnection(this)
        toggleOnlineStatus.isChecked = isConnected

        toggleOnlineStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                toggleOnlineStatus.text = "Online"
            } else {
                toggleOnlineStatus.text = "Offline"
            }
        }

        return isConnected
    }
    private fun hasNetworkConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        return false
    }
}