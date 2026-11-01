-- Aplicar uma vez, após backup, sobre o DDL v7.5 existente.
-- Não recria tabelas nem atribui períodos ou eventos a registros históricos.
ALTER TABLE matricula_academica
    ADD COLUMN id_periodo_ingresso INT UNSIGNED NULL,
    ADD CONSTRAINT fk_matricula_periodo_ingresso FOREIGN KEY (id_periodo_ingresso)
        REFERENCES periodo_letivo(id_periodo) ON UPDATE RESTRICT ON DELETE RESTRICT,
    ADD INDEX idx_matricula_aluno_curso (id_aluno,id_curso);

CREATE TABLE historico_matricula (
    id_evento BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_matricula INT UNSIGNED NOT NULL,
    ocorrido_em TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    operacao VARCHAR(20) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    situacao_anterior VARCHAR(20) NULL,
    situacao_nova VARCHAR(20) NOT NULL,
    data_anterior DATE NULL,
    data_nova DATE NOT NULL,
    periodo_anterior INT UNSIGNED NULL,
    periodo_novo INT UNSIGNED NULL,
    CONSTRAINT fk_historico_matricula FOREIGN KEY (id_matricula)
        REFERENCES matricula_academica(id_matricula) ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_historico_matricula (id_matricula,id_evento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
