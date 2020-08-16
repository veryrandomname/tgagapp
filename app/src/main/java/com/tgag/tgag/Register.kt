package com.tgag.tgag

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.android.synthetic.main.activity_register.*
import org.json.JSONObject


class Register : AppCompatActivity() {
    //private val baseurl: String = "https://tgag.app"
    private val baseurl: String = "http://192.168.1.116:5000"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setSupportActionBar(findViewById(R.id.toolbar3))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val button: Button = findViewById(R.id.register_button)
        button.setOnClickListener {

            Client.merge_user(applicationContext, username_input.text.toString(), password_input.text.toString(),
                Response.Listener { response ->
                    finish()
                },
                Response.ErrorListener { error ->
                    textView3.text = "Username taken"
                    textView3.setTextColor(Color.RED)
                })
        }

    }

}