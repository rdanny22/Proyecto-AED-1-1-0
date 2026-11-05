package com.sgai.estructuras;

import com.sgai.modelo.Estudiante;

// Lista simple propia: inserción O(1), búsqueda/eliminación O(n), copia O(n).
public final class ListaEnlazadaEstudiantes {

  private NodoEstudiante cabeza, cola;
  private int cantidad;

  public synchronized void agregar(Estudiante dato) {
    NodoEstudiante nuevo = new NodoEstudiante(dato);
    // Conservar la cola permite agregar al final sin recorrer toda la lista.
    if (cola == null) cabeza = nuevo;
    else cola.siguiente = nuevo;
    cola = nuevo;
    cantidad++;
  }

  public synchronized Estudiante buscar(long id) {
    for (NodoEstudiante n = cabeza; n != null; n = n.siguiente) if (
      n.dato.getId() == id
    ) return n.dato;
    return null;
  }

  public synchronized boolean eliminar(long id) {
    NodoEstudiante anterior = null,
      actual = cabeza;
    while (actual != null) {
      if (actual.dato.getId() == id) {
        // Enlazamos el nodo anterior con el siguiente; si era el primero, movemos la cabeza.
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

  public synchronized Estudiante[] aArreglo() {
    Estudiante[] datos = new Estudiante[cantidad];
    int i = 0;
    for (NodoEstudiante n = cabeza; n != null; n = n.siguiente) datos[i++] =
      n.dato;
    return datos;
  }
}
