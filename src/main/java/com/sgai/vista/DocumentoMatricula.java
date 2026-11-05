package com.sgai.vista;

import com.sgai.modelo.Curso;
import com.sgai.modelo.Estudiante;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

// Instantánea de matrícula: la pantalla y la impresora dibujan exactamente la misma hoja.
public final class DocumentoMatricula implements DocumentoImprimible {

  private static final int FILAS_POR_PAGINA = 23;
  private final Estudiante estudiante;
  private final List<Curso> cursos;
  private final BufferedImage emblema = RecursosGraficos.emblema();
  private final String fecha = LocalDate.now().toString();

  public DocumentoMatricula(Estudiante estudiante, List<Curso> cursos) {
    this.estudiante = Objects.requireNonNull(estudiante);
    // La hoja conserva los cursos de este momento, aunque después se actualice la pantalla.
    this.cursos = List.copyOf(cursos);
  }

  @Override
  public int getPaginas() {
    return Math.max(
      1,
      (cursos.size() + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA
    );
  }

  @Override
  public int print(Graphics grafico, PageFormat formato, int indice) {
    if (indice < 0 || indice >= getPaginas()) return NO_SUCH_PAGE;
    Graphics2D copia = (Graphics2D) grafico.create();
    try {
      copia.translate(formato.getImageableX(), formato.getImageableY());
      // Ajustamos toda la hoja al área imprimible sin deformar su diseño.
      double escala = Math.min(
        formato.getImageableWidth() / 540.0,
        formato.getImageableHeight() / 720.0
      );
      copia.scale(escala, escala);
      dibujar(copia, indice, 0, 0, 540);
    } finally {
      copia.dispose();
    }
    return PAGE_EXISTS;
  }

  private void texto(
    Graphics2D grafico,
    String texto,
    int x,
    int y,
    int ancho
  ) {
    Font original = grafico.getFont();
    int longitud = grafico.getFontMetrics().stringWidth(texto);
    if (longitud > ancho) grafico.setFont(
      original.deriveFont((original.getSize2D() * ancho) / longitud)
    );
    grafico.drawString(texto, x, y);
    grafico.setFont(original);
  }

  @Override
  public void dibujar(Graphics2D grafico, int indice, int x, int y, int ancho) {
    grafico.setRenderingHint(
      RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON
    );
    grafico.setColor(Color.WHITE);
    grafico.fillRect(x, y, ancho, 720);
    RecursosGraficos.dibujarEmblema(grafico, emblema, x, y + 2, 56, 56);
    int margen = emblema == null ? 0 : 68;
    grafico.setColor(Estilo.AZUL);
    grafico.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
    texto(
      grafico,
      "Colegio Antonio Soldano · Hoja de matrícula",
      x + margen,
      y + 20,
      ancho - margen
    );
    grafico.setColor(Color.BLACK);
    grafico.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
    texto(grafico, estudiante.toString(), x + margen, y + 43, ancho - margen);
    texto(
      grafico,
      "Grado: " + estudiante.getGrado() + " | Fecha de emisión: " + fecha,
      x + margen,
      y + 59,
      ancho - margen
    );
    texto(grafico, "Correo: " + estudiante.getCorreo(), x, y + 80, ancho);
    int[] anchuras = { 100, 380, 60 };
    String[] titulos = { "Código", "Curso matriculado", "Grado" };
    int posicion = x;
    grafico.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
    for (int columna = 0; columna < titulos.length; columna++) {
      grafico.setColor(new Color(230, 237, 246));
      grafico.fillRect(posicion, y + 96, anchuras[columna], 32);
      grafico.setColor(Color.BLACK);
      texto(
        grafico,
        titulos[columna],
        posicion + 4,
        y + 116,
        anchuras[columna] - 8
      );
      posicion += anchuras[columna];
    }
    grafico.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
    // Cada página muestra su propio tramo de cursos y repite el encabezado con el emblema.
    int inicio = indice * FILAS_POR_PAGINA;
    int fin = Math.min(inicio + FILAS_POR_PAGINA, cursos.size());
    for (int fila = inicio; fila < fin; fila++) {
      Curso curso = cursos.get(fila);
      String[] valores = {
        curso.codigo(),
        curso.nombre(),
        String.valueOf(curso.grado()),
      };
      int altura = y + 128 + (fila - inicio) * 22;
      posicion = x;
      for (int columna = 0; columna < valores.length; columna++) {
        grafico.setColor(Color.LIGHT_GRAY);
        grafico.drawRect(posicion, altura, anchuras[columna], 22);
        grafico.setColor(Color.BLACK);
        texto(
          grafico,
          valores[columna],
          posicion + 4,
          altura + 15,
          anchuras[columna] - 8
        );
        posicion += anchuras[columna];
      }
    }
    if (cursos.isEmpty()) grafico.drawString(
      "El estudiante no tiene cursos matriculados.",
      x + 4,
      y + 150
    );
    if (indice == getPaginas() - 1) {
      grafico.drawString(
        "Total de cursos matriculados: " + cursos.size(),
        x,
        y + 650
      );
      grafico.drawString(
        "Firma del responsable: __________________________",
        x,
        y + 686
      );
    }
    grafico.drawString(
      "SGAI | Página " + (indice + 1) + " de " + getPaginas(),
      x,
      y + 711
    );
  }
}
