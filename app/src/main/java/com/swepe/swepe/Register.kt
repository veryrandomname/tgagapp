package com.swepe.swepe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_register.*



class Register : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setSupportActionBar(findViewById(R.id.toolbar3))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val button: Button = findViewById(R.id.register_button)
        button.setOnClickListener {

            Client.register(applicationContext,
                username_input.text.toString(),
                password_input.text.toString(),
                {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.putExtra("logged_in", true)
                    startActivity(intent)
                    finish()
                },
                {
                    textView3.text = "Username taken"
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