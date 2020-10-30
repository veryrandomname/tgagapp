package com.swepe.swepe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.swepe.swepe.Client.deleteLocalLogin
import kotlinx.android.synthetic.main.activity_login.*

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
            Client.login(applicationContext, username, password,
                { response ->
                    deleteLocalLogin(applicationContext)
                    Client.setLocalLogin(applicationContext, username, password, true)
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.putExtra("logged_in", true)
                    Client.logged_in
                    startActivity(intent)
                    finish()
                },
                { error ->
                    login_textView.text = "Login Error"
                    login_textView.setTextColor(Color.RED)
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