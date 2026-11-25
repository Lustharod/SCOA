-- Aplicar UMA vez após V019, com backup e schema explicitamente selecionado.
-- Não reinterpretar transferências legadas ou criar vínculos por migração.
CREATE TABLE processo_transferencia_externa (
    id_processo INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    chave VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    assinatura_pedido CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    id_aluno INT UNSIGNED NOT NULL,
    id_curso INT UNSIGNED NOT NULL,
    id_estrutura INT UNSIGNED NULL,
    id_matricula_origem INT UNSIGNED NULL,
    id_periodo INT UNSIGNED NOT NULL,
    instituicao_externa VARCHAR(255) NOT NULL,
    documentos TEXT NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    versao INT NOT NULL DEFAULT 0,
    id_transferencia INT UNSIGNED NULL,
    id_matricula_efetivada INT UNSIGNED NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    registrado_em_utc DATETIME(6) NOT NULL,
    atualizado_em_utc DATETIME(6) NOT NULL,
    UNIQUE KEY uq_transferencia_externa_chave(chave),
    UNIQUE KEY uq_transferencia_externa_efeito(id_transferencia),
    CHECK (tipo IN ('ENTRADA','SAIDA')),
    CHECK (situacao IN ('EM_ANALISE','EFETIVADA','INDEFERIDA','CANCELADA')),
    CHECK (versao >= 0),
    CHECK ((tipo='ENTRADA' AND id_matricula_origem IS NULL AND id_estrutura IS NOT NULL)
        OR (tipo='SAIDA' AND id_matricula_origem IS NOT NULL AND id_estrutura IS NULL)),
    FOREIGN KEY (id_aluno) REFERENCES aluno(id_aluno) ON DELETE RESTRICT,
    FOREIGN KEY (id_curso) REFERENCES curso(id_curso) ON DELETE RESTRICT,
    FOREIGN KEY (id_estrutura) REFERENCES estrutura_curricular(id_estrutura) ON DELETE RESTRICT,
    FOREIGN KEY (id_matricula_origem) REFERENCES matricula_academica(id_matricula) ON DELETE RESTRICT,
    FOREIGN KEY (id_periodo) REFERENCES periodo_letivo(id_periodo) ON DELETE RESTRICT,
    FOREIGN KEY (id_transferencia) REFERENCES transferencia_academica(id_transferencia) ON DELETE RESTRICT,
    FOREIGN KEY (id_matricula_efetivada) REFERENCES matricula_academica(id_matricula) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE andamento_transferencia_externa (
    id_andamento INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_processo INT UNSIGNED NOT NULL,
    versao INT NOT NULL,
    situacao_anterior VARCHAR(20) NULL,
    situacao_nova VARCHAR(20) NOT NULL,
    documentos TEXT NOT NULL,
    observacao TEXT NOT NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    registrado_em_utc DATETIME(6) NOT NULL,
    UNIQUE KEY uq_andamento_transferencia_versao(id_processo,versao),
    FOREIGN KEY (id_processo) REFERENCES processo_transferencia_externa(id_processo) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
