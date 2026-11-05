package com.sgai.modelo;

import com.sgai.util.Validacion;

// Relación atómica entre el estudiante y cada curso en el que está inscrito.
public record Matricula(long estudianteId, long cursoId) {
  public Matricula {
    Validacion.identificador(estudianteId);
    Validacion.identificador(cursoId);
  }
}
