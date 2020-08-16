package com.tgag.tgag

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.tgag.tgag.Client.login
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager
import java.security.SecureRandom
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.pow


data class Meme(val itemID: Int, val bitmap: Bitmap, val author: String)


class MainActivity : AppCompatActivity() {

    private var uniqueID: String? = null
    private var password: String? = null
    //private val baseurl: String = "https://tgag.app"
    private val baseurl: String = "http://192.168.1.116:5000"

    private var registered = false


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        if(registered)
            menuInflater.inflate(R.menu.menu, menu)
        else
            menuInflater.inflate(R.menu.unregistered_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    private fun fileUpload(imgUri: Uri){
        val filetype = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(imgUri))

        val freq = FileRequest(Request.Method.POST, "$baseurl/upload", contentResolver.openInputStream(imgUri)!!, "androidupload.$filetype", Response.Listener { response ->
        }, Response.ErrorListener { error ->
        })

        Client.getQueue(applicationContext).add(freq)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        if(requestCode == 1){ //upload
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
        }
        else if (requestCode == 2){ //register
            if(resultCode == Activity.RESULT_OK){
                val pref = getPreferences(Context.MODE_PRIVATE)
                val editor = pref.edit()
                uniqueID = intent!!.getStringExtra("new_id")
                password = intent.getStringExtra("new_pw")
                editor.putString("id", uniqueID)
                editor.putString("pw", password)
                editor.putBoolean("registered", true)
                editor.apply()
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
            intent.putExtra("old_username", uniqueID)
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


        CookieHandler.setDefault(CookieManager())

        val t2 = findViewById<TextView>(R.id.textView2)
        val t = findViewById<TextView>(R.id.textView)

        t2.text = ""
        ratingvisual.alpha = 0f
        author.visibility = View.INVISIBLE


        val setup = {
            var memeimgs = HashMap<Int, Meme>()
            var memesdone = HashSet<Int>();

            var frontImage = findViewById<ImageView>(R.id.imageView)
            var backImage = findViewById<ImageView>(R.id.imageView2)
            var frontMeme: Meme? = null
            var backMeme: Meme? = null
            backImage.alpha = 0f

            var lastx = 0f
            val defaultx = frontImage.x
            var defaulty = frontImage.y

            val rate_meme = { rating: Int ->
                val url = "$baseurl/rate_meme_app"

                val jsonBody = JSONObject()

                if (frontMeme != null) {
                    jsonBody.put("itemID", frontMeme!!.itemID)
                    jsonBody.put("rating", rating)

                    val req = JsonObjectRequest(
                        Request.Method.POST, url, jsonBody,
                        Response.Listener { response ->
                            t.text = "rating worked"
                        },
                        Response.ErrorListener { error ->
                            // TODO: Handle error
                            t.text = error.message

                        })
                    Client.getQueue(applicationContext).add(req)
                }

            }

            var counter = 0
            val get_new_memes = { callback: () -> Unit ->
                val url = "$baseurl/top_urls_json"
                counter = 0

                val jsonObjectRequest = JsonObjectRequest(
                    Request.Method.GET, url, null,
                    Response.Listener { response ->
                        //t.text = "Response: %s".format(response.toString())
                        val top = response.getJSONArray("top_rec")
                        for (i in 0 until top.length()) {
                            val item_id = top.getJSONArray(i).getInt(0)
                            val img_url = top.getJSONArray(i).getString(1)
                            val img_author = top.getJSONArray(i).getString(2)

                            if (!memesdone.contains(item_id)) {
                                counter++
                                memesdone.add(item_id)
                                val imReq =
                                    ImageRequest(img_url,
                                        { bitmap: Bitmap ->
                                            memeimgs.put(item_id, Meme(item_id, bitmap, img_author))
                                            counter--
                                            if (counter == 0)
                                                callback()
                                        },
                                        1000,
                                        1000,
                                        frontImage.scaleType,
                                        Bitmap.Config.RGB_565,
                                        { error -> t.text = error.message })

                                //queue.add(jsonObjectRequest)
                                Client.getQueue(applicationContext).add(imReq)
                            }
                        }
                    },
                    Response.ErrorListener { error ->
                        // TODO: Handle error
                        t.text = error.message

                    }
                )

                Client.getQueue(applicationContext).add(jsonObjectRequest)

            }

            fun pick_some_meme(): Pair<Int, Meme>? {
                if (memeimgs.size < 5) {
                    get_new_memes {}
                }
                for ((key, value) in memeimgs) {
                    //TODO: what happens if the rating does not get trough?
                    memeimgs.remove(key)

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

            get_new_memes {
                update_back_meme()
                swap_memes()
                update_back_meme()
            }



            frontImage.setOnTouchListener { v, event ->
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

                        true
                    }
                    MotionEvent.ACTION_UP -> {

                        //img.x = defaultx
                        frontImage.animate().x(defaultx)
                        //img2.animate().alpha(0f)
                        ratingvisual.alpha = 0f

                        if (t2.text == "like" || t2.text == "dislike") {
                            if (frontMeme != null) {
                                if (t2.text == "like")
                                    rate_meme(2)
                                else
                                    rate_meme(1)
                            }
                            t2.text = ""

                            swap_memes()

                            update_back_meme()
                        }



                        false
                    }
                    else -> {
                        false
                    }
                }
            }
        }


        val pref = getPreferences(Context.MODE_PRIVATE)
        uniqueID = pref.getString("id", null)
        if (uniqueID == null) {
            val editor = pref.edit()

            uniqueID = UUID.randomUUID().toString()

            val random = SecureRandom()
            val bytes = ByteArray(21)
            random.nextBytes(bytes)
            val x = Base64.encodeToString(bytes, Base64.NO_WRAP and Base64.NO_PADDING)
            // remove trailing newline as androids prefs is bugged
            // and adds extra characters if the string has one
            password = x.dropLast(1)


            val url = "$baseurl/new_app_user"
            val jsonBody = JSONObject()

            jsonBody.put("username", uniqueID)
            jsonBody.put("password", password)

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                Response.Listener { response ->
                    t.text = "Response: %s".format(response.toString())
                    editor.putString("id", uniqueID)
                    editor.putString("pw", password)
                    editor.apply()
                    setup()
                },
                Response.ErrorListener { error ->
                    // TODO: Handle error
                    t.text = error.message

                }
            )

            Client.getQueue(applicationContext).add(jsonObjectRequest)

        } else {
            //login(applicationContext, pref, Response.Listener { response -> setup() })
            password = pref.getString("pw", null)
            registered = pref.getBoolean("registered", false)

            val url = "$baseurl/login_app"
            val jsonBody = JSONObject()

            jsonBody.put("username", uniqueID)
            jsonBody.put("password", password)

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                Response.Listener { response ->
                    t.text = "Response: %s".format(response.toString())

                    setup()
                },
                Response.ErrorListener { error ->
                    // TODO: Handle error
                    t.text = error.message

                }
            )
            Client.getQueue(applicationContext).add(jsonObjectRequest)

        }


    }


}