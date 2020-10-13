package com.tgag.tgag

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import com.android.volley.Response
import com.tgag.tgag.Client.deleteLocalLogin
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_register.*

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setSupportActionBar(findViewById(R.id.login_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        val button: Button = findViewById(R.id.button)
        button.setOnClickListener {
            val username = username.text.toString()
            val password = password.text.toString()
            Client.login(applicationContext,username , password,
                Response.Listener { response ->
                    deleteLocalLogin(applicationContext)
                    Client.setLocalLogin(applicationContext,username,password ,true)
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                },
                Response.ErrorListener { error ->
                    textView3.text = "Login Error"
                    textView3.setTextColor(Color.RED)
                })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle arrow click here
        if (item.itemId == android.R.id.home) {
            finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }
}