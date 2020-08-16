package com.tgag.tgag

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs
import kotlin.math.pow


data class Meme(val itemID: Int, val bitmap: Bitmap, val author: String)


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
    override fun onPrepareOptionsMenu(menu: Menu) : Boolean {
        menu.clear()
        if (Client.registered)
            menuInflater.inflate(R.menu.menu, menu)
        else
            menuInflater.inflate(R.menu.unregistered_menu, menu)
        return super.onPrepareOptionsMenu(menu);
    }

    private fun fileUpload(imgUri: Uri) {
        val filetype =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(imgUri))

        Client.file_upload(
            applicationContext,
            contentResolver.openInputStream(imgUri)!!,
            filetype!!
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        if (requestCode == 1) { //upload
            if (intent?.clipData != null) {
                val count: Int = intent.clipData!!.itemCount
                for (i in 0 until count) {
                    val imgUri: Uri = intent.clipData!!.getItemAt(i).uri
                    fileUpload(imgUri)
                }
            } else if (intent?.data != null) {
                val imgUri: Uri = intent.data!!
                fileUpload(imgUri)
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

            val intent = Intent()
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1)

            true
        }

        R.id.my_memes -> {
            // User chose the "Favorite" action, mark the current item
            // as a favorite...

            true
        }

        R.id.register -> {
            // User chose the "Favorite" action, mark the current item
            // as a favorite...
            val intent = Intent(this, Register::class.java)
            intent.putExtra("old_username", Client.uniqueID)
            startActivityForResult(intent, 2)
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    @ExperimentalStdlibApi
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar2))


        val t2 = findViewById<TextView>(R.id.textView2)
        val t = findViewById<TextView>(R.id.textView)

        t2.text = ""
        ratingvisual.alpha = 0f
        author.visibility = View.INVISIBLE


        val setup = {
            var frontImage = findViewById<ImageView>(R.id.imageView)
            var backImage = findViewById<ImageView>(R.id.imageView2)
            var frontMeme: Meme? = null
            var backMeme: Meme? = null
            backImage.alpha = 0f

            var lastx = 0f
            val defaultx = frontImage.x
            var defaulty = frontImage.y


            fun pick_some_meme(): Pair<Int, Meme>? {
                if (Client.memeimgs.size < 5) {
                    Client.get_new_memes(applicationContext, frontImage.scaleType) {}
                }
                for ((key, value) in Client.memeimgs) {
                    //TODO: what happens if the rating does not get trough?
                    Client.memeimgs.remove(key)

                    return Pair(key, value)
                }
                return null
                //throw Exception("trying to pick meme from empty memeimgs")
            }

            val swap_memes = {
                frontImage.alpha = 0f
                backImage.alpha = 1f

                frontImage = backImage.also { backImage = frontImage }
                frontMeme = backMeme.also { backMeme = frontMeme }
                frontImage.bringToFront()
                t2.bringToFront()
                t.bringToFront()
                author.bringToFront()
                if (frontMeme != null) {
                    author.text = frontMeme!!.author
                    author.visibility = View.VISIBLE
                } else {
                    author.visibility = View.INVISIBLE
                }
            }

            val update_back_meme = {
                val p = pick_some_meme()

                if (p != null) {
                    backImage.setImageBitmap(p.second.bitmap)
                    backMeme = p.second
                } else {
                    backImage.setImageResource(R.drawable.outofmemes)
                    backMeme = null
                }

            }

            Client.get_new_memes(applicationContext, frontImage.scaleType) {
                update_back_meme()
                swap_memes()
                update_back_meme()
            }


            class ImageTouchListener(ctx : Context) : GestureDetector.OnGestureListener {
                public var mDetector: GestureDetectorCompat = GestureDetectorCompat(ctx,this)

                override fun onShowPress(p0: MotionEvent?) {
                    //TODO("Not yet implemented")
                }

                override fun onSingleTapUp(p0: MotionEvent?): Boolean {
                    //TODO("Not yet implemented")
                    return true
                }

                override fun onDown(p0: MotionEvent?): Boolean {
                    //TODO("Not yet implemented")
                    return true
                }

                override fun onFling(ev1: MotionEvent?, ev2: MotionEvent?, vX: Float, vY: Float): Boolean {
                    //TODO("Not yet implemented")
                    if(abs(vX) > abs(vY) && abs(vX) > 10){

                        ratingvisual.alpha = 1f
                        ratingvisual.scaleX = 1f
                        ratingvisual.scaleY = 1f
                        ratingvisual.animate().scaleX(2f)
                        ratingvisual.animate().scaleY(2f)
                        ratingvisual.animate().alpha(0f)

                        if (vX > 0){
                            ratingvisual.setImageResource(R.drawable.herz)
                            if (frontMeme != null)
                                Client.rate_meme(applicationContext, frontMeme!!, 2)
                        }
                        else{
                            ratingvisual.setImageResource(R.drawable.herzbroke)
                            if (frontMeme != null)
                                Client.rate_meme(applicationContext, frontMeme!!, 1)
                        }

                        swap_memes()

                        ratingvisual.bringToFront()

                        update_back_meme()

                        /*
                      if (frontMeme != null) {
                          if (vX > 0)
                              Client.rate_meme(applicationContext, frontMeme!!, 2)
                          else
                              Client.rate_meme(applicationContext, frontMeme!!, 1)
                      }

                      swap_memes()

                      update_back_meme()



                   */
                    }






                    return true
                }

                override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
                    //TODO("Not yet implemented")
                    return true
                }

                override fun onLongPress(p0: MotionEvent?) {
                    //TODO("Not yet implemented")
                }

            }


            val imglist = ImageTouchListener(applicationContext)
            val bigView = findViewById<View>(R.id.main_layout)

            bigView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        frontImage.animate().cancel()
                        lastx = event.rawX
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {

                        val dx = event.rawX - lastx
                        lastx = event.rawX
                        frontImage.x += dx
                        val w = window.decorView.width
                        backImage.alpha = (kotlin.math.abs(frontImage.x) / w).pow(2f)
                        /*
                        if (frontImage.x > 0f)
                            ratingvisual.setImageResource(R.drawable.herz)
                        else
                            ratingvisual.setImageResource(R.drawable.herzbroke)
                        if (frontImage.x / window.decorView.width > 0.5f) {
                            t2.text = "like"
                            t2.setTextColor(resources.getColor(R.color.like))
                            ratingvisual.alpha = 1f
                        } else if ((frontImage.x + frontImage.width) / window.decorView.width < 0.5f) {
                            t2.text = "dislike"
                            t2.setTextColor(resources.getColor(R.color.dislike))
                            ratingvisual.alpha = 1f
                        } else {
                            t2.text = ""
                            ratingvisual.alpha = 0f
                        }

                         */

                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        frontImage.animate().x(defaultx)
                        ratingvisual.alpha = 0f
                        t2.text = ""


                        /*
                        //img.x = defaultx
                        frontImage.animate().x(defaultx)
                        //img2.animate().alpha(0f)
                        ratingvisual.alpha = 0f

                        if (t2.text == "like" || t2.text == "dislike") {
                            if (frontMeme != null) {
                                if (t2.text == "like")
                                    Client.rate_meme(applicationContext, frontMeme!!, 2)
                                else
                                    Client.rate_meme(applicationContext, frontMeme!!, 1)
                            }
                            t2.text = ""

                            swap_memes()

                            update_back_meme()
                        }


                         */


                        false
                    }
                    else -> {
                        false
                    }

                }
                imglist.mDetector.onTouchEvent(event)
            }
        }

        Client.connect(applicationContext, setup)

    }


}