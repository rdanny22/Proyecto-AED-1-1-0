package com.sgai.estructuras;

import com.sgai.modelo.Estudiante;

// Nodo privado al paquete; no expone enlaces a consumidores.
public final class NodoEstudiante {

  final Estudiante dato;
  NodoEstudiante siguiente;

  NodoEstudiante(Estudiante dato) {
    this.dato = java.util.Objects.requireNonNull(dato);
  }
}
