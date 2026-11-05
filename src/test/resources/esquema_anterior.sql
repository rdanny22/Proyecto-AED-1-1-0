-- MySQL 8.0.16+; ejecutar con una cuenta de migraciones, no con la aplicación.
CREATE DATABASE IF NOT EXISTS `sgai-soldano-db` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `sgai-soldano-db`;
CREATE TABLE estudiantes (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 codigo VARCHAR(24) NOT NULL UNIQUE,
 nombres VARCHAR(80) NOT NULL,
 apellidos VARCHAR(80) NOT NULL,
 correo VARCHAR(150) NOT NULL,
 grado INT NOT NULL CHECK (grado BETWEEN 1 AND 11)
) ENGINE=InnoDB;
CREATE TABLE docentes (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 codigo VARCHAR(24) NOT NULL UNIQUE,
 nombres VARCHAR(80) NOT NULL,
 apellidos VARCHAR(80) NOT NULL,
 correo VARCHAR(150) NOT NULL,
 especialidad VARCHAR(80) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE cursos (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 codigo VARCHAR(24) NOT NULL UNIQUE,
 nombre VARCHAR(80) NOT NULL,
 grado INT NOT NULL CHECK (grado BETWEEN 1 AND 11)
) ENGINE=InnoDB;
CREATE TABLE usuarios (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 nombre VARCHAR(40) NOT NULL UNIQUE,
 clave_hash VARCHAR(255) NOT NULL,
 rol VARCHAR(20) NOT NULL,
 estudiante_id BIGINT UNIQUE,
 docente_id BIGINT UNIQUE,
 FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id) ON DELETE RESTRICT,
 FOREIGN KEY (docente_id) REFERENCES docentes(id) ON DELETE RESTRICT,
 CHECK ((rol='ADMINISTRADOR' AND estudiante_id IS NULL AND docente_id IS NULL)
 OR (rol='ESTUDIANTE' AND estudiante_id IS NOT NULL AND docente_id IS NULL)
 OR (rol='DOCENTE' AND docente_id IS NOT NULL AND estudiante_id IS NULL))
) ENGINE=InnoDB;
CREATE TABLE asignacion_docente_curso (
 docente_id BIGINT NOT NULL,
 curso_id BIGINT NOT NULL,
 PRIMARY KEY (docente_id, curso_id),
 FOREIGN KEY (docente_id) REFERENCES docentes(id) ON DELETE RESTRICT,
 FOREIGN KEY (curso_id) REFERENCES cursos(id) ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE TABLE calificaciones (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 estudiante_id BIGINT NOT NULL,
 curso_id BIGINT NOT NULL,
 docente_id BIGINT NOT NULL,
 periodo INT NOT NULL CHECK (periodo BETWEEN 1 AND 4),
 nota DECIMAL(5,2) NOT NULL CHECK (nota BETWEEN 0 AND 100),
 UNIQUE (estudiante_id, curso_id, periodo),
 FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id) ON DELETE RESTRICT,
 FOREIGN KEY (docente_id, curso_id) REFERENCES asignacion_docente_curso(docente_id, curso_id) ON DELETE RESTRICT,
 INDEX (curso_id)
) ENGINE=InnoDB;
