package com.tgag.tgag

import android.app.Activity
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
            // Do something in response to button click

            val url = "$baseurl/merge_app_user"
            val jsonBody = JSONObject()

            //val pref = getPreferences(Context.MODE_PRIVATE)
            //jsonBody.put("old_username", pref.getString("id", null))
            jsonBody.put("old_username", intent.getStringExtra("old_username"))
            jsonBody.put("username", username_input.text)
            jsonBody.put("password", password_input.text)

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                Response.Listener { response ->
                    val resultIntent = Intent()
                    resultIntent.putExtra("new_id", username_input.text)
                    resultIntent.putExtra("new_pw", password_input.text)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                },
                Response.ErrorListener { error ->
                    // TODO: Handle error
                    textView3.text = "Username taken"
                    textView3.setTextColor(Color.RED)
                }
            )

            Client.getQueue(applicationContext).add(jsonObjectRequest)
        }

    }

}