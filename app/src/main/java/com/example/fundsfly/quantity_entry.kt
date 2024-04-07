package com.example.fundsfly


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.ObjectsCompat.hash
import com.google.android.material.textfield.TextInputEditText
import okhttp3.internal.tls.OkHostnameVerifier.verify
import org.bouncycastle.asn1.ASN1Primitive.fromByteArray
import org.bouncycastle.crypto.params.ECKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.rfc8032.Ed25519.verify
import org.bouncycastle.math.ec.rfc8032.Ed448.verify
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils.fromHexString
import org.bouncycastle.util.Strings.fromByteArray
import org.objectweb.asm.util.CheckClassAdapter.verify
import org.web3j.crypto.*
import org.web3j.crypto.Wallet.decrypt
import org.web3j.utils.Numeric
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Hash.hash
import org.web3j.crypto.Sign
import org.web3j.crypto.Wallet.create
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.Security
import java.util.Objects.hash
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject


class quantity_entry : AppCompatActivity() {
    private val TAG = "amountentry"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quantity_entry)
        var sharedPreferences = getSharedPreferences("online_status", MODE_PRIVATE)
        val isOnline = sharedPreferences.getBoolean("is_online",false)
        val editor = sharedPreferences.edit()
        val accountID = sharedPreferences.getString("accountID", "")
        val amount = findViewById<TextInputEditText>(R.id.amount_send)
        val back_button = findViewById<ImageView>(R.id.back_button_amount)
        val send_button_amount = findViewById<Button>(R.id.send_button_amount)


        send_button_amount.setOnClickListener {
            if (isOnline){
                val sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE)
                val balance = sharedPreferences.getString("balance", "")

                Log.d(TAG, "Balance: " + balance)
                val amt = amount.text.toString()
                if (balance !=null){
                    if(hasNetworkConnection(this)){
                        val queue = Volley.newRequestQueue(this)
                        val url = "http://ec2-54-89-83-185.compute-1.amazonaws.com/transfer"
                        val stringRequest = object : StringRequest(
                            Request.Method.POST, url,
                            Response.Listener { response ->
                                run {
                                    val jsonObject = JSONObject(response)
                                    val bal = jsonObject.getString("message")
                                    Log.d(TAG, "onCreate: Main $bal")
                                    editor.putString("balance", bal)

                                }
                            },
                            Response.ErrorListener { error ->
                                run {
                                    Toast.makeText(applicationContext, "$error", Toast.LENGTH_SHORT).show()
                                    Log.d(TAG, "$error")
                                }
                            }) {
                            override fun getParams(): Map<String, String> {
                                val params = HashMap<String, String>()
                                params["recipient"] = accountID.toString()
                                params["amount"] = amt

                                return params
                            }

                        }
                        queue.add(stringRequest)
                    }
                }
            }else{

                val sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE)
                val balance = sharedPreferences.getString("balance", "")
                Log.d(TAG, "Balance: " + balance)
                val amt = amount.text.toString()
                if (balance != null) {
                    if (balance.toDouble() >= amt.toDouble()) {
                        var generatedsign = generateSignature("056cb6b3a8f2cc317afd6d425ca8cde2ca867c32f1b372227b4f538101f5bce9", amt)
                        Log.d(TAG,"SIgnature: "+ generatedsign)

                        val intent = Intent(this, send::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra("amount", amount.text.toString())
                        startActivity(intent)
                    } else
                        Toast.makeText(
                            applicationContext,
                            "Insufficient Balance, Please try again!",
                            Toast.LENGTH_SHORT
                        ).show()


                }


            }

        }

        back_button.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }




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

    fun generateSignature(privateKey: String, message: String): String {
        val ecKeyPair = ECKeyPair.create(Numeric.hexStringToByteArray(privateKey))
        val messageHash = Hash.sha3(message.toByteArray())
        val signature = Sign.signMessage(messageHash, ecKeyPair, false)
        val r = Numeric.toHexStringWithPrefixZeroPadded(Numeric.toBigInt(signature.r), 64)
        val s = Numeric.toHexStringWithPrefixZeroPadded(Numeric.toBigInt(signature.s), 64)
        val v = BigInteger(1, signature.v).toString(16)
        return "0x$r$s$v"
    }


}