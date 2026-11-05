package com.sgai.estructuras;

import java.util.Comparator;

// Algoritmos sobre copias de las listas: no se delega en Arrays.sort.
public final class Algoritmos {

  private Algoritmos() {}

  // QuickSort: O(n log n) promedio, O(n²) peor caso; pila O(log n) al recursar el tramo menor.
  public static <T> void ordenar(T[] datos, Comparator<? super T> comparador) {
    ordenar(datos, 0, datos.length - 1, comparador);
  }

  private static <T> void ordenar(
    T[] datos,
    int inicio,
    int fin,
    Comparator<? super T> comparador
  ) {
    while (inicio < fin) {
      int izquierda = inicio,
        derecha = fin;
      // El pivote sirve de referencia para separar los valores menores de los mayores.
      T pivote = datos[inicio + (fin - inicio) / 2];
      while (izquierda <= derecha) {
        while (comparador.compare(datos[izquierda], pivote) < 0) izquierda++;
        while (comparador.compare(datos[derecha], pivote) > 0) derecha--;
        if (izquierda <= derecha) {
          T temporal = datos[izquierda];
          datos[izquierda++] = datos[derecha];
          datos[derecha--] = temporal;
        }
      }
      // Ordenamos por recursión el tramo más pequeño y seguimos con el otro en el bucle.
      // Así evitamos acumular demasiadas llamadas en la pila.
      if (derecha - inicio < fin - izquierda) {
        ordenar(datos, inicio, derecha, comparador);
        inicio = izquierda;
      } else {
        ordenar(datos, izquierda, fin, comparador);
        fin = derecha;
      }
    }
  }

  // O(log n); precondición: arreglo ordenado con el MISMO comparador.
  public static <T> int buscarBinario(
    T[] datos,
    T clave,
    Comparator<? super T> comparador
  ) {
    int inicio = 0,
      fin = datos.length - 1;
    while (inicio <= fin) {
      int medio = inicio + (fin - inicio) / 2;
      int resultado = comparador.compare(datos[medio], clave);
      if (resultado == 0) return medio;
      // Como el arreglo está ordenado, podemos descartar la mitad que no contiene la clave.
      if (resultado < 0) inicio = medio + 1;
      else fin = medio - 1;
    }
    return -1;
  }
}
