package com.tgag.tgag

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset


class FileRequest(
    url: String,
    private val listener: Response.Listener<ByteArray>,
    errorListener: Response.ErrorListener
) : Request<ByteArray>(Method.GET, url, errorListener) {

    override fun getHeaders(): MutableMap<String, String> = HashMap()

    override fun deliverResponse(response: ByteArray) = listener.onResponse(response)

    override fun parseNetworkResponse(response: NetworkResponse?): Response<ByteArray> {
        return Response.success(response?.data,HttpHeaderParser.parseCacheHeaders(response))
    }
}
