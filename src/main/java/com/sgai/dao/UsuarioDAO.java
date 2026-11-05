package com.sgai.dao;

import com.sgai.modelo.*;
import com.sgai.seguridad.Claves;
import com.sgai.util.*;
import java.sql.*;

// DAO de autenticación; los hashes nunca salen de esta capa.
public final class UsuarioDAO {

  private static final String HASH_FICTICIO =
    "600000:AAAAAAAAAAAAAAAAAAAAAA==:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

  public boolean sinUsuarios(Connection conexion) throws SQLException {
    try (
      PreparedStatement consulta = conexion.prepareStatement(
        "SELECT COUNT(*) FROM usuarios"
      );
      ResultSet filas = consulta.executeQuery()
    ) {
      filas.next();
      return filas.getLong(1) == 0;
    }
  }

  public Usuario autenticar(Connection conexion, String nombre, char[] clave)
    throws SQLException {
    try (
      PreparedStatement consulta = conexion.prepareStatement(
        "SELECT * FROM usuarios WHERE nombre=?"
      )
    ) {
      consulta.setString(1, nombre);
      try (ResultSet filas = consulta.executeQuery()) {
        if (!filas.next()) {
          Claves.verificar(clave, HASH_FICTICIO);
          throw new ExcepcionNegocio("Credenciales incorrectas.");
        }
        if (
          !Claves.verificar(clave, filas.getString("clave_hash"))
        ) throw new ExcepcionNegocio("Credenciales incorrectas.");
        return new Usuario(
          filas.getLong("id"),
          filas.getString("nombre"),
          Rol.valueOf(filas.getString("rol")),
          filas.getObject("estudiante_id", Long.class),
          filas.getObject("docente_id", Long.class)
        );
      }
    }
  }

  public void crear(
    Connection conexion,
    String nombre,
    char[] clave,
    Rol rol,
    Long personaId
  ) throws SQLException {
    if (
      nombre == null || !nombre.matches("[a-zA-Z0-9._-]{4,40}")
    ) throw new ExcepcionNegocio(
      "Usuario: de 4 a 40 letras, números, punto, guion o guion bajo."
    );
    if (
      (rol == Rol.ADMINISTRADOR) != (personaId == null)
    ) throw new ExcepcionNegocio(
      "Asocie la cuenta a la persona correspondiente."
    );
    try (
      PreparedStatement consulta = conexion.prepareStatement(
        "INSERT INTO usuarios(nombre,clave_hash,rol,estudiante_id,docente_id) VALUES(?,?,?,?,?)"
      )
    ) {
      consulta.setString(1, nombre);
      consulta.setString(2, Claves.crear(clave));
      consulta.setString(3, rol.name());
      consulta.setObject(4, rol == Rol.ESTUDIANTE ? personaId : null);
      consulta.setObject(5, rol == Rol.DOCENTE ? personaId : null);
      consulta.executeUpdate();
    }
  }

  // El bloqueo asesor serializa el alta inicial entre instancias. Debe liberarse tras commit.
  public void bloquearInicio(Connection conexion) throws SQLException {
    try (
      PreparedStatement consulta = conexion.prepareStatement(
        "SELECT GET_LOCK('sgai_aprovisionamiento',10)"
      );
      ResultSet filas = consulta.executeQuery()
    ) {
      if (!filas.next() || filas.getInt(1) != 1) throw new ExcepcionNegocio(
        "Otra instancia está configurando el sistema. Reintente."
      );
    }
  }

  public void liberarInicio(Connection conexion) throws SQLException {
    try (
      PreparedStatement consulta = conexion.prepareStatement(
        "SELECT RELEASE_LOCK('sgai_aprovisionamiento')"
      )
    ) {
      consulta.execute();
    }
  }
}
