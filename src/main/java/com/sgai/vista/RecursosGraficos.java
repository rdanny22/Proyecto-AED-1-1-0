package com.sgai.vista;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;

// Emblema compartido desde el classpath: funciona tanto en Eclipse como dentro del JAR.
public final class RecursosGraficos {

  private static final String RUTA_EMBLEMA = "/imagenes/emblema-colegio.png";
  private static final int MAXIMO_BYTES = 8 * 1024 * 1024;
  private static final int MAXIMA_DIMENSION = 4096;

  private RecursosGraficos() {}

  // Carga única y diferida; nunca utiliza rutas de Windows ni servicios externos.
  private static final class EmblemaCompartido {

    private static final BufferedImage IMAGEN = leerEmblema(
      RecursosGraficos.class.getResourceAsStream(RUTA_EMBLEMA)
    );
  }

  // Imagen interna de solo lectura. Ausencia o formato inválido no impiden usar SGAI.
  static BufferedImage emblema() {
    return EmblemaCompartido.IMAGEN;
  }

  // Verifica tamaño antes de descomprimir y cierra incluso recursos inválidos.
  static BufferedImage leerEmblema(InputStream entrada) {
    if (entrada == null) return null;
    try (InputStream recurso = entrada) {
      byte[] datos = recurso.readNBytes(MAXIMO_BYTES + 1);
      if (datos.length > MAXIMO_BYTES) return null;
      try (
        ImageInputStream imagen = new MemoryCacheImageInputStream(
          new ByteArrayInputStream(datos)
        )
      ) {
        Iterator<ImageReader> lectores = ImageIO.getImageReaders(imagen);
        if (!lectores.hasNext()) return null;
        ImageReader lector = lectores.next();
        try {
          lector.setInput(imagen, true, true);
          int ancho = lector.getWidth(0),
            alto = lector.getHeight(0);
          if (
            ancho < 1 ||
            alto < 1 ||
            ancho > MAXIMA_DIMENSION ||
            alto > MAXIMA_DIMENSION
          ) return null;
          return lector.read(0);
        } finally {
          lector.dispose();
        }
      }
    } catch (IOException | RuntimeException error) {
      // No se inventa un emblema sustituto ni se bloquea el acceso por un recurso opcional.
      return null;
    }
  }

  // Ajuste centrado sin deformación ni recorte; coordenadas válidas también al imprimir.
  static Rectangle limites(
    BufferedImage imagen,
    int x,
    int y,
    int ancho,
    int alto
  ) {
    if (ancho <= 0 || alto <= 0) throw new IllegalArgumentException(
      "Área del emblema inválida"
    );
    if (imagen == null) return new Rectangle(x, y, 0, 0);
    double escala = Math.min(
      (double) ancho / imagen.getWidth(),
      (double) alto / imagen.getHeight()
    );
    int destinoAncho = Math.min(
      ancho,
      Math.max(1, (int) Math.round(imagen.getWidth() * escala))
    );
    int destinoAlto = Math.min(
      alto,
      Math.max(1, (int) Math.round(imagen.getHeight() * escala))
    );
    return new Rectangle(
      x + (ancho - destinoAncho) / 2,
      y + (alto - destinoAlto) / 2,
      destinoAncho,
      destinoAlto
    );
  }

  // Reutiliza el original de alta resolución en el Printable, sin alterar el Graphics recibido.
  static void dibujarEmblema(
    Graphics2D grafico,
    BufferedImage imagen,
    int x,
    int y,
    int ancho,
    int alto
  ) {
    Rectangle destino = limites(imagen, x, y, ancho, alto);
    if (destino.isEmpty()) return;
    Graphics2D copia = (Graphics2D) grafico.create();
    try {
      copia.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BICUBIC
      );
      copia.setRenderingHint(
        RenderingHints.KEY_RENDERING,
        RenderingHints.VALUE_RENDER_QUALITY
      );
      copia.drawImage(
        imagen,
        destino.x,
        destino.y,
        destino.width,
        destino.height,
        null
      );
    } finally {
      copia.dispose();
    }
  }

  public static JLabel etiquetaEmblema(int ancho, int alto) {
    return etiquetaEmblema(emblema(), ancho, alto);
  }

  static JLabel etiquetaEmblema(BufferedImage original, int ancho, int alto) {
    // También se validan las dimensiones cuando no hay imagen.
    limites(original, 0, 0, ancho, alto);
    JLabel etiqueta = new JLabel();
    if (original == null) return etiqueta;
    BufferedImage ajustada = new BufferedImage(
      ancho,
      alto,
      BufferedImage.TYPE_INT_ARGB
    );
    Graphics2D grafico = ajustada.createGraphics();
    try {
      dibujarEmblema(grafico, original, 0, 0, ancho, alto);
    } finally {
      grafico.dispose();
    }
    etiqueta.setIcon(new ImageIcon(ajustada));
    etiqueta.setToolTipText("Emblema del Colegio Antonio Soldano");
    etiqueta
      .getAccessibleContext()
      .setAccessibleName("Emblema del Colegio Antonio Soldano");
    return etiqueta;
  }

  // Encabezado con emblema opcional a la izquierda y texto existente a la derecha.
  public static JPanel cabecera(JComponent texto, int ancho, int alto) {
    JPanel panel = new JPanel(new BorderLayout(12, 0));
    panel.setOpaque(false);
    JLabel etiqueta = etiquetaEmblema(ancho, alto);
    if (etiqueta.getIcon() != null) panel.add(etiqueta, BorderLayout.WEST);
    panel.add(texto, BorderLayout.CENTER);
    return panel;
  }

  public static void aplicarIcono(Window ventana) {
    BufferedImage imagen = emblema();
    if (imagen != null) ventana.setIconImage(imagen);
  }
}
