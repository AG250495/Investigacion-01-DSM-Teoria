package com.example.investigacion01

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * ========== PASO 11: PERSISTENCIA DE DATOS ==========
 *
 * Android ofrece varias técnicas para guardar datos localmente:
 * - SharedPreferences: almacén clave-valor síncrono (API clásica, hoy en desuso).
 * - DataStore (Preferences): sucesor recomendado por Google; lectura/escritura
 *   asíncrona y segura ante accesos concurrentes.
 * - Archivos con serialización JSON: se escribe un .json en el disco del dispositivo
 *   (p. ej. con FileOutputStream); útil para datos grandes o exportables.
 *
 * ¿Qué se eligió y por qué?
 * - Se usa DataStore + Gson (serialización JSON), NO SharedPreferences ni un archivo
 *   .json suelto en el sistema de archivos.
 * - El historial es una List<Sesion> (objetos con varios campos); Gson la convierte
 *   a un String JSON y DataStore la guarda bajo la clave "historial_json".
 * - DataStore evita bloquear el hilo principal y reemplaza a SharedPreferences para
 *   este tipo de preferencias pequeñas.
 * - Un archivo JSON manual habría exigido gestionar rutas, permisos y errores de I/O;
 *   para un historial compacto, DataStore + Gson es más simple y estable.
 *
 * La lectura y escritura concretas están en PomodoroViewModel (init y finalizarSesion).
 */
// La clave es que el nombre sea consistente
val Context.dataStore by preferencesDataStore(name = "pomodoro_prefs")
