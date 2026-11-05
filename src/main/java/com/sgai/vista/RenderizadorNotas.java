package com.sgai.vista;

import java.awt.*;
import java.math.BigDecimal;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

// Rojo y negrita para notas <60, incluso cuando la fila está seleccionada.
public final class RenderizadorNotas extends DefaultTableCellRenderer {

  private static final long serialVersionUID = 1L;

  @Override
  public Component getTableCellRendererComponent(
    JTable tabla,
    Object valor,
    boolean seleccionado,
    boolean foco,
    int fila,
    int columna
  ) {
    Component celda = super.getTableCellRendererComponent(
      tabla,
      valor,
      seleccionado,
      foco,
      fila,
      columna
    );
    boolean reprobada =
      valor instanceof BigDecimal nota &&
      nota.compareTo(new BigDecimal("60")) < 0;
    celda.setForeground(
      reprobada
        ? new Color(170, 15, 30)
        : seleccionado
          ? tabla.getSelectionForeground()
          : tabla.getForeground()
    );
    celda.setBackground(
      reprobada
        ? new Color(255, 225, 228)
        : seleccionado
          ? tabla.getSelectionBackground()
          : Color.WHITE
    );
    celda.setFont(
      tabla.getFont().deriveFont(reprobada ? Font.BOLD : Font.PLAIN)
    );
    return celda;
  }
}
