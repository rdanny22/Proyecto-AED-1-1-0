package com.sgai.vista;

import java.awt.Graphics2D;
import java.awt.print.Printable;

// Contrato compartido por la vista previa y la impresión de documentos carta.
public interface DocumentoImprimible extends Printable {
  int getPaginas();
  void dibujar(Graphics2D grafico, int indice, int x, int y, int ancho);
}
