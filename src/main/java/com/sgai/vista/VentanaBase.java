package com.sgai.vista;

import com.sgai.controlador.ControladorAcademico;
import com.sgai.util.ExcepcionNegocio;
import java.awt.event.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.*;

// Puente asíncrono común: todas las actualizaciones visuales vuelven al EDT.
public abstract class VentanaBase extends JFrame {

  private static final long serialVersionUID = 1L;
  protected final ControladorAcademico controlador;
  protected final JLabel estado = new JLabel("Listo");
  private boolean ocupado;

  protected VentanaBase(ControladorAcademico controlador, String titulo) {
    super(titulo);
    this.controlador = controlador;
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(
      new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent evento) {
          if (ocupado) {
            JOptionPane.showMessageDialog(
              VentanaBase.this,
              "Espere a que termine la operación antes de salir."
            );
            return;
          }
          controlador.close();
          dispose();
        }
      }
    );
  }

  protected boolean disponible() {
    return !ocupado;
  }

  protected void accion(Runnable tarea) {
    if (ocupado) return;
    try {
      tarea.run();
    } catch (ExcepcionNegocio | IllegalArgumentException error) {
      error(error);
    }
  }

  protected <T> void ejecutar(CompletableFuture<T> futuro, Consumer<T> exito) {
    ocupado = true;
    estado.setText("Procesando… No cierre la ventana.");
    setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
    futuro.whenComplete((resultado, fallo) ->
      SwingUtilities.invokeLater(() -> {
        ocupado = false;
        setCursor(java.awt.Cursor.getDefaultCursor());
        if (fallo != null) {
          Throwable causa = fallo;
          while (
              (causa instanceof CompletionException ||
                causa instanceof ExecutionException) &&
              causa.getCause() != null
            )
            causa = causa.getCause();
          error(causa);
        } else {
          estado.setText("Operación completada");
          try {
            exito.accept(resultado);
          } catch (RuntimeException error) {
            error(error);
          }
        }
      })
    );
  }

  protected void error(Throwable error) {
    estado.setText("Operación no completada");
    String mensaje =
      error instanceof ExcepcionNegocio
        ? error.getMessage()
        : "Ocurrió un error inesperado. Verifique los datos y vuelva a intentar.";
    JOptionPane.showMessageDialog(
      this,
      mensaje,
      "SGAI · Aviso",
      JOptionPane.ERROR_MESSAGE
    );
  }

  protected boolean confirmar(String mensaje) {
    return (
      JOptionPane.showConfirmDialog(
        this,
        mensaje,
        "Confirmar operación",
        JOptionPane.YES_NO_OPTION
      ) == JOptionPane.YES_OPTION
    );
  }
}
