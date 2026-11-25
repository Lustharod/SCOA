-- Aplicar UMA vez após V023. NULL preserva a base aprovada; não adota a versão mais recente.
CREATE TABLE versao_parametros_academicos (
    id_versao INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    numero INT NOT NULL UNIQUE,
    antecedencia_primeira INT NOT NULL,
    duracao_primeira INT NOT NULL,
    inicio_segunda INT NOT NULL,
    duracao_segunda INT NOT NULL,
    cancelamento_imediato_ate INT NOT NULL,
    cancelamento_pedido_ate INT NOT NULL,
    transferencia_ate INT NOT NULL,
    retificacao_docente_dias INT NOT NULL,
    tolerancia_atraso INT NOT NULL,
    frequencia_percentual DECIMAL(4,1) NOT NULL,
    media_dispensa_pf DECIMAL(3,1) NOT NULL,
    media_final_minima DECIMAL(3,1) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    registrado_em_utc DATETIME(6) NOT NULL,
    assinatura CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE periodo_letivo ADD COLUMN id_versao_parametros INT UNSIGNED NULL,
    ADD COLUMN versao_vinculo_parametros INT NOT NULL DEFAULT 0,
    ADD FOREIGN KEY (id_versao_parametros) REFERENCES versao_parametros_academicos(id_versao) ON DELETE RESTRICT;

CREATE TABLE historico_parametros_periodo (
    id_evento INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_periodo INT UNSIGNED NOT NULL,
    versao_vinculo INT NOT NULL,
    id_versao_anterior INT UNSIGNED NULL,
    id_versao_nova INT UNSIGNED NULL,
    motivo VARCHAR(500) NOT NULL,
    id_usuario_autor INT UNSIGNED NOT NULL,
    registrado_em_utc DATETIME(6) NOT NULL,
    UNIQUE (id_periodo,versao_vinculo),
    FOREIGN KEY (id_periodo) REFERENCES periodo_letivo(id_periodo) ON DELETE RESTRICT,
    FOREIGN KEY (id_versao_anterior) REFERENCES versao_parametros_academicos(id_versao) ON DELETE RESTRICT,
    FOREIGN KEY (id_versao_nova) REFERENCES versao_parametros_academicos(id_versao) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_autor) REFERENCES usuario(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE resultado_inscricao ADD COLUMN id_versao_parametros INT UNSIGNED NULL,
    ADD COLUMN fonte_parametros VARCHAR(30) NULL,
    ADD FOREIGN KEY (id_versao_parametros) REFERENCES versao_parametros_academicos(id_versao) ON DELETE RESTRICT;
ALTER TABLE retificacao_resultado ADD COLUMN id_versao_parametros INT UNSIGNED NULL,
    ADD COLUMN fonte_parametros VARCHAR(30) NULL,
    ADD FOREIGN KEY (id_versao_parametros) REFERENCES versao_parametros_academicos(id_versao) ON DELETE RESTRICT;
