-- 1 Creando usuario exclusivo para la aplicacion
CREATE USER 'sgai_app'@'localhost'
IDENTIFIED BY 'M1ll@ve';

-- 2 Otorgando los permisos
GRANT SELECT, INSERT, UPDATE, DELETE
ON `sgai-soldano-db`.*
TO 'sgai_app'@'localhost';

-- 3 Verificar
SHOW GRANTS FOR 'sgai_app'@'localhost';
