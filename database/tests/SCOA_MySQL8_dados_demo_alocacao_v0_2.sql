-- Dados sintéticos exclusivos do schema de testes. Não modifica o schema scoa.
USE scoa_java_teste;
SET NAMES utf8mb4;
SET @demo = CONCAT('DEMO-', LEFT(REPLACE(UUID(), '-', ''), 8));
START TRANSACTION;
INSERT INTO professor (identificacao, situacao) VALUES (CONCAT(@demo, '-Professor'), 'ATIVO');
SET @professor = LAST_INSERT_ID();
INSERT INTO disciplina (codigo, nome, carga_horaria, situacao)
VALUES (@demo, 'Engenharia de Software demonstrativa', 60, 'ATIVO');
SET @disciplina = LAST_INSERT_ID();
INSERT INTO periodo_letivo (descricao, data_inicio, data_fim, situacao)
VALUES (@demo, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 6 MONTH), 'ATIVO');
SET @periodo = LAST_INSERT_ID();
INSERT INTO turma (id_disciplina, id_professor, id_periodo, codigo, capacidade, situacao)
VALUES (@disciplina, @professor, @periodo, CONCAT(@demo, '-T1'), 30, 'ATIVO');
SET @turma_1 = LAST_INSERT_ID();
INSERT INTO turma (id_disciplina, id_professor, id_periodo, codigo, capacidade, situacao)
VALUES (@disciplina, @professor, @periodo, CONCAT(@demo, '-T2'), 30, 'ATIVO');
SET @turma_2 = LAST_INSERT_ID();
INSERT INTO sala (identificacao, localizacao, capacidade, situacao)
VALUES (CONCAT(@demo, '-Sala A'), 'Bloco demonstrativo', 40, 'ATIVO');
SET @sala_a = LAST_INSERT_ID();
INSERT INTO sala (identificacao, localizacao, capacidade, situacao)
VALUES (CONCAT(@demo, '-Sala B'), 'Bloco demonstrativo', 40, 'ATIVO');
SET @sala_b = LAST_INSERT_ID();
INSERT INTO sala (identificacao, localizacao, capacidade, situacao)
VALUES (CONCAT(@demo, '-Sala pequena'), 'Bloco demonstrativo', 20, 'ATIVO');
SET @sala_pequena = LAST_INSERT_ID();
COMMIT;
SELECT @demo AS conjunto, @turma_1 AS turma_1, @turma_2 AS turma_2,
       @sala_a AS sala_a, @sala_b AS sala_b, @sala_pequena AS sala_pequena;
