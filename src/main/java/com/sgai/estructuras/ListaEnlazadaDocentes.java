package com.sgai.estructuras;

import com.sgai.modelo.Docente;

// Lista simple propia: inserción O(1), búsqueda/eliminación O(n), copia O(n).
public final class ListaEnlazadaDocentes {

  private NodoDocente cabeza, cola;
  private int cantidad;

  public synchronized void agregar(Docente dato) {
    NodoDocente nuevo = new NodoDocente(dato);
    if (cola == null) cabeza = nuevo;
    else cola.siguiente = nuevo;
    cola = nuevo;
    cantidad++;
  }

  public synchronized Docente buscar(long id) {
    for (NodoDocente n = cabeza; n != null; n = n.siguiente) if (
      n.dato.getId() == id
    ) return n.dato;
    return null;
  }

  public synchronized boolean eliminar(long id) {
    NodoDocente anterior = null,
      actual = cabeza;
    while (actual != null) {
      if (actual.dato.getId() == id) {
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

  public synchronized Docente[] aArreglo() {
    Docente[] datos = new Docente[cantidad];
    int i = 0;
    for (NodoDocente n = cabeza; n != null; n = n.siguiente) datos[i++] =
      n.dato;
    return datos;
  }
}
