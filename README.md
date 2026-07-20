# Investigación 01 — DSM: Temporizador Pomodoro (Android)
 
Aplicación Android hecha en Kotlin que implementa un temporizador, con lista de tareas, registro de sesiones e historial que se guarda entre reinicios de la app.
 
## Descripción
 
La app permite crear tareas, elegir una como "activa" y correr una cuenta regresiva (Pomodoro) asociada a esa tarea. Cuando la cuenta regresiva termina, se guarda automáticamente una sesión en el historial y se avisa al usuario. También se lleva un resumen con las tareas pendientes y los pomodoros completados.
 
Está hecha con arquitectura MVVM: el `ViewModel` guarda todo el estado (tareas, historial, temporizador) y la `Activity` solo observa esos datos y actualiza la pantalla, por lo que el estado no se pierde si el celular rota la pantalla.
 
## Funcionalidades
 
- Agregar, eliminar y marcar tareas como completadas.
- Seleccionar una tarea como activa (se resalta en la lista).
- Temporizador con botones de Iniciar / Pausar / Reanudar / Reiniciar.
- Muestra el tiempo restante (mm:ss) y una barra de progreso.
- Al completar el temporizador, se genera una sesión asociada a la tarea activa.
- Notificación (Snackbar) cuando se completa una sesión.
- Historial de sesiones con fecha, duración y tarea.
- Resumen con contador de tareas pendientes y pomodoros completados.
- El historial se guarda con DataStore, así que no se pierde al cerrar la app.
- Si la app pasa a segundo plano con el temporizador corriendo, al volver se ajusta el tiempo real que pasó.
## Flujo de uso
 
1. Al abrir la app aparecen tres tareas de ejemplo (solo la primera vez que se abre).
2. El usuario toca una tarea para dejarla como "activa".
3. Presiona **Iniciar** para arrancar la cuenta regresiva (1 minuto por defecto).
4. Puede pausar y reanudar cuando quiera, o reiniciar para volver al tiempo completo.
5. Si intenta iniciar sin tener una tarea activa, la app le avisa que primero debe seleccionar una.
6. Cuando el tiempo llega a cero, sale un mensaje confirmando que la sesión se guardó en el historial.
7. El resumen (tareas pendientes / pomodoros completados) se actualiza solo.
8. Si el usuario sale de la app mientras el temporizador corre, al regresar se recalcula cuánto tiempo pasó realmente.
## Archivos principales
 
### Código Kotlin (`app/src/main/java/.../investigacion01/`)
 
- `MainActivity.kt` — la pantalla principal. Observa el ViewModel, dibuja la lista de tareas y el historial, y conecta los botones (agregar tarea, iniciar/pausar/reiniciar el temporizador).
- `PomodoroViewModel.kt` — el cerebro de la app. Guarda todo el estado (tareas, tarea activa, historial, temporizador) y la lógica de negocio: agregar/eliminar tareas, controlar el timer, generar sesiones y guardarlas.
- `PomodoroTimer.kt` — la cuenta regresiva (Punto 6). Usa `CountDownTimer` y maneja iniciar, pausar, reanudar y reiniciar.
- `Sesion.kt` — el modelo de una sesión Pomodoro completada (Punto 7): a qué tarea pertenece, duración y fecha.
- `Tarea.kt` — el modelo de una tarea: id, título y si está completada.
- `DataStoreManager.kt` — configura el DataStore donde se guarda el historial de sesiones.
### Recursos (`app/src/main/res/`)
 
- `layout/activity_main.xml` — el diseño de la pantalla principal (temporizador, botones, listas, resumen).
- `layout/item_tarea.xml` — el diseño de cada tarea dentro de la lista.
- `layout/item_historial.xml` — el diseño de cada sesión dentro del historial.
- `values/strings.xml` — los textos de la app.
- `values/colors.xml` y `values/themes.xml` — colores y tema visual.
### Configuración
 
- `AndroidManifest.xml` — declara la Activity principal y los permisos/configuración de la app.
- `app/build.gradle.kts` — dependencias del proyecto (Gson, DataStore, Lifecycle, Material, etc.) y configuración del SDK.
- `gradle/libs.versions.toml` — catálogo con las versiones de todas las librerías usadas.

## Video demostrativo
 
📹 [Ver video](https://drive.google.com/file/d/147Ls4996pEDrjQTicTyzJ85YYWxPejvR/view?usp=sharing)
 
## Cómo ejecutarlo
 
1. Abrir el proyecto con Android Studio.
2. Esperar que sincronice Gradle.
3. Correrlo en un emulador o celular con Android 7.0 (API 24) o superior.
