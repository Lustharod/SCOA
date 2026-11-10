-- Aplicar UMA vez após backup, seleção explícita do schema e V008.
-- Aditiva: preserva decisões e registros legados. Não cria usuários ou concessões.
CREATE TABLE revisao_reconhecimento (
 id_revisao INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 id_pedido INT UNSIGNED NOT NULL,
 solicitante INT UNSIGNED NOT NULL,
 justificativa VARCHAR(2000) NOT NULL,
 situacao VARCHAR(30) NOT NULL DEFAULT 'ABERTA',
 parecer VARCHAR(2000) NULL,
 analista INT UNSIGNED NULL,
 registrado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 atualizado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 revisao_aberta INT UNSIGNED GENERATED ALWAYS AS
 (CASE WHEN situacao IN ('ABERTA','COMPLEMENTACAO','TRATAMENTO') THEN id_pedido ELSE NULL END) STORED,
 UNIQUE KEY uq_revisao_aberta(revisao_aberta),
 FOREIGN KEY(id_pedido) REFERENCES pedido_equivalencia(id_pedido) ON DELETE RESTRICT,
 FOREIGN KEY(solicitante) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
 FOREIGN KEY(analista) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
 CHECK(situacao IN ('ABERTA','COMPLEMENTACAO','TRATAMENTO','REJEITADA','REVOGADA'))
) ENGINE=InnoDB;

CREATE TABLE evidencia_requisito_inscricao (
 id_inscricao INT UNSIGNED NOT NULL,
 id_requisito INT UNSIGNED NOT NULL,
 tipo VARCHAR(20) NOT NULL,
 id_pedido INT UNSIGNED NULL,
 id_resultado INT UNSIGNED NULL,
 registrado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(id_inscricao,id_requisito),
 FOREIGN KEY(id_inscricao) REFERENCES inscricao_disciplina(id_inscricao) ON DELETE RESTRICT,
 FOREIGN KEY(id_requisito) REFERENCES disciplina(id_disciplina) ON DELETE RESTRICT,
 FOREIGN KEY(id_pedido) REFERENCES pedido_equivalencia(id_pedido) ON DELETE RESTRICT,
 FOREIGN KEY(id_resultado) REFERENCES resultado_inscricao(id_inscricao) ON DELETE RESTRICT,
 CHECK((tipo='RECONHECIMENTO' AND id_pedido IS NOT NULL AND id_resultado IS NULL)
 OR (tipo='RESULTADO' AND id_resultado IS NOT NULL AND id_pedido IS NULL))
) ENGINE=InnoDB;

CREATE TABLE conciliacao_legado (
 id_conciliacao INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 tipo VARCHAR(20) NOT NULL,
 id_origem INT UNSIGNED NOT NULL,
 id_matricula INT UNSIGNED NOT NULL,
 id_estrutura INT UNSIGNED NOT NULL,
 origem_hash CHAR(64) NOT NULL,
 documentos VARCHAR(1000) NOT NULL,
 solicitante INT UNSIGNED NOT NULL,
 situacao VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
 analista INT UNSIGNED NULL,
 parecer VARCHAR(2000) NULL,
 id_pedido INT UNSIGNED NULL,
 registrado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 decidido_em DATETIME(6) NULL,
 origem_em_uso INT UNSIGNED GENERATED ALWAYS AS
 (CASE WHEN situacao IN ('ABERTA','APROVADA') THEN id_origem ELSE NULL END) STORED,
 UNIQUE KEY uq_conciliacao_origem(tipo,origem_em_uso),
 FOREIGN KEY(id_matricula) REFERENCES matricula_academica(id_matricula) ON DELETE RESTRICT,
 FOREIGN KEY(id_estrutura) REFERENCES estrutura_curricular(id_estrutura) ON DELETE RESTRICT,
 FOREIGN KEY(solicitante) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
 FOREIGN KEY(analista) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
 FOREIGN KEY(id_pedido) REFERENCES pedido_equivalencia(id_pedido) ON DELETE RESTRICT,
 CHECK(tipo IN ('PLANO','EQUIVALENCIA')),
 CHECK(situacao IN ('ABERTA','APROVADA','REJEITADA'))
) ENGINE=InnoDB;

CREATE TABLE evento_processo_academico (
 id_evento BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 tipo VARCHAR(30) NOT NULL,
 id_processo INT UNSIGNED NOT NULL,
 id_usuario INT UNSIGNED NOT NULL,
 acao VARCHAR(30) NOT NULL,
 descricao TEXT NOT NULL,
 registrado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE inscricao_preservada_revisao (
 id_revisao INT UNSIGNED NOT NULL,
 id_inscricao INT UNSIGNED NOT NULL,
 fundamento VARCHAR(30) NOT NULL,
 situacao_observada VARCHAR(20) NOT NULL,
 PRIMARY KEY(id_revisao,id_inscricao),
 FOREIGN KEY(id_revisao) REFERENCES revisao_reconhecimento(id_revisao) ON DELETE RESTRICT,
 FOREIGN KEY(id_inscricao) REFERENCES inscricao_disciplina(id_inscricao) ON DELETE RESTRICT,
 CHECK(fundamento IN ('COMPROVADO','POTENCIAL_HISTORICO'))
) ENGINE=InnoDB;
