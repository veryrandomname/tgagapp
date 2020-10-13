package com.tgag.tgag

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Button
import kotlinx.android.synthetic.main.activity_upload.*

class Upload : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        setSupportActionBar(findViewById(R.id.toolbar4))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val imguri: Uri = intent.extras?.get("imguri") as Uri

        upload_preview.setImageURI(imguri)

        val button: Button = findViewById(R.id.upload_button)
        button.setOnClickListener {
            fileUpload(imguri, meme_title_input.text.toString(), show_username_switch.isChecked)
            finish()
        }
    }

    private fun fileUpload(imgUri: Uri, title: String, show_username : Boolean) {
        val filetype =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(imgUri))


        Client.file_upload(
            applicationContext,
            contentResolver.openInputStream(imgUri)!!,
            filetype!!,
            title,
            show_username
        )
    }
}