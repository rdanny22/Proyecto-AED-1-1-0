package com.sgai.dao;

import com.sgai.util.ExcepcionNegocio;
import java.sql.*;

// Configuración externa: nunca se incluyen credenciales en el repositorio.
public final class Conexion {

  private Conexion() {}

  public static Connection abrir() throws SQLException {
    String usuario = System.getenv("SGAI_DB_USUARIO"),
      clave = System.getenv("SGAI_DB_CLAVE");
    if (usuario == null || clave == null) throw new ExcepcionNegocio(
      "Configure SGAI_DB_USUARIO y SGAI_DB_CLAVE antes de iniciar."
    );
    String url = System.getenv().getOrDefault(
      "SGAI_DB_URL",
      "jdbc:mysql://localhost:3306/sgai-soldano-db?sslMode=VERIFY_IDENTITY&connectTimeout=5000&socketTimeout=15000&connectionTimeZone=UTC"
    );
    return DriverManager.getConnection(url, usuario, clave);
  }
}
