-- SCOA — primeira bateria de testes de integração do banco
-- Compatível com SCOA_MySQL8_schema_v7_5.sql / MySQL 8.0.16 ou superior.
-- Executar o ARQUIVO INTEIRO, em uma NOVA CONEXÃO ao banco de desenvolvimento.
-- Não executar por trechos. Não usar uma conexão com trabalho pendente:
-- CREATE/DROP PROCEDURE causam commit implícito no MySQL.
-- Requer CREATE ROUTINE, ALTER ROUTINE, EXECUTE e permissões de consulta e DML.
-- A rotina auxiliar abaixo é recriada e removida pelo próprio roteiro.
-- Nenhuma tabela de negócio é criada, alterada, apagada ou truncada.
-- Os dados de teste são desfeitos por ROLLBACK; AUTO_INCREMENT pode avançar.
-- Resultados ficam em memória, em uma variável desta conexão, sem gravar tabelas.
-- Erros esperados são capturados: avaliar a coluna resultado, não só o Output.
-- Esta bateria não testa autenticação, capacidade de inscrições ou cálculo de folha.

USE scoa;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS scoa_ct_bd_v75_20261018;
DELIMITER $$
CREATE PROCEDURE scoa_ct_bd_v75_20261018()
SQL SECURITY INVOKER
BEGIN
    DECLARE v_tag VARCHAR(40);
    DECLARE v_usuario, v_perfil, v_perfil_ausente, v_disciplina,
            v_usuario_opcional, v_aluno INT UNSIGNED;
    DECLARE v_errno INT DEFAULT 0;
    DECLARE v_mensagem VARCHAR(2048) DEFAULT '';
    DECLARE v_ok BOOLEAN DEFAULT FALSE;
    DECLARE v_restantes INT DEFAULT 0;
    DECLARE v_transacao BOOLEAN DEFAULT FALSE;
    DECLARE v_resultados JSON DEFAULT (JSON_ARRAY());
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
      IF v_transacao THEN ROLLBACK; END IF;
      RESIGNAL;
    END;

    IF @@SESSION.foreign_key_checks <> 1 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Abortado: habilite FOREIGN_KEY_CHECKS em uma nova conexão.';
    END IF;
    IF FIND_IN_SET('STRICT_TRANS_TABLES', @@SESSION.sql_mode) = 0
       AND FIND_IN_SET('STRICT_ALL_TABLES', @@SESSION.sql_mode) = 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Abortado: esta bateria exige SQL_MODE estrito.';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = 'scoa' AND TABLE_TYPE = 'BASE TABLE' AND ENGINE = 'InnoDB'
          AND TABLE_NAME IN ('usuario','perfil','usuario_perfil','parametro_sistema',
             'sala','periodo_letivo','produto','disciplina','disciplina_prerequisito','aluno')) <> 10 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Abortado: as dez tabelas usadas devem existir e ser InnoDB.';
    END IF;

    SET v_tag = CONCAT('CTBD', REPLACE(UUID(), '-', ''));
    START TRANSACTION;
    SET v_transacao = TRUE;

    -- Somente registros fictícios desta execução; nenhum ID existente é reutilizado.
    INSERT INTO usuario (identificacao, credencial_hash, situacao)
      VALUES (v_tag, 'HASH_FICTICIO_NAO_USAR_EM_LOGIN', 'ATIVO');
    SET v_usuario = LAST_INSERT_ID();
    INSERT INTO perfil (nome, descricao, situacao)
      VALUES (v_tag, 'Perfil fictício para teste', 'ATIVO');
    SET v_perfil = LAST_INSERT_ID();
    INSERT INTO usuario_perfil (id_usuario, id_perfil) VALUES (v_usuario, v_perfil);
    INSERT INTO perfil (nome, descricao, situacao)
      VALUES (CONCAT(v_tag, ' AUSENTE'), 'Será excluído para teste de FK', 'ATIVO');
    SET v_perfil_ausente = LAST_INSERT_ID();
    DELETE FROM perfil WHERE id_perfil = v_perfil_ausente;
    INSERT INTO parametro_sistema (chave, valor, tipo, modulo, descricao, situacao)
      VALUES (v_tag, '1', 'INTEIRO', 'TESTE_INTEGRIDADE', 'Parâmetro fictício', 'ATIVO');
    INSERT INTO disciplina (codigo, nome, carga_horaria, situacao)
      VALUES (LEFT(v_tag, 30), v_tag, 60, 'ATIVO');
    SET v_disciplina = LAST_INSERT_ID();
    INSERT INTO usuario (identificacao, credencial_hash, situacao)
      VALUES (CONCAT(v_tag, ' OPCIONAL'), 'HASH_FICTICIO_NAO_USAR_EM_LOGIN', 'ATIVO');
    SET v_usuario_opcional = LAST_INSERT_ID();
    INSERT INTO aluno (id_usuario, identificacao, situacao)
      VALUES (v_usuario_opcional, v_tag, 'ATIVO');
    SET v_aluno = LAST_INSERT_ID();

    -- CT-BD-001: Cadastrar usuário, perfil e vínculo válidos
    SAVEPOINT caso;
    -- Os registros de apoio foram inseridos dentro desta mesma transação.
    SET v_ok = (SELECT COUNT(*) = 1 FROM usuario_perfil WHERE id_usuario = v_usuario AND id_perfil = v_perfil);
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-001', 'cenario', 'Cadastrar usuário, perfil e vínculo válidos', 'esperado', 'Vínculo válido existente',
      'resultado', IF(v_ok, 'APROVADO', 'REPROVADO'), 'codigo_mysql', 0,
      'observado', IF(v_ok, 'Condição confirmada por consulta.', 'A consulta não confirmou o estado esperado.')));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-002: Rejeitar identificação nula
    SAVEPOINT caso;
    SET v_errno = 0, v_mensagem = '';
    BEGIN
      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
      BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_mensagem = MESSAGE_TEXT;
      END;
      INSERT INTO usuario (identificacao, credencial_hash, situacao) VALUES (NULL, 'HASH_FICTICIO_PARA_TESTE', 'ATIVO');
    END;
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-002', 'cenario', 'Rejeitar identificação nula', 'esperado', 'Erro 1048 no campo identificacao',
      'resultado', IF(v_errno = 1048 AND LOCATE('identificacao', v_mensagem) > 0,
          'APROVADO', 'REPROVADO'), 'codigo_mysql', v_errno,
      'observado', IF(v_errno = 0, 'A operação inválida foi aceita.', v_mensagem)));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-003: Rejeitar parâmetro duplicado
    SAVEPOINT caso;
    SET v_errno = 0, v_mensagem = '';
    BEGIN
      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
      BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_mensagem = MESSAGE_TEXT;
      END;
      INSERT INTO parametro_sistema (chave, valor, tipo, modulo, descricao, situacao) VALUES (v_tag, '2', 'INTEIRO', 'TESTE_INTEGRIDADE', 'Duplicata controlada', 'ATIVO');
    END;
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-003', 'cenario', 'Rejeitar parâmetro duplicado', 'esperado', 'Erro 1062 na unicidade do parâmetro',
      'resultado', IF(v_errno = 1062 AND LOCATE('uq_parametro_modulo_chave', v_mensagem) > 0,
          'APROVADO', 'REPROVADO'), 'codigo_mysql', v_errno,
      'observado', IF(v_errno = 0, 'A operação inválida foi aceita.', v_mensagem)));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-004: Rejeitar vínculo repetido
    SAVEPOINT caso;
    SET v_errno = 0, v_mensagem = '';
    BEGIN
      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
      BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_mensagem = MESSAGE_TEXT;
      END;
      INSERT INTO usuario_perfil (id_usuario, id_perfil) VALUES (v_usuario, v_perfil);
    END;
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-004', 'cenario', 'Rejeitar vínculo repetido', 'esperado', 'Erro 1062 na chave primária composta',
      'resultado', IF(v_errno = 1062 AND LOCATE('PRIMARY', v_mensagem) > 0,
          'APROVADO', 'REPROVADO'), 'codigo_mysql', v_errno,
      'observado', IF(v_errno = 0, 'A operação inválida foi aceita.', v_mensagem)));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-005: Rejeitar perfil inexistente
    SAVEPOINT caso;
    SET v_errno = 0, v_mensagem = '';
    BEGIN
      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
      BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_mensagem = MESSAGE_TEXT;
      END;
      INSERT INTO usuario_perfil (id_usuario, id_perfil) VALUES (v_usuario, v_perfil_ausente);
    END;
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-005', 'cenario', 'Rejeitar perfil inexistente', 'esperado', 'Erro 1452 na chave estrangeira do perfil',
      'resultado', IF(v_errno = 1452 AND LOCATE('fk_usuario_perfil_perfil', v_mensagem) > 0,
          'APROVADO', 'REPROVADO'), 'codigo_mysql', v_errno,
      'observado', IF(v_errno = 0, 'A operação inválida foi aceita.', v_mensagem)));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-006: Bloquear exclusão de usuário vinculado
    SAVEPOINT caso;
    SET v_errno = 0, v_mensagem = '';
    BEGIN
      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
      BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_mensagem = MESSAGE_TEXT;
      END;
      DELETE FROM usuario WHERE id_usuario = v_usuario;
    END;
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-006', 'cenario', 'Bloquear exclusão de usuário vinculado', 'esperado', 'Erro 1451; usuário preservado pelo vínculo',
      'resultado', IF(v_errno = 1451 AND LOCATE('fk_usuario_perfil_usuario', v_mensagem) > 0,
          'APROVADO', 'REPROVADO'), 'codigo_mysql', v_errno,
      'observado', IF(v_errno = 0, 'A operação inválida foi aceita.', v_mensagem)));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-007: Aceitar capacidade mínima positiva
    SAVEPOINT caso;
    INSERT INTO sala (identificacao, localizacao, capacidade, situacao) VALUES (v_tag, 'LOCAL FICTICIO', 1, 'ATIVO');
    SET v_ok = (SELECT COUNT(*) = 1 FROM sala WHERE identificacao = v_tag AND capacidade = 1);
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-007', 'cenario', 'Aceitar capacidade mínima positiva', 'esperado', 'Sala com capacidade 1 aceita',
      'resultado', IF(v_ok, 'APROVADO', 'REPROVADO'), 'codigo_mysql', 0,
      'observado', IF(v_ok, 'Condição confirmada por consulta.', 'A consulta não confirmou o estado esperado.')));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-008: Rejeitar capacidade zero
    SAVEPOINT caso;
    SET v_errno = 0, v_mensagem = '';
    BEGIN
      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
      BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_mensagem = MESSAGE_TEXT;
      END;
      INSERT INTO sala (identificacao, localizacao, capacidade, situacao) VALUES (v_tag, 'LOCAL FICTICIO', 0, 'ATIVO');
    END;
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-008', 'cenario', 'Rejeitar capacidade zero', 'esperado', 'Erro 3819 no CHECK de capacidade',
      'resultado', IF(v_errno = 3819 AND LOCATE('ck_sala_capacidade', v_mensagem) > 0,
          'APROVADO', 'REPROVADO'), 'codigo_mysql', v_errno,
      'observado', IF(v_errno = 0, 'A operação inválida foi aceita.', v_mensagem)));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-009: Aceitar datas iguais no período
    SAVEPOINT caso;
    INSERT INTO periodo_letivo (descricao, data_inicio, data_fim, situacao) VALUES (v_tag, '2026-01-10', '2026-01-10', 'ATIVO');
    SET v_ok = (SELECT COUNT(*) = 1 FROM periodo_letivo WHERE descricao = v_tag AND data_inicio = data_fim);
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-009', 'cenario', 'Aceitar datas iguais no período', 'esperado', 'Data final igual à inicial aceita pelo DDL',
      'resultado', IF(v_ok, 'APROVADO', 'REPROVADO'), 'codigo_mysql', 0,
      'observado', IF(v_ok, 'Condição confirmada por consulta.', 'A consulta não confirmou o estado esperado.')));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-010: Rejeitar término anterior ao início
    SAVEPOINT caso;
    SET v_errno = 0, v_mensagem = '';
    BEGIN
      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
      BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_mensagem = MESSAGE_TEXT;
      END;
      INSERT INTO periodo_letivo (descricao, data_inicio, data_fim, situacao) VALUES (v_tag, '2026-01-10', '2026-01-09', 'ATIVO');
    END;
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-010', 'cenario', 'Rejeitar término anterior ao início', 'esperado', 'Erro 3819 no CHECK de datas',
      'resultado', IF(v_errno = 3819 AND LOCATE('ck_periodo_datas', v_mensagem) > 0,
          'APROVADO', 'REPROVADO'), 'codigo_mysql', v_errno,
      'observado', IF(v_errno = 0, 'A operação inválida foi aceita.', v_mensagem)));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-011: Aceitar estoque e limites iguais a zero
    SAVEPOINT caso;
    INSERT INTO produto (descricao, tipo_produto, quantidade_disponivel, estoque_minimo, estoque_maximo, referencia, localizacao, codigo_barras, situacao) VALUES (v_tag, 'CONSUMIVEL', 0, 0, 0, v_tag, 'LOCAL FICTICIO', v_tag, 'ATIVO');
    SET v_ok = (SELECT COUNT(*) = 1 FROM produto WHERE descricao = v_tag AND quantidade_disponivel = 0 AND estoque_minimo = 0 AND estoque_maximo = 0);
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-011', 'cenario', 'Aceitar estoque e limites iguais a zero', 'esperado', 'Estoque zero e limites zero aceitos',
      'resultado', IF(v_ok, 'APROVADO', 'REPROVADO'), 'codigo_mysql', 0,
      'observado', IF(v_ok, 'Condição confirmada por consulta.', 'A consulta não confirmou o estado esperado.')));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-012: Rejeitar quantidade negativa
    SAVEPOINT caso;
    SET v_errno = 0, v_mensagem = '';
    BEGIN
      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
      BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_mensagem = MESSAGE_TEXT;
      END;
      INSERT INTO produto (descricao, tipo_produto, quantidade_disponivel, estoque_minimo, estoque_maximo, referencia, localizacao, codigo_barras, situacao) VALUES (v_tag, 'CONSUMIVEL', -1, 0, 10, v_tag, 'LOCAL FICTICIO', v_tag, 'ATIVO');
    END;
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-012', 'cenario', 'Rejeitar quantidade negativa', 'esperado', 'Erro 3819 no CHECK de quantidade',
      'resultado', IF(v_errno = 3819 AND LOCATE('ck_produto_quantidade', v_mensagem) > 0,
          'APROVADO', 'REPROVADO'), 'codigo_mysql', v_errno,
      'observado', IF(v_errno = 0, 'A operação inválida foi aceita.', v_mensagem)));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-013: Rejeitar máximo menor que mínimo
    SAVEPOINT caso;
    SET v_errno = 0, v_mensagem = '';
    BEGIN
      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
      BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_mensagem = MESSAGE_TEXT;
      END;
      INSERT INTO produto (descricao, tipo_produto, quantidade_disponivel, estoque_minimo, estoque_maximo, referencia, localizacao, codigo_barras, situacao) VALUES (v_tag, 'CONSUMIVEL', 0, 5, 4, v_tag, 'LOCAL FICTICIO', v_tag, 'ATIVO');
    END;
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-013', 'cenario', 'Rejeitar máximo menor que mínimo', 'esperado', 'Erro 3819 na relação entre limites de estoque',
      'resultado', IF(v_errno = 3819 AND LOCATE('ck_produto_estoque_maximo', v_mensagem) > 0,
          'APROVADO', 'REPROVADO'), 'codigo_mysql', v_errno,
      'observado', IF(v_errno = 0, 'A operação inválida foi aceita.', v_mensagem)));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-014: Rejeitar disciplina como próprio pré-requisito
    SAVEPOINT caso;
    SET v_errno = 0, v_mensagem = '';
    BEGIN
      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
      BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_mensagem = MESSAGE_TEXT;
      END;
      INSERT INTO disciplina_prerequisito (id_disciplina, id_disciplina_prerequisito) VALUES (v_disciplina, v_disciplina);
    END;
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-014', 'cenario', 'Rejeitar disciplina como próprio pré-requisito', 'esperado', 'Erro 3819 no CHECK de disciplinas distintas',
      'resultado', IF(v_errno = 3819 AND LOCATE('ck_disciplina_prerequisito_distinto', v_mensagem) > 0,
          'APROVADO', 'REPROVADO'), 'codigo_mysql', v_errno,
      'observado', IF(v_errno = 0, 'A operação inválida foi aceita.', v_mensagem)));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;

    -- CT-BD-015: Desvincular aluno ao excluir usuário opcional
    SAVEPOINT caso;
    DELETE FROM usuario WHERE id_usuario = v_usuario_opcional;
    SET v_ok = (SELECT COUNT(*) = 1 FROM aluno WHERE id_aluno = v_aluno AND id_usuario IS NULL);
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-015', 'cenario', 'Desvincular aluno ao excluir usuário opcional', 'esperado', 'Aluno preservado com id_usuario nulo',
      'resultado', IF(v_ok, 'APROVADO', 'REPROVADO'), 'codigo_mysql', 0,
      'observado', IF(v_ok, 'Condição confirmada por consulta.', 'A consulta não confirmou o estado esperado.')));
    ROLLBACK TO SAVEPOINT caso;
    RELEASE SAVEPOINT caso;
    -- CT-BD-016: o ROLLBACK não desfaz variáveis locais com os resultados.
    ROLLBACK;
    SET v_transacao = FALSE;
    SELECT SUM(qtd) INTO v_restantes FROM (
      SELECT COUNT(*) AS qtd FROM usuario WHERE identificacao LIKE CONCAT(v_tag, '%')
      UNION ALL SELECT COUNT(*) FROM perfil WHERE nome LIKE CONCAT(v_tag, '%')
      UNION ALL SELECT COUNT(*) FROM parametro_sistema WHERE chave = v_tag AND modulo = 'TESTE_INTEGRIDADE'
      UNION ALL SELECT COUNT(*) FROM disciplina WHERE nome = v_tag
      UNION ALL SELECT COUNT(*) FROM aluno WHERE identificacao = v_tag
      UNION ALL SELECT COUNT(*) FROM sala WHERE identificacao = v_tag
      UNION ALL SELECT COUNT(*) FROM periodo_letivo WHERE descricao = v_tag
      UNION ALL SELECT COUNT(*) FROM produto WHERE descricao = v_tag
      UNION ALL SELECT COUNT(*) FROM usuario_perfil WHERE id_usuario IN (v_usuario, v_usuario_opcional)
      UNION ALL SELECT COUNT(*) FROM disciplina_prerequisito WHERE id_disciplina = v_disciplina
    ) AS limpeza;
    SET v_resultados = JSON_ARRAY_APPEND(v_resultados, '$', JSON_OBJECT(
      'teste', 'CT-BD-016', 'cenario', 'Confirmar remoção dos dados de teste',
      'esperado', 'Zero registros desta execução após ROLLBACK',
      'resultado', IF(v_restantes = 0, 'APROVADO', 'REPROVADO'), 'codigo_mysql', 0,
      'observado', CONCAT('Registros restantes desta execução: ', v_restantes)));

    SET @scoa_ct_bd_resultados_v75 = v_resultados;
    SELECT VERSION() AS versao_mysql, DATABASE() AS banco,
           NOW() AS data_hora_servidor, @@SESSION.sql_mode AS sql_mode,
           v_tag AS identificador_execucao;
    SELECT * FROM JSON_TABLE(v_resultados, '$[*]' COLUMNS (
      teste VARCHAR(15) PATH '$.teste',
      cenario VARCHAR(150) PATH '$.cenario',
      esperado VARCHAR(200) PATH '$.esperado',
      resultado VARCHAR(15) PATH '$.resultado',
      codigo_mysql INT PATH '$.codigo_mysql',
      observado VARCHAR(2048) PATH '$.observado'
    )) AS detalhes ORDER BY teste;
    SELECT COUNT(*) AS total,
           SUM(resultado = 'APROVADO') AS aprovados,
           SUM(resultado = 'REPROVADO') AS reprovados,
           IF(COUNT(*) = 16 AND SUM(resultado = 'REPROVADO') = 0,
              'BATERIA APROVADA', 'BATERIA REPROVADA') AS conclusao
      FROM JSON_TABLE(v_resultados, '$[*]' COLUMNS (
        resultado VARCHAR(15) PATH '$.resultado'
      )) AS resumo;
END$$
DELIMITER ;

CALL scoa_ct_bd_v75_20261018();
DROP PROCEDURE scoa_ct_bd_v75_20261018;

-- Para consultar novamente os resultados, na MESMA CONEXÃO:
-- SELECT * FROM JSON_TABLE(@scoa_ct_bd_resultados_v75, '$[*]' COLUMNS (
--   teste VARCHAR(15) PATH '$.teste', resultado VARCHAR(15) PATH '$.resultado',
--   observado VARCHAR(2048) PATH '$.observado')) AS resultado ORDER BY teste;
-- Ao fechar a conexão, a variável desaparece. Exporte o resultado antes.
