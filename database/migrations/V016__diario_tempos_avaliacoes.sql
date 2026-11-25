-- Aplicar UMA vez após V015, no schema explicitamente selecionado e com backup.
-- O legado continua sem tempo/tipo/autoria presumidos. Nenhuma carga é regravada.
ALTER TABLE aula
    ADD COLUMN tempo_letivo VARCHAR(2) NULL,
    ADD COLUMN id_usuario_autor INT UNSIGNED NULL,
    ADD COLUMN registrado_em_utc DATETIME(6) NULL,
    ADD UNIQUE KEY uq_aula_tempo (id_turma,data,tempo_letivo),
    ADD FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;
ALTER TABLE avaliacao
    ADD COLUMN tipo_prova VARCHAR(2) NULL,
    ADD COLUMN id_usuario_autor INT UNSIGNED NULL,
    ADD COLUMN registrado_em_utc DATETIME(6) NULL,
    ADD UNIQUE KEY uq_avaliacao_tipo (id_turma,tipo_prova),
    ADD FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;
ALTER TABLE nota
    ADD COLUMN versao INT UNSIGNED NOT NULL DEFAULT 0,
    ADD COLUMN desconsiderada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN id_usuario_autor INT UNSIGNED NULL,
    ADD COLUMN atualizado_em_utc DATETIME(6) NULL,
    ADD FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;
ALTER TABLE registro_frequencia
    ADD COLUMN versao INT UNSIGNED NOT NULL DEFAULT 0,
    ADD COLUMN id_usuario_autor INT UNSIGNED NULL,
    ADD COLUMN atualizado_em_utc DATETIME(6) NULL,
    ADD FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;

CREATE TABLE evento_acesso_simulado (
    origem VARCHAR(80) NOT NULL,
    chave VARCHAR(100) NOT NULL,
    id_aula INT UNSIGNED NOT NULL,
    id_inscricao INT UNSIGNED NOT NULL,
    chegada_local DATETIME(6) NOT NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    registrado_em_utc DATETIME(6) NOT NULL,
    PRIMARY KEY (origem,chave),
    FOREIGN KEY (id_aula) REFERENCES aula(id_aula) ON DELETE RESTRICT,
    FOREIGN KEY (id_inscricao) REFERENCES inscricao_disciplina(id_inscricao) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
