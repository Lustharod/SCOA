-- Aplicar UMA vez após backup, seleção explícita do schema, V006 e V007.
-- Não altera nem reconhece automaticamente equivalências ou planos legados.
CREATE TABLE pedido_equivalencia (
    id_pedido INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_matricula INT UNSIGNED NOT NULL,
    id_estrutura INT UNSIGNED NOT NULL,
    id_disciplina_destino INT UNSIGNED NOT NULL,
    instituicao_origem VARCHAR(255) NOT NULL,
    disciplina_origem VARCHAR(255) NOT NULL,
    carga_horaria_origem INT NOT NULL,
    ementa_origem TEXT NOT NULL,
    documentos VARCHAR(1000) NOT NULL,
    conclusao_origem DATE NOT NULL,
    ementa_destino TEXT NOT NULL,
    carga_horaria_destino INT NOT NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'EM_ANALISE',
    parecer VARCHAR(2000) NULL,
    responsavel VARCHAR(255) NULL,
    registrado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    decidido_em DATETIME(6) NULL,
    destino_em_uso INT UNSIGNED GENERATED ALWAYS AS
        (CASE WHEN situacao IN ('EM_ANALISE','DEFERIDO') THEN id_disciplina_destino ELSE NULL END) STORED,
    CONSTRAINT uq_pedido_destino UNIQUE (id_matricula,destino_em_uso),
    CONSTRAINT fk_pedido_matricula FOREIGN KEY (id_matricula) REFERENCES matricula_academica(id_matricula) ON DELETE RESTRICT,
    CONSTRAINT fk_pedido_estrutura FOREIGN KEY (id_estrutura) REFERENCES estrutura_curricular(id_estrutura) ON DELETE RESTRICT,
    CONSTRAINT fk_pedido_destino FOREIGN KEY (id_disciplina_destino) REFERENCES disciplina(id_disciplina) ON DELETE RESTRICT,
    CONSTRAINT ck_pedido_cargas CHECK (carga_horaria_origem>0 AND carga_horaria_destino>0),
    CONSTRAINT ck_pedido_decisao CHECK (
        (situacao='EM_ANALISE' AND parecer IS NULL AND responsavel IS NULL AND decidido_em IS NULL)
        OR (situacao IN ('DEFERIDO','INDEFERIDO') AND parecer IS NOT NULL AND responsavel IS NOT NULL AND decidido_em IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
