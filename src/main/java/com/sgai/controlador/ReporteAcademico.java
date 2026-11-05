package com.sgai.controlador;

import com.sgai.estructuras.Algoritmos;
import com.sgai.modelo.*;
import com.sgai.util.ExcepcionNegocio;
import java.math.*;
import java.util.*;

// Reporte inmutable: cuatro columnas de parciales, ausencias sin convertirlas en cero.
public final class ReporteAcademico {

  public record Fila(
    Curso curso,
    BigDecimal primero,
    BigDecimal segundo,
    BigDecimal tercero,
    BigDecimal cuarto
  ) {
    public BigDecimal nota(int periodoParcial) {
      return switch (periodoParcial) {
        case 1 -> primero;
        case 2 -> segundo;
        case 3 -> tercero;
        case 4 -> cuarto;
        default -> throw new IllegalArgumentException(
          "Período parcial inválido"
        );
      };
    }

    public boolean completo() {
      return (
        primero != null && segundo != null && tercero != null && cuarto != null
      );
    }

    public BigDecimal promedioFinal() {
      return completo()
        ? primero
            .add(segundo)
            .add(tercero)
            .add(cuarto)
            .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP)
        : null;
    }
  }

  private final Estudiante estudiante;
  private final int parcialOrden;
  private final List<Fila> filas;
  private final BigDecimal promedio;
  private final long aprobados, reprobados, pendientes;

  public ReporteAcademico(
    Estudiante estudiante,
    Collection<Curso> cursos,
    Collection<Calificacion> notas,
    int parcialOrden
  ) {
    this.estudiante = Objects.requireNonNull(estudiante);
    this.parcialOrden = parcialOrden;
    if (parcialOrden < 0 || parcialOrden > 4) throw new ExcepcionNegocio(
      "Período parcial inválido."
    );
    List<Fila> resultado = new ArrayList<>();
    BigDecimal suma = BigDecimal.ZERO;
    int cantidad = 0;
    for (Curso curso : cursos) {
      // Una posición nula significa que aún no hay nota; no debe contarse como cero.
      BigDecimal[] valores = new BigDecimal[4];
      for (Calificacion nota : notas)
        if (
          nota.estudianteId() == estudiante.getId() &&
          nota.cursoId() == curso.id()
        ) valores[nota.periodoParcial() - 1] = nota.nota();
      for (BigDecimal valor : valores)
        if (valor != null) {
          suma = suma.add(valor);
          cantidad++;
        }
      resultado.add(
        new Fila(curso, valores[0], valores[1], valores[2], valores[3])
      );
    }
    Fila[] ordenadas = resultado.toArray(Fila[]::new);
    Comparator<BigDecimal> notasDescendentes = Comparator.nullsLast(
      Comparator.reverseOrder()
    );
    Algoritmos.ordenar(ordenadas, (a, b) -> {
      if (parcialOrden > 0) {
        int diferencia = notasDescendentes.compare(
          a.nota(parcialOrden),
          b.nota(parcialOrden)
        );
        if (diferencia != 0) return diferencia;
      } else for (int parcial = 1; parcial <= 4; parcial++) {
        int diferencia = notasDescendentes.compare(
          a.nota(parcial),
          b.nota(parcial)
        );
        if (diferencia != 0) return diferencia;
      }
      return a.curso().codigo().compareTo(b.curso().codigo());
    });
    filas = List.of(ordenadas);
    promedio =
      cantidad == 0
        ? null
        : suma.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    aprobados = filas
      .stream()
      .filter(Fila::completo)
      .filter(f -> f.promedioFinal().compareTo(BigDecimal.valueOf(60)) >= 0)
      .count();
    reprobados = filas
      .stream()
      .filter(Fila::completo)
      .filter(f -> f.promedioFinal().compareTo(BigDecimal.valueOf(60)) < 0)
      .count();
    pendientes = filas.size() - aprobados - reprobados;
  }

  public String orden() {
    return parcialOrden == 0
      ? "Períodos parciales 1 a 4, de mayor a menor nota"
      : "Período parcial " + parcialOrden + ", de mayor a menor nota";
  }

  public Estudiante estudiante() {
    return estudiante;
  }

  public List<Fila> filas() {
    return filas;
  }

  public BigDecimal promedio() {
    return promedio;
  }

  public long aprobados() {
    return aprobados;
  }

  public long reprobados() {
    return reprobados;
  }

  public long pendientes() {
    return pendientes;
  }

  public String resumen() {
    return (
      "Promedio de notas registradas: " +
      (promedio == null ? "Sin notas" : promedio.toPlainString()) +
      " | Cursos aprobados: " +
      aprobados +
      " | Reprobados: " +
      reprobados +
      " | Pendientes: " +
      pendientes
    );
  }
}
