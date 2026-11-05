package com.sgai;

import com.sgai.controlador.ControladorAcademico;
import com.sgai.vista.Estilo;
import com.sgai.vista.VentanaInstalacion;
import javax.swing.SwingUtilities;

// Lanzador independiente para la estación administrada; nunca se abre desde el login.
public final class Instalacion {

  private Instalacion() {}

  public static void main(String[] argumentos) {
    SwingUtilities.invokeLater(() -> {
      Estilo.instalar();
      new VentanaInstalacion(new ControladorAcademico()).setVisible(true);
    });
  }
}
