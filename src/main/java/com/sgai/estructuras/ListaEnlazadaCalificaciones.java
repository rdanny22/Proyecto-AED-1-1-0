package com.sgai.estructuras;

import com.sgai.modelo.Calificacion;

// Lista simple propia: inserción O(1), búsqueda/eliminación O(n), copia O(n).
public final class ListaEnlazadaCalificaciones {

  private NodoCalificacion cabeza, cola;
  private int cantidad;

  public synchronized void agregar(Calificacion dato) {
    NodoCalificacion nuevo = new NodoCalificacion(dato);
    if (cola == null) cabeza = nuevo;
    else cola.siguiente = nuevo;
    cola = nuevo;
    cantidad++;
  }

  public synchronized Calificacion buscar(long id) {
    for (NodoCalificacion n = cabeza; n != null; n = n.siguiente) if (
      n.dato.id() == id
    ) return n.dato;
    return null;
  }

  public synchronized boolean eliminar(long id) {
    NodoCalificacion anterior = null,
      actual = cabeza;
    while (actual != null) {
      if (actual.dato.id() == id) {
        if (anterior == null) cabeza = actual.siguiente;
        else anterior.siguiente = actual.siguiente;
        if (cola == actual) cola = anterior;
        cantidad--;
        return true;
      }
      anterior = actual;
      actual = actual.siguiente;
    }
    return false;
  }

  public synchronized int getCantidad() {
    return cantidad;
  }

  public synchronized Calificacion[] aArreglo() {
    Calificacion[] datos = new Calificacion[cantidad];
    int i = 0;
    for (NodoCalificacion n = cabeza; n != null; n = n.siguiente) datos[i++] =
      n.dato;
    return datos;
  }
}
