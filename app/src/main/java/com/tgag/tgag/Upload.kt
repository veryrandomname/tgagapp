package com.tgag.tgag

import android.net.Uri
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import com.android.volley.Response
import kotlinx.android.synthetic.main.activity_upload.*

class Upload : AppCompatActivity() {
    var current_upload_request : UploadRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        setSupportActionBar(findViewById(R.id.toolbar4))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val imguri: Uri = intent.extras?.get("imguri") as Uri

        upload_preview.setImageURI(imguri)


        val button: Button = findViewById(R.id.upload_button)
        button.setOnClickListener {
            button.setOnClickListener {}

            current_upload_request = fileUpload(imguri, meme_title_input.text.toString(), show_username_switch.isChecked)
        }
    }

    private fun fileUpload(imgUri: Uri, title: String, show_username : Boolean): UploadRequest {
        val filetype =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(imgUri))

        upload_progressbar.visibility = View.VISIBLE

        return Client.file_upload(
            applicationContext,
            contentResolver.openInputStream(imgUri)!!,
            filetype!!,
            title,
            show_username,
            Response.Listener{ _ ->
                finish()
            },
            Response.ErrorListener { _ ->
                finish()
            }
        )
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle arrow click here
        if (item.itemId == android.R.id.home) {
            current_upload_request?.cancel()
            finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }
}