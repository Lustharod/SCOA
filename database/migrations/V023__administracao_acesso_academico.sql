-- Aplicar UMA vez após V022; preserva contas, credenciais, IDs e concessões existentes.
ALTER TABLE usuario ADD COLUMN versao_acesso INT NOT NULL DEFAULT 0,
    ADD COLUMN geracao_sessao INT NOT NULL DEFAULT 0;
ALTER TABLE perfil ADD COLUMN versao_acesso INT NOT NULL DEFAULT 0;

CREATE TABLE historico_acesso_academico (
    id_evento INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_usuario_alvo INT UNSIGNED NULL,
    id_perfil INT UNSIGNED NULL,
    operacao VARCHAR(30) NOT NULL,
    versao_referencia INT NOT NULL,
    anterior LONGTEXT NULL,
    novo LONGTEXT NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    registrado_em_utc DATETIME(6) NOT NULL,
    CHECK (id_usuario_alvo IS NOT NULL OR id_perfil IS NOT NULL),
    FOREIGN KEY (id_usuario_alvo) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    FOREIGN KEY (id_perfil) REFERENCES perfil(id_perfil) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
