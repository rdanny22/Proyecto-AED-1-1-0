package com.sgai.vista;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;

// Formularios controlados: límites de longitud y selectores numéricos.
public final class Formulario extends JPanel {

  private static final long serialVersionUID = 1L;
  private int fila;

  public Formulario() {
    super(new GridBagLayout());
  }

  public <T extends JComponent> T campo(String nombre, T componente) {
    GridBagConstraints posicion = new GridBagConstraints();
    posicion.gridy = fila++;
    posicion.insets = new Insets(6, 8, 6, 8);
    posicion.anchor = GridBagConstraints.WEST;
    posicion.gridx = 0;
    add(new JLabel(nombre), posicion);
    posicion.gridx = 1;
    posicion.weightx = 1;
    posicion.fill = GridBagConstraints.HORIZONTAL;
    add(componente, posicion);
    return componente;
  }

  public static JTextField texto(String valor, int limite) {
    JTextField campo = new JTextField(valor, 25);
    ((AbstractDocument) campo.getDocument()).setDocumentFilter(
      new DocumentFilter() {
        @Override
        public void insertString(
          FilterBypass filtro,
          int posicion,
          String texto,
          AttributeSet atributos
        ) throws BadLocationException {
          replace(filtro, posicion, 0, texto, atributos);
        }

        @Override
        public void replace(
          FilterBypass filtro,
          int posicion,
          int longitud,
          String texto,
          AttributeSet atributos
        ) throws BadLocationException {
          if (texto == null) texto = "";
          if (
            filtro.getDocument().getLength() - longitud + texto.length() <=
            limite
          ) super.replace(filtro, posicion, longitud, texto, atributos);
        }
      }
    );
    return campo;
  }

  public static JPasswordField clave() {
    JPasswordField campo = new JPasswordField(25);
    ((AbstractDocument) campo.getDocument()).setDocumentFilter(
      new DocumentFilter() {
        @Override
        public void insertString(
          FilterBypass filtro,
          int posicion,
          String texto,
          AttributeSet atributos
        ) throws BadLocationException {
          replace(filtro, posicion, 0, texto, atributos);
        }

        @Override
        public void replace(
          FilterBypass filtro,
          int posicion,
          int longitud,
          String texto,
          AttributeSet atributos
        ) throws BadLocationException {
          if (texto == null) texto = "";
          if (
            filtro.getDocument().getLength() - longitud + texto.length() <= 128
          ) super.replace(filtro, posicion, longitud, texto, atributos);
        }
      }
    );
    return campo;
  }

  public static JSpinner numero(int valor, int minimo, int maximo) {
    return new JSpinner(new SpinnerNumberModel(valor, minimo, maximo, 1));
  }

  public static int entero(JSpinner campo) {
    try {
      campo.commitEdit();
      return ((Number) campo.getValue()).intValue();
    } catch (java.text.ParseException error) {
      throw new com.sgai.util.ExcepcionNegocio(
        "Ingrese un número entero válido."
      );
    }
  }

  public boolean aceptar(Component padre, String titulo) {
    return (
      JOptionPane.showConfirmDialog(
        padre,
        this,
        titulo,
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
      ) == JOptionPane.OK_OPTION
    );
  }
}
