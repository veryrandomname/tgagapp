package com.tgag.tgag

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.CookieHandler
import java.net.CookieManager
import java.security.SecureRandom
import java.util.*

open class Meme(val itemID: Int, val title: String?, val author: String?)
class ImageMeme(itemID: Int, val bitmap: Bitmap, title: String?, author: String?) : Meme(
    itemID,
    title,
    author
)
class VideoMeme(itemID: Int, val file: File, title: String?, author: String?) : Meme(
    itemID,
    title,
    author
)

fun optString(json: JSONObject, key: String?): String? {
    // http://code.google.com/p/android/issues/detail?id=13830
    return if (json.isNull(key)) null else json.optString(key, null)
}

class MemeInfo(private val obj: JSONObject){
    val item_id = obj.getInt("itemID")
    val title: String? = optString(obj,"title")
    val url: String = obj.getString("url")
    val thumbnail_url = obj.getString("thumbnail_url")
    val author: String? = optString(obj,"author")
    val filename = obj.getString("filename")
    val file_extension = obj.getString("file_extension")
    val type = obj.getString("type")
    fun dislikes() : Int {
        return obj.getJSONArray("rating").getInt(0)
    }
    fun likes() : Int {
        return obj.getJSONArray("rating").getInt(1)
    }
}

object Client {
    private var queue: RequestQueue? = null
    var uniqueID: String? = null
    var password: String? = null
    var registered: Boolean = false
    val k = BuildConfig.DEBUG
    var logged_in : Boolean = false
    
    val baseurl: String = "http://192.168.1.116:5000"
    //val baseurl: String = "https://tgag.app"

    private fun getQueue(ctx: Context): RequestQueue {
        if (queue == null)
            queue = Volley.newRequestQueue(ctx)

        return queue!!
    }

    private fun <T> addRequest(ctx: Context, req: Request<T>){
        getQueue(ctx).add(req)
    }

    fun logout(ctx: Context){
        val url = "$baseurl/logout"

        val req = StringRequest(
            Request.Method.POST, url,
            { _ -> logged_in = false },
            { _ -> }
        )
        Client.getQueue(ctx).add(req)
    }

    fun login(
        ctx: Context,
        username: String,
        password: String,
        listener: Response.Listener<JSONObject>,
        error_listener: Response.ErrorListener
    ){
        val url = "$baseurl/login_app"
        val jsonBody = JSONObject()

        jsonBody.put("username", username)
        jsonBody.put("password", password)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response : JSONObject ->

                setLocalLogin(ctx, username, password, true)

                listener.onResponse(response)
            },
            error_listener
        )
        Client.getQueue(ctx).add(jsonObjectRequest)
    }

    fun login_saved(ctx: Context, listener: Response.Listener<JSONObject>) {
        val pref = ctx.getSharedPreferences("client", Context.MODE_PRIVATE)
        uniqueID = pref.getString("id", null)
        password = pref.getString("pw", null)
        registered = pref.getBoolean("registered", false)

        login(ctx, uniqueID!!, password!!, listener, Response.ErrorListener { _ -> })
    }

    fun file_upload(
        ctx: Context,
        filestream: InputStream,
        filetype: String,
        title: String,
        show_username: Boolean,
        listener: Response.Listener<NetworkResponse>,
        error_listener: Response.ErrorListener
    ): UploadRequest {
        val freq = UploadRequest(
            "$baseurl/upload",
            filestream,
            "androidupload.$filetype",
            title,
            show_username,
            listener,
            error_listener)

        Client.getQueue(ctx).add(freq)

        return freq
    }

    fun register_anon(
        ctx: Context,
        listener: Response.Listener<JSONObject>
    ) {
        val pref = ctx.getSharedPreferences("client", Context.MODE_PRIVATE)

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
            { response ->

                setLocalLogin(ctx, uniqueID!!, password!!, false)

                val editor = pref.edit()
                editor.putBoolean("not_first_start", true)
                editor.apply()

                listener.onResponse(response)
            },
            { error ->
            }
        )

        Client.getQueue(ctx).add(jsonObjectRequest)
    }

    var memeimgs = HashMap<Int, Meme>()
    private var memesdone = HashSet<Int>()

    private fun get_meme_remote(
        ctx: Context,
        m: MemeInfo,
        thumbnail: Boolean,
        callback: (Meme) -> Unit
    ) {
        if (m.type == "image" || (m.type == "video" && thumbnail)) {
            val url = if(m.type == "video" && thumbnail) m.thumbnail_url else m.url
            val imReq =
                ImageRequest(url,
                    { bitmap: Bitmap ->
                        callback(ImageMeme(m.item_id, bitmap, m.title, m.author))
                    },
                    1000,
                    1000,
                    ImageView.ScaleType.FIT_CENTER,
                    Bitmap.Config.RGB_565,
                    { _ -> })

            getQueue(ctx).add(imReq)

        } else if (m.type == "video" && !thumbnail) {
            var tmp = File(ctx.cacheDir, m.filename)
            if (!tmp.exists()) {

                val fileReq =
                    FileRequest(m.url,
                        { bytes: ByteArray ->
                            tmp = createTempFile(m.filename, null, ctx.cacheDir)
                            tmp.writeBytes(bytes)
                            callback(VideoMeme(m.item_id, tmp, m.title, m.author))
                        },
                        { _ -> })

                getQueue(ctx).add(fileReq)
            } else {
                callback(VideoMeme(m.item_id, tmp, m.title, m.author))
            }
        }
    }


    private fun put_remote_meme_in_pool(ctx: Context, m: MemeInfo, callback: () -> Unit){
        if (!memesdone.contains(m.item_id)) {
            memesdone.add(m.item_id)

            get_meme_remote(ctx, m, false) { meme ->
                memeimgs[meme.itemID] = meme
                callback()
            }
        }
    }


    fun get_new_memes(ctx: Context, callback: () -> Unit) {
        val url = "$baseurl/top_urls_json"

        var once = false

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                //t.text = "Response: %s".format(response.toString())
                val top = response.getJSONArray("top_rec")
                for (i in 0 until top.length()) {
                    val obj = top.getJSONObject(i)
                    put_remote_meme_in_pool(ctx, MemeInfo(obj)) {
                        if (!once) {
                            once = true
                            callback()
                        }
                    }
                }
            },
            { error ->
            }
        )
        Client.getQueue(ctx).add(jsonObjectRequest)
    }

    private fun create_image(
        ctx: Context,
        container: LinearLayout,
        bitmap: Bitmap,
        likes: Int,
        dislikes: Int
    ){
        val imgView = ImageView(ctx)
        imgView.setImageBitmap(bitmap)
        container.addView(imgView, MATCH_PARENT, 1000)
        val likesView = TextView(ctx)
        val dislikesView = TextView(ctx)
        likesView.text = "likes: $likes"
        dislikesView.text = "dislikes: $dislikes"
        likesView.textSize = 24f
        dislikesView.textSize = 24f
        container.addView(likesView)
        container.addView(dislikesView)
    }

    var my_upload_cache = HashMap<Int, Meme>()

    fun display_my_uploads(ctx: Context, container: LinearLayout) {
        val url = "$baseurl/my_uploads_app"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                //t.text = "Response: %s".format(response.toString())
                val top = response.getJSONArray("my_uploads")
                for (i in 0 until top.length()) {
                    val obj = top.getJSONObject(i)
                    val rating = obj.getJSONArray("rating")
                    val dislikes = rating.getInt(0)
                    val likes = rating.getInt(1)
                    val m = MemeInfo(obj)

                    if (!my_upload_cache.containsKey(m.item_id)) {
                        get_meme_remote(ctx, m, true) { meme ->
                            if (meme is ImageMeme) {
                                val img = meme as ImageMeme
                                my_upload_cache[img.itemID] = img
                                create_image(ctx, container, img.bitmap, likes, dislikes)
                            }

                        }
                    } else {
                        val meme = my_upload_cache[m.item_id]
                        if (meme is ImageMeme) {
                            create_image(ctx, container, meme.bitmap, likes, dislikes)
                        }

                    }
                }
            },
            { error ->
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
            { response ->
            },
            { error ->

            })
        Client.getQueue(ctx).add(req)


    }

    fun hasLocalLogin(ctx: Context) : Boolean {
        val pref = ctx.getSharedPreferences("client", Context.MODE_PRIVATE)
        return pref.contains("id")
    }

    fun deleteLocalLogin(ctx: Context){
        uniqueID = null
        password = null
        registered = false

        val pref = ctx.getSharedPreferences("client", Context.MODE_PRIVATE)
        val editor  = pref.edit()
        editor.remove("id")
        editor.remove("pw")
        editor.remove("registered")
        editor.apply()
    }

    fun setLocalLogin(ctx: Context, username: String, password: String, registered: Boolean){
        Client.uniqueID = username
        Client.password = password
        Client.registered = registered

        val pref = ctx.getSharedPreferences("client", Context.MODE_PRIVATE)
        val editor  = pref.edit()
        editor.putString("id", username)
        editor.putString("pw", password)
        editor.putBoolean("registered", registered)
        editor.apply()

        logged_in = true
    }

    fun connect(ctx: Context, setup: () -> Unit) {
        CookieHandler.setDefault(CookieManager())

        val pref = ctx.getSharedPreferences("client", Context.MODE_PRIVATE)

        uniqueID = pref.getString("id", null)
        if (uniqueID == null) {
            register_anon(ctx, Response.Listener { response ->
                setup()
            })
        } else {
            login_saved(ctx, Response.Listener { response -> setup() })
        }

    }

    fun register(
        ctx: Context,
        new_username: String,
        new_password: String,
        listener: Response.Listener<JSONObject>,
        error_listener: Response.ErrorListener
    ){

        val pref = ctx.getSharedPreferences("client", Context.MODE_PRIVATE)

        val jsonBody = JSONObject()

        //val pref = getPreferences(Context.MODE_PRIVATE)
        //jsonBody.put("old_username", pref.getString("id", null))
        jsonBody.put("username", new_username)
        jsonBody.put("password", new_password)

        val url = if (pref.contains("id")){
            jsonBody.put("old_username", uniqueID)
            "$baseurl/merge_app_user"
        }
        else{
            "$baseurl/new_app_user"
        }


        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            Response.Listener { response ->
                setLocalLogin(ctx, new_username, new_password, true)
                listener.onResponse(response)
            },
            error_listener
        )

        Client.getQueue(ctx).add(jsonObjectRequest)
    }
}
