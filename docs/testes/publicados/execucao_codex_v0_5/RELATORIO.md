# Relatório da verificação do Codex v0.5

A versão 0.5.0-SNAPSHOT foi verificada em 31/10/2026, com encerramento às 22:42:50 UTC−03:00, em instância MySQL exclusiva desta conversa. Resultado: BUILD SUCCESS, 74 testes unitários e 66 de integração, sem falhas, erros ou ignorados. As 114 execuções da v0.4 estão incluídas nos totais; foram acrescentadas 26 execuções.

Base Git consultada: 9c0f2e1, código anterior b714404. O código foi identificado posteriormente pelo commit dc1a6f36f6ec0f86aed7048be4e8ad846bb0dfd4. Os 53 arquivos de FONTES_SHA256.json foram comparados no fechamento e permaneceram idênticos. O commit foi criado depois dos testes, sem atribuir à execução uma data posterior. Os 11 XMLs originais e o console Maven completo foram preservados na pasta privada testes/evidencias/execucao_codex_v0_5.

## Correspondência com o plano anterior à codificação

| Caso | Evidência executável |
|---|---|
| P05-01 | BaseAcademicaTest.p0501CamposObrigatorios (4 execuções), p0501LimitesUnicodeENormalizacao, p0501SituacoesEIdentidadesValidas |
| P05-02 | BaseAcademicaJdbcIT.p0502AlunoMantemContaIdentidadeESituacao |
| P05-03 | BaseAcademicaJdbcIT.p0503InativacaoPreservaHistoricoENaoReativa |
| P05-04 | BaseAcademicaJdbcIT.p0504CursoEStruturaAtualizamSemMudancas (2 modos de useAffectedRows) |
| P05-05 | BaseAcademicaJdbcIT.p0505EstruturaPreservaCursoESituacao |
| P05-06 e P05-07 | BaseAcademicaJdbcIT.p0506ComponentesEP0507DuplicidadeEAusencia |
| P05-08 | BaseAcademicaTest.p0508IdentificadoresInvalidos (3 execuções); BaseAcademicaJdbcIT.p0508ReferenciasAusentes, p0508ReferenciasInativas, p0508RepositorioPreservaIdentidadesESituacoes e p0508SemUnicidadeInventada |
| P05-09 | BaseAcademicaJdbcIT.p0509PlanoProtegeInclusaoERetirada |
| P05-10 | BaseAcademicaJdbcIT.p0510TransacaoExternaDesfazOperacoesEFalhaNaoDesfazAnterior e p0510TransacaoPropriaConfirmaERestauraAposFalha |
| P05-11 | BaseAcademicaJdbcIT.p0511InclusoesConcorrentesNaoDuplicam |
| P05-12 | BaseAcademicaJdbcIT.p0512ConsoleConfirmaFluxoCompleto |
| P05-13 | BaseAcademicaJdbcIT.p0513ConsoleCancelaTodasAsGravacoes e p0513ConsoleRecusaInvalidosESaiNoFimDaEntrada |
| P05-14 | Todas as classes e os 140 resultados listados em RESULTADOS.md |

Os testes de console utilizam entrada controlada com asserções sobre as mensagens e a persistência, não capturas de uma sessão humana. A entrada pela opção principal 13 também foi conferida com a aplicação completa. A instância isolada continha as 71 tabelas do DDL v7.5; após a bateria, não restaram alunos com a etiqueta exclusiva CTP05. Fixtures concorrentes foram limpas por ID. Os bancos de uso do autor não foram alterados.

## Limites

A validação de Rodrigo Lustosa foi concluída em execução própria e está documentada separadamente em execucao_autor_v0_5. Nenhuma captura ou aprovação do autor foi produzida pelos testes do Codex. Matrícula, inscrição, análise curricular, equivalências, autenticação, autorização e demais módulos permanecem pendentes. A proteção curricular aplica-se aos caminhos implementados e exige que futuros escritores de planos respeitem o bloqueio da estrutura. Resultados completos estão em RESULTADOS.md, sem publicar propriedades privadas dos XMLs.
