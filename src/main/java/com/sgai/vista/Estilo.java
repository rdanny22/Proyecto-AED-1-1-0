package com.sgai.vista;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Paleta institucional y componentes Swing accesibles por teclado.
public final class Estilo {

  public static final Color AZUL = new Color(20, 58, 105),
    FONDO = new Color(242, 246, 251);

  private Estilo() {}

  public static void instalar() {
    try {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
    } catch (
      ReflectiveOperationException
      | UnsupportedLookAndFeelException error
    ) {
      // Look and feel estándar disponible.
    }
    UIManager.put("control", FONDO);
    UIManager.put("nimbusBase", AZUL);
    UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
  }

  public static JPanel panel() {
    JPanel panel = new JPanel(new BorderLayout(12, 12));
    panel.setBorder(new EmptyBorder(18, 18, 18, 18));
    panel.setBackground(FONDO);
    return panel;
  }

  public static JButton boton(String texto, Runnable accion) {
    JButton boton = new JButton(texto);
    boton.addActionListener(evento -> accion.run());
    return boton;
  }

  // Color sólido consistente incluso con Nimbus, para las acciones solicitadas.
  public static JButton colorear(JButton boton, Color color) {
    boton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
    boton.setOpaque(true);
    boton.setBackground(color);
    boton.setForeground(AZUL);
    boton.setBorder(
      javax.swing.BorderFactory.createCompoundBorder(
        javax.swing.BorderFactory.createLineBorder(color.darker()),
        javax.swing.BorderFactory.createEmptyBorder(9, 14, 9, 14)
      )
    );
    return boton;
  }

  public static JLabel titulo(String texto) {
    JLabel etiqueta = new JLabel(texto);
    etiqueta.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
    etiqueta.setForeground(AZUL);
    return etiqueta;
  }
}
