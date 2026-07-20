package com.example.investigacion01

import android.os.CountDownTimer

/**
 * ========== PUNTO 6: TEMPORIZADOR POMODORO ==========
 *
 * Encapsula toda la lógica de la cuenta regresiva, separada por completo de
 * la interfaz (esta clase no conoce TextView, ProgressBar ni Activity alguna).
 *
 * Se eligió [CountDownTimer] porque:
 *  - Está diseñado específicamente para cuentas regresivas y entrega sus
 *    callbacks (onTick/onFinish) en el hilo principal, así que se puede
 *    refrescar la UI directamente sin manejar Handlers ni hilos manualmente.
 *  - El proyecto no usa corrutinas en ningún otro punto; usar `delay()` en un
 *    bucle habría exigido introducir un CoroutineScope, su cancelación y
 *    dispatchers solo para esto, complejidad innecesaria en esta etapa.
 *
 * [CountDownTimer] no soporta "pausar" de forma nativa, así que la pausa se
 * resuelve cancelando el timer interno y conservando [milisRestantes]; al
 * reanudar se crea una nueva instancia que arranca desde ese remanente.
 *
 * Antes de iniciar SIEMPRE se cancela cualquier timer interno previo, por lo
 * que nunca pueden quedar dos [CountDownTimer] corriendo al mismo tiempo.
 */
class PomodoroTimer(
    private var duracionTotalMillis: Long = DURACION_DEFECTO_MILLIS
) {

    companion object {
        const val DURACION_DEFECTO_MINUTOS = 1
        const val DURACION_DEFECTO_MILLIS = DURACION_DEFECTO_MINUTOS * 60 * 1000L
        private const val INTERVALO_TICK_MILLIS = 1000L

        /** Convierte milisegundos restantes al formato mm:ss pedido por la rúbrica. */
        fun formatearTiempo(millis: Long): String {
            val totalSegundos = millis / 1000
            val minutos = totalSegundos / 60
            val segundos = totalSegundos % 60
            return String.format("%02d:%02d", minutos, segundos)
        }
    }

    enum class Estado { DETENIDO, EN_CURSO, PAUSADO }

    /** Contrato que la Activity implementa para reaccionar a los cambios del timer. */
    interface Listener {
        fun onTick(milisRestantes: Long, milisTotales: Long)
        fun onFinish()
    }

    var listener: Listener? = null

    var estado: Estado = Estado.DETENIDO
        private set

    var milisRestantes: Long = duracionTotalMillis
        private set

    val milisTotales: Long
        get() = duracionTotalMillis

    private var countDownTimerInterno: CountDownTimer? = null

    /**
     * Permite configurar la duración de la sesión (por defecto 25 minutos,
     * ver [DURACION_DEFECTO_MINUTOS]). Solo se aplica si el timer está
     * detenido, para no alterar una cuenta regresiva en curso.
     */
    fun configurarDuracion(minutos: Int) {
        if (estado == Estado.DETENIDO && minutos > 0) {
            duracionTotalMillis = minutos * 60 * 1000L
            milisRestantes = duracionTotalMillis
        }
    }

    /** Inicia la cuenta regresiva desde el total. No hace nada si ya está en curso. */
    fun iniciar() {
        if (estado == Estado.EN_CURSO) return
        cancelarTimerInterno()
        arrancarDesde(duracionTotalMillis)
        estado = Estado.EN_CURSO
    }

    /** Pausa la cuenta regresiva, conservando el tiempo restante. */
    fun pausar() {
        if (estado != Estado.EN_CURSO) return
        cancelarTimerInterno()
        estado = Estado.PAUSADO
    }

    /** Reanuda la cuenta regresiva desde donde quedó al pausar. */
    fun reanudar() {
        if (estado != Estado.PAUSADO) return
        arrancarDesde(milisRestantes)
        estado = Estado.EN_CURSO
    }

    /** Cancela cualquier cuenta en curso y regresa el tiempo restante al total. */
    fun reiniciar() {
        cancelarTimerInterno()
        milisRestantes = duracionTotalMillis
        estado = Estado.DETENIDO
        listener?.onTick(milisRestantes, duracionTotalMillis)
    }

    private fun arrancarDesde(desdeMillis: Long) {
        countDownTimerInterno = object : CountDownTimer(desdeMillis, INTERVALO_TICK_MILLIS) {
            override fun onTick(millisUntilFinished: Long) {
                milisRestantes = millisUntilFinished
                listener?.onTick(milisRestantes, duracionTotalMillis)
            }

            override fun onFinish() {
                milisRestantes = 0L
                estado = Estado.DETENIDO
                countDownTimerInterno = null
                listener?.onTick(0L, duracionTotalMillis)
                listener?.onFinish()
            }
        }.start()
    }

    private fun cancelarTimerInterno() {
        countDownTimerInterno?.cancel()
        countDownTimerInterno = null
    }
    /**
     * PASO 10 — Se llama al volver del segundo plano (desde el ViewModel).
     * Descuenta el tiempo real transcurrido y reanuda o finaliza según
     * corresponda.
     */
    fun ajustarPorTiempoTranscurrido(millisTranscurridos: Long) {
        if (estado != Estado.PAUSADO) return

        val nuevoRestante = milisRestantes - millisTranscurridos
        if (nuevoRestante <= 0) {
            milisRestantes = 0L
            estado = Estado.DETENIDO
            listener?.onTick(0L, duracionTotalMillis)
            listener?.onFinish()
        } else {
            milisRestantes = nuevoRestante
            arrancarDesde(milisRestantes)
            estado = Estado.EN_CURSO
        }
    }
}
