-- Aplicar UMA vez após V013, com backup e seleção explícita do schema.
-- Campos novos são nulos no legado: nenhum destino ou autor histórico é presumido.
ALTER TABLE transferencia_academica
    ADD COLUMN id_matricula_destino INT UNSIGNED NULL,
    ADD COLUMN id_curso_destino INT UNSIGNED NULL,
    ADD COLUMN id_estrutura_destino INT UNSIGNED NULL,
    ADD COLUMN id_periodo_transferencia INT UNSIGNED NULL,
    ADD COLUMN id_usuario_autor INT UNSIGNED NULL,
    ADD COLUMN motivo VARCHAR(500) NULL,
    ADD COLUMN registrado_em DATETIME(6) NULL,
    ADD UNIQUE KEY uq_transferencia_destino (id_matricula_destino),
    ADD FOREIGN KEY (id_matricula_destino) REFERENCES matricula_academica(id_matricula) ON DELETE RESTRICT,
    ADD FOREIGN KEY (id_curso_destino) REFERENCES curso(id_curso) ON DELETE RESTRICT,
    ADD FOREIGN KEY (id_estrutura_destino) REFERENCES estrutura_curricular(id_estrutura) ON DELETE RESTRICT,
    ADD FOREIGN KEY (id_periodo_transferencia) REFERENCES periodo_letivo(id_periodo) ON DELETE RESTRICT,
    ADD FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;

CREATE TABLE aproveitamento_transferencia (
    id_transferencia INT UNSIGNED NOT NULL,
    id_pedido INT UNSIGNED NOT NULL,
    id_inscricao_origem INT UNSIGNED NOT NULL,
    id_disciplina INT UNSIGNED NOT NULL,
    resultado_origem JSON NOT NULL,
    PRIMARY KEY (id_transferencia,id_disciplina),
    UNIQUE KEY uq_aproveitamento_transferencia_pedido (id_pedido),
    KEY ix_transferencia_resultado_origem (id_inscricao_origem),
    FOREIGN KEY (id_transferencia) REFERENCES transferencia_academica(id_transferencia) ON DELETE RESTRICT,
    FOREIGN KEY (id_pedido) REFERENCES pedido_equivalencia(id_pedido) ON DELETE RESTRICT,
    FOREIGN KEY (id_inscricao_origem) REFERENCES resultado_inscricao(id_inscricao) ON DELETE RESTRICT,
    FOREIGN KEY (id_disciplina) REFERENCES disciplina(id_disciplina) ON DELETE RESTRICT
) ENGINE=InnoDB;
