package com.sgai.vista;

import com.sgai.controlador.ReporteAcademico;
import com.sgai.modelo.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.io.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

// Pruebas headless con figuras sintéticas: no se confunden con el emblema oficial.
public final class VerificacionesEmblema {

  private VerificacionesEmblema() {}

  private static void comprobar(boolean condicion) {
    if (!condicion) throw new AssertionError("Regresión del emblema");
  }

  private static BufferedImage imagen(int ancho, int alto) {
    BufferedImage imagen = new BufferedImage(
      ancho,
      alto,
      BufferedImage.TYPE_INT_ARGB
    );
    Graphics2D grafico = imagen.createGraphics();
    try {
      grafico.setColor(Color.MAGENTA);
      grafico.fillRect(0, 0, ancho, alto);
    } finally {
      grafico.dispose();
    }
    return imagen;
  }

  public static void carga() throws Exception {
    comprobar(RecursosGraficos.leerEmblema(null) == null);
    comprobar(
      RecursosGraficos.leerEmblema(
        new ByteArrayInputStream(new byte[] { 1, 2, 3 })
      ) == null
    );
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    ImageIO.write(imagen(120, 60), "png", salida);
    boolean[] cerrado = { false };
    ByteArrayInputStream entrada = new ByteArrayInputStream(
      salida.toByteArray()
    ) {
      @Override
      public void close() throws IOException {
        cerrado[0] = true;
        super.close();
      }
    };
    BufferedImage leida = RecursosGraficos.leerEmblema(entrada);
    comprobar(
      cerrado[0] &&
        leida != null &&
        leida.getWidth() == 120 &&
        leida.getHeight() == 60
    );
    comprobar(leida.getRGB(0, 0) == Color.MAGENTA.getRGB());
    comprobar(
      RecursosGraficos.leerEmblema(
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("Fallo simulado");
          }
        }
      ) == null
    );
    comprobar(
      RecursosGraficos.leerEmblema(
        new ByteArrayInputStream(new byte[8 * 1024 * 1024 + 1])
      ) == null
    );
    salida.reset();
    ImageIO.write(imagen(4097, 1), "png", salida);
    comprobar(
      RecursosGraficos.leerEmblema(
        new ByteArrayInputStream(salida.toByteArray())
      ) == null
    );
  }

  public static void proporciones() throws Exception {
    comprobar(
      RecursosGraficos.limites(imagen(120, 60), 10, 20, 80, 80).equals(
        new Rectangle(10, 40, 80, 40)
      )
    );
    comprobar(
      RecursosGraficos.limites(imagen(60, 120), 10, 20, 80, 80).equals(
        new Rectangle(30, 20, 40, 80)
      )
    );
    comprobar(RecursosGraficos.limites(null, 0, 0, 56, 56).isEmpty());
    SwingUtilities.invokeAndWait(() -> {
      var etiqueta = RecursosGraficos.etiquetaEmblema(imagen(120, 60), 80, 80);
      comprobar(
        etiqueta.getIcon().getIconWidth() == 80 &&
          etiqueta.getIcon().getIconHeight() == 80
      );
      comprobar(
        etiqueta.getAccessibleContext().getAccessibleName().contains("Emblema")
      );
      BufferedImage icono = (BufferedImage) (
        (javax.swing.ImageIcon) etiqueta.getIcon()
      ).getImage();
      comprobar(icono.getRGB(0, 0) >>> 24 == 0); // Márgenes transparentes, no fondo artificial.
      comprobar(icono.getRGB(40, 40) == Color.MAGENTA.getRGB());
      comprobar(
        RecursosGraficos.etiquetaEmblema(null, 80, 80).getIcon() == null
      );
    });
  }

  public static void impresion() {
    var cursos = new java.util.ArrayList<Curso>();
    for (int i = 1; i <= 50; i++) cursos.add(
      new Curso(i, "CURS" + i, "Curso de prueba", 7)
    );
    var reporte = new ReporteAcademico(
      new Estudiante(1, "ESTU1", "María", "Pérez", "maria@colegio.edu.ni", 7),
      cursos,
      List.of(),
      0
    );
    var conEmblema = new DocumentoReporte(reporte, imagen(120, 60));
    var sinEmblema = new DocumentoReporte(reporte, null);
    comprobar(conEmblema.getPaginas() == 3 && sinEmblema.getPaginas() == 3);
    for (int pagina = 0; pagina < 3; pagina++) {
      BufferedImage hoja = new BufferedImage(
        612,
        792,
        BufferedImage.TYPE_INT_RGB
      );
      Graphics2D grafico = hoja.createGraphics();
      try {
        grafico.setColor(Color.WHITE);
        grafico.fillRect(0, 0, 612, 792);
        comprobar(
          conEmblema.print(grafico, DocumentoReporte.formatoCarta(), pagina) ==
            Printable.PAGE_EXISTS
        );
        // El rectángulo magenta está centrado en 56x56, dentro del margen carta.
        comprobar(hoja.getRGB(40, 56) == Color.MAGENTA.getRGB());
        comprobar(hoja.getRGB(0, 0) == Color.WHITE.getRGB());
        comprobar(hoja.getRGB(611, 791) == Color.WHITE.getRGB());
        // El recurso opcional no modifica la tabla, totales ni pie de página.
        BufferedImage anterior = new BufferedImage(
          612,
          792,
          BufferedImage.TYPE_INT_RGB
        );
        Graphics2D otro = anterior.createGraphics();
        try {
          otro.setColor(Color.WHITE);
          otro.fillRect(0, 0, 612, 792);
          sinEmblema.print(otro, DocumentoReporte.formatoCarta(), pagina);
        } finally {
          otro.dispose();
        }
        for (int y = 100; y < 792; y++) for (int x = 0; x < 612; x++) comprobar(
          hoja.getRGB(x, y) == anterior.getRGB(x, y)
        );
        // Dibujar el emblema no cambia la transformación ni el color del contexto ajeno.
        var transformacion = grafico.getTransform();
        Color color = grafico.getColor();
        RecursosGraficos.dibujarEmblema(grafico, imagen(10, 20), 0, 0, 30, 30);
        comprobar(
          transformacion.equals(grafico.getTransform()) &&
            color.equals(grafico.getColor())
        );
      } finally {
        grafico.dispose();
      }
    }
  }

  // Comprueba que la imagen entregada por el colegio viaja en el classpath/JAR.
  public static void recursoInstitucional() {
    BufferedImage emblema = RecursosGraficos.emblema();
    comprobar(emblema != null);
    comprobar(emblema.getWidth() > 0 && emblema.getHeight() > 0);
    comprobar(RecursosGraficos.etiquetaEmblema(80, 80).getIcon() != null);
    var reporte = new ReporteAcademico(
      new Estudiante(1, "ESTU1", "Ana", "Pérez", "ana@colegio.edu.ni", 7),
      List.of(),
      List.of(),
      0
    );
    var documento = new DocumentoReporte(reporte);
    BufferedImage hoja = new BufferedImage(
      612,
      792,
      BufferedImage.TYPE_INT_RGB
    );
    BufferedImage esperado = new BufferedImage(
      612,
      792,
      BufferedImage.TYPE_INT_RGB
    );
    Graphics2D pagina = hoja.createGraphics(),
      referencia = esperado.createGraphics();
    try {
      pagina.setColor(Color.WHITE);
      pagina.fillRect(0, 0, 612, 792);
      referencia.setColor(Color.WHITE);
      referencia.fillRect(0, 0, 612, 792);
      documento.print(pagina, DocumentoReporte.formatoCarta(), 0);
      RecursosGraficos.dibujarEmblema(referencia, emblema, 36, 38, 56, 56);
      int pintados = 0;
      for (int y = 38; y < 94; y++) for (int x = 36; x < 92; x++) {
        comprobar(hoja.getRGB(x, y) == esperado.getRGB(x, y));
        if (hoja.getRGB(x, y) != Color.WHITE.getRGB()) pintados++;
      }
      comprobar(pintados > 0);
    } finally {
      pagina.dispose();
      referencia.dispose();
    }
  }

  public static void main(String[] argumentos) throws Exception {
    carga();
    proporciones();
    impresion();
    recursoInstitucional();
  }
}
