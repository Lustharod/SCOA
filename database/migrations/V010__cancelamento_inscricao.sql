-- Aplicar após V009 no schema selecionado. Não atribui coordenação nem altera cancelamentos anteriores.
CREATE TABLE coordenacao_curso (
    id_curso INT UNSIGNED NOT NULL,
    id_usuario INT UNSIGNED NOT NULL,
    PRIMARY KEY (id_curso,id_usuario),
    FOREIGN KEY (id_curso) REFERENCES curso(id_curso) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB;

ALTER TABLE inscricao_disciplina
    ADD COLUMN vaga_retida BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_inscricao_vaga_retida CHECK (vaga_retida IN (0,1) AND (NOT vaga_retida OR situacao='CANCELADO'));

CREATE TABLE cancelamento_inscricao (
    id_pedido INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_inscricao INT UNSIGNED NOT NULL,
    solicitante INT UNSIGNED NOT NULL,
    justificativa VARCHAR(500) NOT NULL,
    solicitado_em DATETIME(6) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    coordenador INT UNSIGNED NULL,
    parecer VARCHAR(500) NULL,
    decidido_em DATETIME(6) NULL,
    pedido_aberto INT UNSIGNED GENERATED ALWAYS AS
        (CASE WHEN situacao='PENDENTE' THEN id_inscricao ELSE NULL END) STORED,
    UNIQUE KEY uq_cancelamento_aberto (pedido_aberto),
    KEY ix_cancelamento_inscricao (id_inscricao),
    FOREIGN KEY (id_inscricao) REFERENCES inscricao_disciplina(id_inscricao) ON DELETE RESTRICT,
    FOREIGN KEY (solicitante) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    FOREIGN KEY (coordenador) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    CHECK (
        (situacao='PENDENTE' AND coordenador IS NULL AND parecer IS NULL AND decidido_em IS NULL)
        OR (situacao='AUTOMATICO' AND coordenador IS NULL AND parecer IS NULL AND decidido_em IS NOT NULL)
        OR (situacao IN ('DEFERIDO','INDEFERIDO') AND coordenador IS NOT NULL AND parecer IS NOT NULL AND decidido_em IS NOT NULL)
    )
) ENGINE=InnoDB;
