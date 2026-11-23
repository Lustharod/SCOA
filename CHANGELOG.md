# Alterações do SCOA

## 0.13.0-SNAPSHOT em validação

- Corrigida OC-V12-01: criação de conta escolhe apenas acesso técnico, com SEM_PERMISSOES como padrão. Aluno, professor e coordenação são obtidos dos vínculos; uma conta pode reunir vínculos legítimos sem ganhar concessões técnicas.
- Menus 18.12 e 18.13 consultam conta, vínculos e permissões técnicas, respectivamente para a própria pessoa e para administração. O provisionamento distingue ID da conta das referências de aluno, professor e curso.
- Preservados perfis legados, credenciais, IDs, permissões e históricos, sem migração de dados. Login único e restrições dos cadastros continuam vigentes; a aplicação não funde pessoas automaticamente.
- Regressão Codex aprovada em 23/11/2026: 130 unitários e 194 integrações. Rodada do autor e validação manual pendentes. Plano para 30/11 e 07/12 prioriza lançamento autenticado e propõe histórico próprio, sem declarar o módulo acadêmico concluído.

## 0.12.0-SNAPSHOT

- Fechamento em 23/11/2026: autor aprovou 128 testes unitários e 179 de integração e concluiu as rodadas manuais SEGUNDA_FASE, TARDIO e RETIFICACAO. A rechecagem Codex aprovou 128 unitários. Relatório, ambiente, resultados por classe e manifestos identificam a entrega; Word v7.16 e guias permanecem privados. PRIMEIRA_FASE foi opcional e não executada manualmente.
- Achado OC-V12-01, identificado pelo autor durante a validação manual em 21/11/2026: repetição da escolha de ALUNO/PROFESSOR/COORDENADOR na criação da conta e no vínculo acadêmico, sem verificação de correspondência entre essas escolhas, confirmada por inspeção do código. Decidido concluir a rodada atual e corrigir o provisionamento na próxima iteração, junto da próxima implementação.
- Menu 19 do console apresenta horários das inscrições ativas e notas e frequência do resultado final registrado. A consulta usa o vínculo entre a conta autenticada e o aluno e funciona com o schema da v0.11.
- A primeira fase de inscrição vai de D−14 a D−8 e a segunda de D+7 a D+13, sendo D o início das aulas. Desistência automática até D+13 libera vaga; deferimento posterior pela coordenação mantém a vaga ociosa. O histórico preserva solicitação e decisão. Cancelar impede nova inscrição na mesma disciplina e no mesmo período.
- Menu 20 reúne confirmação independente de turmas, cancelamento, retificação, rematrícula institucional e registro de pendência bibliotecária. A retificação mantém o resultado anterior no histórico e usa a primeira fase do período sucessor do curso como limite para deferimento. A renovação preserva o número da matrícula e a inscrição em turma exige confirmação em separado.
- V010 e V011 acrescentam dados e restrições das operações. O schema v7.5 e as migrações anteriores continuam como base da instalação em `scoa_java_teste`.
- V012 coordena o cadastro do calendário. Criação e alteração de datas recusam sobreposição. Todas as turmas usam as datas do período associado e a retificação encontra automaticamente o primeiro período ativo posterior.
- Menu 18.11 vincula contas a alunos, professores e cursos coordenados com autorização administrativa e histórico. Os papéis ALUNO, PROFESSOR e COORDENADOR podem ser criados pelo mesmo submenu.
- Corrigida a gravação da renovação institucional para a chave composta de matrícula e período.
- Atualizada a regressão do planejamento para verificar a vaga liberada, o bloqueio de reinscrição do aluno no mesmo período e a oferta permitida em período posterior.

## 0.11.0-SNAPSHOT

- O menu 17 encaminha listagem e simulação por `ServicoPlanejamento` e pelo contrato `RepositorioPlanejamento`.
- O menu 18, opção 9, encaminha a consulta por `ServicoConsultaProcessosAcademicos`. A autorização integra a transação JDBC da consulta.
- Autor e Codex aprovaram 283 testes em execuções independentes. O [relatório da v0.11](docs/testes/publicados/execucao_autor_v0_11/RELATORIO.md) registra o percurso manual e a cobertura do caso de período duplicado.

## 0.10.0-SNAPSHOT

- Menu 18: acesso autenticado, permissões técnicas, conciliação documental explícita de planos/equivalências e análise humana de revisão/revogação.
- Suspensão de novos usos durante revisão; complemento e tratamento mantêm suspensão; rejeição restaura uso sujeito às demais validações; revogação preserva expressamente inscrições identificadas, sem cancelamento ou alteração de resultados.
- Rastreabilidade dos pré-requisitos usados em novas inscrições. Histórico sem prova é apresentado como impacto potencial, sem causalidade inventada.
- Migração aditiva V009 com cinco tabelas; decisões originais, legados e versões documentais anteriores preservados. Guias e evidências brutas privados. Sem migração de scoa ou acesso ao banco do autor.

- Autor: 282 execuções automatizadas sem falhas, erros ou ignorados e validação manual concluída em 09/11/2026. Implementação validada: c7950be8f4c86cbc26e0616e86454adf3c7a083e.

## 0.9.0-SNAPSHOT

- Menu 17: simulação com períodos e ofertas cadastrados, prioridade explícita, pré-requisitos já cumpridos, diagnóstico de conflitos/vagas e soma das horas sugeridas.
- Consulta sem persistir plano ou inscrição; sugestões e andamento não liberam dependentes. Sem migração nova, créditos, limite institucional ou reserva de vagas.
- Autor: verify encerrado em 03/11/2026 21:57:27 UTC−03:00, duração 17.900 s; 113 unitárias e 144 integração aprovadas. Codex separado em 02/11/2026. 62 originais preservados; seleção pública revisada com seis PDFs e 15 CSVs.
- Word v7.12 privado acrescenta seção 9.14 e Quadros 43/44, preservando a v7.11. Código validado identificado retrospectivamente por b0a3930; fontes preservadas no fechamento.

## 0.8.0-SNAPSHOT

- Menu 16: pedido de equivalência com uma origem documental e um destino, decisão manual do funcionário administrativo e preservação da decisão final.
- Histórico acadêmico distingue resultados, pedidos e decisões; plano básico é derivado da estrutura vinculada e das evidências formais, com situação atual da matrícula.
- Reconhecimento formal pode cumprir pré-requisitos na mesma matrícula; inscrições e resultados anteriores permanecem preservados. Casos com impacto e legados não conciliados são recusados explicitamente.
- Migração aditiva V008 acrescenta pedido_equivalencia, sem alterar ou promover dados legados. Distribuição por períodos, revisão/revogação, autenticação e demais limites constam da nota técnica.
- Validação do autor em 02/11/2026 às 16:22:06 UTC−03:00: 103 unitárias e 130 de integração aprovadas; 233 execuções, zero falhas/erros/ignorados. Relatório, ambiente, manifestos e seis cópias revisadas preservam a separação da execução do Codex. Fechamento documental em revisão Word v7.11, privada.

## 0.7.0-SNAPSHOT

- Menu 15: inscrição com confirmação, contexto curricular explícito, janela [início−14 dias, início−7 dias), horários, pré-requisitos por resultado formal, duplicidade e vagas.
- Notas 0–10 com uma casa, P1/P2/PF e frequência mínima exata de 75%; resultado final e situação persistidos atomicamente após encerramento do período.
- Migração aditiva V007 e proteção de composição curricular e horários vinculados. Equivalências, plano e retificação continuam pendentes conforme a nota técnica.
- Validação do autor em 01/11/2026 às 21:07:52 UTC−03:00: 97 unitários e 109 de integração aprovados. Execução do Codex e originais preservados separadamente.

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
