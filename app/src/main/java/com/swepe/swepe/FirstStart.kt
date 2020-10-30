package com.swepe.swepe

import android.content.Intent
import android.content.res.loader.ResourcesLoader
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_first_start.*


class FirstStart : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_start)

        val string = SpannableString( resources.getString(R.string.tos_accept))
        string.setSpan(
            URLSpan("${Client.baseurl}/tos"),
            47,
            63,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        string.setSpan(
            URLSpan("${Client.baseurl}/pp"),
            69,
            83,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tosview.text = string;
        tosview.movementMethod = LinkMovementMethod.getInstance(); // enable clicking on url span


        no_account.setOnClickListener {
            Client.connect(applicationContext) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }


        login_first.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        register_first.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
    }
}