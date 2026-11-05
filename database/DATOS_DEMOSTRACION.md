# Cargar datos ficticios en SGAI

Archivo ejecutable: **[`datos_demostracion.sql`](datos_demostracion.sql)**.

## Qué agrega

| Registros nuevos | Cantidad |
|---|---:|
| Estudiantes: 5 por cada grado de 1.º a 11.º | 55 |
| Cursos: 2 por grado | 22 |
| Docentes: uno distinto por curso | 22 |
| Asignaciones docente–curso | 22 |
| Matrículas: cada estudiante en los dos cursos de su grado | 110 |
| Notas: cuatro períodos parciales por cada matrícula | 440 |

Son cantidades **añadidas**, no objetivos para el total de una BD que ya tenga datos. Los nombres son ficticios y los correos usan el dominio reservado no operativo `sgai-demo-v1.invalid`. Las notas reproducibles están entre 45.00 y 100.00, con casos aprobados y reprobados para probar tablas y reportes.

Se crean personas, **no cuentas de acceso**. No se insertan usuarios ni contraseñas. Para que una persona del lote inicie sesión, cree su cuenta desde **Cuentas** como administrador. Si la BD aún no tiene usuarios, la carga no impide crear después el primer administrador mediante el instalador.

## Cursos y fuentes MINED

Se seleccionan **Matemática** y **Lengua y Literatura** en cada grado, diferenciadas por su atributo `grado`. Matemática y Lengua y Literatura figuran en los materiales oficiales MINED de primaria; el catálogo incluye recursos para primer grado y otros grados de primaria. [1](https://www.mined.gob.ni/biblioteca/product-tag/primar-grado/?view=list-view) [2](https://www.mined.gob.ni/biblioteca/product-category/primaria/page/5/?per_page=16)

Las mallas curriculares oficiales publicadas para los ciclos IV y V de secundaria incluyen ambas disciplinas. [3](https://nicaraguaeduca.mined.gob.ni/index.php/mallas-curriculares-s/)

Este lote solo toma **dos disciplinas comunes**, no reproduce toda la malla curricular, contenidos por unidad, cargas horarias, modalidades ni certifica una edición curricular 2026. Los nombres se normalizan a `Matemática` y `Lengua y Literatura`; el grado se guarda por separado.

## Antes de ejecutarlo

1. Use preferentemente una **instancia de MySQL de pruebas**, con una copia llamada `sgai-soldano-db`. El archivo contiene `USE sgai-soldano-db`: seleccionar otra base en Workbench **no cambia su destino**. Compruebe el servidor de la conexión. No es aconsejable mezclar datos ficticios con expedientes reales.
2. Haga un respaldo completo de `sgai-soldano-db` desde Workbench (Server > Data Export) y compruebe que puede recuperarlo.
3. Cierre todas las instancias de SGAI y detenga otras cargas/escrituras manuales.
4. La BD debe tener el **esquema actual**: ocho tablas, `calificaciones.periodo_parcial` y las tres filas de `secuencias_codigo`. Para una instalación anterior, aplique primero la migración 002; para una nueva, ejecute solo `esquema.sql`.
5. Use una conexión nueva de Workbench con autocommit activo y comprobación de claves foráneas activa. No ejecute el archivo en medio de otra transacción.
6. Utilice una cuenta administradora/de migraciones. El script necesita permisos DML y `CREATE ROUTINE`, `ALTER ROUTINE` y `EXECUTE` sobre esta BD. **No amplíe los permisos de `sgai_app`** para ejecutar esta carga; la aplicación debe seguir usando solo sus cuatro permisos DML.

## Ejecución en MySQL Workbench

1. Abra **File > Open SQL Script** y seleccione `database/datos_demostracion.sql`.
2. Ejecute **todo el archivo**, no una línea o una selección. Workbench interpreta los comandos `DELIMITER` del procedimiento.
3. El script crea una rutina auxiliar, la ejecuta y la elimina. No crea ni modifica tablas.
4. Si termina correctamente, el resultado indica `Carga de demostración completada`, las cantidades añadidas y el siguiente código de cada tipo.
5. Abra SGAI o pulse **Actualizar** para recargar la información. Pruebe búsquedas, matrículas y reportes.

En una BD sin registros, los códigos nuevos serán `ESTU1…ESTU55`, `PROF1…PROF22` y `CURS1…CURS22`. Las próximas altas de la aplicación utilizarán **ESTU56**, **PROF23** y **CURS23**. Si ya había datos, los rangos comienzan más adelante.

## Protecciones y límites

- La carga académica y la reserva de códigos se ejecutan en **una transacción InnoDB**. Cualquier excepción provoca `ROLLBACK` y se conserva el error original.
- Los contadores se bloquean con `SELECT … FOR UPDATE` y se avanzan, **nunca se reinician**. Se considera tanto el contador persistido como el mayor sufijo existente de cada prefijo, evitando colisiones si hubo inserciones manuales anteriores. No se cambia ningún código existente.
- Usa un bloqueo exclusivo de carga y, cuando existe, el mismo bloqueo de fila administrativa que utiliza SGAI. Aun así, cierre el sistema y no haga otras escrituras manuales durante la carga.
- No se deshabilitan FK, no se borran datos, no se modifican cuentas y no se emplea `INSERT IGNORE` para ocultar fallos.
- Antes del commit verifica las cantidades del lote, grados de matrícula y notas válidas.
- La creación/eliminación de la rutina es DDL y tiene commit implícito. Solo los registros académicos y las secuencias forman la transacción de datos. Por eso se exige una conexión nueva.
- Un rollback puede dejar huecos en los IDs `AUTO_INCREMENT` internos. Eso es normal: no afecta a las relaciones ni duplica códigos.
- Si detecta cualquier estudiante/docente con correo `@sgai-demo-v1.invalid`, **rechaza toda la carga** con el mensaje `Este lote ya existe total o parcialmente`. Una repetición no duplica personas, cursos, matrículas ni notas. No cambie esos marcadores si desea conservar esta protección. No es un mecanismo de reparación si alguien eliminó o alteró el lote.
- Si Workbench se detiene tras un error, puede quedar únicamente la rutina auxiliar. Una nueva ejecución sustituye esa rutina; no borra registros. No intente resolver errores borrando tablas o reiniciando contadores.
- Para retirar todo el lote de una BD de prueba, lo más seguro es restaurar el respaldo anterior. No se proporciona un borrado indiscriminado de datos ni un reinicio de secuencias.

## Consultas de comprobación

```sql
USE `sgai-soldano-db`;

-- Debe mostrar once filas, con cinco estudiantes por grado.
SELECT grado, COUNT(*) AS estudiantes_demo
FROM estudiantes
WHERE correo LIKE '%@sgai-demo-v1.invalid'
GROUP BY grado ORDER BY grado;

-- Debe mostrar once filas, con dos cursos por grado.
SELECT c.grado, COUNT(DISTINCT c.id) AS cursos_demo
FROM cursos c
JOIN asignacion_docente_curso a ON a.curso_id=c.id
JOIN docentes d ON d.id=a.docente_id
WHERE d.correo LIKE '%@sgai-demo-v1.invalid'
GROUP BY c.grado ORDER BY c.grado;

-- Debe devolver 110 matrículas y 440 notas, respectivamente.
SELECT COUNT(*) AS matriculas_demo
FROM matriculas m JOIN estudiantes e ON e.id=m.estudiante_id
WHERE e.correo LIKE '%@sgai-demo-v1.invalid';

SELECT COUNT(*) AS notas_demo
FROM calificaciones n JOIN estudiantes e ON e.id=n.estudiante_id
WHERE e.correo LIKE '%@sgai-demo-v1.invalid';

SELECT tipo, siguiente FROM secuencias_codigo ORDER BY tipo;
```

## Verificación automatizada

`DatosDemoMySqlTest` se habilita **solo** con `SGAI_PRUEBAS_DEMO=true` sobre una BD desechable con el esquema nuevo y sin personas previas. Requiere las variables JDBC y una clave de instalación de pruebas válidas. No active a la vez `SGAI_PRUEBAS_MYSQL` o `SGAI_PRUEBAS_MIGRACION` para esa misma BD.

El test prepara datos anteriores con secuencias adelantadas/atrasadas, fuerza un fallo a mitad de carga y comprueba rollback, ejecuta dos cargas simultáneas (solo una debe insertar), rechaza la reejecución y comprueba que las próximas altas **de la aplicación Java** continúan los códigos sin conflictos. La matriz CI incluye una modalidad `demostracion` independiente.

Este entorno no dispone de un servidor MySQL: la prueba de integración queda implementada, pero no se declara superada localmente. Pruebe primero el archivo en una copia de su BD antes de utilizarlo en otro entorno.
