package com.sgai;

import org.junit.jupiter.api.Test;

// Regresiones funcionales nuevas que no requieren MySQL ni interfaz gráfica.
class AcademicoTest {

  @Test
  void filtraPorDatosAcademicos() {
    VerificacionesAcademicas.filtros();
  }

  @Test
  void generaReporteDeCuatroParciales() {
    VerificacionesAcademicas.reportes();
  }

  @Test
  void paginaEnCartaSinImpresora() {
    VerificacionesAcademicas.impresion();
  }
}
