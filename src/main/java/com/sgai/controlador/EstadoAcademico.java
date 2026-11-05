package com.sgai.controlador;

import com.sgai.estructuras.*;
import com.sgai.modelo.*;
import com.sgai.util.Busquedas;
import com.sgai.util.ExcepcionNegocio;
import java.util.*;

// Instantánea coherente; se publica únicamente después del commit.
public final class EstadoAcademico {

  private final ListaEnlazadaEstudiantes estudiantes =
    new ListaEnlazadaEstudiantes();
  private final ListaEnlazadaCalificaciones calificaciones =
    new ListaEnlazadaCalificaciones();
  private final ListaEnlazadaDocentes docentes = new ListaEnlazadaDocentes();
  private final List<Matricula> matriculas;
  private final List<Curso> cursos;
  private final List<Asignacion> asignaciones;
  private final Estudiante[] indice;
  private final String[] codigos;
  private final Docente[] indiceDocentes;
  private final String[] codigosDocentes;
  private final Calificacion[] reporteOrdenado;

  public EstadoAcademico(
    List<Estudiante> alumnos,
    List<Docente> profesores,
    List<Curso> materias,
    List<Asignacion> relaciones,
    List<Calificacion> notas
  ) {
    this(alumnos, profesores, materias, relaciones, notas, List.of());
  }

  public EstadoAcademico(
    List<Estudiante> alumnos,
    List<Docente> profesores,
    List<Curso> materias,
    List<Asignacion> relaciones,
    List<Calificacion> notas,
    List<Matricula> matriculas
  ) {
    this.matriculas = List.copyOf(matriculas);
    alumnos.forEach(estudiantes::agregar);
    notas.forEach(calificaciones::agregar);
    profesores.forEach(docentes::agregar);
    cursos = List.copyOf(materias);
    asignaciones = List.copyOf(relaciones);
    // Se ordena una copia para buscar por código sin alterar el orden de la lista enlazada.
    indice = estudiantes.aArreglo();
    Algoritmos.ordenar(indice, Comparator.comparing(Estudiante::getCodigo));
    codigos = new String[indice.length];
    for (int i = 0; i < indice.length; i++) codigos[i] = indice[i].getCodigo();
    indiceDocentes = docentes.aArreglo();
    Algoritmos.ordenar(
      indiceDocentes,
      Comparator.comparing(Docente::getCodigo)
    );
    codigosDocentes = new String[indiceDocentes.length];
    for (int i = 0; i < indiceDocentes.length; i++) codigosDocentes[i] =
      indiceDocentes[i].getCodigo();
    reporteOrdenado = calificaciones.aArreglo();
    Algoritmos.ordenar(
      reporteOrdenado,
      Comparator.comparing(Calificacion::nota)
        .thenComparingLong(Calificacion::estudianteId)
        .thenComparingInt(Calificacion::periodoParcial)
    );
  }

  public Estudiante[] estudiantes() {
    return estudiantes.aArreglo();
  }

  public List<Docente> docentes() {
    return List.of(docentes.aArreglo());
  }

  public List<Curso> cursos() {
    return cursos;
  }

  public List<Asignacion> asignaciones() {
    return asignaciones;
  }

  public Calificacion[] calificaciones() {
    return calificaciones.aArreglo();
  }

  // Índice construido una sola vez por instantánea: búsqueda repetida O(log n).
  public Estudiante buscarEstudiante(String codigo) {
    int posicion = Algoritmos.buscarBinario(
      codigos,
      codigo.toUpperCase(Locale.ROOT),
      Comparator.naturalOrder()
    );
    return posicion < 0 ? null : indice[posicion];
  }

  // Índice docente independiente, con la misma precondición de ordenamiento.
  public Docente buscarDocente(String codigo) {
    int posicion = Algoritmos.buscarBinario(
      codigosDocentes,
      codigo.toUpperCase(Locale.ROOT),
      Comparator.naturalOrder()
    );
    return posicion < 0 ? null : indiceDocentes[posicion];
  }

  // El ordenamiento se realiza en el trabajador al construir la instantánea.
  public Calificacion[] reporte() {
    return reporteOrdenado.clone();
  }

  public List<Matricula> matriculas() {
    return matriculas;
  }

  public List<Estudiante> buscarEstudiantes(String consulta) {
    return Arrays.stream(estudiantes())
      .filter(e ->
        Busquedas.coincide(
          consulta,
          e.getCodigo() + " " + e.getNombreCompleto()
        )
      )
      .toList();
  }

  public List<Docente> buscarDocentes(String consulta) {
    return docentes()
      .stream()
      .filter(d ->
        Busquedas.coincide(
          consulta,
          d.getCodigo() + " " + d.getNombreCompleto()
        )
      )
      .toList();
  }

  public List<Curso> buscarCursos(String consulta) {
    return cursos
      .stream()
      .filter(c -> Busquedas.coincide(consulta, c.codigo() + " " + c.nombre()))
      .toList();
  }

  public List<Calificacion> filtrarCalificaciones(
    String estudiante,
    int grado,
    int periodoParcial,
    String curso
  ) {
    Set<Long> alumnos = new HashSet<>();
    buscarEstudiantes(estudiante)
      .stream()
      .filter(e -> grado == 0 || e.getGrado() == grado)
      .forEach(e -> alumnos.add(e.getId()));
    Set<Long> materias = new HashSet<>();
    buscarCursos(curso).forEach(c -> materias.add(c.id()));
    return Arrays.stream(calificaciones())
      .filter(
        n ->
          alumnos.contains(n.estudianteId()) &&
          materias.contains(n.cursoId()) &&
          (periodoParcial == 0 || n.periodoParcial() == periodoParcial)
      )
      .toList();
  }

  public List<Curso> cursosMatriculados(long estudianteId) {
    Set<Long> ids = new HashSet<>();
    matriculas
      .stream()
      .filter(m -> m.estudianteId() == estudianteId)
      .forEach(m -> ids.add(m.cursoId()));
    return cursos
      .stream()
      .filter(c -> ids.contains(c.id()))
      .toList();
  }

  // Solo usa la instantánea autorizada; un ID ajeno nunca permite consultar otro alumno.
  public ReporteAcademico generarReporte(
    long estudianteId,
    String curso,
    int parcialOrden
  ) {
    Estudiante alumno = Arrays.stream(estudiantes())
      .filter(e -> e.getId() == estudianteId)
      .findFirst()
      .orElseThrow(() ->
        new ExcepcionNegocio("Estudiante fuera de su alcance autorizado.")
      );
    return new ReporteAcademico(
      alumno,
      cursosMatriculados(estudianteId)
        .stream()
        .filter(c -> Busquedas.coincide(curso, c.codigo() + " " + c.nombre()))
        .toList(),
      Arrays.asList(calificaciones()),
      parcialOrden
    );
  }
}
