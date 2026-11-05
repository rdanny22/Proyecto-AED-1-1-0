USE `sgai-soldano-db`;
INSERT INTO estudiantes(id,codigo,nombres,apellidos,correo,grado) VALUES(1,'ESTU40','María','Pérez','maria@colegio.edu.ni',7);
INSERT INTO docentes(id,codigo,nombres,apellidos,correo,especialidad) VALUES(1,'PROF7','Ana','Díaz','ana@colegio.edu.ni','Ciencias');
INSERT INTO cursos(id,codigo,nombre,grado) VALUES(1,'CURS90','Ciencias',7);
INSERT INTO asignacion_docente_curso(docente_id,curso_id) VALUES(1,1);
INSERT INTO calificaciones(estudiante_id,curso_id,docente_id,periodo,nota) VALUES(1,1,1,2,85);
