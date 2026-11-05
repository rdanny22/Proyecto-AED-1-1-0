package com.sgai.modelo;

import com.sgai.util.Validacion;

// Entidad docente; especializa polimórficamente a Persona.
public final class Docente extends Persona {

  private final String especialidad;

  public Docente(
    long id,
    String codigo,
    String nombres,
    String apellidos,
    String correo,
    String especialidad
  ) {
    super(id, codigo, nombres, apellidos, correo);
    this.especialidad = Validacion.texto(especialidad, "Especialidad", 80);
  }

  public String getEspecialidad() {
    return especialidad;
  }

  @Override
  public String getDescripcion() {
    return "Especialidad: " + especialidad;
  }
}
