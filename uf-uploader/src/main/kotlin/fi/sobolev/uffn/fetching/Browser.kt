package fi.sobolev.uffn.fetching

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL;
import java.util.logging.Level
import java.util.logging.Logger
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions


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


typealias SeleniumWaiter = (WebDriver) -> Unit
class SeleniumBrowser : Browser {
    private lateinit var driver: WebDriver

    fun initialize() {
        check(!::driver.isInitialized) {
            "this SeleniumBrowser instance is already initialized"
        }

        Logger.getLogger("org.openqa.selenium").level = Level.SEVERE
        val service = ChromeDriverService.Builder().withSilent(true).build()
        val options = ChromeOptions().setHeadless(true)
        driver = ChromeDriver(service, options)
    }

    fun cleanup() {
        if (::driver.isInitialized) {
            driver.quit()
        }
    }

    override fun getContents(address: String): String {
        check(::driver.isInitialized) {
            "this SeleniumBrowser instance is not initialized"
        }

        driver.get(address)
        return driver.pageSource
    }

    fun getContents(address: String, waiter: SeleniumWaiter): String {
        check(::driver.isInitialized) {
            "this SeleniumBrowser instance is not initialized"
        }

        driver.get(address)
        waiter(driver)
        return driver.pageSource
    }
}


class DiskMockBrowser (
    private val regex: Regex,
    private val resolver: (MatchResult) -> String
) : Browser {
    override fun getContents(address: String): String {
        val filePath = regex.matchEntire(address)?.let(resolver)
            ?: throw Exception("failed to match mock address")

        return File(filePath).readText()
    }
}
