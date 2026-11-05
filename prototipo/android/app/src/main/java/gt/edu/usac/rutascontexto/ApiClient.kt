package gt.edu.usac.rutascontexto

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.Executors

/** Cliente nativo del servicio compartido; no implementa un segundo modelo de riesgo. */
class ApiClient {
    private val executor = Executors.newSingleThreadExecutor()
    fun request(base: String, path: String, body: JSONObject? = null, idempotency: String? = null, done: (JSONObject?, String?) -> Unit) {
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                val uri = URI(base.trim().trimEnd('/'))
                require(uri.scheme in listOf("http", "https") && uri.host != null && uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null) { "URL de servicio inválida" }
                val conn = URI(uri.toString() + "/api" + path).toURL().openConnection() as HttpURLConnection
                connection = conn
                conn.connectTimeout = 7000
                conn.readTimeout = 12000
                conn.setRequestProperty("Accept", "application/json")
                if (body != null) {
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("X-Demo-Actor", "android-demo")
                    idempotency?.let { conn.setRequestProperty("Idempotency-Key", it) }
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val result = JSONObject(stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "{}")
                if (status !in 200..299) throw IllegalStateException(result.optJSONObject("error")?.optString("mensaje") ?: "Error HTTP $status")
                done(result, null)
            } catch (e: Exception) {
                done(null, e.message ?: "No se pudo conectar con el servicio")
            } finally { connection?.disconnect() }
        }
    }
    fun close() { executor.shutdownNow() }
}
