USE scoa;

SELECT
    VERSION() AS versao_mysql,
    DATABASE() AS banco_selecionado,
    @@SESSION.foreign_key_checks AS verifica_chaves_estrangeiras;

SELECT
    CONSTRAINT_TYPE AS tipo,
    COUNT(*) AS quantidade
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'scoa'
GROUP BY CONSTRAINT_TYPE
ORDER BY CONSTRAINT_TYPE;

SELECT
    ENFORCED AS aplicada,
    COUNT(*) AS quantidade
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'scoa'
  AND CONSTRAINT_TYPE = 'CHECK'
GROUP BY ENFORCED;

SELECT
    ENGINE,
    TABLE_COLLATION,
    COUNT(*) AS quantidade
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'scoa'
  AND TABLE_TYPE = 'BASE TABLE'
GROUP BY ENGINE, TABLE_COLLATION;

SELECT DISTINCT
    TABLE_NAME AS tabela,
    INDEX_NAME AS indice
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'scoa'
  AND INDEX_NAME IN (
      'idx_turma_disciplina_periodo',
      'idx_inscricao_situacao',
      'idx_pagamento_cobranca_data',
      'idx_pendencia_biblioteca_situacao',
      'idx_produto_descricao',
      'idx_bem_setor'
  )
ORDER BY TABLE_NAME;