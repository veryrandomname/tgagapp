package com.tgag.tgag

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs


class MainActivity : AppCompatActivity() {

    /*
        override fun onCreateOptionsMenu(menu: Menu?): Boolean {
            // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
            // If you don't have res/menu, just create a directory named "menu" inside res
            if (Client.registered)
                menuInflater.inflate(R.menu.menu, menu)
            else
                menuInflater.inflate(R.menu.unregistered_menu, menu)

            return super.onCreateOptionsMenu(menu)
        }
    */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        if (Client.registered)
            menuInflater.inflate(R.menu.menu, menu)
        else
            menuInflater.inflate(R.menu.unregistered_menu, menu)
        return super.onPrepareOptionsMenu(menu)
    }

/*
    private fun upload(uri: Uri) {
        val filetype =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))

        val tmpfilename = "tmp.$filetype"

        openFileOutput(tmpfilename, Context.MODE_PRIVATE).use { stream ->
            val buffer = ByteArray(10240)
            var len: Int = 0
            while (contentResolver.openInputStream(uri)!!.read(buffer).also { len = it } != -1) {
                stream.write(buffer, 0, len)
            }
        }

        val rc = FFmpeg.execute("-i $tmpfilename -c:v mpeg4 file2.mp4")

        if (rc == RETURN_CODE_SUCCESS) {
            Log.i(Config.TAG, "Command execution completed successfully.")
        } else if (rc == RETURN_CODE_CANCEL) {
            Log.i(Config.TAG, "Command execution cancelled by user.")
        } else {
            Log.i(
                Config.TAG,
                String.format("Command execution failed with rc=%d and the output below.", rc)
            )
            Config.printLastCommandOutput(Log.INFO)
        }

        Client.file_upload(
            applicationContext,
            contentResolver.openInputStream(uri)!!,
            filetype!!
        )
    }
*/

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        if (requestCode == 1) { //upload
            if (intent?.clipData != null) {
                /* Disable multiple uploads for now
                val count: Int = intent.clipData!!.itemCount
                for (i in 0 until count) {
                    val imgUri: Uri = intent.clipData!!.getItemAt(i).uri
                    fileUpload(imgUri)
                }
                */

            } else if (intent?.data != null) {
                val new_intent = Intent(this, Upload::class.java)
                val imgUri: Uri = intent.data!!
                new_intent.putExtra("imguri", imgUri)
                startActivity(new_intent)
            }
        } else if (requestCode == 2) { //register
            if (resultCode == Activity.RESULT_OK) {

            }
        }

        super.onActivityResult(requestCode, resultCode, intent)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.upload -> {
            // User chose the "Settings" item, show the app settings UI...
            //val myIntent = Intent(this, Upload::class.java)
            //startActivity(myIntent)

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            val mimetypes =
                arrayOf("image/*", "video/*")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
            //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Upload a picture or a video"), 1)


            //val intent = Intent()
            //intent.type = "image/*"
            //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            //intent.action = Intent.ACTION_GET_CONTENT
            //startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1)


            true
        }

        R.id.my_memes -> {
            val intent = Intent(this, MyUploads::class.java)
            startActivity(intent)
            true
        }

        R.id.register -> {
            val intent = Intent(this, Register::class.java)
            intent.putExtra("old_username", Client.uniqueID)
            startActivityForResult(intent, 2)
            true
        }

        R.id.login -> {
            startActivity(Intent(this, Login::class.java))
            true
        }

        R.id.logout -> {
            Client.deleteLocalLogin(applicationContext)
            val intent = Intent(this, NoLocalAccount::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }
    private lateinit var mediaSession: MediaSession

    @ExperimentalStdlibApi
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar2))

        ratingvisual.alpha = 0f
        author.visibility = View.INVISIBLE

        var mediaplayer : MediaPlayer? = null
        val pref = getSharedPreferences("main",Context.MODE_PRIVATE)
        var volume = false
        var video_is_playing = false

        videoView.setOnPreparedListener(OnPreparedListener { mp ->
            mediaplayer = mp
            if (volume)
                mediaplayer?.setVolume(1f, 1f)
            else
                mediaplayer?.setVolume(0f, 0f)
            mp.isLooping = true
            if(video_is_playing)
                videoView.start()
        })


        //mediaController.setVolumeTo(0,0)

        val setup = {
            var meme : Meme? = null

            var lastx = 0f
            val defaultx = imageView.x
            var defaulty = imageView.y


            fun memeView() : View {
                if (meme is ImageMeme)
                    return imageView
                else if (meme is VideoMeme)
                    return videocontainer
                else
                    return imageView
            }

            fun pick_some_meme(): Pair<Int, Meme>? {
                if (Client.memeimgs.size < 5) {
                    Client.get_new_memes(applicationContext) {}
                }
                for ((key, value) in Client.memeimgs) {
                    //TODO: what happens if the rating does not get trough?
                    Client.memeimgs.remove(key)

                    return Pair(key, value)
                }
                return null
                //throw Exception("trying to pick meme from empty memeimgs")
            }


            val update_meme = {
                val p = pick_some_meme()
                val m = p?.second
                meme = m

                videocontainer.visibility = View.INVISIBLE
                imageView.visibility = View.INVISIBLE

                if (m is ImageMeme) {
                    imageView.setImageBitmap(m.bitmap)
                    videoView.stopPlayback()
                    mute_button.visibility = View.INVISIBLE
                    video_is_playing = false
                } else if(m is VideoMeme) {
                    video_is_playing = true
                    videoView.setVideoPath(m.file.absolutePath)
                    mute_button.visibility = if (!volume) { View.VISIBLE } else {View.INVISIBLE}
                }
                else {
                    imageView.setImageResource(R.drawable.outofmemes)
                }
                memeView().visibility = View.VISIBLE
                memeView().bringToFront()
                author.bringToFront()
                meme_title.bringToFront()
                mute_button.bringToFront()
                if (meme?.author != null) {
                    author.text = meme!!.author
                    author.visibility = View.VISIBLE
                } else {
                    author.visibility = View.INVISIBLE
                }

                if (meme?.title != null) {
                    meme_title.text = meme!!.title
                    meme_title.visibility = View.VISIBLE
                } else {
                    meme_title.visibility = View.INVISIBLE
                }

            }

            Client.get_new_memes(applicationContext) {
                update_meme()
            }


            class ImageTouchListener(ctx: Context) : GestureDetector.OnGestureListener {
                var mDetector: GestureDetectorCompat = GestureDetectorCompat(ctx, this)

                override fun onShowPress(p0: MotionEvent?) {
                }

                override fun onSingleTapUp(p0: MotionEvent?): Boolean {
                    if(mediaplayer != null){
                        volume = !volume
                        mute_button.visibility = if (!volume) { View.VISIBLE } else {View.INVISIBLE}

                        if (volume)
                            mediaplayer!!.setVolume(1f, 1f)
                        else
                            mediaplayer!!.setVolume(0f, 0f)
                    }

                    return true
                }

                override fun onDown(p0: MotionEvent?): Boolean {
                    return true
                }

                override fun onFling(
                    ev1: MotionEvent?,
                    ev2: MotionEvent?,
                    vX: Float,
                    vY: Float
                ): Boolean {
                    if (abs(vX) > abs(vY) && abs(vX) > 10) {

                        ratingvisual.alpha = 1f
                        ratingvisual.scaleX = 1f
                        ratingvisual.scaleY = 1f
                        ratingvisual.animate().scaleX(2f)
                        ratingvisual.animate().scaleY(2f)
                        ratingvisual.animate().alpha(0f)

                        if (vX > 0) {
                            ratingvisual.setImageResource(R.drawable.herz)
                            if (meme != null)
                                Client.rate_meme(applicationContext, meme!!, 2)
                        } else {
                            ratingvisual.setImageResource(R.drawable.herzbroke)
                            if (meme != null)
                                Client.rate_meme(applicationContext, meme!!, 1)
                        }

                        update_meme()

                        ratingvisual.bringToFront()

                    }
                    return true
                }

                override fun onScroll(
                    p0: MotionEvent?,
                    p1: MotionEvent?,
                    p2: Float,
                    p3: Float
                ): Boolean {
                    return true
                }

                override fun onLongPress(p0: MotionEvent?) {
                }

            }


            val imglist = ImageTouchListener(applicationContext)
            val bigView = findViewById<View>(R.id.main_layout)

            bigView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        memeView().animate().cancel()
                        lastx = event.rawX
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {

                        val dx = event.rawX - lastx
                        lastx = event.rawX
                        memeView().x += dx
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        memeView().animate().x(defaultx)
                        false
                    }
                    else -> {
                        false
                    }

                }
                imglist.mDetector.onTouchEvent(event)
            }

            mute_button.setOnClickListener { _ ->
                volume = !volume
                mute_button.visibility = if (!volume) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            }
        }

        if(Client.hasLocalLogin(applicationContext) || !getSharedPreferences("client",Context.MODE_PRIVATE).getBoolean("not_first_start", false))
            if(!intent.getBooleanExtra("logged_in", false))
                Client.connect(applicationContext, setup)
            else
                setup()
        else{
            intent = Intent(this, NoLocalAccount::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

    }


}