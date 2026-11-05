package com.sgai;

import static org.junit.jupiter.api.Assertions.*;

import com.sgai.controlador.*;
import com.sgai.dao.*;
import com.sgai.modelo.*;
import com.sgai.util.ExcepcionNegocio;
import java.math.BigDecimal;
import java.sql.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// Requiere una BD desechable y VACÍA con el esquema aplicado. Nunca usar datos reales.
@EnabledIfEnvironmentVariable(named = "SGAI_PRUEBAS_MYSQL", matches = "true")
class MySqlIntegracionTest {

  private static <T> T esperar(CompletableFuture<T> tarea) throws Exception {
    return tarea.get(45, TimeUnit.SECONDS);
  }

  private static void rechazar(CompletableFuture<?> tarea) {
    ExecutionException fallo = assertThrows(ExecutionException.class, () ->
      tarea.get(45, TimeUnit.SECONDS)
    );
    assertInstanceOf(ExcepcionNegocio.class, fallo.getCause());
  }

  @Test
  void recorreFlujoCompletoConPermisosYRollback() throws Exception {
    try (
      ControladorAcademico administrador = new ControladorAcademico();
      ControladorAcademico profesor = new ControladorAcademico();
      ControladorAcademico estudiante = new ControladorAcademico()
    ) {
      rechazar(
        administrador.configurar("intruso", "Clave-Intruso-2026".toCharArray())
      );
      rechazar(administrador.autorizarInstalacion("incorrecta".toCharArray()));
      String llave = System.getenv("SGAI_CLAVE_INSTALACION");
      assertNotNull(
        llave,
        "Configure SGAI_CLAVE_INSTALACION solo en la instancia de pruebas"
      );
      assertTrue(
        esperar(administrador.autorizarInstalacion(llave.toCharArray())),
        "La BD de integración debe ser vacía y desechable"
      );
      esperar(
        administrador.configurar(
          "admin_prueba",
          "Clave-Admin-2026".toCharArray()
        )
      );
      try (ControladorAcademico otraInstalacion = new ControladorAcademico()) {
        assertFalse(
          esperar(otraInstalacion.autorizarInstalacion(llave.toCharArray()))
        );
        rechazar(
          otraInstalacion.configurar(
            "otro_admin",
            "Clave-Admin-2026".toCharArray()
          )
        );
      }
      esperar(
        administrador.autenticar(
          "admin_prueba",
          "Clave-Admin-2026".toCharArray()
        )
      );
      rechazar(
        administrador.configurar("otro_admin", "Clave-Admin-2026".toCharArray())
      );
      esperar(
        administrador.guardarPersona(
          new Estudiante(0, "ES01", "Ana", "Pérez", "ana@colegio.edu.ni", 7)
        )
      );
      esperar(
        administrador.guardarPersona(
          new Estudiante(0, "ES02", "Luis", "Díaz", "luis@colegio.edu.ni", 8)
        )
      );
      esperar(
        administrador.guardarPersona(
          new Docente(
            0,
            "DO01",
            "Laura",
            "López",
            "laura@colegio.edu.ni",
            "Ciencias"
          )
        )
      );
      esperar(
        administrador.guardarCurso(new Curso(0, "CU07", "Matemáticas", 7))
      );
      esperar(administrador.guardarCurso(new Curso(0, "CU08", "Historia", 8)));
      esperar(
        administrador.guardarPersona(
          new Estudiante(
            0,
            null,
            "Pedro",
            "Sin matrícula",
            "pedro@colegio.edu.ni",
            7
          )
        )
      );
      EstadoAcademico datos = administrador.getEstado();
      long ana = datos.buscarEstudiante("ESTU1").getId(),
        luis = datos.buscarEstudiante("ESTU2").getId();
      long docente = datos.docentes().get(0).getId();
      long curso = datos
        .cursos()
        .stream()
        .filter(c -> c.codigo().equals("CURS1"))
        .findFirst()
        .orElseThrow()
        .id();
      long otroCurso = datos
        .cursos()
        .stream()
        .filter(c -> c.codigo().equals("CURS2"))
        .findFirst()
        .orElseThrow()
        .id();
      esperar(administrador.asignar(new Asignacion(docente, curso), false));
      assertEquals("PROF1", datos.docentes().get(0).getCodigo());
      assertEquals(0, datos.matriculas().size());
      rechazar(
        administrador.guardarNota(
          new Calificacion(0, ana, curso, docente, 1, BigDecimal.TEN)
        )
      );
      rechazar(
        administrador.matricular(ana, java.util.List.of(curso, otroCurso))
      );
      assertEquals(
        0,
        administrador.getEstado().matriculas().size(),
        "Una matrícula múltiple inválida revierte por completo"
      );
      rechazar(administrador.eliminarMatricula(ana));
      rechazar(administrador.eliminarMatricula(0));
      esperar(administrador.matricular(ana, java.util.List.of(curso)));
      esperar(administrador.eliminarMatricula(ana));
      assertTrue(administrador.getEstado().cursosMatriculados(ana).isEmpty());
      assertNotNull(administrador.getEstado().buscarEstudiante("ESTU1"));
      assertEquals(2, administrador.getEstado().cursos().size());
      esperar(administrador.matricular(ana, java.util.List.of(curso)));
      esperar(administrador.matricular(ana, java.util.List.of(curso)));
      assertEquals(
        1,
        administrador.getEstado().matriculas().size(),
        "Matricular dos veces no duplica filas"
      );

      esperar(
        administrador.crearUsuario(
          "docente_prueba",
          "Clave-Docente-2026".toCharArray(),
          datos.docentes().get(0)
        )
      );
      esperar(
        administrador.crearUsuario(
          "alumno_prueba",
          "Clave-Alumno-2026".toCharArray(),
          datos.buscarEstudiante("ESTU1")
        )
      );
      // El nombre del usuario no concede roles: la persona registrada determina el rol.
      esperar(
        administrador.crearUsuario(
          "AdminAlumno",
          "Clave-Alumno-2026".toCharArray(),
          datos.buscarEstudiante("ESTU2")
        )
      );
      esperar(
        administrador.crearAdministrador(
          "Admin2",
          "Clave-Admin2-2026".toCharArray()
        )
      );
      try (ControladorAcademico nuevaCuenta = new ControladorAcademico()) {
        Usuario admin2 = esperar(
          nuevaCuenta.autenticar("Admin2", "Clave-Admin2-2026".toCharArray())
        );
        assertEquals(Rol.ADMINISTRADOR, admin2.rol());
        assertNull(admin2.estudianteId());
        assertNull(admin2.docenteId());
      }
      try (ControladorAcademico nuevaCuenta = new ControladorAcademico()) {
        Usuario alumno = esperar(
          nuevaCuenta.autenticar(
            "AdminAlumno",
            "Clave-Alumno-2026".toCharArray()
          )
        );
        assertEquals(Rol.ESTUDIANTE, alumno.rol());
        assertEquals(Long.valueOf(luis), alumno.estudianteId());
        assertNull(alumno.docenteId());
      }
      rechazar(
        administrador.crearUsuario(
          "sin_persona",
          "Clave-Prueba-2026".toCharArray(),
          null
        )
      );
      rechazar(
        administrador.crearUsuario(
          "sin_registro",
          "Clave-Prueba-2026".toCharArray(),
          new Estudiante(0, null, "Ana", "Pérez", "ana@colegio.edu.ni", 7)
        )
      );
      rechazar(
        administrador.crearUsuario(
          "ya_eliminado",
          "Clave-Prueba-2026".toCharArray(),
          new Docente(
            999999,
            "PROF999999",
            "Ana",
            "Pérez",
            "ana@colegio.edu.ni",
            "Ciencias"
          )
        )
      );
      rechazar(
        administrador.crearUsuario(
          "duplicado",
          "Clave-Prueba-2026".toCharArray(),
          datos.docentes().get(0)
        )
      );
      esperar(
        profesor.autenticar(
          "docente_prueba",
          "Clave-Docente-2026".toCharArray()
        )
      );
      assertEquals(Rol.DOCENTE, profesor.getSesion().rol());
      assertEquals(Long.valueOf(docente), profesor.getSesion().docenteId());
      assertNull(profesor.getSesion().estudianteId());
      rechazar(
        profesor.crearAdministrador(
          "intruso_admin",
          "Clave-Prueba-2026".toCharArray()
        )
      );
      rechazar(
        profesor.crearUsuario(
          "intruso_persona",
          "Clave-Prueba-2026".toCharArray(),
          datos.buscarEstudiante("ESTU1")
        )
      );
      rechazar(
        profesor.configurar("intruso", "Clave-Intruso-2026".toCharArray())
      );
      rechazar(profesor.autorizarInstalacion(llave.toCharArray()));
      assertEquals(1, profesor.getEstado().estudiantes().length);
      rechazar(profesor.eliminarMatricula(ana));
      rechazar(profesor.matricular(luis, java.util.List.of(curso)));
      assertEquals(
        "Laura López",
        profesor.getEstado().docentes().get(0).getNombreCompleto()
      );
      esperar(
        profesor.guardarNota(
          new Calificacion(0, ana, curso, docente, 1, new BigDecimal("59.99"))
        )
      );
      assertEquals(1, profesor.getEstado().calificaciones().length);
      rechazar(
        profesor.guardarNota(
          new Calificacion(0, luis, curso, docente, 1, BigDecimal.TEN)
        )
      );
      rechazar(
        profesor.guardarNota(
          new Calificacion(0, luis, otroCurso, docente, 1, BigDecimal.TEN)
        )
      );
      rechazar(profesor.guardarCurso(new Curso(0, "FALLO", "Sin permiso", 7)));
      rechazar(
        profesor.guardarNota(
          new Calificacion(0, ana, curso, docente, 1, new BigDecimal("70"))
        )
      );
      assertEquals(
        1,
        profesor.getEstado().calificaciones().length,
        "El fallo no publica datos fantasma"
      );
      esperar(
        estudiante.autenticar(
          "alumno_prueba",
          "Clave-Alumno-2026".toCharArray()
        )
      );
      assertEquals(Rol.ESTUDIANTE, estudiante.getSesion().rol());
      assertEquals(Long.valueOf(ana), estudiante.getSesion().estudianteId());
      assertNull(estudiante.getSesion().docenteId());
      rechazar(
        estudiante.crearUsuario(
          "intruso_persona",
          "Clave-Prueba-2026".toCharArray(),
          datos.docentes().get(0)
        )
      );
      rechazar(
        estudiante.configurar("intruso", "Clave-Intruso-2026".toCharArray())
      );
      rechazar(estudiante.autorizarInstalacion(llave.toCharArray()));
      assertEquals(ana, estudiante.getEstado().estudiantes()[0].getId());
      assertEquals(1, estudiante.getEstado().cursos().size());
      assertEquals(curso, estudiante.getEstado().cursos().get(0).id());
      assertEquals(
        "Laura López",
        estudiante.getEstado().docentes().get(0).getNombreCompleto()
      );
      rechazar(estudiante.eliminarMatricula(ana));
      // La nota se guardó desde la sesión del docente. Actualizamos la del administrador
      // antes de comprobar que el intento rechazado conserva la matrícula y la nota.
      esperar(administrador.actualizar());
      rechazar(administrador.eliminarMatricula(ana));
      assertEquals(1, administrador.getEstado().cursosMatriculados(ana).size());
      assertEquals(1, administrador.getEstado().calificaciones().length);
      rechazar(estudiante.matricular(ana, java.util.List.of(otroCurso)));
      assertEquals(1, estudiante.getEstado().calificaciones().length);
      rechazar(
        estudiante.eliminar(
          AcademicoDAO.Tabla.CALIFICACIONES,
          estudiante.getEstado().calificaciones()[0].id()
        )
      );
      rechazar(
        estudiante.crearAdministrador(
          "intruso",
          "Clave-Intruso-2026".toCharArray()
        )
      );
      rechazar(administrador.eliminar(AcademicoDAO.Tabla.ESTUDIANTES, ana));
      rechazar(administrador.asignar(new Asignacion(docente, curso), true));
      rechazar(
        administrador.guardarPersona(
          new Estudiante(ana, "ES01", "Ana", "Pérez", "ana@colegio.edu.ni", 8)
        )
      );
      assertThrows(ExcepcionNegocio.class, () ->
        Transacciones.ejecutar(conexion -> {
          new AcademicoDAO().guardarCurso(
            conexion,
            new Curso(0, "ROLLBACK", "Debe desaparecer", 7)
          );
          throw new ExcepcionNegocio("Provocar rollback");
        })
      );
      esperar(
        administrador.guardarPersona(
          new Estudiante(ana, "MANUAL", "Ana", "Pérez", "ana@colegio.edu.ni", 7)
        )
      );
      assertNotNull(administrador.getEstado().buscarEstudiante("ESTU1"));
      assertNull(administrador.getEstado().buscarEstudiante("MANUAL"));
      esperar(
        administrador.guardarCurso(new Curso(0, null, "Después de rollback", 7))
      );
      assertTrue(
        administrador
          .getEstado()
          .cursos()
          .stream()
          .anyMatch(c -> c.codigo().equals("CURS3")),
        "El rollback no consume un consecutivo"
      );
      esperar(administrador.actualizar());
      assertFalse(
        administrador
          .getEstado()
          .cursos()
          .stream()
          .anyMatch(c -> c.nombre().equals("Debe desaparecer"))
      );
      esperar(
        administrador.guardarCurso(
          new Curso(0, null, "Curso del mismo grado sin matrícula", 7)
        )
      );
      esperar(estudiante.actualizar());
      assertEquals(
        1,
        estudiante.getEstado().cursos().size(),
        "No basta pertenecer al mismo grado para ver el curso"
      );
      assertEquals(1, estudiante.getEstado().docentes().size());
      try (
        ControladorAcademico otroAdministrador = new ControladorAcademico()
      ) {
        esperar(
          otroAdministrador.autenticar(
            "admin_prueba",
            "Clave-Admin-2026".toCharArray()
          )
        );
        CompletableFuture<EstadoAcademico> uno = administrador.guardarCurso(
          new Curso(0, null, "Alta concurrente A", 7)
        );
        CompletableFuture<EstadoAcademico> dos = otroAdministrador.guardarCurso(
          new Curso(0, null, "Alta concurrente B", 7)
        );
        esperar(uno);
        esperar(dos);
        esperar(administrador.actualizar());
        assertEquals(6, administrador.getEstado().cursos().size());
        assertEquals(
          6,
          administrador
            .getEstado()
            .cursos()
            .stream()
            .map(Curso::codigo)
            .distinct()
            .count()
        );
      }
      // PreparedStatement no interpreta una cadena de inyección como SQL.
      try (ControladorAcademico intruso = new ControladorAcademico()) {
        rechazar(
          intruso.autenticar("' OR '1'='1", "Clave-Admin-2026".toCharArray())
        );
        assertNull(intruso.getSesion());
      }
      esperar(administrador.eliminarUsuario("docente_prueba"));
      rechazar(profesor.actualizar()); // Revocación comprobada también en el controlador.
      rechazar(administrador.eliminarUsuario("admin_prueba"));
      rechazar(administrador.eliminarUsuario("ADMIN_PRUEBA"));
    }
  }
}
