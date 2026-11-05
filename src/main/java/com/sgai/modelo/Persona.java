package com.sgai.modelo;

import com.sgai.util.Validacion;

// Base abstracta inmutable: identidad y datos personales encapsulados.
public abstract class Persona {

  private final long id;
  private final String codigo, nombres, apellidos, correo;

  protected Persona(
    long id,
    String codigo,
    String nombres,
    String apellidos,
    String correo
  ) {
    if (id < 0) throw new IllegalArgumentException("Identidad negativa");
    this.id = id;
    this.codigo =
      id == 0 && codigo == null ? "AUTO" : Validacion.codigo(codigo);
    this.nombres = Validacion.texto(nombres, "Nombres", 80);
    this.apellidos = Validacion.texto(apellidos, "Apellidos", 80);
    this.correo = Validacion.correo(correo);
  }

  public long getId() {
    return id;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getNombres() {
    return nombres;
  }

  public String getApellidos() {
    return apellidos;
  }

  public String getCorreo() {
    return correo;
  }

  public String getNombreCompleto() {
    return nombres + " " + apellidos;
  }

  public abstract String getDescripcion();

  @Override
  public String toString() {
    return codigo + " — " + getNombreCompleto();
  }
}
