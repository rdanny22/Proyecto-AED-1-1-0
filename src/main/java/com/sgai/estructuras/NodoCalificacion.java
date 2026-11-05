package com.sgai.estructuras;

import com.sgai.modelo.Calificacion;

// Nodo privado al paquete; no expone enlaces a consumidores.
public final class NodoCalificacion {

  final Calificacion dato;
  NodoCalificacion siguiente;

  NodoCalificacion(Calificacion dato) {
    this.dato = java.util.Objects.requireNonNull(dato);
  }
}
