-- ============================================================================
-- SGAI · Colegio Antonio Soldano · Esquema completo para una instalación NUEVA
-- Requiere MySQL Server 8.0.16 o posterior; recomendado MySQL 8.4.
-- Ejecutar el archivo completo con una cuenta administradora/de migraciones.
--
-- Incluye matrícula, períodos parciales y secuencias de códigos automáticos.
-- NO ejecutar la migración 002 después de este archivo: sus cambios ya están aquí.
-- NO utilizar sobre una instalación existente ni como script de actualización.
-- No contiene DROP, usuarios MySQL, contraseñas ni cuentas escolares de ejemplo.
-- Si ya existen tablas, CREATE TABLE fallará para evitar ocultar otro esquema.
-- MySQL realiza commits implícitos de DDL; revisar errores antes de reintentar.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `sgai-soldano-db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `sgai-soldano-db`;

-- 1. Estudiantes. La aplicación asigna ESTU1, ESTU2, ... al guardar una alta.
CREATE TABLE estudiantes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(24) NOT NULL UNIQUE,
    nombres VARCHAR(80) NOT NULL,
    apellidos VARCHAR(80) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    grado INT NOT NULL CHECK (grado BETWEEN 1 AND 11)
) ENGINE = InnoDB;

-- 2. Docentes. La aplicación asigna PROF1, PROF2, ... al guardar una alta.
CREATE TABLE docentes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(24) NOT NULL UNIQUE,
    nombres VARCHAR(80) NOT NULL,
    apellidos VARCHAR(80) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    especialidad VARCHAR(80) NOT NULL
) ENGINE = InnoDB;

-- 3. Cursos. La aplicación asigna CURS1, CURS2, ... al guardar una alta.
CREATE TABLE cursos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(24) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    grado INT NOT NULL CHECK (grado BETWEEN 1 AND 11)
) ENGINE = InnoDB;

-- 4. Cuentas escolares. Los hashes se generan con PBKDF2 en la aplicación.
-- Administrador sin persona asociada; estudiante/docente con su persona única.
CREATE TABLE usuarios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(40) NOT NULL UNIQUE,
    clave_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    estudiante_id BIGINT UNIQUE,
    docente_id BIGINT UNIQUE,
    FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id) ON DELETE RESTRICT,
    FOREIGN KEY (docente_id) REFERENCES docentes(id) ON DELETE RESTRICT,
    CHECK (
        (rol = 'ADMINISTRADOR' AND estudiante_id IS NULL AND docente_id IS NULL)
        OR (rol = 'ESTUDIANTE' AND estudiante_id IS NOT NULL AND docente_id IS NULL)
        OR (rol = 'DOCENTE' AND estudiante_id IS NULL AND docente_id IS NOT NULL)
    )
) ENGINE = InnoDB;

-- 5. Asignaciones de docentes a cursos: una fila por relación, sin duplicados.
CREATE TABLE asignacion_docente_curso (
    docente_id BIGINT NOT NULL,
    curso_id BIGINT NOT NULL,
    PRIMARY KEY (docente_id, curso_id),
    FOREIGN KEY (docente_id) REFERENCES docentes(id) ON DELETE RESTRICT,
    FOREIGN KEY (curso_id) REFERENCES cursos(id) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- 6. Matrículas: una fila por estudiante/curso.
-- El controlador comprueba transaccionalmente que ambos tengan el mismo grado.
CREATE TABLE matriculas (
    estudiante_id BIGINT NOT NULL,
    curso_id BIGINT NOT NULL,
    PRIMARY KEY (estudiante_id, curso_id),
    FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id) ON DELETE RESTRICT,
    FOREIGN KEY (curso_id) REFERENCES cursos(id) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- 7. Calificaciones: cuatro períodos parciales, notas entre 0 y 100.
-- Una sola nota por estudiante/curso/período parcial.
-- Las FK compuestas exigen matrícula y docente asignado al curso.
CREATE TABLE calificaciones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    estudiante_id BIGINT NOT NULL,
    curso_id BIGINT NOT NULL,
    docente_id BIGINT NOT NULL,
    periodo_parcial INT NOT NULL CHECK (periodo_parcial BETWEEN 1 AND 4),
    nota DECIMAL(5,2) NOT NULL CHECK (nota BETWEEN 0 AND 100),
    UNIQUE (estudiante_id, curso_id, periodo_parcial),
    FOREIGN KEY (estudiante_id, curso_id)
        REFERENCES matriculas(estudiante_id, curso_id) ON DELETE RESTRICT,
    FOREIGN KEY (docente_id, curso_id)
        REFERENCES asignacion_docente_curso(docente_id, curso_id) ON DELETE RESTRICT,
    INDEX (curso_id)
) ENGINE = InnoDB;

-- 8. Contadores de códigos. La aplicación reserva el siguiente número mediante
-- SELECT ... FOR UPDATE y lo incrementa en la misma transacción que la alta.
-- No son triggers ni columnas AUTO_INCREMENT de los códigos de negocio.
CREATE TABLE secuencias_codigo (
    tipo VARCHAR(4) PRIMARY KEY,
    siguiente BIGINT NOT NULL CHECK (siguiente > 0)
) ENGINE = InnoDB;

-- Únicos datos iniciales: cada tipo de código comienza por 1.
INSERT INTO secuencias_codigo (tipo, siguiente) VALUES
    ('ESTU', 1),
    ('PROF', 1),
    ('CURS', 1);

-- Tras aplicar el esquema:
-- 1. Crear una cuenta MySQL exclusiva con SELECT, INSERT, UPDATE y DELETE
--    sobre `sgai-soldano-db`.*; no ejecutar la aplicación como root.
-- 2. Configurar SGAI_DB_USUARIO, SGAI_DB_CLAVE y, si procede, SGAI_DB_URL.
-- 3. Crear el primer administrador desde com.sgai.Instalacion con su autorización.
-- 4. Usar com.sgai.Aplicacion para el acceso habitual.
