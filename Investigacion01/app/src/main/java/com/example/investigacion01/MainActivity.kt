package com.example.investigacion01

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
//  Esta importación es automática por el  "View Binding" que active en el gradle.
// Une este archivo de código (Kotlin) con la pantalla visual (activity_main.xml).
import com.example.investigacion01.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    /*
      ¿QUÉ ES ESTO?: Es la variable global que guardará el acceso a todos los botones,
      textos y barras de la pantalla sin usar el viejo 'findViewById'.
      El 'lateinit' le dice a Kotlin: "Tranquilo, la voy a inicializar un poquito más abajo".
    */
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
            mensaje para barcita y los demas ahi les deje comentarios para que vean como funciona cada parte,
            si no saben pa que sirve escribanme a mi o al grupo y borran esto cuando terminen
          ASOCIACIÓN DE LA VISTA:
          1. Inflate: Lee el archivo XML (activity_main.xml) y lo traduce a código que el teléfono entiende.
          2. setContentView: Le dice a la actividad: "Muestra esta pantalla en el celular".
        */
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*



          ¿
          Porque la lógica de qué pasa cuando aprietas el botón "Añadir" o "Iniciar" se programa
          en los puntos 4, 5 y 6. Por ahora, el código solo se encarga de mostrar los diseños limpios.

          Ejemplo de cómo lo asociaremos más adelante:
          binding.btnAgregarTarea.setOnClickListener { ... }
        */
    }
}