package com.swepe.swepe

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_NONE
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.media.MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.GestureDetectorCompat
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs


class MainActivity : AppCompatActivity() {

    var active_meme : Meme? = null
    var volume = false
    var video_is_playing = false
    var video_has_audio = false
    var mediaplayer : MediaPlayer? = null


    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        if (Client.registered)
            menuInflater.inflate(R.menu.menu, menu)
        else
            menuInflater.inflate(R.menu.unregistered_menu, menu)
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        if (requestCode == 1) { //image/video for upload was chosen
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
                //startActivity(new_intent)
                startActivityForResult(Intent.createChooser(new_intent, "Choose a title"), 3)

            }
        } else if (requestCode == 2) { //register

        } else if (requestCode == 3){ //upload
            if (resultCode == Activity.RESULT_OK) {
                Snackbar.make(main_layout, "Upload successful", LENGTH_SHORT).show()
            }
            else if(resultCode == Activity.RESULT_CANCELED) {
                Snackbar.make(main_layout, "Upload canceled", LENGTH_SHORT).show()
            }
            else {
                Snackbar.make(main_layout, "Something went wrong with your upload", LENGTH_LONG).show()
            }
        }

        super.onActivityResult(requestCode, resultCode, intent)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.upload -> {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            val mimetypes =
                arrayOf("image/*", "video/*")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
            startActivityForResult(Intent.createChooser(intent, "Upload a picture or a video"), 1)
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

        R.id.report -> {
            if (active_meme != null) {
                Client.report(applicationContext, active_meme!!.itemID, 0, {
                    Snackbar.make(main_layout, "We have received your report.", LENGTH_LONG).show()
                }, {
                    Snackbar.make(
                        main_layout,
                        "We were not able to send your report, please try again later.",
                        LENGTH_LONG
                    ).show()
                })
            }

            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (active_meme != null)
            outState.putInt("active_meme_id", active_meme!!.itemID)
        outState.putBoolean("volume", volume)
    }

    fun pick_some_meme(): Pair<Int, Meme>? {
        if (Client.memeimgs.size < 5) {
            Client.get_new_memes(applicationContext) {}
        }
        for ((key, value) in Client.memeimgs) {
            //TODO: what happens if the rating does not get trough?
            //Client.memeimgs.remove(key)

            return Pair(key, value)
        }
        return null
        //throw Exception("trying to pick meme from empty memeimgs")
    }

    fun memeView(meme: Meme?) : View {
        if (meme is ImageMeme)
            return imageView
        else if (meme is VideoMeme)
            return videoView
        else
            return imageView
    }

    var mFocusRequest : AudioFocusRequest? = null

    fun abandonAudioFocus(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            mFocusRequest?.let {
                val mAudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                mAudioManager.abandonAudioFocusRequest(it)
            }
            mFocusRequest = null
        }
    }

    fun maybePlayAudio(){
        if (!volume){
            mediaplayer?.setVolume(0f, 0f)
            abandonAudioFocus()
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && volume){
            val mAudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val mPlaybackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()

            mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(mPlaybackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener({}, Handler())
                .build()
            val mMediaPlayer = MediaPlayer()
            mMediaPlayer.setAudioAttributes(mPlaybackAttributes)
            val mFocusLock = Any()

            // requesting audio focus
            val res: Int = mAudioManager.requestAudioFocus(mFocusRequest!!)
            synchronized(mFocusLock) {
                if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                    mediaplayer?.setVolume(0f,0f)
                } else if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    if (volume)
                        mediaplayer?.setVolume(1f, 1f)
                    else
                        mediaplayer?.setVolume(0f, 0f)
                }
                else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
                }
                else {}
            }

        }

    }


    fun display_meme(meme: Meme){
        videoView.visibility = View.GONE
        imageView.visibility = View.GONE

        if (meme is ImageMeme) {
            imageView.setImageBitmap(meme.bitmap)
            videoView.stopPlayback()
            mute_button.visibility = View.INVISIBLE
            video_is_playing = false

            abandonAudioFocus()

        } else if(meme is VideoMeme) {
            video_is_playing = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !volume)
                videoView.setAudioFocusRequest(AUDIOFOCUS_NONE);

            videoView.setVideoPath(meme.file.absolutePath)
        }

        memeView(meme).visibility = View.VISIBLE
        //meme_container.bringToFront()
        //author.bringToFront()
        //meme_title.bringToFront()
        //mute_button.bringToFront()
        if (meme.author != null) {
            author.text = meme.author
            author.visibility = View.VISIBLE
        } else {
            author.visibility = View.INVISIBLE
        }

        if (meme.title != null) {
            meme_title.text = meme.title
            meme_title.visibility = View.VISIBLE
        } else {
            meme_title.visibility = View.INVISIBLE
        }

    }


    fun audio_present(mediaPlayer: MediaPlayer): Boolean {
        for ( track_info in mediaPlayer.trackInfo ){
            if (track_info.trackType ==  MEDIA_TRACK_TYPE_AUDIO)
                return true
        }
        return false
    }

    @ExperimentalStdlibApi
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar2))



        val bigView = findViewById<View>(R.id.main_layout)
        ratingvisual.alpha = 0f
        author.visibility = View.INVISIBLE

        mediaplayer = null

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("active_meme_id")) {
                val active_meme_id = savedInstanceState.getInt("active_meme_id")
                active_meme = Client.memeimgs[active_meme_id]
            }
            if (savedInstanceState.containsKey("volume")) {
                volume = savedInstanceState.getBoolean("volume")
            }
        }

        videoView.setOnPreparedListener(OnPreparedListener { mp ->
            mediaplayer = mp
            maybePlayAudio()
            mp.isLooping = true
            if (video_is_playing) {
                videoView.start()
            }

            if (audio_present(mp)) {
                video_has_audio = true
                mute_button.visibility = if (!volume) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            } else {
                video_has_audio = false
            }
        })




        val setup = {

            var lastx = 0f
            var defaultx = 0f

            val update_meme = {
                active_meme?.let { Client.popMeme(it.itemID) }


                val p = pick_some_meme()
                val m = p?.second
                active_meme = m

                if (active_meme != null)
                    display_meme(active_meme!!)
                else {
                    imageView.setImageResource(R.drawable.outofmemes)
                    imageView.visibility = View.VISIBLE
                    //imageView.bringToFront()
                }
            }

            if(active_meme == null){
                main_progressBar.visibility = View.VISIBLE
                Client.get_new_memes(applicationContext) {
                    update_meme()
                    main_progressBar.visibility = View.INVISIBLE
                }
            }
            else{
                display_meme(active_meme!!)
            }


            class ImageTouchListener(ctx: Context) : GestureDetector.OnGestureListener {
                var mDetector: GestureDetectorCompat = GestureDetectorCompat(ctx, this)

                override fun onShowPress(p0: MotionEvent?) {
                }

                override fun onSingleTapUp(p0: MotionEvent?): Boolean {
                    if(mediaplayer != null && video_has_audio && active_meme is VideoMeme){
                        volume = !volume
                        mute_button.visibility = if (!volume) { View.VISIBLE } else {View.INVISIBLE}

                        maybePlayAudio()
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
                    if (abs(vX) > abs(vY) && abs(vX) > 20) {

                        ratingvisual.alpha = 1f
                        ratingvisual.scaleX = 1f
                        ratingvisual.scaleY = 1f
                        ratingvisual.animate().scaleX(2f)
                        ratingvisual.animate().scaleY(2f)
                        ratingvisual.animate().alpha(0f)

                        if (vX > 0) {
                            ratingvisual.setImageResource(R.drawable.herz)
                            if (active_meme != null)
                                Client.rate_meme(applicationContext, active_meme!!, 2)
                        } else {
                            ratingvisual.setImageResource(R.drawable.herzbroke)
                            if (active_meme != null)
                                Client.rate_meme(applicationContext, active_meme!!, 1)
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

            bigView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        defaultx = memeView(active_meme).x
                        memeView(active_meme).animate().cancel()
                        lastx = event.rawX
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {

                        val dx = event.rawX - lastx
                        lastx = event.rawX
                        memeView(active_meme).x += dx
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        memeView(active_meme).animate().x(defaultx)
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

        if (!getSharedPreferences(
                "client",
                Context.MODE_PRIVATE
            ).getBoolean("not_first_start", false)){
            intent = Intent(this, FirstStart::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
        else if(Client.hasLocalLogin(applicationContext)) {
            if(!Client.logged_in)
                Client.connect(applicationContext, { setup() }, {})
            else
                setup()
        }
        else{
            intent = Intent(this, NoLocalAccount::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

    }


}