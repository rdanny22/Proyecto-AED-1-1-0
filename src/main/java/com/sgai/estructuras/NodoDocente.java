package com.sgai.estructuras;

import com.sgai.modelo.Docente;

// Nodo privado al paquete; no expone enlaces a consumidores.
public final class NodoDocente {

  final Docente dato;
  NodoDocente siguiente;

  NodoDocente(Docente dato) {
    this.dato = java.util.Objects.requireNonNull(dato);
  }
}
