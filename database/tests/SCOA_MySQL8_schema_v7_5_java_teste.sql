-- Cópia do DDL v7.5 para o schema exclusivo dos testes Java.
-- Executar com scoa_java_teste inicialmente vazio. Sem DROP ou migrações.

-- ============================================================
-- SCOA - Sistema de Controle Acadêmico
-- Script de criação do banco - MySQL 8.0.16 ou superior na série 8.0
-- Baseado no Modelo Físico v7
-- ============================================================

CREATE DATABASE IF NOT EXISTS scoa_java_teste
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE scoa_java_teste;
SET NAMES utf8mb4;

-- ============================================================
-- 1. ESTRUTURAS GERAIS E TRANSVERSAIS
-- ============================================================

CREATE TABLE usuario (
    id_usuario INT UNSIGNED AUTO_INCREMENT,
    identificacao VARCHAR(100) NOT NULL,
    credencial_hash VARCHAR(255) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE perfil (
    id_perfil INT UNSIGNED AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_perfil PRIMARY KEY (id_perfil)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE permissao (
    id_permissao INT UNSIGNED AUTO_INCREMENT,
    codigo VARCHAR(100) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    CONSTRAINT pk_permissao PRIMARY KEY (id_permissao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE setor (
    id_setor INT UNSIGNED AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_setor PRIMARY KEY (id_setor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE usuario_perfil (
    id_usuario INT UNSIGNED NOT NULL,
    id_perfil INT UNSIGNED NOT NULL,
    CONSTRAINT pk_usuario_perfil PRIMARY KEY (id_usuario, id_perfil),
    CONSTRAINT fk_usuario_perfil_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_usuario_perfil_perfil FOREIGN KEY (id_perfil)
        REFERENCES perfil(id_perfil) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE perfil_permissao (
    id_perfil INT UNSIGNED NOT NULL,
    id_permissao INT UNSIGNED NOT NULL,
    CONSTRAINT pk_perfil_permissao PRIMARY KEY (id_perfil, id_permissao),
    CONSTRAINT fk_perfil_permissao_perfil FOREIGN KEY (id_perfil)
        REFERENCES perfil(id_perfil) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_perfil_permissao_permissao FOREIGN KEY (id_permissao)
        REFERENCES permissao(id_permissao) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE auditoria (
    id_auditoria INT UNSIGNED AUTO_INCREMENT,
    id_usuario INT UNSIGNED NOT NULL,
    modulo VARCHAR(50) NOT NULL,
    entidade VARCHAR(80) NOT NULL,
    id_registro INT UNSIGNED NOT NULL,
    acao VARCHAR(50) NOT NULL,
    data_hora DATETIME NOT NULL,
    detalhes TEXT NOT NULL,
    CONSTRAINT pk_auditoria PRIMARY KEY (id_auditoria),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_auditoria_modulo_entidade (modulo, entidade),
    INDEX idx_auditoria_data_hora (data_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE parametro_sistema (
    id_parametro INT UNSIGNED AUTO_INCREMENT,
    chave VARCHAR(100) NOT NULL,
    valor VARCHAR(500) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    modulo VARCHAR(50) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_parametro_sistema PRIMARY KEY (id_parametro),
    CONSTRAINT uq_parametro_modulo_chave UNIQUE (modulo, chave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 2. MÓDULO ACADÊMICO
-- ============================================================

CREATE TABLE aluno (
    id_aluno INT UNSIGNED AUTO_INCREMENT,
    id_usuario INT UNSIGNED NULL,
    identificacao VARCHAR(100) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_aluno PRIMARY KEY (id_aluno),
    CONSTRAINT uq_aluno_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_aluno_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE professor (
    id_professor INT UNSIGNED AUTO_INCREMENT,
    id_usuario INT UNSIGNED NULL,
    identificacao VARCHAR(100) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_professor PRIMARY KEY (id_professor),
    CONSTRAINT uq_professor_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_professor_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE curso (
    id_curso INT UNSIGNED AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_curso PRIMARY KEY (id_curso)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE disciplina (
    id_disciplina INT UNSIGNED AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    carga_horaria SMALLINT UNSIGNED NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_disciplina PRIMARY KEY (id_disciplina),
    CONSTRAINT ck_disciplina_carga_horaria CHECK (carga_horaria > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE estrutura_curricular (
    id_estrutura INT UNSIGNED AUTO_INCREMENT,
    id_curso INT UNSIGNED NOT NULL,
    identificacao VARCHAR(100) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_estrutura_curricular PRIMARY KEY (id_estrutura),
    CONSTRAINT fk_estrutura_curricular_curso FOREIGN KEY (id_curso)
        REFERENCES curso(id_curso) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE estrutura_disciplina (
    id_estrutura INT UNSIGNED NOT NULL,
    id_disciplina INT UNSIGNED NOT NULL,
    CONSTRAINT pk_estrutura_disciplina PRIMARY KEY (id_estrutura, id_disciplina),
    CONSTRAINT fk_estrutura_disciplina_estrutura FOREIGN KEY (id_estrutura)
        REFERENCES estrutura_curricular(id_estrutura) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_estrutura_disciplina_disciplina FOREIGN KEY (id_disciplina)
        REFERENCES disciplina(id_disciplina) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE disciplina_prerequisito (
    id_disciplina INT UNSIGNED NOT NULL,
    id_disciplina_prerequisito INT UNSIGNED NOT NULL,
    CONSTRAINT pk_disciplina_prerequisito PRIMARY KEY (id_disciplina, id_disciplina_prerequisito),
    CONSTRAINT fk_disciplina_prerequisito_disciplina FOREIGN KEY (id_disciplina)
        REFERENCES disciplina(id_disciplina) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_disciplina_prerequisito_prereq FOREIGN KEY (id_disciplina_prerequisito)
        REFERENCES disciplina(id_disciplina) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_disciplina_prerequisito_distinto CHECK (id_disciplina <> id_disciplina_prerequisito)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE periodo_letivo (
    id_periodo INT UNSIGNED AUTO_INCREMENT,
    descricao VARCHAR(50) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_periodo_letivo PRIMARY KEY (id_periodo),
    CONSTRAINT ck_periodo_datas CHECK (data_fim >= data_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sala (
    id_sala INT UNSIGNED AUTO_INCREMENT,
    identificacao VARCHAR(50) NOT NULL,
    localizacao VARCHAR(150) NOT NULL,
    capacidade SMALLINT UNSIGNED NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_sala PRIMARY KEY (id_sala),
    CONSTRAINT ck_sala_capacidade CHECK (capacidade > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE turma (
    id_turma INT UNSIGNED AUTO_INCREMENT,
    id_disciplina INT UNSIGNED NOT NULL,
    id_professor INT UNSIGNED NOT NULL,
    id_periodo INT UNSIGNED NOT NULL,
    codigo VARCHAR(40) NOT NULL,
    capacidade SMALLINT UNSIGNED NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_turma PRIMARY KEY (id_turma),
    CONSTRAINT fk_turma_disciplina FOREIGN KEY (id_disciplina)
        REFERENCES disciplina(id_disciplina) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_turma_professor FOREIGN KEY (id_professor)
        REFERENCES professor(id_professor) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_turma_periodo FOREIGN KEY (id_periodo)
        REFERENCES periodo_letivo(id_periodo) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_turma_capacidade CHECK (capacidade > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE horario_aula (
    id_horario INT UNSIGNED AUTO_INCREMENT,
    id_turma INT UNSIGNED NOT NULL,
    id_sala INT UNSIGNED NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    CONSTRAINT pk_horario_aula PRIMARY KEY (id_horario),
    CONSTRAINT fk_horario_turma FOREIGN KEY (id_turma)
        REFERENCES turma(id_turma) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_horario_sala FOREIGN KEY (id_sala)
        REFERENCES sala(id_sala) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_horario_dia_semana CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horario_intervalo CHECK (hora_fim > hora_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE matricula_academica (
    id_matricula INT UNSIGNED AUTO_INCREMENT,
    id_aluno INT UNSIGNED NOT NULL,
    id_curso INT UNSIGNED NOT NULL,
    data_ingresso DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_matricula_academica PRIMARY KEY (id_matricula),
    CONSTRAINT fk_matricula_aluno FOREIGN KEY (id_aluno)
        REFERENCES aluno(id_aluno) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_matricula_curso FOREIGN KEY (id_curso)
        REFERENCES curso(id_curso) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE inscricao_disciplina (
    id_inscricao INT UNSIGNED AUTO_INCREMENT,
    id_matricula INT UNSIGNED NOT NULL,
    id_turma INT UNSIGNED NOT NULL,
    data_inscricao DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_inscricao_disciplina PRIMARY KEY (id_inscricao),
    CONSTRAINT fk_inscricao_matricula FOREIGN KEY (id_matricula)
        REFERENCES matricula_academica(id_matricula) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_inscricao_turma FOREIGN KEY (id_turma)
        REFERENCES turma(id_turma) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE plano_estudo (
    id_plano INT UNSIGNED AUTO_INCREMENT,
    id_matricula INT UNSIGNED NOT NULL,
    id_estrutura INT UNSIGNED NOT NULL,
    data_atualizacao DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_plano_estudo PRIMARY KEY (id_plano),
    CONSTRAINT fk_plano_matricula FOREIGN KEY (id_matricula)
        REFERENCES matricula_academica(id_matricula) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_plano_estrutura FOREIGN KEY (id_estrutura)
        REFERENCES estrutura_curricular(id_estrutura) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE item_plano_estudo (
    id_item_plano INT UNSIGNED AUTO_INCREMENT,
    id_plano INT UNSIGNED NOT NULL,
    id_disciplina INT UNSIGNED NOT NULL,
    id_periodo_previsto INT UNSIGNED NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_item_plano_estudo PRIMARY KEY (id_item_plano),
    CONSTRAINT fk_item_plano_plano FOREIGN KEY (id_plano)
        REFERENCES plano_estudo(id_plano) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_item_plano_disciplina FOREIGN KEY (id_disciplina)
        REFERENCES disciplina(id_disciplina) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_item_plano_periodo FOREIGN KEY (id_periodo_previsto)
        REFERENCES periodo_letivo(id_periodo) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE transferencia_academica (
    id_transferencia INT UNSIGNED AUTO_INCREMENT,
    id_matricula INT UNSIGNED NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    data DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    historico TEXT NOT NULL,
    CONSTRAINT pk_transferencia_academica PRIMARY KEY (id_transferencia),
    CONSTRAINT fk_transferencia_matricula FOREIGN KEY (id_matricula)
        REFERENCES matricula_academica(id_matricula) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aproveitamento_equivalencia (
    id_aproveitamento INT UNSIGNED AUTO_INCREMENT,
    id_matricula INT UNSIGNED NOT NULL,
    id_disciplina_origem INT UNSIGNED NULL,
    id_disciplina_destino INT UNSIGNED NOT NULL,
    decisao VARCHAR(20) NOT NULL,
    data DATE NOT NULL,
    resultado VARCHAR(255) NOT NULL,
    CONSTRAINT pk_aproveitamento_equivalencia PRIMARY KEY (id_aproveitamento),
    CONSTRAINT fk_aproveitamento_matricula FOREIGN KEY (id_matricula)
        REFERENCES matricula_academica(id_matricula) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_aproveitamento_disc_origem FOREIGN KEY (id_disciplina_origem)
        REFERENCES disciplina(id_disciplina) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_aproveitamento_disc_destino FOREIGN KEY (id_disciplina_destino)
        REFERENCES disciplina(id_disciplina) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aula (
    id_aula INT UNSIGNED AUTO_INCREMENT,
    id_turma INT UNSIGNED NOT NULL,
    data DATE NOT NULL,
    pauta VARCHAR(255) NOT NULL,
    conteudo TEXT NOT NULL,
    CONSTRAINT pk_aula PRIMARY KEY (id_aula),
    CONSTRAINT fk_aula_turma FOREIGN KEY (id_turma)
        REFERENCES turma(id_turma) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE avaliacao (
    id_avaliacao INT UNSIGNED AUTO_INCREMENT,
    id_turma INT UNSIGNED NOT NULL,
    identificacao VARCHAR(100) NOT NULL,
    data DATE NOT NULL,
    descricao TEXT NOT NULL,
    CONSTRAINT pk_avaliacao PRIMARY KEY (id_avaliacao),
    CONSTRAINT fk_avaliacao_turma FOREIGN KEY (id_turma)
        REFERENCES turma(id_turma) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE nota (
    id_nota INT UNSIGNED AUTO_INCREMENT,
    id_avaliacao INT UNSIGNED NOT NULL,
    id_inscricao INT UNSIGNED NOT NULL,
    valor DECIMAL(5,2) NOT NULL,
    CONSTRAINT pk_nota PRIMARY KEY (id_nota),
    CONSTRAINT fk_nota_avaliacao FOREIGN KEY (id_avaliacao)
        REFERENCES avaliacao(id_avaliacao) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_nota_inscricao FOREIGN KEY (id_inscricao)
        REFERENCES inscricao_disciplina(id_inscricao) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_nota_avaliacao_inscricao UNIQUE (id_avaliacao, id_inscricao),
    CONSTRAINT ck_nota_valor CHECK (valor >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE registro_frequencia (
    id_frequencia INT UNSIGNED AUTO_INCREMENT,
    id_aula INT UNSIGNED NOT NULL,
    id_inscricao INT UNSIGNED NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    origem VARCHAR(30) NOT NULL,
    CONSTRAINT pk_registro_frequencia PRIMARY KEY (id_frequencia),
    CONSTRAINT fk_frequencia_aula FOREIGN KEY (id_aula)
        REFERENCES aula(id_aula) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_frequencia_inscricao FOREIGN KEY (id_inscricao)
        REFERENCES inscricao_disciplina(id_inscricao) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_frequencia_aula_inscricao UNIQUE (id_aula, id_inscricao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE diario_classe (
    id_diario INT UNSIGNED AUTO_INCREMENT,
    id_turma INT UNSIGNED NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_diario_classe PRIMARY KEY (id_diario),
    CONSTRAINT uq_diario_turma UNIQUE (id_turma),
    CONSTRAINT fk_diario_turma FOREIGN KEY (id_turma)
        REFERENCES turma(id_turma) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE auditoria_diario (
    id_auditoria_diario INT UNSIGNED AUTO_INCREMENT,
    id_diario INT UNSIGNED NOT NULL,
    id_usuario_responsavel INT UNSIGNED NOT NULL,
    data_hora DATETIME NOT NULL,
    observacoes TEXT NOT NULL,
    CONSTRAINT pk_auditoria_diario PRIMARY KEY (id_auditoria_diario),
    CONSTRAINT fk_auditoria_diario_diario FOREIGN KEY (id_diario)
        REFERENCES diario_classe(id_diario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_auditoria_diario_usuario FOREIGN KEY (id_usuario_responsavel)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE prazo_meta_docente (
    id_prazo_meta INT UNSIGNED AUTO_INCREMENT,
    id_professor INT UNSIGNED NOT NULL,
    id_periodo INT UNSIGNED NULL,
    descricao VARCHAR(255) NOT NULL,
    prazo DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_prazo_meta_docente PRIMARY KEY (id_prazo_meta),
    CONSTRAINT fk_prazo_meta_professor FOREIGN KEY (id_professor)
        REFERENCES professor(id_professor) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_prazo_meta_periodo FOREIGN KEY (id_periodo)
        REFERENCES periodo_letivo(id_periodo) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE solicitacao_documento (
    id_solicitacao_documento INT UNSIGNED AUTO_INCREMENT,
    id_matricula INT UNSIGNED NOT NULL,
    tipo_documento VARCHAR(80) NOT NULL,
    data_solicitacao DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_solicitacao_documento PRIMARY KEY (id_solicitacao_documento),
    CONSTRAINT fk_solicitacao_documento_matricula FOREIGN KEY (id_matricula)
        REFERENCES matricula_academica(id_matricula) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE manifestacao_aluno (
    id_manifestacao INT UNSIGNED AUTO_INCREMENT,
    id_aluno INT UNSIGNED NOT NULL,
    tipo_manifestacao VARCHAR(20) NOT NULL,
    conteudo TEXT NOT NULL,
    data_hora DATETIME NOT NULL,
    CONSTRAINT pk_manifestacao_aluno PRIMARY KEY (id_manifestacao),
    CONSTRAINT fk_manifestacao_aluno FOREIGN KEY (id_aluno)
        REFERENCES aluno(id_aluno) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 3. MÓDULO ADMINISTRATIVO E RH
-- ============================================================

CREATE TABLE centro_custo (
    id_centro_custo INT UNSIGNED AUTO_INCREMENT,
    denominacao VARCHAR(120) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_centro_custo PRIMARY KEY (id_centro_custo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE recurso (
    id_recurso INT UNSIGNED AUTO_INCREMENT,
    id_centro_custo INT UNSIGNED NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    origem VARCHAR(100) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_recurso PRIMARY KEY (id_recurso),
    CONSTRAINT fk_recurso_centro_custo FOREIGN KEY (id_centro_custo)
        REFERENCES centro_custo(id_centro_custo) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_recurso_valor CHECK (valor >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE transacao_financeira (
    id_transacao INT UNSIGNED AUTO_INCREMENT,
    id_centro_origem INT UNSIGNED NULL,
    id_centro_destino INT UNSIGNED NULL,
    tipo VARCHAR(40) NOT NULL,
    entidade_externa VARCHAR(150) NULL,
    valor DECIMAL(12,2) NOT NULL,
    data_solicitacao DATETIME NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_transacao_financeira PRIMARY KEY (id_transacao),
    CONSTRAINT fk_transacao_centro_origem FOREIGN KEY (id_centro_origem)
        REFERENCES centro_custo(id_centro_custo) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_transacao_centro_destino FOREIGN KEY (id_centro_destino)
        REFERENCES centro_custo(id_centro_custo) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_transacao_valor CHECK (valor > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE movimentacao_recurso (
    id_movimentacao_recurso INT UNSIGNED AUTO_INCREMENT,
    id_centro_origem INT UNSIGNED NOT NULL,
    id_centro_destino INT UNSIGNED NOT NULL,
    id_usuario_responsavel INT UNSIGNED NOT NULL,
    id_transacao INT UNSIGNED NULL,
    valor DECIMAL(12,2) NOT NULL,
    data_hora DATETIME NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_movimentacao_recurso PRIMARY KEY (id_movimentacao_recurso),
    CONSTRAINT uq_movimentacao_recurso_transacao UNIQUE (id_transacao),
    CONSTRAINT fk_mov_recurso_centro_origem FOREIGN KEY (id_centro_origem)
        REFERENCES centro_custo(id_centro_custo) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mov_recurso_centro_destino FOREIGN KEY (id_centro_destino)
        REFERENCES centro_custo(id_centro_custo) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mov_recurso_usuario FOREIGN KEY (id_usuario_responsavel)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_mov_recurso_transacao FOREIGN KEY (id_transacao)
        REFERENCES transacao_financeira(id_transacao) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_mov_recurso_valor CHECK (valor > 0),
    CONSTRAINT ck_mov_recurso_centros_distintos CHECK (id_centro_origem <> id_centro_destino)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE autorizacao_transacao_financeira (
    id_autorizacao INT UNSIGNED AUTO_INCREMENT,
    id_transacao INT UNSIGNED NOT NULL,
    id_usuario_responsavel INT UNSIGNED NOT NULL,
    decisao VARCHAR(20) NOT NULL,
    data_hora DATETIME NOT NULL,
    justificativa TEXT NULL,
    CONSTRAINT pk_autorizacao_transacao PRIMARY KEY (id_autorizacao),
    CONSTRAINT fk_autorizacao_transacao FOREIGN KEY (id_transacao)
        REFERENCES transacao_financeira(id_transacao) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_autorizacao_usuario FOREIGN KEY (id_usuario_responsavel)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE funcao (
    id_funcao INT UNSIGNED AUTO_INCREMENT,
    denominacao VARCHAR(100) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_funcao PRIMARY KEY (id_funcao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE funcionario (
    id_funcionario INT UNSIGNED AUTO_INCREMENT,
    id_usuario INT UNSIGNED NULL,
    id_funcao INT UNSIGNED NOT NULL,
    identificacao VARCHAR(100) NOT NULL,
    situacao_funcional VARCHAR(20) NOT NULL,
    CONSTRAINT pk_funcionario PRIMARY KEY (id_funcionario),
    CONSTRAINT uq_funcionario_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_funcionario_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_funcionario_funcao FOREIGN KEY (id_funcao)
        REFERENCES funcao(id_funcao) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE folha_pagamento (
    id_folha INT UNSIGNED AUTO_INCREMENT,
    competencia CHAR(7) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    data_processamento DATETIME NOT NULL,
    id_usuario_responsavel INT UNSIGNED NOT NULL,
    CONSTRAINT pk_folha_pagamento PRIMARY KEY (id_folha),
    CONSTRAINT fk_folha_usuario FOREIGN KEY (id_usuario_responsavel)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_folha_competencia (competencia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE item_folha_pagamento (
    id_item_folha INT UNSIGNED AUTO_INCREMENT,
    id_folha INT UNSIGNED NOT NULL,
    id_funcionario INT UNSIGNED NOT NULL,
    valor_calculado DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_item_folha_pagamento PRIMARY KEY (id_item_folha),
    CONSTRAINT fk_item_folha_folha FOREIGN KEY (id_folha)
        REFERENCES folha_pagamento(id_folha) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_item_folha_funcionario FOREIGN KEY (id_funcionario)
        REFERENCES funcionario(id_funcionario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_item_folha_funcionario UNIQUE (id_folha, id_funcionario),
    CONSTRAINT ck_item_folha_valor CHECK (valor_calculado >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4. MÓDULO DE COMPRAS
-- ============================================================

CREATE TABLE fornecedor (
    id_fornecedor INT UNSIGNED AUTO_INCREMENT,
    identificacao VARCHAR(100) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    documento VARCHAR(30) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_fornecedor PRIMARY KEY (id_fornecedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE produto (
    id_produto INT UNSIGNED AUTO_INCREMENT,
    descricao VARCHAR(255) NOT NULL,
    tipo_produto VARCHAR(30) NOT NULL,
    quantidade_disponivel DECIMAL(12,3) NOT NULL DEFAULT 0,
    estoque_minimo DECIMAL(12,3) NOT NULL DEFAULT 0,
    estoque_maximo DECIMAL(12,3) NOT NULL DEFAULT 0,
    referencia VARCHAR(100) NOT NULL,
    localizacao VARCHAR(150) NOT NULL,
    codigo_barras VARCHAR(80) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_produto PRIMARY KEY (id_produto),
    CONSTRAINT ck_produto_quantidade CHECK (quantidade_disponivel >= 0),
    CONSTRAINT ck_produto_estoque_minimo CHECK (estoque_minimo >= 0),
    CONSTRAINT ck_produto_estoque_maximo CHECK (estoque_maximo >= estoque_minimo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE solicitacao_compra (
    id_solicitacao INT UNSIGNED AUTO_INCREMENT,
    id_usuario_solicitante INT UNSIGNED NOT NULL,
    id_setor INT UNSIGNED NOT NULL,
    id_centro_custo INT UNSIGNED NOT NULL,
    justificativa TEXT NOT NULL,
    data_solicitacao DATETIME NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    id_usuario_aprovador INT UNSIGNED NULL,
    data_decisao DATETIME NULL,
    motivo_decisao TEXT NULL,
    CONSTRAINT pk_solicitacao_compra PRIMARY KEY (id_solicitacao),
    CONSTRAINT fk_solicitacao_usuario_solicitante FOREIGN KEY (id_usuario_solicitante)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_solicitacao_setor FOREIGN KEY (id_setor)
        REFERENCES setor(id_setor) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_solicitacao_centro_custo FOREIGN KEY (id_centro_custo)
        REFERENCES centro_custo(id_centro_custo) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_solicitacao_usuario_aprovador FOREIGN KEY (id_usuario_aprovador)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_solicitacao_data (data_solicitacao),
    INDEX idx_solicitacao_situacao (situacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE item_solicitacao_compra (
    id_item_solicitacao INT UNSIGNED AUTO_INCREMENT,
    id_solicitacao INT UNSIGNED NOT NULL,
    id_produto INT UNSIGNED NULL,
    descricao VARCHAR(255) NOT NULL,
    quantidade DECIMAL(12,3) NOT NULL,
    tipo_item VARCHAR(20) NOT NULL,
    CONSTRAINT pk_item_solicitacao_compra PRIMARY KEY (id_item_solicitacao),
    CONSTRAINT fk_item_solicitacao FOREIGN KEY (id_solicitacao)
        REFERENCES solicitacao_compra(id_solicitacao) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_item_solicitacao_produto FOREIGN KEY (id_produto)
        REFERENCES produto(id_produto) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_item_solicitacao_quantidade CHECK (quantidade > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE compra (
    id_compra INT UNSIGNED AUTO_INCREMENT,
    id_solicitacao INT UNSIGNED NOT NULL,
    id_fornecedor INT UNSIGNED NOT NULL,
    id_centro_custo INT UNSIGNED NOT NULL,
    data_compra DATETIME NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    condicao_pagamento VARCHAR(20) NOT NULL,
    quantidade_parcelas SMALLINT UNSIGNED NOT NULL,
    data_cancelamento DATETIME NULL,
    motivo_cancelamento TEXT NULL,
    id_usuario_cancelamento INT UNSIGNED NULL,
    CONSTRAINT pk_compra PRIMARY KEY (id_compra),
    CONSTRAINT fk_compra_solicitacao FOREIGN KEY (id_solicitacao)
        REFERENCES solicitacao_compra(id_solicitacao) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_compra_fornecedor FOREIGN KEY (id_fornecedor)
        REFERENCES fornecedor(id_fornecedor) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_compra_centro_custo FOREIGN KEY (id_centro_custo)
        REFERENCES centro_custo(id_centro_custo) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_compra_usuario_cancelamento FOREIGN KEY (id_usuario_cancelamento)
        REFERENCES usuario(id_usuario) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_compra_quantidade_parcelas CHECK (quantidade_parcelas > 0),
    CONSTRAINT ck_compra_cancelamento CHECK (
        situacao <> 'CANCELADA'
        OR (data_cancelamento IS NOT NULL
            AND motivo_cancelamento IS NOT NULL
            AND id_usuario_cancelamento IS NOT NULL)
    ),
    INDEX idx_compra_data (data_compra),
    INDEX idx_compra_situacao (situacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE item_compra (
    id_item_compra INT UNSIGNED AUTO_INCREMENT,
    id_compra INT UNSIGNED NOT NULL,
    id_item_solicitacao INT UNSIGNED NULL,
    id_produto INT UNSIGNED NULL,
    descricao VARCHAR(255) NOT NULL,
    quantidade DECIMAL(12,3) NOT NULL,
    valor_unitario DECIMAL(12,2) NOT NULL,
    valor_total DECIMAL(12,2) NOT NULL,
    tipo_item VARCHAR(20) NOT NULL,
    CONSTRAINT pk_item_compra PRIMARY KEY (id_item_compra),
    CONSTRAINT fk_item_compra_compra FOREIGN KEY (id_compra)
        REFERENCES compra(id_compra) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_item_compra_item_solicitacao FOREIGN KEY (id_item_solicitacao)
        REFERENCES item_solicitacao_compra(id_item_solicitacao) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_item_compra_produto FOREIGN KEY (id_produto)
        REFERENCES produto(id_produto) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_item_compra_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_item_compra_valor_unitario CHECK (valor_unitario >= 0),
    CONSTRAINT ck_item_compra_valor_total CHECK (valor_total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE recebimento_compra (
    id_recebimento INT UNSIGNED AUTO_INCREMENT,
    id_compra INT UNSIGNED NOT NULL,
    id_usuario_responsavel INT UNSIGNED NOT NULL,
    data_recebimento DATETIME NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_recebimento_compra PRIMARY KEY (id_recebimento),
    CONSTRAINT fk_recebimento_compra FOREIGN KEY (id_compra)
        REFERENCES compra(id_compra) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_recebimento_usuario FOREIGN KEY (id_usuario_responsavel)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE item_recebimento_compra (
    id_item_recebimento INT UNSIGNED AUTO_INCREMENT,
    id_recebimento INT UNSIGNED NOT NULL,
    id_item_compra INT UNSIGNED NOT NULL,
    quantidade_recebida DECIMAL(12,3) NOT NULL,
    CONSTRAINT pk_item_recebimento_compra PRIMARY KEY (id_item_recebimento),
    CONSTRAINT fk_item_recebimento_recebimento FOREIGN KEY (id_recebimento)
        REFERENCES recebimento_compra(id_recebimento) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_item_recebimento_item_compra FOREIGN KEY (id_item_compra)
        REFERENCES item_compra(id_item_compra) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_item_recebimento_quantidade CHECK (quantidade_recebida > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pagamento_compra (
    id_pagamento_compra INT UNSIGNED AUTO_INCREMENT,
    id_compra INT UNSIGNED NOT NULL,
    id_transacao INT UNSIGNED NULL,
    id_usuario_responsavel INT UNSIGNED NOT NULL,
    numero_parcela SMALLINT UNSIGNED NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    vencimento DATE NOT NULL,
    data_pagamento DATETIME NULL,
    forma_pagamento VARCHAR(20) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_pagamento_compra PRIMARY KEY (id_pagamento_compra),
    CONSTRAINT uq_pagamento_compra_transacao UNIQUE (id_transacao),
    CONSTRAINT fk_pagamento_compra FOREIGN KEY (id_compra)
        REFERENCES compra(id_compra) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_pagamento_transacao FOREIGN KEY (id_transacao)
        REFERENCES transacao_financeira(id_transacao) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_pagamento_usuario FOREIGN KEY (id_usuario_responsavel)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_pagamento_parcela CHECK (numero_parcela > 0),
    CONSTRAINT ck_pagamento_valor CHECK (valor > 0),
    INDEX idx_pagamento_compra_vencimento (vencimento),
    INDEX idx_pagamento_compra_situacao (situacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 5. MÓDULO ALMOXARIFADO E PATRIMÔNIO
-- ============================================================

CREATE TABLE movimentacao_estoque (
    id_movimentacao_estoque INT UNSIGNED AUTO_INCREMENT,
    id_produto INT UNSIGNED NOT NULL,
    id_usuario_responsavel INT UNSIGNED NOT NULL,
    id_item_recebimento INT UNSIGNED NULL,
    tipo_movimentacao VARCHAR(20) NOT NULL,
    quantidade DECIMAL(12,3) NOT NULL,
    data_hora DATETIME NOT NULL,
    origem VARCHAR(50) NOT NULL,
    motivo VARCHAR(255) NULL,
    CONSTRAINT pk_movimentacao_estoque PRIMARY KEY (id_movimentacao_estoque),
    CONSTRAINT uq_movimentacao_item_recebimento UNIQUE (id_item_recebimento),
    CONSTRAINT fk_mov_estoque_produto FOREIGN KEY (id_produto)
        REFERENCES produto(id_produto) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_mov_estoque_usuario FOREIGN KEY (id_usuario_responsavel)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_mov_estoque_item_recebimento FOREIGN KEY (id_item_recebimento)
        REFERENCES item_recebimento_compra(id_item_recebimento) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_mov_estoque_quantidade CHECK (quantidade > 0),
    INDEX idx_mov_estoque_data (data_hora),
    INDEX idx_mov_estoque_tipo (tipo_movimentacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE retirada_nao_consumivel (
    id_retirada INT UNSIGNED AUTO_INCREMENT,
    id_produto INT UNSIGNED NOT NULL,
    id_usuario_responsavel INT UNSIGNED NOT NULL,
    data_retirada DATETIME NOT NULL,
    data_prevista_devolucao DATE NOT NULL,
    data_devolucao DATETIME NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_retirada_nao_consumivel PRIMARY KEY (id_retirada),
    CONSTRAINT fk_retirada_produto FOREIGN KEY (id_produto)
        REFERENCES produto(id_produto) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_retirada_usuario FOREIGN KEY (id_usuario_responsavel)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE bem_patrimonial (
    id_bem INT UNSIGNED AUTO_INCREMENT,
    id_produto_origem INT UNSIGNED NULL,
    id_setor_responsavel INT UNSIGNED NULL,
    descricao VARCHAR(255) NOT NULL,
    numero_tombo VARCHAR(80) NOT NULL,
    data_tombo DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_bem_patrimonial PRIMARY KEY (id_bem),
    CONSTRAINT uq_bem_numero_tombo UNIQUE (numero_tombo),
    CONSTRAINT fk_bem_produto_origem FOREIGN KEY (id_produto_origem)
        REFERENCES produto(id_produto) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_bem_setor_responsavel FOREIGN KEY (id_setor_responsavel)
        REFERENCES setor(id_setor) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE movimentacao_patrimonial (
    id_movimentacao_patrimonial INT UNSIGNED AUTO_INCREMENT,
    id_bem INT UNSIGNED NOT NULL,
    id_setor_origem INT UNSIGNED NULL,
    id_setor_destino INT UNSIGNED NOT NULL,
    id_usuario_responsavel INT UNSIGNED NOT NULL,
    data_hora DATETIME NOT NULL,
    CONSTRAINT pk_movimentacao_patrimonial PRIMARY KEY (id_movimentacao_patrimonial),
    CONSTRAINT fk_mov_patrimonial_bem FOREIGN KEY (id_bem)
        REFERENCES bem_patrimonial(id_bem) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_mov_patrimonial_setor_origem FOREIGN KEY (id_setor_origem)
        REFERENCES setor(id_setor) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mov_patrimonial_setor_destino FOREIGN KEY (id_setor_destino)
        REFERENCES setor(id_setor) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mov_patrimonial_usuario FOREIGN KEY (id_usuario_responsavel)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_mov_patrimonial_setores CHECK (
        id_setor_origem IS NULL OR id_setor_origem <> id_setor_destino
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE baixa_patrimonial (
    id_baixa_patrimonial INT UNSIGNED AUTO_INCREMENT,
    id_bem INT UNSIGNED NOT NULL,
    id_usuario_responsavel INT UNSIGNED NOT NULL,
    motivo TEXT NOT NULL,
    data_hora DATETIME NOT NULL,
    CONSTRAINT pk_baixa_patrimonial PRIMARY KEY (id_baixa_patrimonial),
    CONSTRAINT uq_baixa_bem UNIQUE (id_bem),
    CONSTRAINT fk_baixa_bem FOREIGN KEY (id_bem)
        REFERENCES bem_patrimonial(id_bem) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_baixa_usuario FOREIGN KEY (id_usuario_responsavel)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 6. MÓDULO FINANCEIRO
-- ============================================================

CREATE TABLE cobranca (
    id_cobranca INT UNSIGNED AUTO_INCREMENT,
    id_matricula INT UNSIGNED NOT NULL,
    origem VARCHAR(50) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    competencia CHAR(7) NOT NULL,
    vencimento DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_cobranca PRIMARY KEY (id_cobranca),
    CONSTRAINT fk_cobranca_matricula FOREIGN KEY (id_matricula)
        REFERENCES matricula_academica(id_matricula) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_cobranca_valor CHECK (valor >= 0),
    INDEX idx_cobranca_competencia (competencia),
    INDEX idx_cobranca_vencimento (vencimento),
    INDEX idx_cobranca_situacao (situacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE mensalidade (
    id_mensalidade INT UNSIGNED AUTO_INCREMENT,
    id_matricula INT UNSIGNED NOT NULL,
    id_periodo INT UNSIGNED NOT NULL,
    id_cobranca INT UNSIGNED NULL,
    modalidade_mensalidade VARCHAR(30) NOT NULL,
    carga_horaria SMALLINT UNSIGNED NULL,
    valor DECIMAL(12,2) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_mensalidade PRIMARY KEY (id_mensalidade),
    CONSTRAINT uq_mensalidade_cobranca UNIQUE (id_cobranca),
    CONSTRAINT fk_mensalidade_matricula FOREIGN KEY (id_matricula)
        REFERENCES matricula_academica(id_matricula) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_mensalidade_periodo FOREIGN KEY (id_periodo)
        REFERENCES periodo_letivo(id_periodo) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_mensalidade_cobranca FOREIGN KEY (id_cobranca)
        REFERENCES cobranca(id_cobranca) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_mensalidade_valor CHECK (valor >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE boleto (
    id_boleto INT UNSIGNED AUTO_INCREMENT,
    id_cobranca INT UNSIGNED NOT NULL,
    identificacao VARCHAR(100) NOT NULL,
    data_emissao DATE NOT NULL,
    vencimento DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_boleto PRIMARY KEY (id_boleto),
    CONSTRAINT fk_boleto_cobranca FOREIGN KEY (id_cobranca)
        REFERENCES cobranca(id_cobranca) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pagamento_cobranca (
    id_pagamento_cobranca INT UNSIGNED AUTO_INCREMENT,
    id_cobranca INT UNSIGNED NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    data_pagamento DATETIME NOT NULL,
    origem_confirmacao VARCHAR(50) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_pagamento_cobranca PRIMARY KEY (id_pagamento_cobranca),
    CONSTRAINT fk_pagamento_cobranca FOREIGN KEY (id_cobranca)
        REFERENCES cobranca(id_cobranca) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_pagamento_cobranca_valor CHECK (valor > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE bolsa_estudo (
    id_bolsa INT UNSIGNED AUTO_INCREMENT,
    id_matricula INT UNSIGNED NOT NULL,
    origem VARCHAR(100) NOT NULL,
    valor DECIMAL(12,2) NULL,
    percentual DECIMAL(5,2) NULL,
    vigencia_inicio DATE NOT NULL,
    vigencia_fim DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_bolsa_estudo PRIMARY KEY (id_bolsa),
    CONSTRAINT fk_bolsa_matricula FOREIGN KEY (id_matricula)
        REFERENCES matricula_academica(id_matricula) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_bolsa_valor CHECK (valor IS NULL OR valor >= 0),
    CONSTRAINT ck_bolsa_percentual CHECK (percentual IS NULL OR percentual BETWEEN 0 AND 100),
    CONSTRAINT ck_bolsa_vigencia CHECK (vigencia_fim >= vigencia_inicio),
    CONSTRAINT ck_bolsa_valor_ou_percentual CHECK (valor IS NOT NULL OR percentual IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE financiamento_estudantil (
    id_financiamento INT UNSIGNED AUTO_INCREMENT,
    id_matricula INT UNSIGNED NOT NULL,
    programa_agente VARCHAR(150) NOT NULL,
    condicoes TEXT NOT NULL,
    vigencia_inicio DATE NOT NULL,
    vigencia_fim DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_financiamento_estudantil PRIMARY KEY (id_financiamento),
    CONSTRAINT fk_financiamento_matricula FOREIGN KEY (id_matricula)
        REFERENCES matricula_academica(id_matricula) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_financiamento_vigencia CHECK (vigencia_fim >= vigencia_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 7. MÓDULO BIBLIOTECA
-- ============================================================

CREATE TABLE usuario_biblioteca (
    id_usuario_biblioteca INT UNSIGNED AUTO_INCREMENT,
    id_usuario INT UNSIGNED NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_usuario_biblioteca PRIMARY KEY (id_usuario_biblioteca),
    CONSTRAINT uq_usuario_biblioteca_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_usuario_biblioteca_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE item_acervo (
    id_item_acervo INT UNSIGNED AUTO_INCREMENT,
    titulo VARCHAR(255) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    classificacao VARCHAR(100) NOT NULL,
    informacoes_bibliograficas TEXT NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_item_acervo PRIMARY KEY (id_item_acervo),
    INDEX idx_item_acervo_titulo (titulo),
    INDEX idx_item_acervo_classificacao (classificacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE exemplar (
    id_exemplar INT UNSIGNED AUTO_INCREMENT,
    id_item_acervo INT UNSIGNED NOT NULL,
    identificacao VARCHAR(100) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_exemplar PRIMARY KEY (id_exemplar),
    CONSTRAINT fk_exemplar_item_acervo FOREIGN KEY (id_item_acervo)
        REFERENCES item_acervo(id_item_acervo) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE responsavel_acervo (
    id_responsavel_acervo INT UNSIGNED AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    CONSTRAINT pk_responsavel_acervo PRIMARY KEY (id_responsavel_acervo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE participacao_acervo (
    id_item_acervo INT UNSIGNED NOT NULL,
    id_responsavel_acervo INT UNSIGNED NOT NULL,
    papel VARCHAR(40) NOT NULL,
    CONSTRAINT pk_participacao_acervo PRIMARY KEY (id_item_acervo, id_responsavel_acervo, papel),
    CONSTRAINT fk_participacao_item_acervo FOREIGN KEY (id_item_acervo)
        REFERENCES item_acervo(id_item_acervo) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_participacao_responsavel FOREIGN KEY (id_responsavel_acervo)
        REFERENCES responsavel_acervo(id_responsavel_acervo) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE emprestimo (
    id_emprestimo INT UNSIGNED AUTO_INCREMENT,
    id_usuario_biblioteca INT UNSIGNED NOT NULL,
    id_exemplar INT UNSIGNED NOT NULL,
    data_emprestimo DATETIME NOT NULL,
    data_prevista_devolucao DATE NOT NULL,
    data_devolucao DATETIME NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_emprestimo PRIMARY KEY (id_emprestimo),
    CONSTRAINT fk_emprestimo_usuario_biblioteca FOREIGN KEY (id_usuario_biblioteca)
        REFERENCES usuario_biblioteca(id_usuario_biblioteca) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_emprestimo_exemplar FOREIGN KEY (id_exemplar)
        REFERENCES exemplar(id_exemplar) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_emprestimo_prevista_devolucao (data_prevista_devolucao),
    INDEX idx_emprestimo_situacao (situacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE alerta_devolucao (
    id_alerta INT UNSIGNED AUTO_INCREMENT,
    id_emprestimo INT UNSIGNED NOT NULL,
    canal VARCHAR(20) NOT NULL,
    data_envio DATETIME NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_alerta_devolucao PRIMARY KEY (id_alerta),
    CONSTRAINT fk_alerta_emprestimo FOREIGN KEY (id_emprestimo)
        REFERENCES emprestimo(id_emprestimo) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pendencia_biblioteca (
    id_pendencia INT UNSIGNED AUTO_INCREMENT,
    id_emprestimo INT UNSIGNED NOT NULL,
    id_cobranca INT UNSIGNED NULL,
    tipo_pendencia VARCHAR(30) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    impeditiva TINYINT(1) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_pendencia_biblioteca PRIMARY KEY (id_pendencia),
    CONSTRAINT fk_pendencia_emprestimo FOREIGN KEY (id_emprestimo)
        REFERENCES emprestimo(id_emprestimo) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_pendencia_cobranca FOREIGN KEY (id_cobranca)
        REFERENCES cobranca(id_cobranca) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_pendencia_valor CHECK (valor >= 0),
    CONSTRAINT ck_pendencia_impeditiva CHECK (impeditiva IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 8. ÍNDICES COMPLEMENTARES
-- ============================================================

CREATE INDEX idx_turma_disciplina_periodo ON turma (id_disciplina, id_periodo);
CREATE INDEX idx_inscricao_situacao ON inscricao_disciplina (situacao);
CREATE INDEX idx_pagamento_cobranca_data ON pagamento_cobranca (data_pagamento);
CREATE INDEX idx_pendencia_biblioteca_situacao ON pendencia_biblioteca (situacao);
CREATE INDEX idx_produto_descricao ON produto (descricao);
CREATE INDEX idx_bem_setor ON bem_patrimonial (id_setor_responsavel);

-- ============================================================
-- FIM DO SCRIPT
-- ============================================================
