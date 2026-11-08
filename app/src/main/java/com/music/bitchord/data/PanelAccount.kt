package com.music.bitchord.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/**
 * Cálculos de la vigencia de la cuenta del panel: cuánto falta, cómo se
 * muestra la fecha y el contador, y la regla de cuándo avisar.
 */
object PanelAccount {

    /** El aviso aparece cuando faltan 5 días o menos (demos incluidos). */
    const val NOTICE_DAYS = 5

    private const val DAY_MS = 86_400_000L

    /** Milisegundos que faltan, según la hora del servidor. Sin vencimiento: [Long.MAX_VALUE]. */
    fun remainingMs(expiresAt: Long, offsetMs: Long): Long =
        if (expiresAt <= 0L) Long.MAX_VALUE
        else expiresAt * 1000 - (System.currentTimeMillis() + offsetMs)

    /** Días que faltan, redondeados hacia arriba (queda 1 día aunque falten 3 horas). */
    fun daysLeft(remainingMs: Long): Long =
        if (remainingMs <= 0L) 0L else ceil(remainingMs / DAY_MS.toDouble()).toLong()

    /** ¿Toca mostrar el aviso? Solo con vencimiento, sin vencer todavía y a [NOTICE_DAYS] días o menos. */
    fun shouldNotify(expiresAt: Long, offsetMs: Long): Boolean {
        if (expiresAt <= 0L) return false
        val left = remainingMs(expiresAt, offsetMs)
        return left > 0L && daysLeft(left) <= NOTICE_DAYS
    }

    fun formatDate(expiresAt: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(expiresAt * 1000))

    /** "2 d 05:12:33" o "05:12:33". */
    fun formatCountdown(remainingMs: Long): String {
        val total = (remainingMs.coerceAtLeast(0L)) / 1000
        val days = total / 86_400
        val h = (total % 86_400) / 3_600
        val m = (total % 3_600) / 60
        val s = total % 60
        val clock = String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        return if (days > 0) "$days d $clock" else clock
    }
}
