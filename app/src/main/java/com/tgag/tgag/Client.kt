package com.tgag.tgag

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import android.widget.ImageView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_register.*
import org.json.JSONObject
import java.io.InputStream
import java.net.CookieHandler
import java.net.CookieManager
import java.security.SecureRandom
import java.util.*

object Client {
    private var queue: RequestQueue? = null
    var uniqueID: String? = null
    var password: String? = null
    var registered: Boolean = false

    // val baseurl: String = "https://tgag.app"
    val baseurl: String = "http://192.168.1.116:5000"

    fun getQueue(ctx: Context): RequestQueue {
        if (queue == null)
            queue = Volley.newRequestQueue(ctx)

        return queue!!
    }

    fun login(ctx: Context, listener: Response.Listener<JSONObject>) {
        val pref = ctx.getSharedPreferences("client",Context.MODE_PRIVATE)
        uniqueID = pref.getString("id", null)
        password = pref.getString("pw", null)
        registered = pref.getBoolean("registered", false)

        val url = "$baseurl/login_app"
        val jsonBody = JSONObject()

        jsonBody.put("username", uniqueID)
        jsonBody.put("password", password)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            listener,
            Response.ErrorListener { error ->
            }
        )
        Client.getQueue(ctx).add(jsonObjectRequest)
    }

    fun file_upload(ctx: Context, filestream: InputStream, filetype: String) {
        val freq = FileRequest(
            Request.Method.POST,
            "$baseurl/upload",
            filestream,
            "androidupload.$filetype",
            Response.Listener { response ->
            },
            Response.ErrorListener { error ->
            })

        Client.getQueue(ctx).add(freq)
    }

    fun register_anon(
        ctx: Context,
        listener: Response.Listener<JSONObject>
    ) {
        val pref = ctx.getSharedPreferences("client",Context.MODE_PRIVATE)

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

                val editor = pref.edit()
                editor.putString("id", uniqueID)
                editor.putString("pw", password)
                editor.putBoolean("registered", false)
                editor.apply()

                listener.onResponse(response)
            },
            Response.ErrorListener { error ->
            }
        )

        Client.getQueue(ctx).add(jsonObjectRequest)
    }

    private var counter = 0
    var memeimgs = HashMap<Int, Meme>()
    var memesdone = HashSet<Int>()

    fun get_new_memes(ctx: Context, scale_type: ImageView.ScaleType, callback: () -> Unit) {
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
                                scale_type,
                                Bitmap.Config.RGB_565,
                                { _ -> })

                        //queue.add(jsonObjectRequest)
                        Client.getQueue(ctx).add(imReq)
                    }
                }
            },
            Response.ErrorListener { error ->
            }
        )

        Client.getQueue(ctx).add(jsonObjectRequest)

    }

    fun rate_meme(ctx: Context, meme: Meme, rating: Int) {
        val url = "$baseurl/rate_meme_app"

        val jsonBody = JSONObject()

        jsonBody.put("itemID", meme.itemID)
        jsonBody.put("rating", rating)

        val req = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            Response.Listener { response ->
            },
            Response.ErrorListener { error ->

            })
        Client.getQueue(ctx).add(req)


    }

    fun connect(ctx: Context, setup: () -> Unit) {
        CookieHandler.setDefault(CookieManager())

        val pref = ctx.getSharedPreferences("client",Context.MODE_PRIVATE)

        uniqueID = pref.getString("id", null)
        if (uniqueID == null) {
            register_anon(ctx, Response.Listener { response ->
                setup()
            })
        } else {
            login(ctx, Response.Listener { response -> setup() })
        }

    }

    fun merge_user(ctx: Context, new_username : String, new_password : String, listener: Response.Listener<JSONObject>,
                   error_listener : Response.ErrorListener){

        val pref = ctx.getSharedPreferences("client",Context.MODE_PRIVATE)

        val url = "$baseurl/merge_app_user"
        val jsonBody = JSONObject()

        //val pref = getPreferences(Context.MODE_PRIVATE)
        //jsonBody.put("old_username", pref.getString("id", null))
        jsonBody.put("old_username", uniqueID)
        jsonBody.put("username", new_username)
        jsonBody.put("password", new_password)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            Response.Listener { response ->
                uniqueID = new_username
                password = new_password
                registered = true

                val editor = pref.edit()
                editor.putString("id", uniqueID)
                editor.putString("pw", password)
                editor.putBoolean("registered", true)
                editor.apply()

                listener.onResponse(response)
            },
            error_listener
        )

        Client.getQueue(ctx).add(jsonObjectRequest)
    }
}
