package com.tgag.tgag

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream


class UploadRequest(
    url: String,
    private val stream: InputStream,
    private val fileName: String,
    private val title: String,
    private val show_username: Boolean,
    private val listener: Response.Listener<NetworkResponse>,
    errorListener: Response.ErrorListener
) : Request<NetworkResponse>(Request.Method.POST, url, errorListener) {

    private val boundary = "apiclient-" + System.currentTimeMillis()
    private val mimeType = "multipart/form-data;boundary=$boundary"

    // Create multi part byte array
    private val multipartBody: ByteArray = {
        val bos = ByteArrayOutputStream()

        val dos = DataOutputStream(bos)

        val twoHyphens = "--"
        val lineEnd = "\r\n"

        dos.writeBytes(twoHyphens + boundary + lineEnd)
        dos.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"$fileName\"$lineEnd")
        dos.writeBytes(lineEnd)
        val buffer = ByteArray(10240)
        var len: Int = 0
        while (stream.read(buffer).also { len = it } != -1) {
            dos.write(buffer, 0, len)
        }
        //dos.write(stream.read())
        dos.writeBytes(lineEnd)
        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)

        bos.toByteArray()
    }()


    // Request header, if needed
    private val headers = HashMap<String, String>()
    //headers["API-TOKEN"] = "458e126682d577c97d225bbd73a75b5989f65e977b6d8d4b2267537019ad9d20"


    override fun getHeaders(): MutableMap<String, String> {
        return if (headers.isEmpty()) super.getHeaders() else headers
    }

    override fun getBodyContentType(): String {
        return mimeType
    }

    override fun getBody(): ByteArray {
        return multipartBody
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<NetworkResponse> {
        return try {
            Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: Exception) {
            Response.error(ParseError(e))
        }
    }

    override fun deliverResponse(response: NetworkResponse?) {
        listener.onResponse(response)
    }
}
