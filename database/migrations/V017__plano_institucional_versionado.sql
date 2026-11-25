-- Aplicar UMA vez após V016, no schema explicitamente selecionado e com backup.
-- Planos legados e suas conciliações permanecem intactos. Versões novas são imutáveis.
CREATE TABLE plano_institucional (
    id_plano INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_matricula INT UNSIGNED NOT NULL,
    id_estrutura INT UNSIGNED NOT NULL,
    versao INT UNSIGNED NOT NULL,
    id_anterior INT UNSIGNED NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    registrado_em_utc DATETIME(6) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    assinatura CHAR(64) NOT NULL,
    periodos JSON NOT NULL,
    prioridades JSON NOT NULL,
    UNIQUE KEY uq_plano_institucional_versao (id_matricula,versao),
    FOREIGN KEY (id_matricula) REFERENCES matricula_academica(id_matricula) ON DELETE RESTRICT,
    FOREIGN KEY (id_estrutura) REFERENCES estrutura_curricular(id_estrutura) ON DELETE RESTRICT,
    FOREIGN KEY (id_anterior) REFERENCES plano_institucional(id_plano) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE componente_plano_institucional (
    id_plano INT UNSIGNED NOT NULL,
    ordem INT UNSIGNED NOT NULL,
    id_disciplina INT UNSIGNED NOT NULL,
    codigo_snapshot VARCHAR(255) NOT NULL,
    nome_snapshot VARCHAR(255) NOT NULL,
    horas_snapshot INT UNSIGNED NOT NULL,
    cadastro_snapshot VARCHAR(20) NOT NULL,
    situacao_snapshot VARCHAR(30) NOT NULL,
    evidencia_snapshot TEXT NOT NULL,
    requisitos_snapshot JSON NOT NULL,
    id_turma_sugerida INT UNSIGNED NULL,
    id_periodo_sugerido INT UNSIGNED NULL,
    periodo_snapshot VARCHAR(255) NULL,
    inicio_snapshot DATE NULL,
    fim_snapshot DATE NULL,
    diagnostico TEXT NOT NULL,
    PRIMARY KEY (id_plano,id_disciplina),
    UNIQUE KEY uq_componente_plano_ordem (id_plano,ordem),
    FOREIGN KEY (id_plano) REFERENCES plano_institucional(id_plano) ON DELETE RESTRICT,
    FOREIGN KEY (id_disciplina) REFERENCES disciplina(id_disciplina) ON DELETE RESTRICT,
    FOREIGN KEY (id_turma_sugerida) REFERENCES turma(id_turma) ON DELETE RESTRICT,
    FOREIGN KEY (id_periodo_sugerido) REFERENCES periodo_letivo(id_periodo) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
