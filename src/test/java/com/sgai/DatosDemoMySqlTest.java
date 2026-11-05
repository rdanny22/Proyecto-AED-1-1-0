package com.sgai;

import static org.junit.jupiter.api.Assertions.*;

import com.sgai.controlador.ControladorAcademico;
import com.sgai.dao.Conexion;
import com.sgai.modelo.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// Solo para una BD de pruebas desechable con esquema nuevo; nunca para datos reales.
@EnabledIfEnvironmentVariable(named = "SGAI_PRUEBAS_DEMO", matches = "true")
class DatosDemoMySqlTest {

  private long numero(Connection conexion, String sql) throws SQLException {
    try (
      Statement consulta = conexion.createStatement();
      ResultSet filas = consulta.executeQuery(sql)
    ) {
      filas.next();
      return filas.getLong(1);
    }
  }

  private void ejecutar(Connection conexion, String sql) throws SQLException {
    try (Statement consulta = conexion.createStatement()) {
      consulta.execute(sql);
    }
  }

  private long secuencia(Connection conexion, String tipo) throws SQLException {
    return numero(
      conexion,
      "SELECT siguiente FROM secuencias_codigo WHERE tipo='" + tipo + "'"
    );
  }

  private void comprobarBaseSinDemo(Connection conexion) throws SQLException {
    assertEquals(1, numero(conexion, "SELECT COUNT(*) FROM estudiantes"));
    assertEquals(1, numero(conexion, "SELECT COUNT(*) FROM docentes"));
    assertEquals(1, numero(conexion, "SELECT COUNT(*) FROM cursos"));
    assertEquals(0, numero(conexion, "SELECT COUNT(*) FROM matriculas"));
    assertEquals(0, numero(conexion, "SELECT COUNT(*) FROM calificaciones"));
    assertEquals(
      0,
      numero(conexion, "SELECT COUNT(*) FROM asignacion_docente_curso")
    );
    assertEquals(180, secuencia(conexion, "ESTU"));
    assertEquals(1, secuencia(conexion, "PROF"));
    assertEquals(250, secuencia(conexion, "CURS"));
  }

  private boolean cargar() throws Exception {
    try (Connection conexion = Conexion.abrir()) {
      try {
        ejecutar(conexion, "CALL sgai_cargar_demostracion_v1()");
        return true;
      } catch (SQLException error) {
        if (
          "45000".equals(error.getSQLState()) &&
          error.getMessage().contains("Este lote ya existe")
        ) return false;
        throw error;
      }
    }
  }

  @Test
  void cargaAtomicaSinDuplicadosNiConflictosConLaAplicacion() throws Exception {
    try (
      Connection conexion = Conexion.abrir();
      ControladorAcademico administrador = new ControladorAcademico()
    ) {
      assertEquals(
        0,
        numero(conexion, "SELECT COUNT(*) FROM estudiantes"),
        "Solo ejecutar contra una BD nueva y desechable"
      );
      assertTrue(
        administrador
          .autorizarInstalacion(
            System.getenv("SGAI_CLAVE_INSTALACION").toCharArray()
          )
          .get(45, TimeUnit.SECONDS)
      );
      administrador
        .configurar("admin_carga", "Clave-Carga-Prueba-2026".toCharArray())
        .get(45, TimeUnit.SECONDS);
      administrador
        .autenticar("admin_carga", "Clave-Carga-Prueba-2026".toCharArray())
        .get(45, TimeUnit.SECONDS);
      ejecutar(
        conexion,
        "INSERT INTO estudiantes(codigo,nombres,apellidos,correo,grado) VALUES('ESTU120','Existente','Conservar','existente@example.invalid',7)"
      );
      ejecutar(
        conexion,
        "INSERT INTO docentes(codigo,nombres,apellidos,correo,especialidad) VALUES('PROF150','Existente','Conservar','docente@example.invalid','Matemática')"
      );
      ejecutar(
        conexion,
        "INSERT INTO cursos(codigo,nombre,grado) VALUES('CURS200','Curso existente',7)"
      );
      ejecutar(
        conexion,
        "UPDATE secuencias_codigo SET siguiente=180 WHERE tipo='ESTU'"
      );
      ejecutar(
        conexion,
        "UPDATE secuencias_codigo SET siguiente=250 WHERE tipo='CURS'"
      );

      String archivo = Files.readString(
        Path.of("database/datos_demostracion.sql"),
        StandardCharsets.UTF_8
      );
      String rutina = archivo.substring(
        archivo.indexOf("CREATE PROCEDURE sgai_cargar_demostracion_v1"),
        archivo.indexOf("END$$") + 3
      );
      ejecutar(
        conexion,
        "DROP PROCEDURE IF EXISTS sgai_cargar_demostracion_v1"
      );
      ejecutar(conexion, rutina);
      try {
        ejecutar(conexion, "SET SESSION foreign_key_checks=0");
        try {
          SQLException rechazo = assertThrows(SQLException.class, () ->
            ejecutar(conexion, "CALL sgai_cargar_demostracion_v1()")
          );
          assertTrue(rechazo.getMessage().contains("foreign_key_checks=1"));
          comprobarBaseSinDemo(conexion);
        } finally {
          ejecutar(conexion, "SET SESSION foreign_key_checks=1");
        }
        // Forzar un error después de insertar personas, cursos, matrículas y dos notas.
        ejecutar(
          conexion,
          "ALTER TABLE calificaciones ADD CONSTRAINT chk_sgai_demo_prueba_fallo CHECK (periodo_parcial<>3)"
        );
        try {
          SQLException fallo = assertThrows(SQLException.class, () ->
            ejecutar(conexion, "CALL sgai_cargar_demostracion_v1()")
          );
          assertTrue(fallo.getMessage().contains("chk_sgai_demo_prueba_fallo"));
          comprobarBaseSinDemo(conexion);
          assertEquals(
            1,
            numero(
              conexion,
              "SELECT IS_FREE_LOCK('sgai_carga_demostracion_v1')"
            )
          );
        } finally {
          ejecutar(
            conexion,
            "ALTER TABLE calificaciones DROP CHECK chk_sgai_demo_prueba_fallo"
          );
        }

        // Dos sesiones simultáneas: exactamente una inserta y la otra rechaza el lote.
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
          Future<Boolean> primera = hilos.submit(this::cargar),
            segunda = hilos.submit(this::cargar);
          assertNotEquals(
            primera.get(60, TimeUnit.SECONDS),
            segunda.get(60, TimeUnit.SECONDS)
          );
        } finally {
          hilos.shutdownNow();
        }
        assertEquals(56, numero(conexion, "SELECT COUNT(*) FROM estudiantes"));
        assertEquals(23, numero(conexion, "SELECT COUNT(*) FROM docentes"));
        assertEquals(23, numero(conexion, "SELECT COUNT(*) FROM cursos"));
        assertEquals(
          22,
          numero(conexion, "SELECT COUNT(*) FROM asignacion_docente_curso")
        );
        assertEquals(110, numero(conexion, "SELECT COUNT(*) FROM matriculas"));
        assertEquals(
          440,
          numero(conexion, "SELECT COUNT(*) FROM calificaciones")
        );
        assertEquals(1, numero(conexion, "SELECT COUNT(*) FROM usuarios"));
        assertEquals(
          0,
          numero(
            conexion,
            "SELECT COUNT(*) FROM (SELECT grado FROM estudiantes WHERE correo LIKE '%@sgai-demo-v1.invalid' GROUP BY grado HAVING COUNT(*)<>5) g"
          )
        );
        assertEquals(
          11,
          numero(
            conexion,
            "SELECT COUNT(DISTINCT grado) FROM estudiantes WHERE correo LIKE '%@sgai-demo-v1.invalid'"
          )
        );
        assertEquals(
          0,
          numero(
            conexion,
            "SELECT COUNT(*) FROM matriculas m JOIN estudiantes e ON e.id=m.estudiante_id JOIN cursos c ON c.id=m.curso_id WHERE e.grado<>c.grado"
          )
        );
        assertEquals(235, secuencia(conexion, "ESTU"));
        assertEquals(173, secuencia(conexion, "PROF"));
        assertEquals(272, secuencia(conexion, "CURS"));
        assertFalse(cargar());
        assertEquals(
          440,
          numero(conexion, "SELECT COUNT(*) FROM calificaciones")
        );
        assertEquals(235, secuencia(conexion, "ESTU"));
        assertEquals(
          1,
          numero(
            conexion,
            "SELECT COUNT(*) FROM estudiantes WHERE codigo='ESTU120' AND apellidos='Conservar'"
          )
        );

        // La siguiente alta usa los contadores de la carga, no vuelve a empezar en 1.
        administrador
          .guardarPersona(
            new Estudiante(
              0,
              null,
              "Después",
              "De carga",
              "nuevo@example.invalid",
              7
            )
          )
          .get(45, TimeUnit.SECONDS);
        administrador
          .guardarPersona(
            new Docente(
              0,
              null,
              "Después",
              "De carga",
              "nuevo.docente@example.invalid",
              "Matemática"
            )
          )
          .get(45, TimeUnit.SECONDS);
        administrador
          .guardarCurso(new Curso(0, null, "Después de carga", 7))
          .get(45, TimeUnit.SECONDS);
        assertNotNull(administrador.getEstado().buscarEstudiante("ESTU235"));
        assertNotNull(administrador.getEstado().buscarDocente("PROF173"));
        assertTrue(
          administrador
            .getEstado()
            .cursos()
            .stream()
            .anyMatch(c -> c.codigo().equals("CURS272"))
        );
      } finally {
        ejecutar(
          conexion,
          "DROP PROCEDURE IF EXISTS sgai_cargar_demostracion_v1"
        );
      }
    }
  }
}
