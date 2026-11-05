package com.sgai.util;

// Error esperado que puede mostrarse al usuario sin revelar detalles internos.
public class ExcepcionNegocio extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ExcepcionNegocio(String mensaje) {
    super(mensaje);
  }

  public ExcepcionNegocio(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
