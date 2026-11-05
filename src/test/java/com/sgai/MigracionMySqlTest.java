package com.sgai;

import static org.junit.jupiter.api.Assertions.*;

import com.sgai.controlador.*;
import com.sgai.dao.*;
import com.sgai.modelo.*;
import com.sgai.util.ExcepcionNegocio;
import java.sql.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// Ejecutar solo con los fixtures anteriores seguidos por la migración 002, en una BD desechable.
@EnabledIfEnvironmentVariable(
  named = "SGAI_PRUEBAS_MIGRACION",
  matches = "true"
)
class MigracionMySqlTest {

  @Test
  void conservaHistorialYContinuaConsecutivos() throws Exception {
    try (ControladorAcademico controlador = new ControladorAcademico()) {
      assertTrue(
        controlador
          .autorizarInstalacion(
            System.getenv("SGAI_CLAVE_INSTALACION").toCharArray()
          )
          .get(45, TimeUnit.SECONDS)
      );
      controlador
        .configurar("admin_migracion", "Clave-Migracion-2026".toCharArray())
        .get(45, TimeUnit.SECONDS);
      controlador
        .autenticar("admin_migracion", "Clave-Migracion-2026".toCharArray())
        .get(45, TimeUnit.SECONDS);
      EstadoAcademico anterior = controlador.getEstado();
      assertEquals(1, anterior.matriculas().size());
      assertEquals(2, anterior.calificaciones()[0].periodoParcial());
      assertEquals(85, anterior.calificaciones()[0].nota().intValue());
      assertNotNull(anterior.buscarEstudiante("ESTU40"));
      controlador
        .guardarPersona(
          new Estudiante(0, null, "Luis", "López", "luis@colegio.edu.ni", 7)
        )
        .get(45, TimeUnit.SECONDS);
      controlador
        .guardarPersona(
          new Docente(
            0,
            null,
            "Laura",
            "Díaz",
            "laura@colegio.edu.ni",
            "Historia"
          )
        )
        .get(45, TimeUnit.SECONDS);
      controlador
        .guardarCurso(new Curso(0, null, "Historia", 7))
        .get(45, TimeUnit.SECONDS);
      assertNotNull(controlador.getEstado().buscarEstudiante("ESTU41"));
      assertNotNull(controlador.getEstado().buscarDocente("PROF8"));
      assertTrue(
        controlador
          .getEstado()
          .cursos()
          .stream()
          .anyMatch(c -> c.codigo().equals("CURS91"))
      );
      assertThrows(ExcepcionNegocio.class, () ->
        Transacciones.ejecutar(c -> {
          try (
            PreparedStatement consulta = c.prepareStatement(
              "INSERT INTO calificaciones(estudiante_id,curso_id,docente_id,periodo_parcial,nota) VALUES(2,1,1,1,80)"
            )
          ) {
            consulta.executeUpdate();
          }
          return null;
        })
      );
      assertThrows(ExcepcionNegocio.class, () ->
        Transacciones.ejecutar(c -> {
          try (
            PreparedStatement consulta = c.prepareStatement(
              "UPDATE calificaciones SET periodo_parcial=5 WHERE id=1"
            )
          ) {
            consulta.executeUpdate();
          }
          return null;
        })
      );
    }
  }
}
