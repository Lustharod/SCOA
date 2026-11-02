-- Aplicar uma vez APÓS backup, seleção explícita do schema e V006.
-- Não contém USE, não cria banco, não preenche vínculos ou resultados retroativos.
-- Coordenação transacional conservadora para a interface de console.
CREATE TABLE controle_inscricao (
    id TINYINT NOT NULL PRIMARY KEY,
    CONSTRAINT ck_controle_inscricao CHECK (id=1)
) ENGINE=InnoDB;
INSERT INTO controle_inscricao(id) VALUES (1);

CREATE TABLE vinculo_curricular_matricula (
    id_matricula INT UNSIGNED NOT NULL PRIMARY KEY,
    id_estrutura INT UNSIGNED NOT NULL,
    registrado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_vinculo_matricula FOREIGN KEY (id_matricula) REFERENCES matricula_academica(id_matricula) ON DELETE RESTRICT,
    CONSTRAINT fk_vinculo_estrutura FOREIGN KEY (id_estrutura) REFERENCES estrutura_curricular(id_estrutura) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE resultado_inscricao (
    id_inscricao INT UNSIGNED NOT NULL PRIMARY KEY,
    p1 DECIMAL(3,1) NOT NULL,
    p2 DECIMAL(3,1) NOT NULL,
    pf DECIMAL(3,1) NULL,
    total_minutos BIGINT NOT NULL,
    presentes_minutos BIGINT NOT NULL,
    media_inicial DECIMAL(3,1) NOT NULL,
    media_final DECIMAL(3,1) NULL,
    situacao VARCHAR(20) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    registrado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_resultado_inscricao FOREIGN KEY (id_inscricao) REFERENCES inscricao_disciplina(id_inscricao) ON DELETE RESTRICT,
    CONSTRAINT ck_resultado_notas CHECK (p1 BETWEEN 0 AND 10 AND p2 BETWEEN 0 AND 10 AND (pf IS NULL OR pf BETWEEN 0 AND 10)),
    CONSTRAINT ck_resultado_frequencia CHECK (total_minutos>0 AND presentes_minutos BETWEEN 0 AND total_minutos),
    CONSTRAINT ck_resultado_situacao CHECK (situacao IN ('APROVADO','REPROVADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
