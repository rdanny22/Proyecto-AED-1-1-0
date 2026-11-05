package com.sgai;

import org.junit.jupiter.api.Test;

// Adaptador JUnit para el mismo núcleo de pruebas usado en la verificación local.
class SistemaTest {

  @Test
  void validaDominio() {
    Verificaciones.dominio();
  }

  @Test
  void verificaListas() {
    Verificaciones.listas();
  }

  @Test
  void comparaAlgoritmos() {
    Verificaciones.algoritmos();
  }

  @Test
  void compruebaInstantaneas() {
    Verificaciones.instantaneas();
  }

  @Test
  void protegeClaves() {
    Verificaciones.seguridad();
  }

  @Test
  void rechazaSinSesion() {
    Verificaciones.permisosSinSesion();
  }

  @Test
  void verificaSwing() throws Exception {
    Verificaciones.componentesSwing();
  }
}
