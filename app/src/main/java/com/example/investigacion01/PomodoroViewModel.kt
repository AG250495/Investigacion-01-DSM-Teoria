package com.example.investigacion01

import android.app.Application
import android.os.SystemClock
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/*
VERSIÓN 2 del ViewModel — adaptada para usar las clases que ya hizo el
equipo en la rama pasos-6-y-7:
- Tarea (id: Int, titulo: String, completada: Boolean)
- Sesion (id, tareaId, tareaTitulo, duracionMinutos, fechaHora)
- PomodoroTimer (la clase del cronómetro, con Listener + Estado)

Requiere que en PomodoroTimer.kt se agregue el método
ajustarPorTiempoTranscurrido(millis) — ver el archivo
"PARA_TU_COMPANERO_agregar_a_PomodoroTimer.kt".

IMPORTANTE PARA QUIEN CONECTE ESTO A MainActivity:
Las variables listaTareas, historialSesiones, tareaActivaId y
pomodoroTimer que hoy viven como propiedades de la Activity deben
eliminarse de ahí. En su lugar, la Activity solo debe:
- obtener el ViewModel con `by viewModels()`
- observar tareas / historial / tareaActivaId / milisRestantes /
milisTotales / estadoTimer / sesionCompletadaEvento
- llamar a las funciones públicas de este ViewModel desde los
listeners de los botones (agregarTarea, iniciarTimer, etc.)
- llamar a viewModel.onAppForeground() en onResume()
- llamar a viewModel.onAppBackground() en onPause()
*/

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    // ========== PASO 11: PERSISTENCIA (DataStore + serialización JSON con Gson) ==========
    // Solo el historial de sesiones se guarda entre reinicios de la app.
    // SharedPreferences no se usa: DataStore es su reemplazo (ver DataStoreManager.kt).
    // No hay archivo .json en disco: Gson produce un String que DataStore almacena por clave.
    private val dataStore = application.dataStore
    private val HISTORIAL_KEY = stringPreferencesKey("historial_json")
    private val gson = Gson()

    // =========================================================
    //  TAREAS
    // =========================================================
    private val _tareas = MutableLiveData<List<Tarea>>(emptyList())
    val tareas: LiveData<List<Tarea>> = _tareas
    private var siguienteIdTarea = 1

    fun agregarTarea(titulo: String): Boolean {
        val limpio = titulo.trim()
        if (limpio.isEmpty()) return false
        val nueva = Tarea(id = siguienteIdTarea++, titulo = limpio)
        _tareas.value = (_tareas.value ?: emptyList()) + nueva
        return true
    }

    fun eliminarTarea(id: Int) {
        _tareas.value = (_tareas.value ?: emptyList()).filter { it.id != id }
        if (_tareaActivaId.value == id) _tareaActivaId.value = null
    }

    fun marcarCompletada(id: Int, completada: Boolean) {
        _tareas.value = (_tareas.value ?: emptyList()).map {
            if (it.id == id) it.copy(completada = completada) else it
        }
    }

    private val _tareaActivaId = MutableLiveData<Int?>(null)
    val tareaActivaId: LiveData<Int?> = _tareaActivaId

    fun seleccionarTareaActiva(id: Int) { _tareaActivaId.value = id }

    private fun tituloTareaActiva(): String {
        val id = _tareaActivaId.value ?: return "Sin tarea activa"
        return _tareas.value?.firstOrNull { it.id == id }?.titulo ?: "Sin tarea activa"
    }

    // =========================================================
    //  HISTORIAL
    // =========================================================
    private val _historial = MutableLiveData<List<Sesion>>(emptyList())
    val historial: LiveData<List<Sesion>> = _historial

    // =========================================================
    //  TEMPORIZADOR
    // =========================================================
    private val pomodoroTimer = PomodoroTimer()
    val milisRestantes = MutableLiveData(pomodoroTimer.milisRestantes)
    val milisTotales = MutableLiveData(pomodoroTimer.milisTotales)
    val estadoTimer = MutableLiveData(pomodoroTimer.estado)
    private val _sesionCompletadaEvento = MutableLiveData<Int?>(null)
    val sesionCompletadaEvento: LiveData<Int?> = _sesionCompletadaEvento

    init {
        // PASO 11 — Lectura: cargar historial desde DataStore al iniciar la app.
        // dataStore.data.first() obtiene las preferencias; gson.fromJson() deserializa
        // el String JSON guardado en "historial_json" de vuelta a List<Sesion>.
        viewModelScope.launch {
            try {
                val prefs = dataStore.data.first()
                val json = prefs[HISTORIAL_KEY]
                if (json != null) {
                    val type = object : TypeToken<List<Sesion>>() {}.type
                    // Cambio: postValue para notificar a la UI de forma segura desde la corrutina
                    _historial.postValue(gson.fromJson(json, type) ?: emptyList())
                }
            } catch (e: Exception) {
                // PASO 11 — Si el JSON está corrupto, no crashear; empezar con historial vacío
                _historial.postValue(emptyList())
            }
        }

        pomodoroTimer.listener = object : PomodoroTimer.Listener {
            override fun onTick(milis: Long, total: Long) {
                milisRestantes.postValue(milis)
                milisTotales.postValue(total)
            }
            override fun onFinish() {
                estadoTimer.postValue(PomodoroTimer.Estado.PAUSADO)
                // Cambio para asegurar el hilo principal y evitar crashes:
                // viewModelScope se cancela con el ViewModel; Dispatchers.Main garantiza el hilo UI
                viewModelScope.launch(Dispatchers.Main) {
                    finalizarSesion()
                }
            }
        }
    }

    fun iniciarTimer() { pomodoroTimer.iniciar(); estadoTimer.value = pomodoroTimer.estado }
    fun pausarTimer() { pomodoroTimer.pausar(); estadoTimer.value = pomodoroTimer.estado }
    fun reanudarTimer() { pomodoroTimer.reanudar(); estadoTimer.value = pomodoroTimer.estado }
    fun reiniciarTimer() {
        pomodoroTimer.reiniciar()
        estadoTimer.value = pomodoroTimer.estado
        milisRestantes.value = pomodoroTimer.milisRestantes
    }

    // =========================================================
    //  CICLO DE VIDA
    // =========================================================
    private var momentoBackgroundElapsed: Long? = null
    private var estabaEnCursoAntesDeBackground = false

    fun onAppBackground() {
        estabaEnCursoAntesDeBackground = pomodoroTimer.estado == PomodoroTimer.Estado.EN_CURSO
        if (estabaEnCursoAntesDeBackground) {
            pomodoroTimer.pausar()
            momentoBackgroundElapsed = SystemClock.elapsedRealtime()
        }
        estadoTimer.value = pomodoroTimer.estado
    }

    fun onAppForeground() {
        val inicio = momentoBackgroundElapsed
        if (inicio != null && estabaEnCursoAntesDeBackground) {
            pomodoroTimer.ajustarPorTiempoTranscurrido(SystemClock.elapsedRealtime() - inicio)
        }
        momentoBackgroundElapsed = null
        estabaEnCursoAntesDeBackground = false
        milisRestantes.value = pomodoroTimer.milisRestantes
        estadoTimer.value = pomodoroTimer.estado
    }

    // =========================================================
    //  LÓGICA INTERNA
    // =========================================================
    private fun finalizarSesion() {
        val sesion = Sesion(
            id = (_historial.value?.size ?: 0) + 1,
            tareaId = _tareaActivaId.value,
            tareaTitulo = tituloTareaActiva(),
            duracionMinutos = (pomodoroTimer.milisTotales / 60000L).toInt(),
            fechaHora = System.currentTimeMillis().toString()
        )

        // Actualizamos la lista local en memoria (esto hará que la UI se actualice)
        val listaActualizada = (_historial.value ?: emptyList()) + sesion
        // Cambio: postValue para entregar el cambio a observers de LiveData de forma thread-safe
        _historial.postValue(listaActualizada)

        // PASO 11 — Escritura: serializar la lista con Gson y guardarla en DataStore.
        // gson.toJson() convierte List<Sesion> → String JSON; dataStore.edit() persiste
        // ese valor bajo HISTORIAL_KEY. try-catch evita crash si falla la serialización.
        viewModelScope.launch {
            try {
                val json = gson.toJson(listaActualizada)
                dataStore.edit { prefs -> prefs[HISTORIAL_KEY] = json }
            } catch (e: Exception) {
                // La sesión ya está en memoria y la UI ya se actualizó; el guardado puede fallar sin cerrar la app
            }
        }

        // Cambio: postValue para que MainActivity reciba el evento y muestre el Snackbar
        _sesionCompletadaEvento.postValue(sesion.id)
    }

    fun consumirEventoSesionCompletada() { _sesionCompletadaEvento.value = null }
}