-- Renommages sans suppression des comptes, des fichiers ni des tags existants.
-- V1 reste identique a la version deja appliquee dans les bases de developpement.
ALTER TABLE app_users RENAME TO "user";
ALTER TABLE "user" RENAME COLUMN password_hash TO password_user;
ALTER TABLE stored_files RENAME TO file;
ALTER TABLE file RENAME COLUMN password_hash TO password_file;
