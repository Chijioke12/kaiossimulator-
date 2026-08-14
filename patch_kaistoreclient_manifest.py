with open("app/src/main/java/com/example/ui/simulator/KaiStoreClient.kt", "r") as f:
    code = f.read()

new_func = """    fun fetchManifest(manifestUrl: String): String {
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
        connection.setRequestProperty("Kai-Request-Info", "ct=\\"wifi\\", rt=\\"auto\\", utc=\\"${System.currentTimeMillis()}\\", utc_off=\\"1\\", mcc=\\"0\\", mnc=\\"0\\", net_mcc=\\"null\\", net_mnc=\\"null\\"")
        connection.setRequestProperty("Kai-Device-Info", "imei=\\"123456789012345\\", curef=\\"4044O-2BAQUS1-R\\"")
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
}"""

code = code.replace("    }\n}", new_func)

with open("app/src/main/java/com/example/ui/simulator/KaiStoreClient.kt", "w") as f:
    f.write(code)
