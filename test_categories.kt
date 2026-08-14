import java.net.URL
import java.net.HttpURLConnection
fun main() {
    val connection = URL("https://api.kaiostech.com/v3.0/categories").openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    println(connection.inputStream.bufferedReader().use { it.readText() })
}
