package com.sgai;

import com.sgai.controlador.*;
import com.sgai.modelo.*;
import com.sgai.util.*;
import com.sgai.vista.DocumentoReporte;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

// Pruebas puras de filtros, matrícula, reportes y paginación sin servidor ni impresora.
public final class VerificacionesAcademicas {

  private VerificacionesAcademicas() {}

  private static void comprobar(boolean condicion) {
    if (!condicion) throw new AssertionError("Regresión académica");
  }

  private static BigDecimal decimal(String valor) {
    return new BigDecimal(valor);
  }

  private static Estudiante alumno(long id, String nombre, int grado) {
    return new Estudiante(
      id,
      "ESTU" + id,
      nombre,
      "Pérez López",
      "alumno@colegio.edu.ni",
      grado
    );
  }

  private static EstadoAcademico ejemplo() {
    List<Curso> cursos = List.of(
      new Curso(1, "CURS1", "Matemáticas", 7),
      new Curso(2, "CURS2", "Ciencias", 7),
      new Curso(3, "CURS3", "Historia", 7),
      new Curso(4, "CURS4", "Inglés", 8)
    );
    List<Calificacion> notas = new ArrayList<>();
    long id = 1;
    for (int parcial = 1; parcial <= 4; parcial++) {
      notas.add(
        new Calificacion(
          id++,
          1,
          1,
          1,
          parcial,
          decimal(parcial == 1 ? "60" : "80")
        )
      );
      notas.add(new Calificacion(id++, 1, 2, 1, parcial, decimal("59")));
    }
    notas.add(new Calificacion(id++, 1, 3, 1, 1, decimal("95")));
    notas.add(new Calificacion(id, 2, 4, 2, 1, decimal("100")));
    return new EstadoAcademico(
      List.of(alumno(1, "María José", 7), alumno(2, "Luis", 8)),
      List.of(
        new Docente(1, "PROF1", "Ana", "Díaz", "ana@colegio.edu.ni", "Ciencias")
      ),
      cursos,
      List.of(new Asignacion(1, 1)),
      notas,
      List.of(
        new Matricula(1, 1),
        new Matricula(1, 2),
        new Matricula(1, 3),
        new Matricula(2, 4)
      )
    );
  }

  public static void filtros() {
    EstadoAcademico estado = ejemplo();
    comprobar(estado.buscarEstudiantes("maria lopez").size() == 1);
    comprobar(
      estado.buscarEstudiantes("estu2").get(0).getNombres().equals("Luis")
    );
    comprobar(estado.buscarEstudiantes("NO EXISTE").isEmpty());
    comprobar(estado.buscarDocentes("ana diaz").size() == 1);
    comprobar(estado.buscarDocentes("prof1").size() == 1);
    comprobar(estado.buscarCursos("matematicas").size() == 1);
    comprobar(estado.buscarCursos("curs2").get(0).nombre().equals("Ciencias"));
    comprobar(estado.filtrarCalificaciones("maria", 7, 2, "matem").size() == 1);
    comprobar(estado.filtrarCalificaciones("", 0, 0, "").size() == 10);
    comprobar(estado.filtrarCalificaciones("Luis", 7, 1, "").isEmpty());
    comprobar(estado.cursosMatriculados(1).size() == 3);
    comprobar(estado.cursosMatriculados(999).isEmpty());
    try {
      estado.generarReporte(999, "", 0);
      throw new AssertionError("Fuga de reporte");
    } catch (ExcepcionNegocio esperado) {
      // Identidad ajena bloqueada.
    }
  }

  public static void reportes() {
    EstadoAcademico estado = ejemplo();
    ReporteAcademico reporte = estado.generarReporte(1, "", 1);
    comprobar(reporte.filas().size() == 3);
    comprobar(reporte.filas().get(0).curso().id() == 3); // 95,60,59 descendentes del primer parcial.
    comprobar(reporte.filas().get(1).primero().compareTo(decimal("60")) == 0);
    comprobar(reporte.filas().get(1).segundo().compareTo(decimal("80")) == 0);
    comprobar(
      reporte.aprobados() == 1 &&
        reporte.reprobados() == 1 &&
        reporte.pendientes() == 1
    );
    comprobar(reporte.promedio().compareTo(decimal("70.11")) == 0); // (300+236+95)/9
    ReporteAcademico segundo = estado.generarReporte(1, "", 2);
    comprobar(
      segundo.filas().get(0).curso().id() == 1 &&
        segundo.filas().get(2).segundo() == null
    );
    comprobar(estado.generarReporte(1, "ciencias", 0).filas().size() == 1);
    var limite = new ReporteAcademico(
      alumno(1, "María", 7),
      List.of(new Curso(1, "CURS1", "Curso", 7)),
      List.of(
        new Calificacion(1, 1, 1, 1, 1, decimal("60")),
        new Calificacion(2, 1, 1, 1, 2, decimal("60")),
        new Calificacion(3, 1, 1, 1, 3, decimal("60")),
        new Calificacion(4, 1, 1, 1, 4, decimal("60"))
      ),
      0
    );
    comprobar(
      limite.aprobados() == 1 &&
        limite.reprobados() == 0 &&
        limite.pendientes() == 0
    );
    ReporteAcademico vacio = estado.generarReporte(1, "no-existe", 0);
    comprobar(
      vacio.promedio() == null &&
        vacio.aprobados() == 0 &&
        vacio.reprobados() == 0
    );
    comprobar(
      new Estudiante(0, null, "Ana", "López", "ana@colegio.edu.ni", 7)
        .getCodigo()
        .equals("AUTO")
    );
  }

  public static void impresion() {
    List<Curso> materias = new ArrayList<>();
    for (int i = 1; i <= 50; i++) materias.add(
      new Curso(i, "CURS" + i, "Curso de prueba " + i, 7)
    );
    DocumentoReporte documento = new DocumentoReporte(
      new ReporteAcademico(alumno(1, "María", 7), materias, List.of(), 0)
    );
    PageFormat carta = DocumentoReporte.formatoCarta();
    comprobar(
      carta.getWidth() == 612 &&
        carta.getHeight() == 792 &&
        carta.getImageableWidth() == 540
    );
    comprobar(documento.getPaginas() == 3);
    BufferedImage hoja = new BufferedImage(
      612,
      792,
      BufferedImage.TYPE_INT_RGB
    );
    Graphics2D grafico = hoja.createGraphics();
    try {
      grafico.setColor(Color.WHITE);
      grafico.fillRect(0, 0, 612, 792);
      for (int pagina = 0; pagina < 3; pagina++) comprobar(
        documento.print(grafico, carta, pagina) == Printable.PAGE_EXISTS
      );
      comprobar(documento.print(grafico, carta, 3) == Printable.NO_SUCH_PAGE);
      comprobar(documento.print(grafico, carta, -1) == Printable.NO_SUCH_PAGE);
      comprobar(hoja.getRGB(0, 0) == Color.WHITE.getRGB());
      comprobar(hoja.getRGB(611, 791) == Color.WHITE.getRGB());
    } finally {
      grafico.dispose();
    }
  }

  public static void main(String[] argumentos) {
    filtros();
    reportes();
    impresion();
  }
}
