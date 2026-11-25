-- Aplicar UMA vez após V018, no schema explicitamente selecionado e com backup.
-- Gestão é vínculo acadêmico institucional explícito; não concede permissões técnicas.
CREATE TABLE gestao_academica (
    id_gestor INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT UNSIGNED NOT NULL UNIQUE,
    situacao VARCHAR(20) NOT NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    registrado_em_utc DATETIME(6) NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    CHECK (situacao IN ('ATIVO','INATIVO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE prazo_meta_docente
    ADD COLUMN id_curso INT UNSIGNED NULL,
    ADD COLUMN tipo VARCHAR(20) NULL,
    ADD COLUMN versao INT UNSIGNED NOT NULL DEFAULT 0,
    ADD COLUMN id_usuario_autor INT UNSIGNED NULL,
    ADD COLUMN registrado_em_utc DATETIME(6) NULL,
    ADD COLUMN atualizado_em_utc DATETIME(6) NULL,
    ADD FOREIGN KEY (id_curso) REFERENCES curso(id_curso) ON DELETE RESTRICT,
    ADD FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;

CREATE TABLE historico_prazo_meta (
    id_historico BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_prazo_meta INT UNSIGNED NOT NULL,
    versao INT UNSIGNED NOT NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    registrado_em_utc DATETIME(6) NOT NULL,
    anterior LONGTEXT NULL,
    posterior LONGTEXT NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    UNIQUE KEY uq_meta_historico (id_prazo_meta,versao),
    FOREIGN KEY (id_prazo_meta) REFERENCES prazo_meta_docente(id_prazo_meta) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE auditoria_diario
    ADD COLUMN id_curso INT UNSIGNED NULL,
    ADD COLUMN conclusao VARCHAR(30) NULL,
    ADD COLUMN assinatura CHAR(64) NULL,
    ADD COLUMN dados_avaliados LONGTEXT NULL,
    ADD FOREIGN KEY (id_curso) REFERENCES curso(id_curso) ON DELETE RESTRICT;
