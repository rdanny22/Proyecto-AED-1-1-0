package com.sgai.modelo;

import com.sgai.util.Validacion;

// Relación atómica docente–curso.
public record Asignacion(long docenteId, long cursoId) {
  public Asignacion {
    Validacion.identificador(docenteId);
    Validacion.identificador(cursoId);
  }
}
