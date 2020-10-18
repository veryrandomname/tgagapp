package com.tgag.tgag

import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_upload.*

class Upload : AppCompatActivity() {
    var current_upload_request : UploadRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        setSupportActionBar(findViewById(R.id.toolbar4))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        upload_preview_video.setOnPreparedListener(MediaPlayer.OnPreparedListener { mp ->
            mp.setVolume(0f, 0f)
            mp.isLooping = true
            mp.start()
        })


        val upload_uri: Uri = intent.extras?.get("imguri") as Uri

        if (contentResolver.getType(upload_uri)!!.substringBefore('/') != "video"){
            upload_preview_image.setImageURI(upload_uri)
            upload_preview_video.visibility = View.INVISIBLE
        }
        else {
            upload_preview_video.setVideoURI(upload_uri)
            upload_preview_image.visibility = View.INVISIBLE
        }

        val button: Button = findViewById(R.id.upload_button)
        button.setOnClickListener {
            button.setOnClickListener {}

            current_upload_request = fileUpload(upload_uri, meme_title_input.text.toString(), show_username_switch.isChecked)
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
            { _ ->
                setResult(Activity.RESULT_OK)
                finish()
            },
            { _ ->
                setResult(Activity.RESULT_FIRST_USER)
                finish()
            }
        )
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle arrow click here
        if (item.itemId == android.R.id.home) {
            current_upload_request?.cancel()
            setResult(Activity.RESULT_CANCELED)
            finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }
}