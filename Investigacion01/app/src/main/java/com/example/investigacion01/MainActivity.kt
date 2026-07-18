package com.example.investigacion01

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.investigacion01.databinding.ActivityMainBinding
import com.example.investigacion01.databinding.ItemHistorialBinding
import com.example.investigacion01.databinding.ItemTareaBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // ========== DATOS (PUNTO 5) ==========
    // Lista donde guardaremos todas las tareas
    private val listaTareas = mutableListOf<Tarea>()

    // Contador para generar IDs únicos
    private var contadorId = 0

    // Lista para el historial de sesiones completadas
    private val historialSesiones = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ========== CONFIGURAR BOTONES (PUNTO 5) ==========
        setupBotones()

        // Agregar algunas tareas de ejemplo para probar
        agregarTareaEjemplo("Aprender View Binding")
        agregarTareaEjemplo("Hacer ejercicio")
        agregarTareaEjemplo("Leer un libro")
    }

    // ========== CONFIGURAR BOTONES ==========
    private fun setupBotones() {
        // Botón "Añadir" - Punto 5
        binding.btnAgregarTarea.setOnClickListener {
            val texto = binding.etNuevaTarea.text.toString().trim()
            if (texto.isNotEmpty()) {
                agregarTarea(texto)
                binding.etNuevaTarea.text.clear() // Limpiar el campo
            }
        }

        // Botones del Pomodoro (para el punto 6)
        // Los dejamos vacíos por ahora, los completarás después
        binding.btnStartPause.setOnClickListener {
            // TODO: Punto 6 - Iniciar/Pausar cronómetro
        }

        binding.btnReset.setOnClickListener {
            // TODO: Punto 6 - Reiniciar cronómetro
        }
    }

    // ========== PUNTO 4: GENERAR VISTAS DINÁMICAMENTE ==========
    private fun redibujarTareas() {
        // 1. Obtener el contenedor
        val contenedor = binding.containerTareas

        // 2. LIMPIAR el contenedor antes de redibujar (¡importante!)
        contenedor.removeAllViews()

        // 3. Si no hay tareas, mostrar mensaje de vacío
        if (listaTareas.isEmpty()) {
            binding.tvTareasVacio.visibility = View.VISIBLE
            return
        }

        // 4. Ocultar mensaje de vacío
        binding.tvTareasVacio.visibility = View.GONE

        // 5. Inflar UNA vista por cada tarea usando View Binding
        for (tarea in listaTareas) {
            // Inflar el layout item_tarea.xml
            val itemBinding = ItemTareaBinding.inflate(layoutInflater)

            // Asignar los datos a la vista
            itemBinding.tvNombreTarea.text = tarea.titulo
            itemBinding.cbCompletada.isChecked = tarea.completada

            // Si está completada, tachar el texto
            if (tarea.completada) {
                itemBinding.tvNombreTarea.setTextColor(
                    android.graphics.Color.parseColor("#888888")
                )
                // Opcional: tachar el texto
                itemBinding.tvNombreTarea.paintFlags =
                    itemBinding.tvNombreTarea.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                itemBinding.tvNombreTarea.setTextColor(
                    android.graphics.Color.parseColor("#000000")
                )
                itemBinding.tvNombreTarea.paintFlags =
                    itemBinding.tvNombreTarea.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // ========== PUNTO 5: LISTENERS DE CADA TAREA ==========
            // Completar tarea (CheckBox)
            itemBinding.cbCompletada.setOnClickListener {
                // Actualizar el dato
                tarea.completada = itemBinding.cbCompletada.isChecked

                // Si se completó, agregar al historial (Punto 5)
                if (tarea.completada) {
                    agregarAlHistorial(tarea.titulo)
                }

                // Redibujar TODO
                redibujarTareas()
                actualizarContadores()
            }

            // Eliminar tarea (botón basurero)
            itemBinding.btnEliminarTarea.setOnClickListener {
                // Eliminar de la lista de datos
                listaTareas.remove(tarea)

                // Redibujar TODO
                redibujarTareas()
                actualizarContadores()
            }

            // Agregar la vista inflada al contenedor
            contenedor.addView(itemBinding.root)
        }

        // Actualizar contadores
        actualizarContadores()
    }

    // ========== PUNTO 5: CRUD DE TAREAS ==========

    // Agregar tarea
    private fun agregarTarea(titulo: String) {
        val nuevaTarea = Tarea(
            id = contadorId++,
            titulo = titulo
        )
        listaTareas.add(nuevaTarea)
        redibujarTareas()
        actualizarContadores()
    }

    // Agregar tarea de ejemplo (solo para pruebas)
    private fun agregarTareaEjemplo(titulo: String) {
        val nuevaTarea = Tarea(
            id = contadorId++,
            titulo = titulo
        )
        listaTareas.add(nuevaTarea)
        redibujarTareas()
        actualizarContadores()
    }

    // Agregar al historial (Punto 5)
    private fun agregarAlHistorial(tituloTarea: String) {
        val mensaje = "Sesión completada: $tituloTarea"
        historialSesiones.add(mensaje)
        redibujarHistorial()
    }

    // ========== HISTORIAL (PUNTO 4) ==========
    private fun redibujarHistorial() {
        val contenedor = binding.containerHistorial
        contenedor.removeAllViews()

        if (historialSesiones.isEmpty()) {
            binding.tvHistorialVacio.visibility = View.VISIBLE
            return
        }

        binding.tvHistorialVacio.visibility = View.GONE

        for (sesion in historialSesiones) {
            val itemBinding = ItemHistorialBinding.inflate(layoutInflater)
            itemBinding.tvHistorialInfo.text = sesion
            // Podemos mostrar la tarea asociada en el subtítulo
            itemBinding.tvHistorialTarea.text = "Completada correctamente"
            contenedor.addView(itemBinding.root)
        }
    }

    // ========== ACTUALIZAR CONTADORES (PUNTO 5) ==========
    private fun actualizarContadores() {
        val pendientes = listaTareas.count { !it.completada }
        val completadas = listaTareas.count { it.completada }

        binding.tvTareasPendientes.text = "Tareas pendientes: $pendientes"
        binding.tvSesionesCompletadas.text = "Pomodoros: $completadas"
    }
}