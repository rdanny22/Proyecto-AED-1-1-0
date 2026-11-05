package com.sgai.modelo;

// Sesión sin contraseña ni hash. La identidad asociada determina el alcance.
public record Usuario(
  long id,
  String nombre,
  Rol rol,
  Long estudianteId,
  Long docenteId
) {
  public Usuario {
    if (
      id <= 0 || nombre == null || rol == null
    ) throw new IllegalArgumentException("Usuario inválido");
    boolean valido = switch (rol) {
      case ADMINISTRADOR -> estudianteId == null && docenteId == null;
      case ESTUDIANTE -> estudianteId != null &&
        estudianteId > 0 &&
        docenteId == null;
      case DOCENTE -> docenteId != null &&
        docenteId > 0 &&
        estudianteId == null;
    };
    if (!valido) throw new IllegalArgumentException(
      "Asociación de rol inválida"
    );
  }
}
