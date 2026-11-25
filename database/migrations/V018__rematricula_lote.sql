-- Aplicar UMA vez após V017, com seleção explícita do schema e backup.
-- O lote renova o vínculo; as propostas nunca são inscrições nem reservas de vaga.
CREATE TABLE lote_rematricula (
    id_lote INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    chave VARCHAR(80) NOT NULL UNIQUE,
    id_curso INT UNSIGNED NOT NULL,
    id_periodo_origem INT UNSIGNED NOT NULL,
    id_periodo_destino INT UNSIGNED NOT NULL,
    selecao JSON NOT NULL,
    assinatura CHAR(64) NOT NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    registrado_em_utc DATETIME(6) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    FOREIGN KEY (id_curso) REFERENCES curso(id_curso) ON DELETE RESTRICT,
    FOREIGN KEY (id_periodo_origem) REFERENCES periodo_letivo(id_periodo) ON DELETE RESTRICT,
    FOREIGN KEY (id_periodo_destino) REFERENCES periodo_letivo(id_periodo) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE item_lote_rematricula (
    id_lote INT UNSIGNED NOT NULL,
    id_matricula INT UNSIGNED NOT NULL,
    identificacao_snapshot VARCHAR(255) NOT NULL,
    id_plano INT UNSIGNED NULL,
    situacao VARCHAR(30) NOT NULL,
    mensagem TEXT NOT NULL,
    PRIMARY KEY (id_lote,id_matricula),
    FOREIGN KEY (id_lote) REFERENCES lote_rematricula(id_lote) ON DELETE RESTRICT,
    FOREIGN KEY (id_matricula) REFERENCES matricula_academica(id_matricula) ON DELETE RESTRICT,
    FOREIGN KEY (id_plano) REFERENCES plano_institucional(id_plano) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE proposta_lote_rematricula (
    id_lote INT UNSIGNED NOT NULL,
    id_matricula INT UNSIGNED NOT NULL,
    id_disciplina INT UNSIGNED NOT NULL,
    id_turma INT UNSIGNED NULL,
    situacao VARCHAR(30) NOT NULL,
    mensagem TEXT NOT NULL,
    PRIMARY KEY (id_lote,id_matricula,id_disciplina),
    FOREIGN KEY (id_lote,id_matricula) REFERENCES item_lote_rematricula(id_lote,id_matricula) ON DELETE RESTRICT,
    FOREIGN KEY (id_disciplina) REFERENCES disciplina(id_disciplina) ON DELETE RESTRICT,
    FOREIGN KEY (id_turma) REFERENCES turma(id_turma) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
