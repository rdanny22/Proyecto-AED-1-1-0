package com.sgai.vista;

import org.junit.jupiter.api.Test;

// Regresiones ejecutables sin escritorio, archivo oficial ni impresora.
class RecursosGraficosTest {

  @Test
  void cargaOpcionalYLimites() throws Exception {
    VerificacionesEmblema.carga();
  }

  @Test
  void conservaProporcionesYAccesibilidad() throws Exception {
    VerificacionesEmblema.proporciones();
  }

  @Test
  void imprimeEnTodasLasPaginasSinAlterarLaTabla() {
    VerificacionesEmblema.impresion();
  }

  @Test
  void incluyeElEmblemaRealEnElReporte() {
    VerificacionesEmblema.recursoInstitucional();
  }
}
