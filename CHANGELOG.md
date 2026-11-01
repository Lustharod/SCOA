# Alterações do SCOA

## 0.6.0-SNAPSHOT

- Matrícula institucional no menu 14, com período de ingresso, correção, trancamento, retorno, cancelamento e histórico atômico.
- Consulta bibliotecária real: impedimento, identidade não resolvida, situação desconhecida ou falha de consulta não liberam matrícula/retorno.
- Duplicidade de vínculo vigente impedida; reingresso após encerramento mantém histórico e nova identidade, sem créditos automáticos.
- Migração aditiva V006; DDL v7.5 original e dados legados preservados. Período usado em matrícula admite somente correção de descrição.
- Plano P06 anterior ao código. Codex: 77 testes unitários e 95 de integração aprovados; validação do autor concluída em 01/11/2026 às 15:04:41 UTC−03:00, com os mesmos totais. Dois PDFs manuais (6 e 8 páginas) e sete CSVs preservados; falhas de consulta e retomada documentadas. Código validado: 87fb486. Word v7.9 atualiza o capítulo 9, preservando a v7.8.
## 0.5.0-SNAPSHOT

- Incluídos cadastro básico de alunos e cursos e estruturas curriculares com inclusão e retirada de disciplinas, no menu principal 13, com confirmação e cancelamento.
- Preservados conta opcional, identidade, situação e vínculos acadêmicos. Inativação de aluno é lógica; correção cadastral não reativa registros.
- Protegidas composições curriculares já vinculadas a planos. Inclusões duplicadas, inclusive concorrentes, são recusadas.
- Plano de 14 casos registrado antes da implementação. Verificação do Codex em 31/10/2026: 74 testes unitários e 66 de integração aprovados, incluindo as regressões anteriores. Validação do autor concluída com os mesmos totais em 31/10/2026 às 22:52:45 UTC−03:00; PDF manual de 12 páginas e seis CSVs registrados em 01/11/2026. Código validado: dc1a6f3.
- DDL e diagramas preservados; nota técnica e roteiro privado de validação preparados; revisão Word v7.8 atualiza o capítulo 9. Matrícula, inscrição e análise curricular permanecem pendentes.

## 0.4.0-SNAPSHOT

- Incluídos cadastro e manutenção de professores e disciplinas e inclusão, consulta e retirada de pré-requisitos, com confirmação pelo console no menu principal 12.
- Preservados vínculos de usuário e histórico de professores; protegida inativação com turmas ativas em período atual ou futuro.
- Rejeitadas autorreferência, duplicidade e ciclos diretos, indiretos e concorrentes de pré-requisitos, com transações e leituras coordenadas.
- Plano da entrega registrado antes da codificação, com 15 casos executados pelo Codex: 65 execuções unitárias e 49 JDBC aprovadas, incluindo as 91 regressões da v0.3 e testes de console. Validação do autor concluída em 31/10/2026, com os mesmos totais aprovados, PDF manual de 17 páginas, três capturas automatizadas e seis CSVs. Código validado: b714404.
- DDL e diagramas v7.5 preservados. Word v7.7 atualiza o capítulo 9 com a validação do autor; planos e evidências complementares rastreados.

## 0.3.0-SNAPSHOT

- Incluído relatório da validação do autor em 27–28/10/2026, com ambiente, resultados extraídos dos relatórios JUnit e seleção de capturas originais.

- Incluídos cadastro, consulta, listagem, atualização e inativação de períodos letivos e turmas, com confirmação das operações no console.
- Turmas vinculam disciplina e professor previamente cadastrados e ativos e um período letivo ativo.
- Protegidas datas e situação de períodos vinculados a turmas, preservados os horários e os vínculos das turmas e validada a capacidade frente às salas alocadas e inscrições registradas.
- Alocação passou a recusar turmas e períodos inativos, mantendo as consultas históricas.
- Validação: 56 execuções unitárias e 35 JDBC aprovadas, incluindo regressões e concorrência; fluxo do console validado. DDL v7.5 preservado.

## 0.2.0-SNAPSHOT

- Corrigida a perda de alterações entre instâncias no cadastro de salas. Consulta para alteração e gravação passaram a ocorrer sob bloqueio na mesma transação.
- Incluídas consulta de turmas e horários, pesquisa de salas disponíveis, alocação e desalocação pelo console.
- Implementadas as verificações de capacidade e disponibilidade de RF-MA-08 / RN-MA-03, considerando horários semanais e sobreposição de períodos letivos.
- Impedidas a inativação e a redução incompatível de capacidade de salas com alocações em períodos atuais ou futuros.
- Incluídos testes de concorrência com conexões MySQL independentes e dados demonstrativos exclusivos do schema de testes.
- Validação: 47 execuções unitárias e 23 JDBC aprovadas. Estrutura SQL v7.5 preservada.

## 0.1.0-SNAPSHOT

- Primeira implementação do gerenciamento de salas em Java e MySQL, com cadastro, consulta, listagem, atualização e inativação.
- Validação inicial: 32 execuções unitárias e seis JDBC aprovadas.
