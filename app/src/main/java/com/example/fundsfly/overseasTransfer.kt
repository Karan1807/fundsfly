package com.example.fundsfly

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout

class overseasTransfer : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overseas_transfer)


        val bob_contact = findViewById<LinearLayout>(R.id.bob_ct)
        val dilan_ctc = findViewById<LinearLayout>(R.id.dilan_ct)


        bob_contact.setOnClickListener {
            val intent = Intent(applicationContext, quantity_entry::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        dilan_ctc.setOnClickListener {
            val intent = Intent(applicationContext, quantity_entry::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }



    }
}