package com.sgai.vista;

import com.sgai.modelo.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

// Regresiones ejecutables sin base de datos, pantalla ni impresora física.
public final class VerificacionesMatricula {

  private VerificacionesMatricula() {}

  private static void comprobar(boolean condicion) {
    if (!condicion) throw new AssertionError(
      "Verificación de matrícula fallida"
    );
  }

  public static void seleccionMultiple() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      SeleccionCursos seleccion = new SeleccionCursos(
        List.of(
          new Curso(1, "CURS1", "Matemáticas", 7),
          new Curso(2, "CURS2", "Lengua y Literatura", 7)
        )
      );
      JPanel opciones = (JPanel) seleccion.getViewport().getView();
      comprobar(seleccion.seleccionados().isEmpty());
      JCheckBox primera = (JCheckBox) opciones.getComponent(0);
      JCheckBox segunda = (JCheckBox) opciones.getComponent(1);
      primera.doClick();
      segunda.doClick();
      comprobar(seleccion.seleccionados().equals(List.of(1L, 2L)));
      primera.doClick();
      comprobar(seleccion.seleccionados().equals(List.of(2L)));
    });
  }

  public static void documento() throws Exception {
    Estudiante alumno = new Estudiante(
      1,
      "ESTU1",
      "Ana",
      "Pérez",
      "ana@colegio.edu.ni",
      7
    );
    BufferedImage imagen = new BufferedImage(
      612,
      792,
      BufferedImage.TYPE_INT_RGB
    );
    Graphics2D grafico = imagen.createGraphics();
    try {
      for (int cantidad : new int[] { 0, 1, 23, 24, 46, 47 }) {
        List<Curso> cursos = new ArrayList<>();
        for (int indice = 1; indice <= cantidad; indice++) cursos.add(
          new Curso(indice, "CURS" + indice, "Curso " + indice, 7)
        );
        DocumentoMatricula documento = new DocumentoMatricula(alumno, cursos);
        cursos.clear(); // La impresión conserva su instantánea aunque cambie el origen.
        int paginas = Math.max(1, (cantidad + 22) / 23);
        comprobar(documento.getPaginas() == paginas);
        for (int pagina = 0; pagina < paginas; pagina++) {
          comprobar(
            documento.print(grafico, DocumentoReporte.formatoCarta(), pagina) ==
              Printable.PAGE_EXISTS
          );
        }
        comprobar(
          documento.print(grafico, DocumentoReporte.formatoCarta(), -1) ==
            Printable.NO_SUCH_PAGE
        );
        comprobar(
          documento.print(grafico, DocumentoReporte.formatoCarta(), paginas) ==
            Printable.NO_SUCH_PAGE
        );
      }
      comprobar(RecursosGraficos.emblema() != null);
      DocumentoMatricula documento = new DocumentoMatricula(
        alumno,
        List.of(new Curso(1, "CURS1", "Matemáticas", 7))
      );
      grafico.setColor(java.awt.Color.BLACK);
      grafico.fillRect(0, 0, 612, 792);
      documento.dibujar(grafico, 0, 36, 36, 540);
      BufferedImage impresa = new BufferedImage(
        612,
        792,
        BufferedImage.TYPE_INT_RGB
      );
      Graphics2D salida = impresa.createGraphics();
      try {
        documento.print(salida, DocumentoReporte.formatoCarta(), 0);
      } finally {
        salida.dispose();
      }
      for (int x = 0; x < 612; x++) for (int y = 0; y < 792; y++) {
        comprobar(imagen.getRGB(x, y) == impresa.getRGB(x, y));
      }
      boolean emblemaVisible = false;
      for (int x = 36; x < 92; x++) for (int y = 38; y < 94; y++) {
        if ((imagen.getRGB(x, y) & 0xffffff) != 0xffffff) emblemaVisible = true;
      }
      comprobar(emblemaVisible);
    } finally {
      grafico.dispose();
    }
  }

  public static void main(String[] argumentos) throws Exception {
    seleccionMultiple();
    documento();
  }
}
