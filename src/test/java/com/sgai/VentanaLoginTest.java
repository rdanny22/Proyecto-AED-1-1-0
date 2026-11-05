package com.sgai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.sgai.controlador.ControladorAcademico;
import com.sgai.vista.VentanaLogin;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

// Regresión visual estructural; requiere un escritorio o un servidor gráfico virtual.
class VentanaLoginTest {

  @Test
  void accesoPublicoSoloOfreceIniciarSesion() throws Exception {
    assumeFalse(
      GraphicsEnvironment.isHeadless(),
      "La comprobación de JFrame requiere entorno gráfico"
    );
    try (ControladorAcademico controlador = new ControladorAcademico()) {
      SwingUtilities.invokeAndWait(() -> {
        VentanaLogin ventana = new VentanaLogin(controlador);
        try {
          List<String> acciones = new ArrayList<>();
          recopilarBotones(ventana.getContentPane(), acciones);
          assertEquals(List.of("Iniciar sesión"), acciones);
        } finally {
          ventana.dispose();
        }
      });
    }
  }

  private void recopilarBotones(Container contenedor, List<String> acciones) {
    for (Component componente : contenedor.getComponents()) {
      if (componente instanceof JButton boton) acciones.add(boton.getText());
      if (componente instanceof Container hijo) recopilarBotones(
        hijo,
        acciones
      );
    }
  }
}
