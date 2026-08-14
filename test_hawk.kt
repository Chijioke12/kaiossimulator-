import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import org.json.JSONObject

fun main() {
    // Fallback base64 impl
    val STORE_API_KEY = "baJ_nea27HqSskijhZlT"
    val TOKEN_URL = "https://api.kaiostech.com/v3.0/applications/CAlTn_6yQsgyJKrr-nCh/tokens"
    val url = URL(TOKEN_URL)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Authorization", "Key \$STORE_API_KEY")
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
    
    val response = connection.inputStream.bufferedReader().use { it.readText() }
    println("TOKEN RESPONSE: \$response")
    
    val json = JSONObject(response)
    val kid = json.getString("kid")
    val macKeyB64 = json.getString("mac_key")
    val macKey = java.util.Base64.getDecoder().decode(macKeyB64)
    
    val targetUrl = "https://api.kaiostech.com/kc_ksfe/v1.0/apps?bookmark=false&imei=123456789012345&os=2.5.4&page_size=50&page_num=1&mnc=0&mcc=0"
    
    val reqUrl = URL(targetUrl)
    val ts = (System.currentTimeMillis() / 1000).toString()
    val nonce = UUID.randomUUID().toString().substring(0, 6)
    val path = if (reqUrl.query != null) "${reqUrl.path}?${reqUrl.query}" else reqUrl.path
    val host = reqUrl.host
    val port = "443"
    
    val normalized = "hawk.1.header\n\$ts\n\$nonce\nGET\n\$path\n\$host\n\$port\n\n\n"
    val mac = Mac.getInstance("HmacSHA256").apply {
        init(SecretKeySpec(macKey, "HmacSHA256"))
    }
    val signatureBytes = mac.doFinal(normalized.toByteArray(Charsets.UTF_8))
    val macValue = java.util.Base64.getEncoder().encodeToString(signatureBytes)
    val authHeader = "Hawk id=\"\$kid\", ts=\"\$ts\", nonce=\"\$nonce\", mac=\"\$macValue\""
    
    println("AUTH HEADER: \$authHeader")
    
    val c2 = reqUrl.openConnection() as HttpURLConnection
    c2.requestMethod = "GET"
    c2.setRequestProperty("Kai-API-Version", "3.0")
    c2.setRequestProperty("Kai-Request-Info", "ct=\"wifi\", rt=\"auto\", utc=\"${System.currentTimeMillis()}\", utc_off=\"1\", mcc=\"0\", mnc=\"0\", net_mcc=\"null\", net_mnc=\"null\"")
    c2.setRequestProperty("Kai-Device-Info", "imei=\"123456789012345\", curef=\"4044O-2BAQUS1-R\"")
    c2.setRequestProperty("User-agent", "Mozilla/5.0 (Mobile; GoFlip2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.4")
    c2.setRequestProperty("Authorization", authHeader)
    
    try {
        println("APPS RESPONSE: " + c2.inputStream.bufferedReader().use { it.readText() }.take(500))
    } catch (e: Exception) {
        println("ERROR: " + c2.responseCode)
        println(c2.errorStream?.bufferedReader()?.use { it.readText() })
    }
}
