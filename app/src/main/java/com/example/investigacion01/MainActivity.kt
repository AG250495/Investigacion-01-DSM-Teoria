package com.example.investigacion01

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.investigacion01.databinding.ActivityMainBinding
import com.example.investigacion01.databinding.ItemHistorialBinding
import com.example.investigacion01.databinding.ItemTareaBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // ========== PASO 9: el estado ya NO vive en la Activity ==========
    // Todo (tareas, historial, tarea activa, temporizador) vive en el
    // ViewModel, que sobrevive a la rotación de pantalla. La Activity solo
    // observa esos datos y redibuja la pantalla cuando cambian.
    private val viewModel: PomodoroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBotones()
        observarViewModel()

        // Solo se agregan las tareas de ejemplo la primera vez (si la lista
        // ya tiene datos, es porque venimos de una rotación de pantalla, y el
        // ViewModel ya conservó lo que había).
        if (viewModel.tareas.value.isNullOrEmpty()) {
            viewModel.agregarTarea("Aprender View Binding")
            viewModel.agregarTarea("Hacer ejercicio")
            viewModel.agregarTarea("Leer un libro")
        }
    }

    // ========== PASO 8: ganchos de ciclo de vida ==========
    override fun onResume() {
        super.onResume()
        // Recalcula el tiempo real transcurrido si el timer seguía corriendo
        // mientras la app estuvo en segundo plano (Paso 10).
        viewModel.onAppForeground()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppBackground()
    }

    // ========== PASO 9: observar el ViewModel ==========

    // Cache local solo para poder redibujar la lista de tareas sabiendo cuál
    // está activa en este momento (evita tener que consultar el ViewModel
    // dos veces desde dentro del bucle de dibujo).
    private var tareaActivaIdActual: Int? = null

    private fun observarViewModel() {
        viewModel.tareas.observe(this) { lista ->
            redibujarTareas(lista)
        }

        viewModel.tareaActivaId.observe(this) { id ->
            tareaActivaIdActual = id
            binding.tvActiveTask.text = if (id != null) {
                val titulo = viewModel.tareas.value?.firstOrNull { it.id == id }?.titulo
                "Tarea activa: ${titulo ?: ""}"
            } else {
                "Sin tarea activa"
            }
            // La tarjeta resaltada depende de cuál es la tarea activa, así que
            // hay que redibujar cuando esta cambia.
            redibujarTareas(viewModel.tareas.value ?: emptyList())
        }

        viewModel.historial.observe(this) { lista ->
            if (lista.isNotEmpty()) {
                binding.tvHistorialVacio.visibility = View.GONE
                redibujarHistorial() // Asegúrate de llamar a esto aquí
            }
        }

        viewModel.milisRestantes.observe(this) { milis ->
            val totales = viewModel.milisTotales.value ?: PomodoroTimer.DURACION_DEFECTO_MILLIS
            actualizarVistaTemporizador(milis, totales)
        }

        viewModel.estadoTimer.observe(this) {
            actualizarBotonInicio()
        }

        viewModel.sesionCompletadaEvento.observe(this) { idEvento ->
            if (idEvento != null) {
                Snackbar.make(
                    binding.root,
                    "¡Sesión completada! 🍅 Se registró en el historial.",
                    Snackbar.LENGTH_LONG
                ).show()
                viewModel.consumirEventoSesionCompletada()
            }
        }
    }

    // Refresca el TextView del tiempo y el ProgressBar de progreso
    private fun actualizarVistaTemporizador(milisRestantes: Long, milisTotales: Long) {
        binding.tvTimer.text = PomodoroTimer.formatearTiempo(milisRestantes)

        val totalSegundos = (milisTotales / 1000).toInt()
        val segundosRestantes = (milisRestantes / 1000).toInt()
        binding.progressBarPomodoro.max = totalSegundos
        binding.progressBarPomodoro.progress = segundosRestantes
    }

    // El botón único (btnStartPause) cambia de texto según el estado del timer.
    private fun actualizarBotonInicio() {
        binding.btnStartPause.text = when (viewModel.estadoTimer.value) {
            PomodoroTimer.Estado.EN_CURSO -> "Pausar"
            PomodoroTimer.Estado.PAUSADO -> "Reanudar"
            else -> "Iniciar"
        }
    }

    // ========== CONFIGURAR BOTONES ==========
    private fun setupBotones() {
        // Botón "Añadir" - Punto 5
        binding.btnAgregarTarea.setOnClickListener {
            val texto = binding.etNuevaTarea.text.toString().trim()
            if (texto.isNotEmpty()) {
                viewModel.agregarTarea(texto)
                binding.etNuevaTarea.text.clear()
            }
        }

        // ========== PUNTO 6: BOTONES DEL TEMPORIZADOR ==========
        binding.btnStartPause.setOnClickListener {
            when (viewModel.estadoTimer.value) {
                PomodoroTimer.Estado.EN_CURSO -> viewModel.pausarTimer()
                PomodoroTimer.Estado.PAUSADO -> viewModel.reanudarTimer()
                else -> {
                    // Buena práctica (Punto 7): si no hay tarea activa, no se
                    // deja arrancar el temporizador.
                    if (viewModel.tareaActivaId.value == null) {
                        Snackbar.make(
                            binding.root,
                            "Selecciona una tarea activa antes de iniciar el temporizador",
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                    viewModel.iniciarTimer()
                }
            }
        }

        binding.btnReset.setOnClickListener {
            viewModel.reiniciarTimer()
        }
    }

    // ========== PUNTO 4: GENERAR VISTAS DINÁMICAMENTE ==========
    private fun redibujarTareas(listaTareas: List<Tarea>) {
        val contenedor = binding.containerTareas
        contenedor.removeAllViews()

        if (listaTareas.isEmpty()) {
            binding.tvTareasVacio.visibility = View.VISIBLE
            actualizarContadores(listaTareas)
            return
        }

        binding.tvTareasVacio.visibility = View.GONE

        for (tarea in listaTareas) {
            val itemBinding = ItemTareaBinding.inflate(layoutInflater)

            itemBinding.tvNombreTarea.text = tarea.titulo
            itemBinding.cbCompletada.isChecked = tarea.completada

            if (tarea.completada) {
                itemBinding.tvNombreTarea.setTextColor(Color.parseColor("#888888"))
                itemBinding.tvNombreTarea.paintFlags =
                    itemBinding.tvNombreTarea.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                itemBinding.tvNombreTarea.setTextColor(Color.parseColor("#000000"))
                itemBinding.tvNombreTarea.paintFlags =
                    itemBinding.tvNombreTarea.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            val esActiva = tarea.id == tareaActivaIdActual
            if (esActiva) {
                itemBinding.root.strokeColor = Color.parseColor("#FF7043")
                itemBinding.root.strokeWidth = 4
            } else {
                itemBinding.root.strokeColor = Color.TRANSPARENT
                itemBinding.root.strokeWidth = 0
            }

            itemBinding.root.setOnClickListener {
                viewModel.seleccionarTareaActiva(tarea.id)
            }

            itemBinding.cbCompletada.setOnClickListener {
                viewModel.marcarCompletada(tarea.id, itemBinding.cbCompletada.isChecked)
            }

            itemBinding.btnEliminarTarea.setOnClickListener {
                viewModel.eliminarTarea(tarea.id)
            }

            contenedor.addView(itemBinding.root)
        }

        actualizarContadores(listaTareas)
    }

    // ========== HISTORIAL (PUNTO 4 + PUNTO 7) ==========
    private fun redibujarHistorial() {
        val historialSesiones = viewModel.historial.value ?: emptyList()
        val contenedor = binding.containerHistorial
        contenedor.removeAllViews()

        if (historialSesiones.isEmpty()) {
            binding.tvHistorialVacio.visibility = View.VISIBLE
            actualizarContadores(viewModel.tareas.value ?: emptyList())
            return
        }

        binding.tvHistorialVacio.visibility = View.GONE

        // Más reciente primero
        for (sesion in historialSesiones.asReversed()) {
            val itemBinding = ItemHistorialBinding.inflate(layoutInflater)
            itemBinding.tvHistorialInfo.text =
                "Sesión Pomodoro completada (${sesion.duracionMinutos} min)"
            itemBinding.tvHistorialTarea.text =
                "Asociada a: ${sesion.tareaTitulo} • ${sesion.fechaFormateada()}"
            contenedor.addView(itemBinding.root)
        }

        actualizarContadores(viewModel.tareas.value ?: emptyList())
    }

    // ========== ACTUALIZAR CONTADORES (PUNTO 5 + PUNTO 7) ==========
    private fun actualizarContadores(listaTareas: List<Tarea>) {
        val pendientes = listaTareas.count { !it.completada }
        val sesionesCompletadas = viewModel.historial.value?.size ?: 0

        binding.tvTareasPendientes.text = "Tareas pendientes: $pendientes"
        binding.tvSesionesCompletadas.text = "Pomodoros: $sesionesCompletadas"
    }


}
