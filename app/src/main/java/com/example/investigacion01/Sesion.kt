package com.example.investigacion01

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ========== PUNTO 7: REGISTRO DE SESIONES ==========
 *
 * Representa una sesión de Pomodoro completada (una cuenta regresiva que
 * llegó a cero), asociada a la tarea que estaba activa en ese momento.
 *
 * Se guarda [tareaTitulo] como copia del título (y no solo [tareaId]) porque
 * si esa tarea llega a eliminarse más adelante, el historial debe seguir
 * mostrando a qué tarea perteneció la sesión en su momento.
 *
 * [tareaId] es nulable porque, según el análisis del punto 7, si el usuario
 * no tiene ninguna tarea activa no debe poder iniciar el temporizador
 * (ver MainActivity.setupBotones). Se deja nulable de todas formas para que
 * la clase sea robusta ante ese caso sin necesitar valores "mágicos".
 */
data class Sesion(
    val id: Int,
    val tareaId: Int?,
    val tareaTitulo: String,
    val duracionMinutos: Int,
    val fechaHora: String // Lo cambia a String para evitar conflictos de tipo

) {
    fun fechaFormateada(): String {
        val millis = fechaHora.toLongOrNull() ?: return fechaHora
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))
    }
}
