-- Aplicar UMA vez após V014, apenas no schema explicitamente selecionado e com backup.
-- Solicitações legadas preservam tipo/situação e autoria desconhecida.
ALTER TABLE solicitacao_documento
    ADD COLUMN id_usuario_solicitante INT UNSIGNED NULL,
    ADD COLUMN solicitado_em_utc DATETIME(6) NULL,
    ADD COLUMN versao INT UNSIGNED NOT NULL DEFAULT 0,
    ADD COLUMN id_usuario_atendente INT UNSIGNED NULL,
    ADD COLUMN atualizado_em_utc DATETIME(6) NULL,
    ADD COLUMN parecer TEXT NULL,
    ADD COLUMN referencia_entrega VARCHAR(500) NULL,
    ADD CONSTRAINT fk_documento_solicitante FOREIGN KEY (id_usuario_solicitante)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_documento_atendente FOREIGN KEY (id_usuario_atendente)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    ADD INDEX ix_documento_matricula (id_matricula,id_solicitacao_documento);

CREATE TABLE andamento_documento (
    id_andamento BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_solicitacao_documento INT UNSIGNED NOT NULL,
    versao INT UNSIGNED NOT NULL,
    situacao_anterior VARCHAR(20) NULL,
    situacao_nova VARCHAR(20) NOT NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    ocorrido_em_utc DATETIME(6) NOT NULL,
    parecer TEXT NULL,
    referencia_entrega VARCHAR(500) NULL,
    CONSTRAINT uq_documento_andamento UNIQUE (id_solicitacao_documento,versao),
    CONSTRAINT fk_andamento_documento FOREIGN KEY (id_solicitacao_documento)
        REFERENCES solicitacao_documento(id_solicitacao_documento) ON DELETE RESTRICT,
    CONSTRAINT fk_andamento_documento_autor FOREIGN KEY (id_usuario_autor)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
