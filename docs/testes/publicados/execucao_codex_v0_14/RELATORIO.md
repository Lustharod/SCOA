# Validação Codex do incremento acadêmico v0.14

Em 25/11/2026, o incremento com gestão acadêmica e auditoria docente passou em **175 testes unitários e 307 testes de integração**, sem falhas, erros ou ignorados. O Maven exibiu BUILD SUCCESS às 11:03:44 UTC-03:00, com duração de 01:24 min. A rodada manual do autor será integrada ao final dos blocos; ainda não foi realizada na v0.14.

## Gestão acadêmica e auditoria docente

Dezoito casos JDBC verificam o alcance institucional do gestor, os cursos do coordenador, a ausência de poder acadêmico implícito de ADMIN, revogação de conta/vínculo, vínculo de gestor na mesma conta sem concessões técnicas, metas/prazos com autoria, versões e histórico, conflito de versão, estado terminal, legados sem contexto e rollback em falha de auditoria. Também verificam filtros de avaliações, turmas compartilhadas com alunos separados por curso, consulta do diário original e do resultado vigente, auditoria com retrato e assinatura conferida antes da gravação e monitoramento sem concluir curso nem presumir dados ausentes. Três casos de domínio verificam atrasos calculados sem alterar estados e validações de meta/auditoria.

A auditoria registra conclusão e observação; não altera notas, chamadas ou resultados. A primeira tentativa falhou em uma fixture que usava Long.MAX_VALUE como ID, fora do limite UINT já validado. A fixture foi corrigida para 4.294.967.295, sem afrouxar a validação de produção. A regressão completa seguinte passou e os relatórios das duas tentativas estão preservados.

## Regressão e rastreabilidade

Todos os testes anteriores de acesso, cadastros, matrícula, inscrição, reconhecimento, planejamento, diário, transferências internas, documentos e atendimento permanecem na bateria. [Resultados por classe](RESULTADOS.md).

As contagens de versões sucessivas não devem ser somadas como casos distintos. O incremento anterior publicado em e6f6e92726f39a016fe7791241bf295a83192811 tinha 172 unitários e 289 integrações. Os originais de cada execução, inclusive tentativas que falharam, permanecem privados e identificados separadamente.

Instância MySQL 8.0.46 nova, scoa_java_teste, porta local 33386; DDL de testes v7.5 e V006 a V019. Nenhuma migração de scoa. As fixtures SQL são preparação/restauração dos testes, sem atribuição a atos auditados do console. Log completo, XML/TXT e manifesto permanecem privados. Os [hashes das fontes](FONTES_SHA256.json) foram conferidos após a execução. [Ambiente](ambiente.txt).

Este ponto amplia o ramo desenvolvimento/academico-completo. Main permanece na v0.13 fechada. A v0.14 e o módulo acadêmico continuam em desenvolvimento. Transferências externas, relatórios/exportações, horários sem sala, administração operacional de concessões, parametrização e interface web seguem pendentes. [Regras e limites](../../../SCOA_Implementacao_v0_14.md).
