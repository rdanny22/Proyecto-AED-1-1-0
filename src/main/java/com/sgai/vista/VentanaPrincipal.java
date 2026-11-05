package com.sgai.vista;

import com.sgai.controlador.*;
import com.sgai.dao.AcademicoDAO.Tabla;
import com.sgai.modelo.*;
import com.sgai.util.ExcepcionNegocio;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;

// Dashboard MVC: formularios tipados, navegación por rol y tablas de solo lectura.
public final class VentanaPrincipal extends VentanaBase {

  private static final long serialVersionUID = 1L;
  private final boolean administrador;
  private final JTabbedPane pestanas = new JTabbedPane();
  private final JLabel resumen = new JLabel();
  private final JLabel resumenReporte = new JLabel();
  private final JTextField filtroEstudiantes = Formulario.texto("", 100),
    filtroDocentes = Formulario.texto("", 100),
    filtroCursos = Formulario.texto("", 100),
    filtroMatricula = Formulario.texto("", 100);
  private FiltrosNotas filtrosNotas, filtrosReportes;
  private final TablaDatos<Estudiante> alumnosReporte = tablaEstudiantes();
  private final TablaDatos<Estudiante> matriculas = new TablaDatos<>(
    new String[] {
      "Código",
      "Nombres",
      "Apellidos",
      "Cursos matriculados",
      "Grado",
    },
    e -> new Object[] {
      e.getCodigo(),
      e.getNombres(),
      e.getApellidos(),
      controlador
        .getEstado()
        .cursosMatriculados(e.getId())
        .stream()
        .map(Curso::toString)
        .collect(java.util.stream.Collectors.joining(", ")),
      e.getGrado(),
    }
  );

  private TablaDatos<Estudiante> tablaEstudiantes() {
    return new TablaDatos<>(
      new String[] { "Código", "Nombres", "Apellidos", "Grado" },
      e -> new Object[] {
        e.getCodigo(),
        e.getNombres(),
        e.getApellidos(),
        e.getGrado(),
      }
    );
  }

  private final TablaDatos<Estudiante> estudiantes = new TablaDatos<>(
    new String[] { "Código", "Nombres", "Apellidos", "Correo", "Grado" },
    e -> new Object[] {
      e.getCodigo(),
      e.getNombres(),
      e.getApellidos(),
      e.getCorreo(),
      e.getGrado(),
    }
  );
  private final TablaDatos<Docente> docentes = new TablaDatos<>(
    new String[] { "Código", "Nombres", "Apellidos", "Correo", "Especialidad" },
    d -> new Object[] {
      d.getCodigo(),
      d.getNombres(),
      d.getApellidos(),
      d.getCorreo(),
      d.getEspecialidad(),
    }
  );
  private final TablaDatos<Curso> cursos = new TablaDatos<>(
    new String[] { "Código", "Curso", "Grado" },
    c -> new Object[] { c.codigo(), c.nombre(), c.grado() }
  );
  private final TablaDatos<Asignacion> asignaciones = new TablaDatos<>(
    new String[] { "Docente", "Especialidad", "Curso" },
    a -> new Object[] {
      nombreDocente(a.docenteId()),
      especialidadDocente(a.docenteId()),
      nombreCurso(a.cursoId()),
    }
  );
  private final TablaDatos<Calificacion> notas = new TablaDatos<>(
    new String[] {
      "Estudiante",
      "Curso",
      "Docente",
      "Período parcial",
      "Nota",
    },
    this::valoresNota
  );
  private final TablaDatos<Calificacion> reporte = new TablaDatos<>(
    new String[] {
      "Estudiante",
      "Curso",
      "Docente",
      "Período parcial",
      "Nota",
    },
    this::valoresNota
  );

  public VentanaPrincipal(ControladorAcademico controlador) {
    super(controlador, "SGAI · Gestión académica");
    administrador = controlador.getSesion().rol() == Rol.ADMINISTRADOR;
    JPanel contenido = Estilo.panel();
    JPanel encabezado = new JPanel(new BorderLayout());
    JPanel marca = new JPanel(new GridLayout(0, 1, 0, 5));
    marca.add(Estilo.titulo("SGAI | Antonio Soldano"));
    Usuario usuario = controlador.getSesion();
    marca.add(new JLabel(usuario.nombre() + " · " + usuario.rol()));
    encabezado.add(RecursosGraficos.cabecera(marca, 64, 64), BorderLayout.WEST);
    RecursosGraficos.aplicarIcono(this);
    JPanel acciones = new JPanel();
    acciones.add(
      Estilo.colorear(
        Estilo.boton("Actualizar", () ->
          accion(() -> guardar(controlador.actualizar()))
        ),
        new Color(173, 216, 245)
      )
    );
    acciones.add(
      Estilo.colorear(
        Estilo.boton("Cerrar sesión", () -> accion(this::salir)),
        new Color(255, 184, 184)
      )
    );
    encabezado.add(acciones, BorderLayout.EAST);
    contenido.add(encabezado, BorderLayout.NORTH);
    JPanel inicio = Estilo.panel();
    inicio.add(Estilo.titulo("Resumen académico"), BorderLayout.NORTH);
    inicio.add(resumen, BorderLayout.CENTER);
    inicio.add(
      new JLabel(
        "Las estadísticas reflejan únicamente los datos autorizados para su rol."
      ),
      BorderLayout.SOUTH
    );
    pestanas.addTab("Inicio", inicio);
    pestanas.addTab("Estudiantes", panelEstudiantes());
    if (administrador) pestanas.addTab("Matrícula", panelMatricula());
    if (administrador) pestanas.addTab("Docentes", panelDocentes());
    pestanas.addTab("Cursos", panelCursos());
    if (administrador) pestanas.addTab("Asignaciones", panelAsignaciones());
    pestanas.addTab("Calificaciones", panelNotas());
    pestanas.addTab("Reportes", panelReportes());
    if (administrador) pestanas.addTab("Cuentas", panelCuentas());
    Color[] colores = {
      new Color(192, 216, 244),
      new Color(190, 235, 208),
      new Color(255, 221, 167),
      new Color(221, 205, 247),
      new Color(171, 226, 235),
      new Color(247, 202, 221),
      new Color(247, 236, 164),
      new Color(201, 216, 249),
      new Color(220, 225, 230),
    };
    for (int i = 0; i < pestanas.getTabCount(); i++) {
      JLabel etiqueta = new JLabel(" " + pestanas.getTitleAt(i) + " ");
      etiqueta.setOpaque(true);
      etiqueta.setBackground(colores[i % colores.length]);
      etiqueta.setForeground(Estilo.AZUL);
      etiqueta.setBorder(
        javax.swing.BorderFactory.createEmptyBorder(9, 5, 9, 5)
      );
      pestanas.setBackgroundAt(i, colores[i % colores.length]);
      pestanas.setTabComponentAt(i, etiqueta);
    }
    contenido.add(pestanas, BorderLayout.CENTER);
    contenido.add(estado, BorderLayout.SOUTH);
    setContentPane(contenido);
    setSize(1200, 760);
    setMinimumSize(new Dimension(900, 600));
    setLocationRelativeTo(null);
    refrescar();
  }

  private JButton boton(String nombre, Runnable tarea) {
    return Estilo.boton(nombre, () -> accion(tarea));
  }

  private JPanel panelTabla(JTable tabla, JPanel acciones) {
    JPanel panel = Estilo.panel();
    panel.add(acciones, BorderLayout.NORTH);
    panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
    return panel;
  }

  private void guardar(CompletableFuture<EstadoAcademico> futuro) {
    ejecutar(futuro, nuevo -> refrescar());
  }

  private void refrescar() {
    EstadoAcademico datos = controlador.getEstado();
    estudiantes.mostrar(datos.buscarEstudiantes(filtroEstudiantes.getText()));
    docentes.mostrar(datos.buscarDocentes(filtroDocentes.getText()));
    cursos.mostrar(datos.buscarCursos(filtroCursos.getText()));
    matriculas.mostrar(datos.buscarEstudiantes(filtroMatricula.getText()));
    asignaciones.mostrar(datos.asignaciones());
    aplicarFiltroNotas();
    Calificacion[] ordenadas = datos.reporte();
    aplicarFiltroReportes();
    long reprobadas = Arrays.stream(ordenadas)
      .filter(n -> n.nota().compareTo(new BigDecimal("60")) < 0)
      .count();
    BigDecimal suma = Arrays.stream(ordenadas)
      .map(Calificacion::nota)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    String promedio =
      ordenadas.length == 0
        ? "Sin notas"
        : suma
            .divide(
              BigDecimal.valueOf(ordenadas.length),
              2,
              java.math.RoundingMode.HALF_UP
            )
            .toPlainString();
    resumen.setText(
      "<html><h1>" +
        datos.estudiantes().length +
        " estudiantes</h1><h2>" +
        datos.docentes().size() +
        " docentes · " +
        datos.cursos().size() +
        " cursos</h2><h2>" +
        ordenadas.length +
        " calificaciones</h2><p>Promedio: " +
        promedio +
        " · Notas menores a 60: " +
        reprobadas +
        "</p></html>"
    );
    resumenReporte.setText(
      "Seleccione un estudiante y pulse Generar reportes. El parcial seleccionado determina el orden descendente."
    );
  }

  // Buscadores comunes: código, nombres y apellidos parciales.
  private JPanel buscador(String etiqueta, JTextField campo, Runnable buscar) {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    campo.setColumns(22);
    panel.add(new JLabel(etiqueta));
    panel.add(campo);
    panel.add(boton("Buscar", buscar));
    panel.add(
      boton("Ver todos", () -> {
        campo.setText("");
        buscar.run();
      })
    );
    campo.addActionListener(evento -> accion(buscar));
    return panel;
  }

  private JPanel barras(JPanel acciones, JPanel buscar) {
    JPanel panel = new JPanel(new GridLayout(0, 1));
    panel.add(acciones);
    panel.add(buscar);
    return panel;
  }

  private JPanel panelEstudiantes() {
    JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
    if (administrador) {
      acciones.add(boton("Nuevo", () -> editarPersona(null, true)));
      acciones.add(
        boton("Editar", () -> editarPersona(estudiantes.seleccionado(), true))
      );
      acciones.add(
        boton("Eliminar", () ->
          eliminar(Tabla.ESTUDIANTES, estudiantes.seleccionado().getId())
        )
      );
    }
    return panelTabla(
      estudiantes,
      barras(
        acciones,
        buscador("Código, nombre o apellidos:", filtroEstudiantes, () ->
          estudiantes.mostrar(
            controlador
              .getEstado()
              .buscarEstudiantes(filtroEstudiantes.getText())
          )
        )
      )
    );
  }

  private JPanel panelDocentes() {
    JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
    acciones.add(boton("Nuevo", () -> editarPersona(null, false)));
    acciones.add(
      boton("Editar", () -> editarPersona(docentes.seleccionado(), false))
    );
    acciones.add(
      boton("Eliminar", () ->
        eliminar(Tabla.DOCENTES, docentes.seleccionado().getId())
      )
    );
    return panelTabla(
      docentes,
      barras(
        acciones,
        buscador("Código, nombre o apellidos:", filtroDocentes, () ->
          docentes.mostrar(
            controlador.getEstado().buscarDocentes(filtroDocentes.getText())
          )
        )
      )
    );
  }

  private JPanel panelMatricula() {
    JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
    acciones.add(boton("Matricular", this::matricular));
    acciones.add(
      Estilo.colorear(
        boton("Eliminar matrícula", () -> {
          Estudiante alumno = matriculas.seleccionado();
          if (
            controlador.getEstado().cursosMatriculados(alumno.getId()).isEmpty()
          ) {
            throw new ExcepcionNegocio(
              "El estudiante no tiene cursos matriculados."
            );
          }
          if (
            confirmar(
              "¿Eliminar todos los cursos matriculados de " +
                alumno +
                "? Si tiene notas registradas, no se eliminará ninguna matrícula."
            )
          ) {
            guardar(controlador.eliminarMatricula(alumno.getId()));
          }
        }),
        new Color(255, 184, 184)
      )
    );
    acciones.add(
      boton("Generar hoja de matrícula", () -> {
        Estudiante alumno = matriculas.seleccionado();
        DocumentoMatricula documento = new DocumentoMatricula(
          alumno,
          controlador.getEstado().cursosMatriculados(alumno.getId())
        );
        new VentanaReporte(
          this,
          "Hoja de matrícula · " + alumno.getCodigo(),
          "SGAI - Matrícula - " + alumno.getCodigo(),
          documento
        ).setVisible(true);
      })
    );
    return panelTabla(
      matriculas,
      barras(
        acciones,
        buscador("Código, nombre o apellidos:", filtroMatricula, () ->
          matriculas.mostrar(
            controlador.getEstado().buscarEstudiantes(filtroMatricula.getText())
          )
        )
      )
    );
  }

  private void matricular() {
    Estudiante alumno = matriculas.seleccionado();
    EstadoAcademico datos = controlador.getEstado();
    Set<Long> inscritos = new HashSet<>();
    datos
      .cursosMatriculados(alumno.getId())
      .forEach(c -> inscritos.add(c.id()));
    // Solo ofrecemos cursos del grado del alumno que todavía no estén matriculados.
    Curso[] disponibles = datos
      .cursos()
      .stream()
      .filter(
        c -> c.grado() == alumno.getGrado() && !inscritos.contains(c.id())
      )
      .toArray(Curso[]::new);
    if (disponibles.length == 0) throw new ExcepcionNegocio(
      "No hay nuevos cursos disponibles para el grado del estudiante."
    );
    SeleccionCursos seleccion = new SeleccionCursos(
      java.util.List.of(disponibles)
    );
    Formulario formulario = new Formulario();
    formulario.campo("Estudiante", new JLabel(alumno.toString()));
    formulario.campo("Grado", new JLabel(String.valueOf(alumno.getGrado())));
    formulario.campo("Cursos (marque uno o varios)", seleccion);
    formulario.campo(
      "Importante",
      new JLabel("Se añaden cursos; las matrículas existentes se conservan.")
    );
    // Las casillas se leen juntas al confirmar; marcar un curso no cierra la ventana.
    if (formulario.aceptar(this, "Matricular estudiante")) guardar(
      controlador.matricular(alumno.getId(), seleccion.seleccionados())
    );
  }

  private void editarPersona(Persona persona, boolean alumno) {
    Formulario formulario = new Formulario();
    formulario.campo(
      "Código",
      new JLabel(
        persona == null ? "Automático al guardar" : persona.getCodigo()
      )
    );
    JTextField nombres = formulario.campo(
      "Nombres",
      Formulario.texto(persona == null ? "" : persona.getNombres(), 80)
    );
    JTextField apellidos = formulario.campo(
      "Apellidos",
      Formulario.texto(persona == null ? "" : persona.getApellidos(), 80)
    );
    JTextField correo = formulario.campo(
      "Correo",
      Formulario.texto(persona == null ? "" : persona.getCorreo(), 150)
    );
    JSpinner grado = Formulario.numero(
      persona instanceof Estudiante e ? e.getGrado() : 1,
      1,
      11
    );
    JTextField especialidad = Formulario.texto(
      persona instanceof Docente d ? d.getEspecialidad() : "",
      80
    );
    if (alumno) formulario.campo("Grado (1–11)", grado);
    else formulario.campo("Especialidad", especialidad);
    if (!formulario.aceptar(this, alumno ? "Estudiante" : "Docente")) return;
    long id = persona == null ? 0 : persona.getId();
    Persona nueva = alumno
      ? new Estudiante(
          id,
          persona == null ? null : persona.getCodigo(),
          nombres.getText(),
          apellidos.getText(),
          correo.getText(),
          Formulario.entero(grado)
        )
      : new Docente(
          id,
          persona == null ? null : persona.getCodigo(),
          nombres.getText(),
          apellidos.getText(),
          correo.getText(),
          especialidad.getText()
        );
    guardar(controlador.guardarPersona(nueva));
  }

  private JPanel panelCursos() {
    JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
    if (administrador) {
      acciones.add(boton("Nuevo curso", () -> editarCurso(null)));
      acciones.add(boton("Editar", () -> editarCurso(cursos.seleccionado())));
      acciones.add(
        boton("Eliminar", () ->
          eliminar(Tabla.CURSOS, cursos.seleccionado().id())
        )
      );
    } else acciones.add(new JLabel("Cursos disponibles para su perfil"));
    return panelTabla(
      cursos,
      barras(
        acciones,
        buscador("Código o nombre del curso:", filtroCursos, () ->
          cursos.mostrar(
            controlador.getEstado().buscarCursos(filtroCursos.getText())
          )
        )
      )
    );
  }

  private void editarCurso(Curso curso) {
    Formulario formulario = new Formulario();
    formulario.campo(
      "Código",
      new JLabel(curso == null ? "Automático al guardar" : curso.codigo())
    );
    JTextField nombre = formulario.campo(
      "Nombre",
      Formulario.texto(curso == null ? "" : curso.nombre(), 80)
    );
    JSpinner grado = formulario.campo(
      "Grado",
      Formulario.numero(curso == null ? 1 : curso.grado(), 1, 11)
    );
    if (formulario.aceptar(this, "Curso")) guardar(
      controlador.guardarCurso(
        new Curso(
          curso == null ? 0 : curso.id(),
          curso == null ? null : curso.codigo(),
          nombre.getText(),
          Formulario.entero(grado)
        )
      )
    );
  }

  private JPanel panelAsignaciones() {
    JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
    acciones.add(boton("Asignar docente a curso", this::asignar));
    acciones.add(
      boton("Retirar asignación", () -> {
        Asignacion seleccion = asignaciones.seleccionado();
        if (
          confirmar(
            "¿Retirar asignación? No es posible si tiene calificaciones."
          )
        ) guardar(controlador.asignar(seleccion, true));
      })
    );
    return panelTabla(asignaciones, acciones);
  }

  private void asignar() {
    EstadoAcademico datos = controlador.getEstado();
    if (
      datos.docentes().isEmpty() || datos.cursos().isEmpty()
    ) throw new ExcepcionNegocio("Registre docentes y cursos primero.");
    Formulario formulario = new Formulario();
    JComboBox<Docente> docente = formulario.campo(
      "Docente",
      new JComboBox<>(
        datos
          .docentes()
          .stream()
          .filter(
            d ->
              administrador || d.getId() == controlador.getSesion().docenteId()
          )
          .toArray(Docente[]::new)
      )
    );
    JComboBox<Curso> curso = formulario.campo(
      "Curso",
      new JComboBox<>(datos.cursos().toArray(Curso[]::new))
    );
    if (formulario.aceptar(this, "Asignación docente–curso")) guardar(
      controlador.asignar(
        new Asignacion(
          ((Docente) docente.getSelectedItem()).getId(),
          ((Curso) curso.getSelectedItem()).id()
        ),
        false
      )
    );
  }

  private JPanel panelNotas() {
    JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
    if (controlador.getSesion().rol() != Rol.ESTUDIANTE) {
      acciones.add(boton("Registrar nota", () -> editarNota(null)));
      acciones.add(boton("Editar", () -> editarNota(notas.seleccionado())));
      acciones.add(
        boton("Eliminar", () ->
          eliminar(Tabla.CALIFICACIONES, notas.seleccionado().id())
        )
      );
    }
    acciones.add(
      new JLabel("Rojo = nota menor a 60 | Períodos parciales 1 a 4")
    );
    filtrosNotas = new FiltrosNotas(this::aplicarFiltroNotas);
    return panelTabla(notas, barras(acciones, filtrosNotas));
  }

  private <T> void seleccionar(
    JComboBox<T> campo,
    java.util.function.Predicate<T> condicion
  ) {
    for (int i = 0; i < campo.getItemCount(); i++) if (
      condicion.test(campo.getItemAt(i))
    ) {
      campo.setSelectedIndex(i);
      return;
    }
  }

  private void editarNota(Calificacion nota) {
    EstadoAcademico datos = controlador.getEstado();
    if (
      datos.estudiantes().length == 0 ||
      datos.cursos().isEmpty() ||
      datos.docentes().isEmpty()
    ) throw new ExcepcionNegocio(
      "Se requieren estudiantes, docentes y cursos asignados."
    );
    Formulario formulario = new Formulario();
    JComboBox<Estudiante> alumno = formulario.campo(
      "Estudiante",
      new JComboBox<>(datos.estudiantes())
    );
    JComboBox<Curso> curso = formulario.campo(
      "Curso",
      new JComboBox<>(datos.cursos().toArray(Curso[]::new))
    );
    JComboBox<Docente> docente = formulario.campo(
      "Docente",
      new JComboBox<>(
        datos
          .docentes()
          .stream()
          .filter(
            d ->
              administrador || d.getId() == controlador.getSesion().docenteId()
          )
          .toArray(Docente[]::new)
      )
    );
    Runnable ajustarCursos = () -> {
      Estudiante elegido = (Estudiante) alumno.getSelectedItem();
      curso.setModel(
        new DefaultComboBoxModel<>(
          elegido == null
            ? new Curso[0]
            : datos.cursosMatriculados(elegido.getId()).toArray(Curso[]::new)
        )
      );
      ajustarDocentes(curso, docente, datos);
    };
    alumno.addActionListener(evento -> ajustarCursos.run());
    curso.addActionListener(evento -> ajustarDocentes(curso, docente, datos));
    ajustarCursos.run();
    JSpinner periodoParcial = formulario.campo(
      "Período parcial",
      Formulario.numero(nota == null ? 1 : nota.periodoParcial(), 1, 4)
    );
    JTextField valor = formulario.campo(
      "Nota (0–100; decimal con punto)",
      Formulario.texto(nota == null ? "" : nota.nota().toPlainString(), 6)
    );
    if (nota != null) {
      seleccionar(alumno, e -> e.getId() == nota.estudianteId());
      seleccionar(curso, c -> c.id() == nota.cursoId());
      seleccionar(docente, d -> d.getId() == nota.docenteId());
    }
    if (!formulario.aceptar(this, "Calificación")) return;
    if (
      alumno.getSelectedItem() == null ||
      curso.getSelectedItem() == null ||
      docente.getSelectedItem() == null
    ) throw new ExcepcionNegocio(
      "Seleccione un estudiante matriculado, un curso y un docente asignado."
    );
    if (
      !valor.getText().matches("(?:[0-9]{1,3})(?:\\.[0-9]{1,2})?")
    ) throw new ExcepcionNegocio("Nota inválida. Ejemplo: 78.50");
    guardar(
      controlador.guardarNota(
        new Calificacion(
          nota == null ? 0 : nota.id(),
          ((Estudiante) alumno.getSelectedItem()).getId(),
          ((Curso) curso.getSelectedItem()).id(),
          ((Docente) docente.getSelectedItem()).getId(),
          Formulario.entero(periodoParcial),
          new BigDecimal(valor.getText())
        )
      )
    );
  }

  private void ajustarDocentes(
    JComboBox<Curso> curso,
    JComboBox<Docente> docente,
    EstadoAcademico datos
  ) {
    Curso elegido = (Curso) curso.getSelectedItem();
    Set<Long> ids = new HashSet<>();
    if (elegido != null) datos
      .asignaciones()
      .stream()
      .filter(a -> a.cursoId() == elegido.id())
      .forEach(a -> ids.add(a.docenteId()));
    docente.setModel(
      new DefaultComboBoxModel<>(
        datos
          .docentes()
          .stream()
          .filter(
            d ->
              ids.contains(d.getId()) &&
              (administrador ||
                d.getId() == controlador.getSesion().docenteId())
          )
          .toArray(Docente[]::new)
      )
    );
  }

  // Los filtros solo operan sobre la instantánea de la sesión autorizada.
  private final class FiltrosNotas extends JPanel {

    private static final long serialVersionUID = 1L;
    final JTextField estudiante = Formulario.texto("", 100),
      curso = Formulario.texto("", 80);
    final JSpinner grado = Formulario.numero(0, 0, 11),
      parcial = Formulario.numero(0, 0, 4);

    FiltrosNotas(Runnable buscar) {
      super(new GridLayout(0, 1));
      JPanel campos = new JPanel(new FlowLayout(FlowLayout.LEFT));
      estudiante.setColumns(15);
      curso.setColumns(12);
      campos.add(new JLabel("Estudiante (código/nombre/apellidos):"));
      campos.add(estudiante);
      campos.add(new JLabel("Curso:"));
      campos.add(curso);
      add(campos);
      JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
      acciones.add(new JLabel("Grado (0=todos):"));
      acciones.add(grado);
      acciones.add(new JLabel("Período parcial (0=todos):"));
      acciones.add(parcial);
      acciones.add(boton("Buscar", buscar));
      acciones.add(
        boton("Ver todos", () -> {
          estudiante.setText("");
          curso.setText("");
          grado.setValue(0);
          parcial.setValue(0);
          buscar.run();
        })
      );
      add(acciones);
      estudiante.addActionListener(e -> accion(buscar));
      curso.addActionListener(e -> accion(buscar));
    }

    List<Calificacion> filtrar() {
      return controlador
        .getEstado()
        .filtrarCalificaciones(
          estudiante.getText(),
          Formulario.entero(grado),
          Formulario.entero(parcial),
          curso.getText()
        );
    }
  }

  private void aplicarFiltroNotas() {
    if (filtrosNotas != null) notas.mostrar(filtrosNotas.filtrar());
  }

  private void aplicarFiltroReportes() {
    if (filtrosReportes == null) return;
    EstadoAcademico datos = controlador.getEstado();
    int grado = Formulario.entero(filtrosReportes.grado),
      parcial = Formulario.entero(filtrosReportes.parcial);
    List<Calificacion> filtradas = filtrosReportes.filtrar();
    reporte.mostrar(filtradas);
    Set<Long> conNotas = new HashSet<>();
    filtradas.forEach(n -> conNotas.add(n.estudianteId()));
    alumnosReporte.mostrar(
      datos
        .buscarEstudiantes(filtrosReportes.estudiante.getText())
        .stream()
        .filter(e -> grado == 0 || e.getGrado() == grado)
        .filter(e -> parcial == 0 || conNotas.contains(e.getId()))
        .filter(
          e ->
            filtrosReportes.curso.getText().isBlank() ||
            datos
              .cursosMatriculados(e.getId())
              .stream()
              .anyMatch(c ->
                com.sgai.util.Busquedas.coincide(
                  filtrosReportes.curso.getText(),
                  c.codigo() + " " + c.nombre()
                )
              )
        )
        .toList()
    );
  }

  private JPanel panelReportes() {
    filtrosReportes = new FiltrosNotas(this::aplicarFiltroReportes);
    JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
    acciones.add(
      boton("Generar reportes", () -> {
        aplicarFiltroReportesSiNecesario();
        Estudiante alumno = alumnosReporte.seleccionado();
        ReporteAcademico generado = controlador
          .getEstado()
          .generarReporte(
            alumno.getId(),
            filtrosReportes.curso.getText(),
            Formulario.entero(filtrosReportes.parcial)
          );
        new VentanaReporte(this, generado).setVisible(true);
      })
    );
    JPanel panel = Estilo.panel();
    panel.add(barras(acciones, filtrosReportes), BorderLayout.NORTH);
    JSplitPane tablas = new JSplitPane(
      JSplitPane.VERTICAL_SPLIT,
      new JScrollPane(alumnosReporte),
      new JScrollPane(reporte)
    );
    tablas.setResizeWeight(0.45);
    panel.add(tablas, BorderLayout.CENTER);
    panel.add(resumenReporte, BorderLayout.SOUTH);
    return panel;
  }

  private void aplicarFiltroReportesSiNecesario() {
    // No reconstruir la tabla aquí: se conserva la fila que el usuario seleccionó tras Buscar.
    if (alumnosReporte.getSelectedRow() < 0) throw new ExcepcionNegocio(
      "Busque y seleccione un estudiante para generar el reporte."
    );
  }

  private JPanel panelCuentas() {
    JPanel panel = Estilo.panel();
    panel.add(Estilo.titulo("Administración de acceso"), BorderLayout.NORTH);
    JPanel centro = new JPanel(new GridLayout(0, 1, 8, 8));
    centro.add(
      new JLabel(
        "Seleccione una persona: el rol es automático. También puede crear un administrador independiente."
      )
    );
    centro.add(
      Estilo.colorear(
        boton("Crear cuenta con rol", this::crearCuenta),
        new Color(190, 235, 208)
      )
    );
    centro.add(
      Estilo.colorear(
        boton("Restablecer contraseña", this::restablecerCuenta),
        new Color(247, 236, 164)
      )
    );
    centro.add(
      Estilo.colorear(
        boton("Eliminar cuenta", this::eliminarCuenta),
        new Color(255, 184, 184)
      )
    );
    centro.add(
      new JLabel(
        "Elimine primero la cuenta vinculada para poder dar de baja a una persona sin notas."
      )
    );
    panel.add(centro, BorderLayout.CENTER);
    return panel;
  }

  private void crearCuenta() {
    Formulario formulario = new Formulario();
    JTextField nombre = formulario.campo("Usuario", Formulario.texto("", 40));
    JPasswordField clave = formulario.campo(
      "Contraseña (12–128)",
      Formulario.clave()
    );
    java.util.List<Persona> personas = new ArrayList<>();
    personas.addAll(Arrays.asList(controlador.getEstado().estudiantes()));
    personas.addAll(controlador.getEstado().docentes());
    JComboBox<Persona> persona = formulario.campo(
      "Persona registrada",
      new JComboBox<>(personas.toArray(Persona[]::new))
    );
    persona.setSelectedIndex(-1);
    persona.setRenderer(
      new DefaultListCellRenderer() {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(
          JList<?> lista,
          Object valor,
          int indice,
          boolean seleccionado,
          boolean foco
        ) {
          super.getListCellRendererComponent(
            lista,
            valor,
            indice,
            seleccionado,
            foco
          );
          if (valor instanceof Persona registrada) {
            setText(
              (registrada instanceof Docente ? "Docente · " : "Estudiante · ") +
                registrada
            );
          } else setText("Seleccione una persona registrada");
          return this;
        }
      }
    );
    JCheckBox administrador = formulario.campo(
      "Cuenta independiente",
      new JCheckBox("Crear administrador (sin persona asociada)")
    );
    JLabel rol = formulario.campo(
      "Rol automático",
      new JLabel("Seleccione una persona registrada")
    );
    Runnable actualizarRol = () -> {
      Object seleccion = persona.getSelectedItem();
      rol.setText(
        administrador.isSelected()
          ? "ADMINISTRADOR"
          : seleccion instanceof Docente
            ? "DOCENTE"
            : seleccion instanceof Estudiante
              ? "ESTUDIANTE"
              : "Seleccione una persona registrada"
      );
    };
    persona.addActionListener(evento -> actualizarRol.run());
    administrador.addActionListener(evento -> {
      persona.setEnabled(!administrador.isSelected());
      if (administrador.isSelected()) persona.setSelectedIndex(-1);
      actualizarRol.run();
    });
    if (!formulario.aceptar(this, "Crear cuenta")) {
      clave.setText("");
      return;
    }
    char[] secreto = clave.getPassword();
    clave.setText("");
    try {
      CompletableFuture<EstadoAcademico> creacion = administrador.isSelected()
        ? controlador.crearAdministrador(nombre.getText().strip(), secreto)
        : controlador.crearUsuario(
            nombre.getText().strip(),
            secreto,
            (Persona) persona.getSelectedItem()
          );
      ejecutar(creacion, nuevo -> {
        refrescar();
        JOptionPane.showMessageDialog(this, "Cuenta creada con éxito");
      });
    } finally {
      Arrays.fill(secreto, '\0');
    }
  }

  private void restablecerCuenta() {
    Formulario formulario = new Formulario();
    JTextField nombre = formulario.campo(
      "Usuario exacto",
      Formulario.texto("", 40)
    );
    JPasswordField clave = formulario.campo(
      "Nueva contraseña (12–128)",
      Formulario.clave()
    );
    if (!formulario.aceptar(this, "Restablecer contraseña")) {
      clave.setText("");
      return;
    }
    char[] secreto = clave.getPassword();
    clave.setText("");
    try {
      guardar(
        controlador.restablecerUsuario(nombre.getText().strip(), secreto)
      );
    } finally {
      Arrays.fill(secreto, '\0');
    }
  }

  private void eliminarCuenta() {
    String nombre = JOptionPane.showInputDialog(
      this,
      "Usuario exacto a eliminar (no puede eliminar su propia cuenta):"
    );
    if (
      nombre != null &&
      confirmar("¿Eliminar permanentemente la cuenta de acceso?")
    ) guardar(controlador.eliminarUsuario(nombre.strip()));
  }

  private void eliminar(Tabla tabla, long id) {
    if (
      confirmar(
        "¿Eliminar el registro? Las dependencias impedirán la baja si existen datos vinculados."
      )
    ) guardar(controlador.eliminar(tabla, id));
  }

  private String nombreAlumno(long id) {
    return Arrays.stream(controlador.getEstado().estudiantes())
      .filter(e -> e.getId() == id)
      .map(Estudiante::toString)
      .findFirst()
      .orElse("Estudiante #" + id);
  }

  private String nombreDocente(long id) {
    return controlador
      .getEstado()
      .docentes()
      .stream()
      .filter(d -> d.getId() == id)
      .map(Docente::getNombreCompleto)
      .findFirst()
      .orElse("Docente #" + id);
  }

  private String especialidadDocente(long id) {
    return controlador
      .getEstado()
      .docentes()
      .stream()
      .filter(d -> d.getId() == id)
      .map(Docente::getEspecialidad)
      .findFirst()
      .orElse("");
  }

  private String nombreCurso(long id) {
    return controlador
      .getEstado()
      .cursos()
      .stream()
      .filter(c -> c.id() == id)
      .map(Curso::toString)
      .findFirst()
      .orElse("Curso #" + id);
  }

  private Object[] valoresNota(Calificacion nota) {
    return new Object[] {
      nombreAlumno(nota.estudianteId()),
      nombreCurso(nota.cursoId()),
      nombreDocente(nota.docenteId()),
      nota.periodoParcial(),
      nota.nota(),
    };
  }

  private void salir() {
    if (!confirmar("¿Cerrar sesión?")) return;
    controlador.close();
    ControladorAcademico nuevo = new ControladorAcademico();
    VentanaLogin login = new VentanaLogin(nuevo);
    login.setVisible(true);
    dispose();
  }
}
