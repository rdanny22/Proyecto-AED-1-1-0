# SGAI · Colegio Antonio Soldano

Aplicación de escritorio **Java 17 / Swing / MySQL 8**, con separación MVC/DAO, listas enlazadas propias y algoritmos explícitos. La interacción del personal y del estudiante es exclusivamente gráfica; no hay menú de consola, credenciales incorporadas ni modo de demostración que simule persistencia.

## Requisitos y ejecución

- JDK 17 o posterior y Maven 3.9+ para compilar.
- MySQL **8.0.16+** (los `CHECK` deben ser efectivos), recomendado 8.4.
- Escritorio gráfico en Windows, Linux o macOS. Swing no es una aplicación web y no se visualiza en una vista previa HTTP.

### 1. Preparar la base de datos

Con una cuenta de migraciones, ejecute **una sola vez** [`database/esquema.sql`](database/esquema.sql) en MySQL Workbench u otro cliente SQL. El script crea `sgai-soldano-db` y las ocho tablas (incluidas `matriculas` y `secuencias_codigo`). No vuelve a crear tablas existentes ni migra esquemas previos.

Cree una cuenta MySQL **específica** [`database/crear_cuenta_MySQL_para_la_app.sql`](database/crear_cuenta_MySQL_para_la_app.sql)para la aplicación con permisos `SELECT`, `INSERT`, `UPDATE`, `DELETE` solamente sobre `sgai-soldano-db` y acceso limitado al origen de la aplicación. No utilice `root`. Las migraciones y las copias de seguridad son responsabilidad de otra cuenta. La aplicación no necesita permisos DDL ni `GRANT`.

### Datos ficticios para pruebas rápidas

Para agregar 55 estudiantes (5 por grado), 22 cursos, 22 docentes, 110 matrículas y 440 notas sin reiniciar códigos, use [`database/datos_demostracion.sql`](database/datos_demostracion.sql). Antes haga un respaldo y lea [`database/DATOS_DEMOSTRACION.md`](database/DATOS_DEMOSTRACION.md). La carga es transaccional, reserva las secuencias existentes y rechaza repetir el mismo lote. No crea cuentas ni contraseñas y se recomienda exclusivamente para bases de prueba.

### 2. Configurar el entorno

Variables obligatorias:

| Variable | Propósito |
|---|---|
| `SGAI_DB_USUARIO` | Cuenta MySQL de la aplicación |
| `SGAI_DB_CLAVE` | Contraseña de esa cuenta; nunca una contraseña de un usuario escolar |
| `SGAI_DB_URL` | Opcional: URL JDBC completa |

URL predeterminada:

```text
jdbc:mysql://localhost:3306/sgai-soldano-db?sslMode=VERIFY_IDENTITY&connectTimeout=5000&socketTimeout=15000&connectionTimeZone=UTC
```

Configure certificados de confianza en la JVM para verificar la identidad del servidor. **Solo para desarrollo en su propia máquina**, se puede especificar una URL con `sslMode=DISABLED`; no desactive TLS en redes de producción. `localhost` aquí pertenece a la aplicación Java instalada, no a un navegador.

Configure las variables mediante el sistema operativo o el entorno de ejecución de su IDE. El archivo `.env` **no se carga automáticamente**. Nunca incluya secretos en el repositorio o en capturas de pantalla.

### 3. Compilar y abrir

```sh
mvn clean verify
java -jar target/sgai-soldano-1.0.0.jar
```

El JAR incluye MySQL Connector/J. En Windows también puede iniciarse con `javaw -jar ...` desde un acceso directo con el entorno configurado, sin ventana de consola. En el IDE, ejecute `com.sgai.Aplicacion`.

### Emblema del colegio

El emblema proporcionado por el colegio ya está incluido en `src/main/resources/imagenes/emblema-colegio.png`. Se conserva el PNG original (1080 × 1079 píxeles, con transparencia), sin redibujarlo, recortarlo ni modificar sus colores. Se carga automáticamente en el login, la ventana principal y los reportes. No requiere variables de entorno ni cambios en la BD.

1. Para sustituir el emblema en el futuro, reemplace el PNG respetando exactamente el nombre y las minúsculas:
   ```text
   src/main/resources/imagenes/emblema-colegio.png
   ```
2. Se recomienda fondo transparente, 512–1024 píxeles y sin márgenes blancos excesivos. Se admiten hasta 8 MiB y 4096 píxeles por dimensión; se rechaza un archivo inválido o sobredimensionado sin bloquear SGAI.
3. En Eclipse, pulse **F5**, reconstruya el proyecto (**Project > Clean**, con construcción automática habilitada) y reinicie la aplicación. La imagen se carga una vez por proceso.
4. Para distribuirlo, ejecute `mvn clean verify`: Maven incorpora el recurso al JAR. No basta copiar el PNG al lado de un JAR ya generado y no se requieren rutas absolutas de Windows ni nuevas variables de entorno.

El emblema aparece en el login (hasta 80 × 80), en el encabezado principal (64 × 64), como icono de ambas ventanas y en todas las páginas de los reportes (56 × 56 puntos). Se centra y conserva sus proporciones, sin deformarlo ni recortarlo. Los reportes reutilizan el original de alta resolución tanto para la vista previa como para imprimir. Se mantienen las 23 filas por página, márgenes carta, tabla, totales y pie de página; el encabezado ajusta el texto para evitar superposiciones.

Si falta la imagen o no puede leerse, se mantienen los títulos del colegio sin reservar un espacio vacío para un emblema ficticio. No cambian la base de datos, las cuentas ni la separación entre login e instalación.

`RecursosGraficosTest` prueba carga, errores, cierre de recursos, dimensiones, transparencia/proporciones y la impresión de tres páginas en modo headless. Las pruebas usan figuras sintéticas que no se empaquetan como imagen oficial. También se verifica la carga del emblema real desde el classpath y su presencia en el documento imprimible. La verificación se ejecutó además desde un JAR temporal con el PNG real, fuera del repositorio. La prueba de ventanas en un escritorio y la impresión física siguen pendientes.

### 4. Primer acceso y flujo de trabajo

1. **Instalación administrativa independiente:** en una estación de confianza, cree una configuración de ejecución cuya clase principal sea `com.sgai.Instalacion` (en Eclipse: Run > Run Configurations > Java Application). Configure sus variables JDBC y, **solo en esta configuración**, `SGAI_CLAVE_INSTALACION`: una clave aleatoria y privada de 32 a 128 caracteres, generada con un gestor de contraseñas. Fuera del IDE, puede ejecutar `java -cp target/sgai-soldano-1.0.0.jar com.sgai.Instalacion` con ese entorno.
2. En la ventana de instalación, introduzca esa clave y el usuario/contraseña del primer administrador (12–128 caracteres). La clave de instalación es diferente de la contraseña de la cuenta. El controlador verifica la autorización antes de acceder a la BD; después verifica que no haya usuarios y serializa la creación con `GET_LOCK`. Una autorización permite un único intento. No hay contraseña predeterminada.
3. Cierre la instalación y elimine `SGAI_CLAVE_INSTALACION` de su configuración de ejecución. No comparta la clave, ni la configuración que la contiene. Si ya hay usuarios, no se permite aprovisionar de nuevo.
4. **Acceso habitual:** ejecute `com.sgai.Aplicacion` o el JAR normalmente. `VentanaLogin` solo presenta usuario, contraseña e inicio de sesión: no hay acceso al instalador, botón de diagnóstico ni consulta automática de configuración/conexión. Los errores de conexión durante el login se notifican de forma genérica.
5. Como administrador, registre estudiantes, docentes y cursos: sus códigos se generan automáticamente. Asigne docentes a cursos y, en **Matrícula**, busque al estudiante, selecciónelo y pulse **Matricular** para añadir cursos de su grado: marque las casillas de uno o varios cursos dentro de la misma ventana y confirme una sola vez. Se conservan las matrículas existentes. **Eliminar matrícula** retira todos los cursos del estudiante seleccionado tras confirmar; si hay notas registradas, se rechaza la operación completa para conservar el historial. Con el estudiante seleccionado, pulse **Generar hoja de matrícula** para visualizar sus cursos ya matriculados e imprimir la hoja tamaño carta con el emblema del colegio, utilizando la misma vista previa paginada del reporte de notas. Los grados válidos son del 1 al 11.
6. En **Cuentas > Crear cuenta con rol**, seleccione una única persona registrada: el sistema asigna automáticamente **DOCENTE** o **ESTUDIANTE** según su registro. El rol mostrado no es editable y no depende del nombre de usuario. Para crear otro administrador (por ejemplo, `Admin2`), marque **Crear administrador (sin persona asociada)** e introduzca únicamente usuario y contraseña: el selector de personas queda deshabilitado y vacío. Solo un administrador autenticado puede crear cualquiera de estas cuentas. También puede restablecer contraseñas o eliminar cuentas; no puede eliminar su propia cuenta.
7. Registre notas solo para estudiantes matriculados en el curso y con un docente asignado. Hay cuatro períodos parciales, escala 0–100 y dos decimales como máximo. Una sola nota por estudiante/curso/período parcial.
8. En **Reportes**, busque por código/nombre/apellidos del estudiante, grado, curso y período parcial. Pulse **Buscar**, seleccione un estudiante y pulse **Generar reportes** para ver sus cuatro parciales por curso en un documento paginado tamaño carta, con **Imprimir** mediante el diálogo del equipo. La tabla inferior permite consultar las notas filtradas. Las notas inferiores a 60 aparecen en rojo.
9. Use **Actualizar** para incorporar cambios hechos desde otra instancia; **Cerrar sesión** descarta la sesión y su caché y vuelve al login público, sin opciones administrativas.

No es posible determinar el rol de una persona antes de autenticarla. Por eso la instalación está separada para **todos** los usuarios del login público, incluidos los administradores, y se protege en el controlador, no solo ocultando componentes. La clave de instalación debe mantenerse fuera de los entornos de estudiantes y docentes. Como con cualquier cliente JDBC directo, quien controla las credenciales de BD o el proceso local puede eludir controles del cliente: se mantiene el requisito de estaciones de confianza.

La baja física no borra cascadas académicas: se rechaza si existen relaciones. Para dar de baja una persona sin historial, revise primero su cuenta, sus matrículas y sus asignaciones. Una asignación con notas no puede retirarse. No cambie grados como mecanismo de promoción si ya existen matrículas asociadas incompatibles.

## Permisos

| Operación | Administrador | Docente | Estudiante |
|---|---|---|---|
| CRUD estudiantes, docentes, cursos | Sí | No | No |
| Gestionar matrículas, asignaciones y cuentas | Sí | No | No |
| Consultar estudiantes | Todos | Matriculados en sus cursos | Solo datos propios |
| Consultar cursos | Todos | Asignados | Solo los de su matrícula |
| Crear/editar/eliminar notas | Todas | Solo cursos asignados, con su identidad docente | No |
| Ver notas y reportes | Todos | Cursos asignados | Solo sus notas |

Los permisos se verifican en el **controlador**, no únicamente ocultando botones. Las notas se filtran también mediante SQL parametrizado. La cuenta vigente se comprueba en cada actualización/escritura: eliminar una cuenta impide operaciones posteriores de su sesión. Una caché ya mostrada no puede retirarse a distancia. Restablecer una contraseña afecta al siguiente inicio de sesión, no expulsa sesiones existentes.

## Arquitectura y fundamentos

```text
com.sgai.Aplicacion (acceso habitual) / com.sgai.Instalacion (aprovisionamiento protegido)
├── modelo       Persona abstracta → Estudiante / Docente; Usuario, Curso, Calificacion, Rol, Asignacion, Matricula
├── estructuras  Nodos y listas propias; QuickSort y búsqueda binaria genérica
├── dao          Conexion, Transacciones, UsuarioDAO, AcademicoDAO
├── controlador  ControladorAcademico, EstadoAcademico, ReporteAcademico
├── vista        Login público, instalación protegida, dashboard, formularios, tablas y renderizador
├── seguridad    PBKDF2-HMAC-SHA256
└── util         Validación y excepción de negocio
```

- **POO (Deitel / Joyanes):** identidad encapsulada, objetos de dominio inmutables, herencia mediante `Persona`, sobrescritura polimórfica de `getDescripcion`, interfaces funcionales y composición en el controlador.
- **Listas propias:** estudiantes, docentes y notas se almacenan en estructuras simples con cabeza/cola. Inserción O(1), búsqueda por ID y eliminación O(n), copia O(n); memoria O(n). No exponen los enlaces mutables.
- **QuickSort propio:** construye índices por código y el reporte individual (notas descendentes por período parcial seleccionado, con desempate por código del curso). O(n log n) promedio, O(n²) peor caso; recursión sobre el tramo menor para limitar la pila a O(log n). No se afirma que sea estable.
- **Buscadores visibles:** coincidencias parciales por código/nombre/apellidos, sin distinguir tildes ni mayúsculas; **Buscar** aplica filtros y **Ver todos** los limpia sin ampliar permisos.
- **Búsqueda binaria propia:** O(log n) en índices de estudiantes y docentes construidos una vez por instantánea; requisito explícito de ordenamiento por el mismo comparador. Construir cada índice cuesta O(n log n) promedio y O(n) memoria.
- **MVC / DAO:** las vistas no ejecutan SQL; los DAO no conocen Swing; el controlador impone las reglas y publica instantáneas tras confirmar la transacción.
- **Asincronía:** un `ExecutorService` por sesión ejecuta JDBC, hashing, índices y ordenamiento. `CompletableFuture` devuelve resultados al EDT con `SwingUtilities.invokeLater`. No se muestran cambios optimistas que después pudieran fallar en la BD.
- **Transacciones:** commit/rollback y `try-with-resources`. Las escrituras de la aplicación se serializan entre instancias bloqueando una fila administrativa, para validar asignación y grado sin carreras entre escrituras cooperantes. Una instantánea se construye en la misma unidad de trabajo y se publica solo después del commit.
- **Errores:** validaciones regex, selectores numéricos, notas `BigDecimal`, errores de integridad traducidos y mensajes gráficos sin SQL ni secretos. Una transacción fallida conserva la última instantánea confirmada.
- **Credenciales:** PBKDF2-HMAC-SHA256, 600 000 iteraciones, sal aleatoria de 16 bytes, salida de 256 bits, comparación constante, borrado de arreglos temporales. Mensaje de credenciales genérico; cinco fallos ocasionan una espera de 30 segundos en esa instancia.

### Diseño relacional en 1FN

Todos los campos contienen valores atómicos; no hay listas serializadas ni grupos repetidos. Las PK identifican cada fila, los códigos y usuarios son únicos, y las FK tienen `ON DELETE RESTRICT`.

- `estudiantes`, `docentes`: identidad y atributos atómicos especializados.
- `usuarios`: rol y asociación opcional, con `CHECK` que impone exactamente la persona correspondiente para cada rol.
- `cursos`: código, nombre y grado.
- `asignacion_docente_curso`: PK compuesta `(docente_id, curso_id)`, relación muchos a muchos.
- `matriculas`: PK `(estudiante_id, curso_id)` y FK a estudiantes/cursos; una fila por inscripción, sin listas de cursos en una celda.
- `secuencias_codigo`: tres filas (`ESTU`, `PROF`, `CURS`), bloqueadas con `FOR UPDATE` al reservar un código dentro de la transacción de alta.
- `calificaciones`: PK propia; unicidad `(estudiante_id, curso_id, periodo_parcial)`; FK compuestas hacia asignación y matrícula. Impide notas sin matrícula o sin docente asignado, incluso ante SQL directo. La compatibilidad de grado se revalida transaccionalmente al matricular, registrar notas o editar grados.

## Verificación

La regresión de instalación también comprueba que el controlador rechaza crear administradores sin autorización y claves de instalación incorrectas antes de abrir JDBC. `VentanaLoginTest` verifica que el único botón del login público sea **Iniciar sesión**; necesita un entorno gráfico y se omite explícitamente en headless. La integración MySQL comprueba además el aprovisionamiento autorizado, su rechazo con BD ya configurada y el rechazo desde sesiones Docente/Estudiante.

`mvn verify` ejecuta el núcleo JUnit de dominio, listas, algoritmos, instantáneas, claves, autorización sin sesión y componentes Swing en modo headless. Los algoritmos se comparan contra un oráculo de biblioteca en cientos de arreglos y miles de búsquedas; se incluyen vacíos, duplicados y un arreglo de 100 000 elementos iguales.

La prueba `MySqlIntegracionTest` está **desactivada por defecto**. Para habilitarla:

1. Cree una BD desechable y vacía con el esquema, en una instancia separada.
2. Configure las variables JDBC apuntando a ella, `SGAI_PRUEBAS_MYSQL=true` y una `SGAI_CLAVE_INSTALACION` de prueba de 32–128 caracteres.
3. Ejecute `mvn verify`. La prueba deja datos de prueba; recree la BD antes de volver a ejecutarla. **Nunca apunte esta prueba a datos reales**.

Comprueba aprovisionamiento, login, códigos automáticos, altas concurrentes, matrícula idempotente y por grado, restricciones por rol, notas duplicadas, FK, rollback sin datos fantasma, revocación e intento de inyección. La matriz de GitHub Actions provisiona MySQL 8.4 y prueba tanto el esquema nuevo como la migración desde los fixtures de la versión anterior. `MigracionMySqlTest` solo se habilita con `SGAI_PRUEBAS_MIGRACION=true`, sobre una BD desechable preparada con `src/test/resources/esquema_anterior.sql`, `datos_anteriores.sql` y la migración 002; no active simultáneamente ambas modalidades sobre la misma BD. Las credenciales visibles en ese flujo son exclusivamente del contenedor efímero de CI.

### Estado de comprobación en este entorno

- Las clases de aplicación y el núcleo de pruebas se compilaron con Eclipse Compiler for Java, destino Java 17.
- Los núcleos `Verificaciones` y `VerificacionesAcademicas` se ejecutaron correctamente en Java 17, incluyendo filtros combinados, tildes, reportes de cuatro parciales, notas pendientes, promedio y paginación carta de 50 cursos sin impresora. Se renderizó una página del reporte para comprobar su distribución visual. No producen salida si todo pasa.
- No se ejecutaron aquí Maven, MySQL real ni ventanas gráficas: el entorno no los proporciona. Las pruebas de integración están implementadas y configuradas en CI, **no se presentan como ya superadas**.

## Alcance y despliegue responsable

Decisiones explícitas:

- Un conjunto académico por instalación, sin años lectivos, matrícula histórica, secciones, asistencia ni promociones automáticas. Los estudiantes acceden solo a los cursos en los que están matriculados. La matrícula nueva se limita a cursos de su grado. Para varios años se necesita una migración de modelo, no reutilizar notas de otro ciclo.
- Dashboard: media aritmética de las notas autorizadas. Reporte individual: promedio de las notas registradas de los cursos incluidos, sin convertir ausencias en cero. Un curso se cuenta aprobado si tiene los cuatro parciales y su media es ≥60; reprobado si, completo, su media es <60. Los cursos incompletos se cuentan como pendientes. La media final se redondea a dos decimales antes de clasificar. No se introducen ponderaciones.
- Reporte de cuatro columnas: se mantienen períodos parciales 1, 2, 3 y 4 de izquierda a derecha. El filtro de parcial ordena las filas por su nota, descendente; las notas ausentes van al final. Con 0/todos se comparan sucesivamente primero, segundo, tercero y cuarto, con desempate por código de curso. El filtro de parcial reduce la lista de estudiantes/notas de búsqueda; el documento individual conserva siempre las cuatro columnas. El filtro de curso limita los cursos incluidos. Un docente obtiene únicamente el reporte de los cursos que tiene asignados.
- Reportes visibles, paginados tamaño carta (8,5 × 11 pulgadas), márgenes de media pulgada, encabezado repetido y resumen en la última página. El botón **Imprimir** utiliza impresoras del equipo; si el sistema ofrece una impresora PDF, también puede elegirla. No se agrega exportación Excel ni un generador PDF independiente.
- Instantáneas completas en memoria: apropiado para un colegio; requiere paginación y estrategias incrementales para grandes volúmenes. Edición concurrente del mismo registro: prevalece la última escritura confirmada; no hay historial ni versionado optimista.
- Aplicación de escritorio con JDBC directo: requiere equipos y conexión de BD **de confianza**. Quien controla una máquina cliente con credenciales MySQL puede eludir el control de roles de Java. Si los estudiantes usan equipos no confiables, **no distribuya esta configuración**: interponga un servicio backend con autenticación y autorización por solicitud y retire los secretos de BD del cliente.
- El bloqueo de intentos es local al proceso; no sustituye controles centralizados, MFA ni auditoría. Proteja el primer aprovisionamiento en una estación administrada.
- Antes de producción: ejecutar CI e integración contra su versión MySQL, hacer pruebas gráficas/manuales con los tres roles, revisar requisitos UNI, políticas de privacidad, permisos y certificados; configurar respaldos y probar su recuperación. El proyecto no incluye auditoría persistente, recuperación por correo ni actualización automática de esquema.