package com.sgai.util;

import java.text.Normalizer;
import java.util.Locale;

// Búsquedas parciales por palabras, sin distinguir mayúsculas ni tildes.
public final class Busquedas {

  private Busquedas() {}

  public static String normalizar(String texto) {
    return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
      .replaceAll("\\p{M}+", "")
      .toLowerCase(Locale.ROOT)
      .strip();
  }

  public static boolean coincide(String consulta, String texto) {
    String normalizado = normalizar(texto);
    for (String palabra : normalizar(consulta).split("\\s+"))
      if (!normalizado.contains(palabra)) return false;
    return true;
  }
}
