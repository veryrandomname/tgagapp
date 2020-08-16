package com.tgag.tgag

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Base64
import android.widget.ImageView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.InputStream
import java.security.SecureRandom
import java.util.*

object Client {
    private var queue : RequestQueue? = null
    var uniqueID : String? = null
    var password : String? = null
    var registered : Boolean = false
    // val baseurl: String = "https://tgag.app"
    val baseurl: String = "http://192.168.1.116:5000"

    fun getQueue( ctx : Context) : RequestQueue {
        if(queue == null)
            queue = Volley.newRequestQueue(ctx)

        return queue!!
    }

    fun login(ctx: Context, pref : SharedPreferences, listener : Response.Listener<JSONObject>) {
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

    fun file_upload(ctx: Context, filestream : InputStream, filetype : String){
        val freq = FileRequest(Request.Method.POST, "$baseurl/upload", filestream, "androidupload.$filetype", Response.Listener { response ->
        }, Response.ErrorListener { error ->
        })

        Client.getQueue(ctx).add(freq)
    }

    fun register_anon(ctx: Context, pref: SharedPreferences, listener: Response.Listener<JSONObject>) {
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
            listener,
            Response.ErrorListener { error ->
            }
        )

        Client.getQueue(ctx).add(jsonObjectRequest)
    }

    private var counter = 0
    var memeimgs = HashMap<Int, Meme>()
    var memesdone = HashSet<Int>();

    fun get_new_memes(ctx: Context, callback: () -> Unit, scale_type : ImageView.ScaleType ) {
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

    fun rate_meme(ctx: Context, meme : Meme, rating: Int ){
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
}
