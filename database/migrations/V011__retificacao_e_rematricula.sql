-- Aplicar após V010 no schema selecionado. Sem alterações nos resultados e vínculos anteriores.
CREATE TABLE sucessao_periodo_curso (
    id_curso INT UNSIGNED NOT NULL,
    id_periodo INT UNSIGNED NOT NULL,
    id_sucessor INT UNSIGNED NOT NULL,
    PRIMARY KEY (id_curso,id_periodo),
    FOREIGN KEY (id_curso) REFERENCES curso(id_curso) ON DELETE RESTRICT,
    FOREIGN KEY (id_periodo) REFERENCES periodo_letivo(id_periodo) ON DELETE RESTRICT,
    FOREIGN KEY (id_sucessor) REFERENCES periodo_letivo(id_periodo) ON DELETE RESTRICT,
    CHECK (id_periodo<>id_sucessor)
) ENGINE=InnoDB;

CREATE TABLE retificacao_resultado (
    id_pedido INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_inscricao INT UNSIGNED NOT NULL,
    solicitante INT UNSIGNED NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    solicitado_em DATETIME(6) NOT NULL,
    anterior JSON NOT NULL,
    p1 DECIMAL(3,1) NOT NULL,
    p2 DECIMAL(3,1) NOT NULL,
    pf DECIMAL(3,1) NULL,
    total_minutos BIGINT NOT NULL,
    presentes_minutos BIGINT NOT NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    coordenador INT UNSIGNED NULL,
    parecer VARCHAR(500) NULL,
    decidido_em DATETIME(6) NULL,
    pedido_aberto INT UNSIGNED GENERATED ALWAYS AS
        (CASE WHEN situacao='PENDENTE' THEN id_inscricao ELSE NULL END) STORED,
    UNIQUE KEY uq_retificacao_aberta (pedido_aberto),
    KEY ix_retificacao_inscricao (id_inscricao),
    FOREIGN KEY (id_inscricao) REFERENCES resultado_inscricao(id_inscricao) ON DELETE RESTRICT,
    FOREIGN KEY (solicitante) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    FOREIGN KEY (coordenador) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    CHECK (p1 BETWEEN 0 AND 10 AND p2 BETWEEN 0 AND 10 AND (pf IS NULL OR pf BETWEEN 0 AND 10)),
    CHECK (total_minutos>0 AND presentes_minutos BETWEEN 0 AND total_minutos),
    CHECK (
        (situacao='PENDENTE' AND coordenador IS NULL AND parecer IS NULL AND decidido_em IS NULL)
        OR (situacao IN ('DEFERIDA','INDEFERIDA') AND coordenador IS NOT NULL AND parecer IS NOT NULL AND decidido_em IS NOT NULL)
    )
) ENGINE=InnoDB;

CREATE TABLE rematricula_academica (
    id_matricula INT UNSIGNED NOT NULL,
    id_periodo INT UNSIGNED NOT NULL,
    id_usuario INT UNSIGNED NOT NULL,
    registrado_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id_matricula,id_periodo),
    FOREIGN KEY (id_matricula) REFERENCES matricula_academica(id_matricula) ON DELETE RESTRICT,
    FOREIGN KEY (id_periodo) REFERENCES periodo_letivo(id_periodo) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB;
