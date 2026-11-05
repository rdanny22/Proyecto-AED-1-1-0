package com.sgai.vista;

import com.sgai.modelo.Curso;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

// Casillas reales: cada curso se marca independientemente, sin Ctrl ni cerrar el diálogo.
public final class SeleccionCursos extends JScrollPane {

  private static final long serialVersionUID = 1L;
  private final List<Curso> cursos;
  private final List<JCheckBox> casillas = new ArrayList<>();

  public SeleccionCursos(List<Curso> cursos) {
    this.cursos = List.copyOf(cursos);
    JPanel opciones = new JPanel();
    opciones.setLayout(new BoxLayout(opciones, BoxLayout.Y_AXIS));
    for (Curso curso : this.cursos) {
      JCheckBox casilla = new JCheckBox(curso.toString());
      casillas.add(casilla);
      opciones.add(casilla);
    }
    setViewportView(opciones);
    setPreferredSize(
      new Dimension(440, Math.min(280, Math.max(80, cursos.size() * 30)))
    );
  }

  public List<Long> seleccionados() {
    List<Long> seleccion = new ArrayList<>();
    // Cada casilla mantiene la posición de su curso; enviamos su ID, no el texto mostrado.
    for (int indice = 0; indice < cursos.size(); indice++) {
      if (casillas.get(indice).isSelected()) seleccion.add(
        cursos.get(indice).id()
      );
    }
    return List.copyOf(seleccion);
  }
}
