package com.sgai.vista;

import com.sgai.controlador.ReporteAcademico;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.time.LocalDate;

// Documento inmutable imprimible y verificable sin entorno gráfico ni impresora.
public final class DocumentoReporte implements DocumentoImprimible {

  private static final int FILAS_POR_PAGINA = 23;
  private final ReporteAcademico reporte;
  private final BufferedImage emblema;
  private final int paginas;
  private final String fecha = LocalDate.now().toString();

  public DocumentoReporte(ReporteAcademico reporte) {
    this(reporte, RecursosGraficos.emblema());
  }

  // Permite probar el diseño con imágenes de prueba sin empaquetar un emblema ficticio.
  DocumentoReporte(ReporteAcademico reporte, BufferedImage emblema) {
    this.emblema = emblema;
    this.reporte = java.util.Objects.requireNonNull(reporte);
    paginas = Math.max(
      1,
      (reporte.filas().size() + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA
    );
  }

  public int getPaginas() {
    return paginas;
  }

  public static PageFormat formatoCarta() {
    Paper papel = new Paper();
    papel.setSize(612, 792);
    papel.setImageableArea(36, 36, 540, 720);
    PageFormat formato = new PageFormat();
    formato.setPaper(papel);
    formato.setOrientation(PageFormat.PORTRAIT);
    return formato;
  }

  @Override
  public int print(Graphics grafico, PageFormat formato, int indice) {
    if (indice < 0 || indice >= paginas) return NO_SUCH_PAGE;
    Graphics2D copia = (Graphics2D) grafico.create();
    try {
      copia.translate(formato.getImageableX(), formato.getImageableY());
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

  private void texto(Graphics2D g, String texto, int x, int y, int ancho) {
    Font original = g.getFont();
    int longitud = g.getFontMetrics().stringWidth(texto);
    if (longitud > ancho) g.setFont(
      original.deriveFont((float) ((original.getSize2D() * ancho) / longitud))
    );
    g.drawString(texto, x, y);
    g.setFont(original);
  }

  public void dibujar(Graphics2D g, int indice, int x, int y, int ancho) {
    g.setRenderingHint(
      RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON
    );
    g.setColor(Color.WHITE);
    g.fillRect(x, y, ancho, 720);
    // El emblema cabe en el encabezado existente: no desplaza filas ni reduce márgenes.
    int margenTexto = emblema == null ? 0 : 68;
    RecursosGraficos.dibujarEmblema(g, emblema, x, y + 2, 56, 56);
    int inicioTexto = x + margenTexto,
      anchoTexto = ancho - margenTexto;
    g.setColor(Estilo.AZUL);
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
    texto(
      g,
      "Colegio Antonio Soldano · Reporte académico",
      inicioTexto,
      y + 20,
      anchoTexto
    );
    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
    g.setColor(Color.BLACK);
    texto(g, reporte.estudiante().toString(), inicioTexto, y + 43, anchoTexto);
    texto(
      g,
      "Grado: " + reporte.estudiante().getGrado() + " | Fecha: " + fecha,
      inicioTexto,
      y + 59,
      anchoTexto
    );
    g.drawString(
      "Períodos parciales · Escala de 0 a 100 · — = sin nota",
      x,
      y + 75
    );
    g.drawString("Orden: " + reporte.orden(), x, y + 87);
    int[] anchuras = { 65, 199, 69, 69, 69, 69 };
    String[] titulos = {
      "Código",
      "Curso",
      "1.er parcial",
      "2.º parcial",
      "3.er parcial",
      "4.º parcial",
    };
    int pos = x;
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
    for (int i = 0; i < 6; i++) {
      g.setColor(new Color(230, 237, 246));
      g.fillRect(pos, y + 96, anchuras[i], 32);
      g.setColor(Color.BLACK);
      texto(
        g,
        i < 2 ? titulos[i] : i - 1 + ".º período",
        pos + 4,
        y + 109,
        anchuras[i] - 8
      );
      if (i >= 2) texto(g, "parcial", pos + 4, y + 121, anchuras[i] - 8);
      pos += anchuras[i];
    }
    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
    int inicio = indice * FILAS_POR_PAGINA,
      fin = Math.min(inicio + FILAS_POR_PAGINA, reporte.filas().size());
    for (int i = inicio; i < fin; i++) {
      var fila = reporte.filas().get(i);
      int altura = y + 128 + (i - inicio) * 22;
      pos = x;
      String[] valores = {
        fila.curso().codigo(),
        fila.curso().nombre(),
        valor(fila.primero()),
        valor(fila.segundo()),
        valor(fila.tercero()),
        valor(fila.cuarto()),
      };
      for (int j = 0; j < 6; j++) {
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(pos, altura, anchuras[j], 22);
        g.setColor(
          j >= 2 &&
            fila.nota(j - 1) != null &&
            fila.nota(j - 1).compareTo(java.math.BigDecimal.valueOf(60)) < 0
            ? new Color(170, 15, 30)
            : Color.BLACK
        );
        texto(g, valores[j], pos + 4, altura + 15, anchuras[j] - 8);
        pos += anchuras[j];
      }
    }
    g.setColor(Color.BLACK);
    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
    if (indice == paginas - 1) {
      g.drawString(
        "Promedio de notas registradas: " +
          (reporte.promedio() == null
            ? "Sin notas"
            : reporte.promedio().toPlainString()),
        x,
        y + 650
      );
      g.drawString(
        "Cursos aprobados: " +
          reporte.aprobados() +
          " | Reprobados: " +
          reporte.reprobados() +
          " | Pendientes: " +
          reporte.pendientes(),
        x,
        y + 668
      );
      g.drawString(
        "Aprobación: promedio de los 4 parciales completos ≥ 60.",
        x,
        y + 686
      );
    }
    g.drawString(
      "SGAI | Página " + (indice + 1) + " de " + paginas,
      x,
      y + 711
    );
  }

  private String valor(java.math.BigDecimal nota) {
    return nota == null ? "—" : nota.toPlainString();
  }
}
