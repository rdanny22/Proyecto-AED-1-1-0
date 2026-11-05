package com.sgai;

import com.sgai.controlador.*;
import com.sgai.dao.AcademicoDAO;
import com.sgai.estructuras.*;
import com.sgai.modelo.*;
import com.sgai.seguridad.Claves;
import com.sgai.util.*;
import com.sgai.vista.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

// Núcleo de pruebas sin dependencias: también se puede ejecutar sin Maven.
public final class Verificaciones {

  private Verificaciones() {}

  private static void comprobar(boolean condicion) {
    if (!condicion) throw new AssertionError(
      "No se cumple la condición de prueba"
    );
  }

  private static void rechazar(Runnable tarea) {
    try {
      tarea.run();
    } catch (ExcepcionNegocio | IllegalArgumentException esperado) {
      return;
    }
    throw new AssertionError("Se aceptó un dato inválido");
  }

  private static Estudiante alumno(long id, String codigo) {
    return new Estudiante(
      id,
      codigo,
      "María José",
      "Pérez O’Connor",
      "maria@colegio.edu.ni",
      7
    );
  }

  public static void dominio() {
    Estudiante estudiante = alumno(1, "es-01");
    comprobar(estudiante.getCodigo().equals("ES-01"));
    Persona persona = estudiante;
    comprobar(persona.getDescripcion().equals("Grado: 7"));
    comprobar(
      new Docente(0, "DO01", "Ana", "Díaz", "ana@colegio.edu.ni", "Matemáticas")
        .getDescripcion()
        .contains("Matemáticas")
    );
    rechazar(() -> alumno(-1, "AB"));
    rechazar(() -> alumno(1, "'; DROP TABLE estudiantes;--"));
    rechazar(() -> new Curso(1, "CU01", "Curso", 12));
    rechazar(() -> Validacion.texto("<script>alert(1)</script>", "Nombre", 80));
    rechazar(() -> Validacion.texto(" ", "Nombre", 80));
    rechazar(() -> Validacion.correo("correo@"));
    rechazar(() -> Validacion.correo("a@b.c\n"));
    rechazar(() -> Validacion.nota(new BigDecimal("100.01")));
    rechazar(() -> Validacion.nota(new BigDecimal("-0.01")));
    rechazar(() -> Validacion.nota(new BigDecimal("1.234")));
    comprobar(Validacion.nota(new BigDecimal("0")).signum() == 0);
    comprobar(Validacion.nota(new BigDecimal("100.00")).intValue() == 100);
    rechazar(() -> new Calificacion(0, 1, 1, 1, 5, BigDecimal.TEN));
    rechazar(() -> new Usuario(1, "test", Rol.ESTUDIANTE, null, null));
    rechazar(() -> new Usuario(1, "test", Rol.ADMINISTRADOR, 1L, null));
  }

  public static void listas() {
    ListaEnlazadaEstudiantes lista = new ListaEnlazadaEstudiantes();
    comprobar(
      lista.getCantidad() == 0 && lista.buscar(1) == null && !lista.eliminar(1)
    );
    for (int i = 1; i <= 4; i++) lista.agregar(alumno(i, "ES0" + i));
    comprobar(lista.getCantidad() == 4 && lista.buscar(3).getId() == 3);
    comprobar(
      lista.eliminar(1) &&
        lista.eliminar(4) &&
        lista.eliminar(2) &&
        lista.eliminar(3)
    );
    lista.agregar(alumno(5, "ES05"));
    comprobar(lista.aArreglo()[0].getId() == 5);
    Estudiante[] copia = lista.aArreglo();
    copia[0] = null;
    comprobar(lista.buscar(5) != null);
    ListaEnlazadaCalificaciones notas = new ListaEnlazadaCalificaciones();
    notas.agregar(new Calificacion(1, 1, 1, 1, 1, BigDecimal.TEN));
    comprobar(
      notas.buscar(1) != null && notas.eliminar(1) && notas.getCantidad() == 0
    );
    notas.agregar(new Calificacion(2, 1, 1, 1, 1, BigDecimal.TEN));
    comprobar(notas.aArreglo().length == 1);
    ListaEnlazadaDocentes docentes = new ListaEnlazadaDocentes();
    docentes.agregar(
      new Docente(1, "DO01", "Ana", "Díaz", "ana@colegio.edu.ni", "Historia")
    );
    comprobar(
      docentes.buscar(1) != null &&
        docentes.eliminar(1) &&
        docentes.getCantidad() == 0
    );
  }

  public static void algoritmos() {
    Random aleatorio = new Random(2026);
    for (int cantidad = 0; cantidad < 300; cantidad++) {
      Integer[] datos = new Integer[cantidad];
      for (int i = 0; i < cantidad; i++) datos[i] = aleatorio.nextInt(50);
      Integer[] esperado = datos.clone();
      Arrays.sort(esperado);
      Algoritmos.ordenar(datos, Comparator.naturalOrder());
      comprobar(Arrays.equals(datos, esperado));
      for (int clave = -1; clave <= 50; clave++) {
        int indice = Algoritmos.buscarBinario(
          datos,
          clave,
          Comparator.naturalOrder()
        );
        comprobar(indice >= 0 == Arrays.asList(esperado).contains(clave));
        if (indice >= 0) comprobar(datos[indice].equals(clave));
      }
    }
    Integer[] iguales = new Integer[100_000];
    Arrays.fill(iguales, 1);
    Algoritmos.ordenar(iguales, Comparator.naturalOrder());
    comprobar(iguales[99_999] == 1);
  }

  public static void instantaneas() {
    Calificacion primera = new Calificacion(
      1,
      1,
      1,
      1,
      1,
      new BigDecimal("80")
    );
    Calificacion segunda = new Calificacion(
      2,
      2,
      1,
      1,
      1,
      new BigDecimal("59.99")
    );
    EstadoAcademico estado = new EstadoAcademico(
      java.util.List.of(alumno(1, "ZZ"), alumno(2, "AA")),
      java.util.List.of(),
      java.util.List.of(),
      java.util.List.of(),
      java.util.List.of(primera, segunda)
    );
    comprobar(
      estado.buscarEstudiante("aa").getId() == 2 &&
        estado.buscarEstudiante("NO") == null
    );
    comprobar(
      estado.reporte()[0].id() == 2 && estado.calificaciones()[0].id() == 1
    );
    EstadoAcademico vacio = new EstadoAcademico(
      java.util.List.of(),
      java.util.List.of(),
      java.util.List.of(),
      java.util.List.of(),
      java.util.List.of()
    );
    comprobar(
      vacio.buscarEstudiante("AA") == null && vacio.reporte().length == 0
    );
  }

  public static void seguridad() {
    char[] clave = "Prueba-Segura-2026".toCharArray();
    String hash = Claves.crear(clave),
      otro = Claves.crear(clave);
    comprobar(!hash.equals(otro) && Claves.verificar(clave, hash));
    comprobar(!Claves.verificar("incorrecta".toCharArray(), hash));
    comprobar(!Claves.verificar(clave, "corrupto"));
    comprobar(!Claves.verificar(clave, "99999999:AA==:AA=="));
    rechazar(() -> Claves.crear("corta".toCharArray()));
    Arrays.fill(clave, '\0');
  }

  public static void permisosSinSesion() {
    try (ControladorAcademico controlador = new ControladorAcademico()) {
      try {
        controlador.eliminar(AcademicoDAO.Tabla.ESTUDIANTES, 1).join();
        throw new AssertionError("Falta autorización");
      } catch (CompletionException esperado) {
        comprobar(esperado.getCause() instanceof ExcepcionNegocio);
      }
      for (CompletableFuture<?> tarea : java.util.List.of(
        controlador.configurar("intruso", "Clave-Intruso-2026".toCharArray()),
        controlador.autorizarInstalacion("incorrecta".toCharArray()),
        controlador.crearUsuario(
          "intruso_persona",
          "Clave-Prueba-2026".toCharArray(),
          alumno(1, "ESTU1")
        ),
        controlador.eliminarMatricula(1),
        controlador.crearAdministrador(
          "intruso_admin",
          "Clave-Prueba-2026".toCharArray()
        )
      )) {
        try {
          tarea.join();
          throw new AssertionError(
            "Operación privilegiada abierta sin autorización"
          );
        } catch (CompletionException esperado) {
          comprobar(esperado.getCause() instanceof ExcepcionNegocio);
        }
      }
      comprobar(
        controlador.getEstado() == null && controlador.getSesion() == null
      );
    }
  }

  public static void componentesSwing() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      JTable tabla = new JTable(1, 1);
      RenderizadorNotas renderizador = new RenderizadorNotas();
      Component celda = renderizador.getTableCellRendererComponent(
        tabla,
        new BigDecimal("59.99"),
        true,
        false,
        0,
        0
      );
      comprobar(celda.getForeground().equals(new Color(170, 15, 30)));
      celda = renderizador.getTableCellRendererComponent(
        tabla,
        new BigDecimal("60"),
        false,
        false,
        0,
        0
      );
      comprobar(celda.getForeground().equals(tabla.getForeground()));
      JTextField campo = Formulario.texto("", 4);
      campo.setText("abc");
      campo.setText("12345");
      comprobar(campo.getText().equals("abc"));
      JSpinner grado = Formulario.numero(7, 1, 11);
      comprobar(Formulario.entero(grado) == 7);
      TablaDatos<String> datos = new TablaDatos<>(
        new String[] { "Nombre" },
        s -> new Object[] { s }
      );
      datos.mostrar(java.util.List.of("Ana"));
      datos.setRowSelectionInterval(0, 0);
      comprobar(datos.seleccionado().equals("Ana"));
      comprobar(!datos.isCellEditable(0, 0));
    });
  }

  public static void main(String[] argumentos) throws Exception {
    dominio();
    listas();
    algoritmos();
    instantaneas();
    seguridad();
    permisosSinSesion();
    componentesSwing();
  }
}
