package fi.sobolev.uffn.common.fetching

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


interface Browser {
    /**
     * Make HTTP request to the specified URL and get the response's contents back.
     * @param   address  Requested URL.
     */
    fun getContents(address: String): String
}


class StaticBrowser : Browser {
    override fun getContents(address: String): String {
        val url = URL(address)
        val conn = (url.openConnection() as HttpURLConnection).also {
             it.addRequestProperty("User-Agent", "uffn/0.1.0 self-hosted fanfic aggregation bot.")
        }

        // @TODO REMOVE BELOW
        conn.connect()
        println("Capacity = ${conn.contentLength}")
        // @TODO REMOVE ABOVE

        val responseContent = StringBuilder()

        conn.inputStream
            .let { stream -> InputStreamReader(stream) }
            .let { reader -> BufferedReader(reader) }
            .use { reader ->
                reader.readLines().forEach { line ->
                    responseContent.append(line)
                }
            }

        return responseContent.toString()
    }
}
