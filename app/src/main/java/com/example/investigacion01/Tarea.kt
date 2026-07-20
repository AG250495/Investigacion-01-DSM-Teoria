package com.example.investigacion01

data class Tarea(
    val id: Int,
    val titulo: String,
    var completada: Boolean = false
)