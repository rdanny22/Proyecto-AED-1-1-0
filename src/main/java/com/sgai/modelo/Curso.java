package com.sgai.modelo;

import com.sgai.util.Validacion;

// Curso indivisible por grado escolar.
public record Curso(long id, String codigo, String nombre, int grado) {
  public Curso {
    if (id < 0) throw new IllegalArgumentException("Identidad negativa");
    codigo = id == 0 && codigo == null ? "AUTO" : Validacion.codigo(codigo);
    nombre = Validacion.texto(nombre, "Curso", 80);
    grado = Validacion.rango(grado, 1, 11, "Grado");
  }

  @Override
  public String toString() {
    return codigo + " — " + nombre;
  }
}
