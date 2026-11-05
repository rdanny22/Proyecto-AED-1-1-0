package com.sgai.vista;

import org.junit.jupiter.api.Test;

class MatriculaTest {

  @Test
  void permiteMarcarVariosCursos() throws Exception {
    VerificacionesMatricula.seleccionMultiple();
  }

  @Test
  void paginaEImprimeConEmblema() throws Exception {
    VerificacionesMatricula.documento();
  }
}
