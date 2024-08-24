package com.example.vacart

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.apache.commons.text.StringEscapeUtils
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream


class HeaderInterceptor : Interceptor {
    private val gsonm = Gson()
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
//            .addHeader("Connection", "keep-alive")
//            .addHeader("Accept-Encoding", "gzip, deflate, br")
//            .addHeader("Accept", "*/*")
//            .addHeader("greq", "1714541213938")
//            .addHeader("Host", "www.irctc.co.in")
//            .addHeader("User-Agent", "PostmanRuntime/7.29.2")
//            .addHeader("Sec-Fetch-Dest", "empty")
//            .addHeader("Sec-Fetch-Mode", "cors")
//            .addHeader("Sec-Fetch-Site", "same-origin")
//            .addHeader("bmirak", "webbm")
//            .addHeader("Accept-Language", "en-US,en;q=0.5")
            .build()
        val response = chain.proceed(request)

        // Log request URL
        println("Request URL: ${request.url}")

        // Log raw JSON response
        val responseBody = response.body
        val contentLength = responseBody?.contentLength() ?: -1L
        val contentType = responseBody?.contentType()
        val resBody = StringBuilder()
        val source = responseBody?.source()?.apply {
            request(Long.MAX_VALUE) // Buffer the entire body
            if (response.headers["Content-Encoding"] == "gzip") {
                GZIPInputStream(this.inputStream()).use { gzip ->
                    val buffer = ByteArray(1024)
                    while (true) {
                        val bytesRead = gzip.read(buffer)
                        if (bytesRead == -1) break
//                        println(String(buffer, 0, bytesRead, charset = Charset.forName("UTF-8")))
//                        resBody += String(buffer, 0, bytesRead, charset = Charset.forName("UTF-8"))
                        resBody.append(String(buffer, 0, bytesRead, charset = Charset.forName("UTF-8")))
                    }
                }
            } else {
                // Decompress if needed, or read directly
                Buffer().also { buffer ->
                    readAll(buffer)
//                    println(buffer.readString(Charset.forName("UTF-8")))
                    resBody.append(buffer.readString(Charset.forName("UTF-8")))
                }
            }
        }

        val responseBodyNew = ResponseBody.create(contentType, resBody.toString())
        return response.newBuilder()
            .body(responseBodyNew)
            .build()
    }
}

fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
    val loggingInterceptor = HttpLoggingInterceptor()
    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS) // Set the desired log level
    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY) // Set the desired log level
    return loggingInterceptor
}

val client = OkHttpClient.Builder()
    .addInterceptor(HeaderInterceptor()) // Add your interceptor here/
    .addInterceptor(provideHttpLoggingInterceptor()) // Add HttpLoggingInterceptor
    .build()

fun String.removeQuotesAndUnescape(): String {
    return this.replace("^\"|\"$".toRegex(), "").let { unescapedString ->
        StringEscapeUtils.unescapeJava(unescapedString)
    }
}


class CustomJsonDeserializer : JsonDeserializer<String> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): String? {
        if (json == null || json.isJsonNull) {
            return null
        }
        val jsonString = json.asString
//        Log.d("Json string", jsonString)
        // Remove double quotes from start and end of the JSON string
        return jsonString.removeQuotesAndUnescape()
    }
}
