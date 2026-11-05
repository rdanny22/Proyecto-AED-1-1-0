-- ============================================================================
-- SGAI · Carga de DEMOSTRACIÓN v1 · MySQL 8.0.16+ (recomendado 8.4).
-- Ejecutar TODO el archivo en una conexión nueva de MySQL Workbench.
-- Requisito: esquema actual instalado (o migración 002 aplicada).
-- Destino explícito: sgai-soldano-db en el servidor de esta conexión.
-- Seleccionar otro esquema en Workbench NO cambia ese destino.
-- Hacer respaldo y cerrar todas las instancias de SGAI antes de ejecutar.
-- Usar preferentemente una BD de pruebas, nunca mezclar datos ficticios con reales.
-- Cuenta requerida: administrador/migraciones con DML y permisos de rutinas.
-- No se crean usuarios ni contraseñas. No se cambian tablas ni datos existentes,
-- salvo avanzar las tres secuencias de códigos reservadas por este lote.
-- No se deshabilitan FK ni se reinician IDs/secuencias.
--
-- Resultado: 55 estudiantes (5 por grado 1..11), 22 cursos (2 por grado),
-- 22 docentes (1 por curso), 22 asignaciones, 110 matrículas y 440 notas.
-- Cursos: Matemática y Lengua y Literatura, disciplinas presentes en materiales
-- oficiales MINED de primaria y secundaria; ver DATOS_DEMOSTRACION.md.
-- Correos ficticios bajo sgai-demo-v1.invalid identifican este lote.
-- Reejecución: si existe cualquier persona con ese dominio, se RECHAZA el lote
-- completo. No se borra, duplica ni intenta reparar una carga ya modificada.
-- ============================================================================

SET NAMES utf8mb4;
USE `sgai-soldano-db`;

-- Solo se reemplaza esta rutina auxiliar, no tablas ni registros académicos.
DROP PROCEDURE IF EXISTS sgai_cargar_demostracion_v1;
DELIMITER $$
CREATE PROCEDURE sgai_cargar_demostracion_v1()
SQL SECURITY INVOKER
BEGIN
    DECLARE v_bloqueo INT DEFAULT 0;
    DECLARE v_admin BIGINT DEFAULT NULL;
    DECLARE v_grado INT DEFAULT 1;
    DECLARE v_curso INT;
    DECLARE v_alumno INT;
    DECLARE v_parcial INT;
    DECLARE v_estudiante_id BIGINT;
    DECLARE v_docente_id BIGINT;
    DECLARE v_curso_id BIGINT;
    DECLARE v_docente_uno BIGINT;
    DECLARE v_docente_dos BIGINT;
    DECLARE v_curso_uno BIGINT;
    DECLARE v_curso_dos BIGINT;
    DECLARE v_estu DECIMAL(65,0);
    DECLARE v_prof DECIMAL(65,0);
    DECLARE v_curs DECIMAL(65,0);
    DECLARE v_nota DECIMAL(5,2);
    DECLARE v_cantidad INT;

    -- Cualquier error revierte TODA la carga y las reservas de códigos.
    -- Los AUTO_INCREMENT pueden tener huecos tras rollback: es normal y seguro.
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        IF v_bloqueo = 1 THEN
            DO RELEASE_LOCK('sgai_carga_demostracion_v1');
        END IF;
        RESIGNAL;
    END;

    IF @@session.autocommit <> 1 OR @@session.foreign_key_checks <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Use una conexión nueva, autocommit activo y foreign_key_checks=1.';
    END IF;

    SELECT COUNT(*) INTO v_cantidad
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND ENGINE = 'InnoDB'
      AND TABLE_NAME IN ('usuarios','estudiantes','docentes','cursos',
                         'asignacion_docente_curso','matriculas',
                         'calificaciones','secuencias_codigo');
    IF v_cantidad <> 8 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Falta el esquema actual InnoDB: aplique esquema.sql o migración 002.';
    END IF;
    SELECT COUNT(*) INTO v_cantidad
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'calificaciones'
      AND COLUMN_NAME = 'periodo_parcial';
    IF v_cantidad <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Falta periodo_parcial: aplique la migración 002 antes de cargar datos.';
    END IF;

    SELECT GET_LOCK('sgai_carga_demostracion_v1', 15) INTO v_bloqueo;
    IF v_bloqueo IS NULL OR v_bloqueo <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Otra sesión está ejecutando esta carga. Espere y vuelva a intentar.';
    END IF;

    START TRANSACTION;

    -- Mismo primer bloqueo que ControladorAcademico: serializa con escritores
    -- de la aplicación. Una BD sin administrador también admite datos de prueba.
    BEGIN
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_admin = NULL;
        SELECT id INTO v_admin FROM usuarios
        WHERE rol = 'ADMINISTRADOR' ORDER BY id LIMIT 1 FOR UPDATE;
    END;

    SELECT COUNT(*) INTO v_cantidad FROM secuencias_codigo
    WHERE tipo IN ('ESTU','PROF','CURS');
    IF v_cantidad <> 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Faltan secuencias ESTU/PROF/CURS. No se ha modificado ningún dato.';
    END IF;

    -- Bloquear los contadores antes de leer el marcador o reservar códigos.
    SELECT siguiente INTO v_estu FROM secuencias_codigo WHERE tipo = 'ESTU' FOR UPDATE;
    SELECT siguiente INTO v_prof FROM secuencias_codigo WHERE tipo = 'PROF' FOR UPDATE;
    SELECT siguiente INTO v_curs FROM secuencias_codigo WHERE tipo = 'CURS' FOR UPDATE;

    SELECT (SELECT COUNT(*) FROM estudiantes WHERE correo LIKE '%@sgai-demo-v1.invalid')
         + (SELECT COUNT(*) FROM docentes WHERE correo LIKE '%@sgai-demo-v1.invalid')
    INTO v_cantidad;
    IF v_cantidad > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Este lote ya existe total o parcialmente. Carga cancelada sin duplicar datos.';
    END IF;

    -- No bajar contadores existentes. Si hubo inserciones manuales con prefijos
    -- oficiales, comenzar también después de su mayor sufijo para no colisionar.
    SELECT GREATEST(v_estu, COALESCE(MAX(CAST(SUBSTRING(codigo,5) AS DECIMAL(20,0))),0)+1)
    INTO v_estu FROM estudiantes WHERE codigo REGEXP '^ESTU[0-9]+$';
    SELECT GREATEST(v_prof, COALESCE(MAX(CAST(SUBSTRING(codigo,5) AS DECIMAL(20,0))),0)+1)
    INTO v_prof FROM docentes WHERE codigo REGEXP '^PROF[0-9]+$';
    SELECT GREATEST(v_curs, COALESCE(MAX(CAST(SUBSTRING(codigo,5) AS DECIMAL(20,0))),0)+1)
    INTO v_curs FROM cursos WHERE codigo REGEXP '^CURS[0-9]+$';
    -- Mantener un siguiente válido dentro del rango long utilizado por Java.
    IF v_estu < 1 OR v_prof < 1 OR v_curs < 1
       OR v_estu > 9223372036854775751
       OR v_prof > 9223372036854775784
       OR v_curs > 9223372036854775784 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Secuencias fuera de rango para Java long. Carga cancelada.';
    END IF;
    UPDATE secuencias_codigo SET siguiente = v_estu + 55 WHERE tipo = 'ESTU';
    UPDATE secuencias_codigo SET siguiente = v_prof + 22 WHERE tipo = 'PROF';
    UPDATE secuencias_codigo SET siguiente = v_curs + 22 WHERE tipo = 'CURS';

    WHILE v_grado <= 11 DO
        SET v_curso = 1;
        WHILE v_curso <= 2 DO
            INSERT INTO cursos(codigo,nombre,grado) VALUES (
                CONCAT('CURS',v_curs+(v_grado-1)*2+v_curso-1),
                IF(v_curso=1,'Matemática','Lengua y Literatura'),v_grado
            );
            SET v_curso_id = LAST_INSERT_ID();
            INSERT INTO docentes(codigo,nombres,apellidos,correo,especialidad) VALUES (
                CONCAT('PROF',v_prof+(v_grado-1)*2+v_curso-1),
                ELT(v_grado,'Ana Lucía','Carlos Alberto','María Elena','José Antonio',
                    'Laura Isabel','Miguel Ángel','Sofía Patricia','Daniel Enrique',
                    'Gabriela Inés','Luis Fernando','Valeria Alejandra'),
                IF(v_curso=1,'Pérez Torres','López Ruiz'),
                CONCAT('docente.g',LPAD(v_grado,2,'0'),'.c',v_curso,'@sgai-demo-v1.invalid'),
                IF(v_curso=1,'Matemática','Lengua y Literatura')
            );
            SET v_docente_id = LAST_INSERT_ID();
            INSERT INTO asignacion_docente_curso(docente_id,curso_id)
                VALUES(v_docente_id,v_curso_id);
            IF v_curso = 1 THEN
                SET v_curso_uno = v_curso_id;
                SET v_docente_uno = v_docente_id;
            ELSE
                SET v_curso_dos = v_curso_id;
                SET v_docente_dos = v_docente_id;
            END IF;
            SET v_curso = v_curso + 1;
        END WHILE;

        SET v_alumno = 1;
        WHILE v_alumno <= 5 DO
            INSERT INTO estudiantes(codigo,nombres,apellidos,correo,grado) VALUES (
                CONCAT('ESTU',v_estu+(v_grado-1)*5+v_alumno-1),
                ELT(v_alumno,'Camila Isabel','Diego Andrés','Lucía Fernanda','Mateo José','Valentina Sofía'),
                ELT(v_grado,'García López','Martínez Pérez','Rodríguez Ruiz','Hernández Flores',
                    'González Castro','Ramírez Morales','Torres Mendoza','López Gutiérrez',
                    'Pérez Castillo','Sánchez Vargas','Díaz Romero'),
                CONCAT('estudiante.g',LPAD(v_grado,2,'0'),'.e',v_alumno,'@sgai-demo-v1.invalid'),
                v_grado
            );
            SET v_estudiante_id = LAST_INSERT_ID();
            SET v_curso = 1;
            WHILE v_curso <= 2 DO
                SET v_curso_id = IF(v_curso=1,v_curso_uno,v_curso_dos);
                SET v_docente_id = IF(v_curso=1,v_docente_uno,v_docente_dos);
                INSERT INTO matriculas(estudiante_id,curso_id) VALUES(v_estudiante_id,v_curso_id);
                SET v_parcial = 1;
                WHILE v_parcial <= 4 DO
                    -- Notas reproducibles entre 45.00 y 100.00, con aprobados/reprobados.
                    SET v_nota = ROUND(45 + MOD(v_grado*997 + v_alumno*389
                                              + v_curso*641 + v_parcial*457,5501)/100.0,2);
                    INSERT INTO calificaciones(estudiante_id,curso_id,docente_id,periodo_parcial,nota)
                        VALUES(v_estudiante_id,v_curso_id,v_docente_id,v_parcial,v_nota);
                    SET v_parcial = v_parcial + 1;
                END WHILE;
                SET v_curso = v_curso + 1;
            END WHILE;
            SET v_alumno = v_alumno + 1;
        END WHILE;
        SET v_grado = v_grado + 1;
    END WHILE;

    -- Comprobar el lote antes del commit, no los totales globales de la BD.
    SELECT COUNT(*) INTO v_cantidad FROM estudiantes WHERE correo LIKE '%@sgai-demo-v1.invalid';
    IF v_cantidad <> 55 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Verificación de estudiantes fallida: rollback.';
    END IF;
    SELECT COUNT(*) INTO v_cantidad FROM docentes WHERE correo LIKE '%@sgai-demo-v1.invalid';
    IF v_cantidad <> 22 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Verificación de docentes fallida: rollback.';
    END IF;
    SELECT COUNT(*) INTO v_cantidad FROM asignacion_docente_curso a
        JOIN docentes d ON d.id=a.docente_id WHERE d.correo LIKE '%@sgai-demo-v1.invalid';
    IF v_cantidad <> 22 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Verificación de asignaciones fallida: rollback.';
    END IF;
    SELECT COUNT(*) INTO v_cantidad FROM matriculas m
        JOIN estudiantes e ON e.id=m.estudiante_id JOIN cursos c ON c.id=m.curso_id
        WHERE e.correo LIKE '%@sgai-demo-v1.invalid' AND e.grado=c.grado;
    IF v_cantidad <> 110 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Verificación de matrículas/grados fallida: rollback.';
    END IF;
    SELECT COUNT(*) INTO v_cantidad FROM calificaciones n
        JOIN estudiantes e ON e.id=n.estudiante_id JOIN docentes d ON d.id=n.docente_id
        WHERE e.correo LIKE '%@sgai-demo-v1.invalid' AND d.correo LIKE '%@sgai-demo-v1.invalid'
          AND n.periodo_parcial BETWEEN 1 AND 4 AND n.nota BETWEEN 0 AND 100;
    IF v_cantidad <> 440 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Verificación de notas fallida: rollback.';
    END IF;

    COMMIT;
    DO RELEASE_LOCK('sgai_carga_demostracion_v1');
    SET v_bloqueo = 0;
    SELECT 'Carga de demostración completada' AS resultado,
           55 AS estudiantes_nuevos,22 AS cursos_nuevos,22 AS docentes_nuevos,
           22 AS asignaciones_nuevas,110 AS matriculas_nuevas,440 AS notas_nuevas,
           CONCAT('ESTU',v_estu+55) AS siguiente_estudiante,
           CONCAT('PROF',v_prof+22) AS siguiente_docente,
           CONCAT('CURS',v_curs+22) AS siguiente_curso;
END$$
DELIMITER ;

CALL sgai_cargar_demostracion_v1();
DROP PROCEDURE IF EXISTS sgai_cargar_demostracion_v1;
