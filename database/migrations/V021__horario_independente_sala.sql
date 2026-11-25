-- Aplicar UMA vez após V020. IDs e associações existentes são preservados.
ALTER TABLE horario_aula
    MODIFY COLUMN id_sala INT UNSIGNED NULL,
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN versao INT NOT NULL DEFAULT 0,
    ADD COLUMN id_usuario_autor INT UNSIGNED NULL,
    ADD COLUMN atualizado_em_utc DATETIME(6) NULL,
    ADD KEY ix_horario_turma_ativo(id_turma,ativo),
    ADD FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;

CREATE TABLE historico_horario_academico (
    id_evento INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_horario INT UNSIGNED NOT NULL,
    versao INT NOT NULL,
    operacao VARCHAR(30) NOT NULL,
    anterior TEXT NULL,
    novo TEXT NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    registrado_em_utc DATETIME(6) NOT NULL,
    UNIQUE KEY uq_historico_horario_versao(id_horario,versao),
    FOREIGN KEY (id_horario) REFERENCES horario_aula(id_horario) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
