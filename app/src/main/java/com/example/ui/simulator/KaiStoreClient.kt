package com.example.ui.simulator

import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.zip.GZIPInputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object KaiStoreClient {
    private const val CLIENT_APP_ID = "CAlTn_6yQsgyJKrr-nCh"
    private const val STORE_API_KEY = "baJ_nea27HqSskijhZlT"
    private const val TOKEN_URL = "https://api.kaiostech.com/v3.0/applications/CAlTn_6yQsgyJKrr-nCh/tokens"
    private const val APPS_URL = "https://api.kaiostech.com/kc_ksfe/v1.0/apps?bookmark=false&imei=123456789012345&os=2.5.4&page_size=50&page_num=1&mnc=0&mcc=0"

    private var currentKid: String? = null
    private var currentMacKey: ByteArray? = null

    private fun fetchToken() {
        val url = URL(TOKEN_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Key $STORE_API_KEY")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val payload = """
            {
              "brand": "AlcatelOneTouch",
              "device_id": "123456789012345",
              "device_type": 999999,
              "model": "GoFlip2",
              "os": "KaiOS",
              "os_version": "2.5.4",
              "reference": "4044O-2BAQUS1-R"
            }
        """.trimIndent()

        connection.outputStream.write(payload.toByteArray(Charsets.UTF_8))
        connection.outputStream.close()

        if (connection.responseCode in 200..299) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            currentKid = json.getString("kid")
            val macKeyB64 = json.getString("mac_key")
            currentMacKey = Base64.decode(macKeyB64, Base64.DEFAULT)
        } else {
            throw Exception("Failed to fetch token: ${connection.responseCode}")
        }
    }

    private fun generateHawkHeader(requestUrl: String, kid: String, macKey: ByteArray, method: String = "GET"): String {
        val url = URL(requestUrl)
        val ts = (System.currentTimeMillis() / 1000).toString()
        val nonce = UUID.randomUUID().toString().substring(0, 6)
        
        val path = if (url.query != null) "${url.path}?${url.query}" else url.path
        val host = url.host
        val port = if (url.port != -1) url.port.toString() else if (url.protocol == "https") "443" else "80"
        
        val normalized = "hawk.1.header\n" +
                "$ts\n" +
                "$nonce\n" +
                "${method.uppercase()}\n" +
                "$path\n" +
                "$host\n" +
                "$port\n" +
                "\n" + // hash
                "\n"   // ext
        
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(macKey, "HmacSHA256"))
        }
        val signatureBytes = mac.doFinal(normalized.toByteArray(Charsets.UTF_8))
        val macValue = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        
        return "Hawk id=\"$kid\", ts=\"$ts\", nonce=\"$nonce\", mac=\"$macValue\""
    }

    fun downloadApp(targetUrl: String, destFile: File) {
        if (currentKid == null || currentMacKey == null) {
            fetchToken()
        }
        val kid = currentKid ?: throw Exception("Missing kid")
        val macKey = currentMacKey ?: throw Exception("Missing macKey")

        val url = URL(targetUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        
        if (targetUrl.contains("kaiostech.com")) {
            val authHeader = generateHawkHeader(targetUrl, kid, macKey)
            connection.setRequestProperty("Kai-API-Version", "3.0")
            connection.setRequestProperty("Kai-Request-Info", "ct=\"wifi\", rt=\"auto\", utc=\"${System.currentTimeMillis()}\", utc_off=\"1\", mcc=\"0\", mnc=\"0\", net_mcc=\"null\", net_mnc=\"null\"")
            connection.setRequestProperty("Kai-Device-Info", "imei=\"123456789012345\", curef=\"4044O-2BAQUS1-R\"")
            connection.setRequestProperty("User-agent", "Mozilla/5.0 (Mobile; GoFlip2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.4")
            connection.setRequestProperty("Authorization", authHeader)
        }
        
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        
        if (connection.responseCode in 200..299) {
            connection.inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            throw Exception("Download failed: ${connection.responseCode}")
        }
    }

    /**
     * Tries to fetch list of apps using the real api.kaiostech.com with Hawk authentication header,
     * or falls back to Github Open-KaiStore-Registry if failed or no connection.
     */
    fun fetchApps(category: String? = null, query: String? = null): String {
        return try {
            if (currentKid == null || currentMacKey == null) {
                fetchToken()
            }
            
            val kid = currentKid ?: throw Exception("Missing kid")
            val macKey = currentMacKey ?: throw Exception("Missing macKey")

            var targetUrl = if (!query.isNullOrEmpty()) {
                "https://search.kaiostech.com/v3/_search?bookmark=false&imei=123456789012345&platform=2.5.4&page=0&size=50&mnc=0&mcc=0&query=${java.net.URLEncoder.encode(query, "UTF-8")}&locale=en-US"
            } else {
                "https://api.kaiostech.com/kc_ksfe/v1.0/apps?bookmark=false&imei=123456789012345&os=2.5.4&page_size=50&page_num=1&mnc=0&mcc=0"
            }
            
            if (category != null && query.isNullOrEmpty()) {
                targetUrl += "&category=$category"
            }

            val url = URL(targetUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            val authHeader = generateHawkHeader(targetUrl, kid, macKey)
            
            connection.setRequestProperty("Kai-API-Version", "3.0")
            connection.setRequestProperty("Kai-Request-Info", "ct=\"wifi\", rt=\"auto\", utc=\"${System.currentTimeMillis()}\", utc_off=\"1\", mcc=\"0\", mnc=\"0\", net_mcc=\"null\", net_mnc=\"null\"")
            connection.setRequestProperty("Kai-Device-Info", "imei=\"123456789012345\", curef=\"4044O-2BAQUS1-R\"")
            connection.setRequestProperty("User-agent", "Mozilla/5.0 (Mobile; GoFlip2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.4")
            connection.setRequestProperty("Authorization", authHeader)
            connection.setRequestProperty("Accept-Encoding", "gzip")
            
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            if (connection.responseCode in 200..299) {
                val inputStream: InputStream = if ("gzip".equals(connection.contentEncoding, ignoreCase = true)) {
                    GZIPInputStream(connection.inputStream)
                } else {
                    connection.inputStream
                }
                inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw Exception("Response code ${connection.responseCode}: $errorMsg")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("KaiStoreClient", "Fetch apps failed", e)
            // Fallback to the Github Registry URL which is always reliable
            val fallbackUrl = "https://raw.githubusercontent.com/Chijioke12/Open-KaiStore-Registry/refs/heads/main/apps.json"
            URL(fallbackUrl).readText()
        }
    }

    fun fetchManifest(manifestUrl: String): String {
        if (currentKid == null || currentMacKey == null) {
            fetchToken()
        }
        val kid = currentKid ?: throw Exception("Missing kid")
        val macKey = currentMacKey ?: throw Exception("Missing macKey")

        val url = URL(manifestUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        
        val authHeader = generateHawkHeader(manifestUrl, kid, macKey)
        connection.setRequestProperty("Kai-API-Version", "3.0")
        connection.setRequestProperty("Kai-Request-Info", "ct=\"wifi\", rt=\"auto\", utc=\"${System.currentTimeMillis()}\", utc_off=\"1\", mcc=\"0\", mnc=\"0\", net_mcc=\"null\", net_mnc=\"null\"")
        connection.setRequestProperty("Kai-Device-Info", "imei=\"123456789012345\", curef=\"4044O-2BAQUS1-R\"")
        connection.setRequestProperty("User-agent", "Mozilla/5.0 (Mobile; GoFlip2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.4")
        connection.setRequestProperty("Authorization", authHeader)
        connection.setRequestProperty("Accept-Encoding", "gzip")
        
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        if (connection.responseCode in 200..299) {
            val inputStream: InputStream = if ("gzip".equals(connection.contentEncoding, ignoreCase = true)) {
                GZIPInputStream(connection.inputStream)
            } else {
                connection.inputStream
            }
            return inputStream.bufferedReader().use { it.readText() }
        } else {
            val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw Exception("Response code ${connection.responseCode}: $errorMsg")
        }
    }
}
