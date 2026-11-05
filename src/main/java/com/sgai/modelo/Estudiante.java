package com.sgai.modelo;

import com.sgai.util.Validacion;

// Entidad estudiante; especializa polimórficamente a Persona.
public final class Estudiante extends Persona {

  private final int grado;

  public Estudiante(
    long id,
    String codigo,
    String nombres,
    String apellidos,
    String correo,
    int grado
  ) {
    super(id, codigo, nombres, apellidos, correo);
    this.grado = Validacion.rango(grado, 1, 11, "Grado");
  }

  public int getGrado() {
    return grado;
  }

  @Override
  public String getDescripcion() {
    return "Grado: " + grado;
  }
}
