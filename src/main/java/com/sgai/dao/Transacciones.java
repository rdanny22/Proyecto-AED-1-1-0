package com.sgai.dao;

import com.sgai.util.ExcepcionNegocio;
import java.sql.*;

// Unidad de trabajo JDBC: commit o rollback y cierre determinista.
public final class Transacciones {

  private Transacciones() {}

  @FunctionalInterface
  public interface Trabajo<T> {
    T ejecutar(Connection conexion) throws SQLException;
  }

  public static <T> T ejecutar(Trabajo<T> trabajo) {
    try (Connection conexion = Conexion.abrir()) {
      conexion.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
      conexion.setAutoCommit(false);
      try {
        T resultado = trabajo.ejecutar(conexion);
        // Confirmamos los cambios juntos, solo después de completar todo el trabajo.
        conexion.commit();
        return resultado;
      } catch (SQLException | RuntimeException error) {
        try {
          // Ante un error se deshace lo escrito para no dejar datos a medio guardar.
          conexion.rollback();
        } catch (SQLException fallo) {
          error.addSuppressed(fallo);
        }
        throw error;
      }
    } catch (SQLException error) {
      if (
        error.getSQLState() != null && error.getSQLState().startsWith("23")
      ) throw new ExcepcionNegocio(
        "Operación rechazada: código/usuario/nota duplicado o registro relacionado. Revise las dependencias.",
        error
      );
      throw new ExcepcionNegocio(
        "No se completó la operación de base de datos. Verifique conexión, permisos y esquema; reintente tras comprobar los datos.",
        error
      );
    }
  }
}
