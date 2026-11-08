package com.music.bitchord.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Inicio de sesión con usuario y contraseña ligado al panel ibo.
 *
 * Mismo flujo que la app OTTMTY: se pide al panel la lista de servidores
 * (RTXRebrand.php y, si no sirve, dns.php) y se le pregunta a cada servidor
 * Xtream (player_api.php) si el usuario y la contraseña son válidos y si la
 * cuenta sigue vigente.
 */
object PanelLogin {

    /** Dirección del panel. */
    const val PANEL_BASE = "https://ottmty.xyz/ott17"

    sealed interface Result {
        /** Cuenta válida y vigente; [host] es el servidor donde se validó. */
        data class Success(val host: String) : Result
        data object Invalid : Result
        data object Expired : Result
        /** No se pudo hablar con el panel ni con ningún servidor. */
        data object NoConnection : Result
    }

    private enum class State { ACTIVE, EXPIRED, INVALID, NO_SERVER }

    private val client by lazy {
        Http.client.newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    /** Primer inicio de sesión: prueba todos los servidores del panel. */
    suspend fun login(user: String, pass: String): Result = withContext(Dispatchers.IO) {
        val hosts = runCatching { portals() }.getOrDefault(emptyList())
        if (hosts.isEmpty()) return@withContext Result.NoConnection
        var invalid = false
        var expired = false
        for (host in hosts) {
            when (stateOn(host, user, pass)) {
                State.ACTIVE -> return@withContext Result.Success(host)
                State.EXPIRED -> expired = true
                State.INVALID -> invalid = true
                State.NO_SERVER -> Unit
            }
        }
        when {
            expired -> Result.Expired
            invalid -> Result.Invalid
            else -> Result.NoConnection
        }
    }

    /**
     * Revisa una sesión ya guardada contra el servidor donde se validó. Sin
     * conexión devuelve [Result.NoConnection] y quien llama conserva la sesión.
     */
    suspend fun revalidate(host: String, user: String, pass: String): Result =
        withContext(Dispatchers.IO) {
            when (stateOn(host, user, pass)) {
                State.ACTIVE -> Result.Success(host)
                State.EXPIRED -> Result.Expired
                State.INVALID -> Result.Invalid
                State.NO_SERVER -> Result.NoConnection
            }
        }

    // ---- Panel: lista de servidores ----------------------------------------

    private fun portals(): List<String> {
        // 1) RTXRebrand.php: JSON { "<clave>": "<hex al revés>" } con "portals"
        runCatching {
            val j = JSONObject(get("$PANEL_BASE/api/RTXRebrand.php"))
            val key = j.keys().asSequence().first()
            val json = JSONObject(hexReversed(j.getString(key)))
            val arr = json.getJSONArray("portals")
            val out = ArrayList<String>()
            for (i in 0 until arr.length()) {
                val url = arr.getJSONObject(i).optString("rtxurls").trim().trimEnd('/')
                if (url.isNotEmpty()) out.add(url)
            }
            if (out.isNotEmpty()) return out
        }
        // 2) dns.php: AES-256-CBC en hexadecimal
        val arr = JSONArray(decryptDns(get("$PANEL_BASE/api/dns.php")))
        val out = ArrayList<String>()
        for (i in 0 until arr.length()) {
            val url = arr.getJSONObject(i).optString("url").trim().trimEnd('/')
            if (url.isNotEmpty()) out.add(url)
        }
        return out
    }

    // ---- Servidor Xtream ----------------------------------------------------

    private fun stateOn(host: String, user: String, pass: String): State {
        return try {
            val base = if (host.startsWith("http", ignoreCase = true)) host else "http://$host"
            val url = "${base.trimEnd('/')}/player_api.php".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("username", user)
                ?.addQueryParameter("password", pass)
                ?.build() ?: return State.NO_SERVER
            val body = client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) return State.NO_SERVER
                r.body?.string().orEmpty()
            }
            val info = JSONObject(body).optJSONObject("user_info") ?: return State.INVALID
            if ((info.opt("auth")?.toString() ?: "0") != "1") return State.INVALID
            val status = info.optString("status")
            val exp = info.optString("exp_date").toLongOrNull() ?: 0L
            when {
                status.equals("Expired", ignoreCase = true) -> State.EXPIRED
                exp > 0L && exp < System.currentTimeMillis() / 1000 -> State.EXPIRED
                status.equals("Active", ignoreCase = true) -> State.ACTIVE
                else -> State.INVALID
            }
        } catch (e: java.io.IOException) {
            State.NO_SERVER
        } catch (e: Exception) {
            State.INVALID
        }
    }

    // ---- HTTP y descifrado --------------------------------------------------

    private fun get(url: String): String {
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        return client.newCall(request).execute().use { r ->
            if (!r.isSuccessful) throw java.io.IOException("HTTP ${r.code}")
            r.body?.string().orEmpty()
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "respuesta no válida" }
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(hex[i * 2], 16)
            val lo = Character.digit(hex[i * 2 + 1], 16)
            require(hi >= 0 && lo >= 0) { "respuesta no válida" }
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    /** Texto volteado y convertido de hexadecimal a texto. */
    private fun hexReversed(text: String): String =
        String(hexToBytes(text.trim().reversed()), Charsets.UTF_8)

    /** Hexadecimal cifrado con AES-256-CBC más, al final, la clave (16 caracteres) al revés. */
    private fun decryptDns(text: String): String {
        val t = text.trim()
        require(t.length > 16) { "respuesta no válida" }
        val key = t.takeLast(16).reversed()
        val data = hexToBytes(t.dropLast(16))
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec((key + key).toByteArray(Charsets.UTF_8), "AES"),
            IvParameterSpec("R4tghjg^425(@#Gg".toByteArray(Charsets.UTF_8)),
        )
        return String(cipher.doFinal(data), Charsets.UTF_8)
    }
}
