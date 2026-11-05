package com.sgai;

import com.sgai.controlador.ControladorAcademico;
import com.sgai.vista.*;
import javax.swing.*;

// Punto de entrada; no lee ni escribe en consola. Requiere entorno gráfico.
public final class Aplicacion {

  private Aplicacion() {}

  public static void main(String[] argumentos) {
    SwingUtilities.invokeLater(() -> {
      Estilo.instalar();
      VentanaLogin login = new VentanaLogin(new ControladorAcademico());
      login.setVisible(true);
    });
  }
}
