package fi.sobolev.uffn.fetching

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level
import java.util.logging.Logger
import mu.KotlinLogging
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File


data class ResponseInfo (
    val code: Int,
    val contents: String
)

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
            // @todo    Set UA according to include "bot"
            // @see     https://archiveofourown.org/admin_posts/18804
            // it.addRequestProperty("User-Agent", "< ... >")
        }

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
    val regex: Regex,
    val resolver: (MatchResult) -> String
) : Browser {
    override fun getContents(address: String): String {
        val match = regex.matchEntire(address) ?: throw Exception("failed to match mock address")
        return File(resolver(match)).readText()
    }
}
