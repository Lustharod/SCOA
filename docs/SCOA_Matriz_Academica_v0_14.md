# Matriz acadêmica — v0.14 em aceitação

Rastreabilidade do escopo acadêmico do Word de referência: **28 requisitos funcionais e 30 casos de uso**. O levantamento da v0.13 é a linha de base; as decisões do autor definem os contratos da implementação v0.14. Este documento distingue implementação disponível, evidência automática e aceitação manual pendente.

**Estado de todos os percursos abaixo:** implementados no protótipo; sujeitos à rodada manual integrada do autor e ao fechamento documental. A existência de código e testes não é registrada como aceitação manual, conclusão do projeto inteiro ou implantação institucional.

| Requisito | Caso de uso | Percurso disponível | Principais testes JDBC |
| --- | --- | --- | --- |
| RF-MA-01 Alunos | UC-MA-01 | Secretaria: criar/consultar/atualizar/inativar, preservando ID e histórico. | `BaseAcademicaJdbcIT`, `AcessoAcademicoJdbcIT`. |
| RF-MA-02 Professores | UC-MA-02 | Secretaria: cadastro e atividade; conta vinculada preservada. | `ProfessoresDisciplinasJdbcIT`. |
| RF-MA-03 Cursos | UC-MA-03 | Secretaria: cadastro, consulta e atualização. | `BaseAcademicaJdbcIT`, `PortalAcademicoJdbcIT`. |
| RF-MA-04 Disciplinas/requisitos | UC-MA-04 | Cadastro, carga, pré-requisitos, ciclos e compromissos. | `ProfessoresDisciplinasJdbcIT`, `InscricaoJdbcIT`. |
| RF-MA-05 Estrutura curricular | UC-MA-05 | Estruturas/componentes e vínculo curricular explícito da matrícula. | `BaseAcademicaJdbcIT`, `EquivalenciaJdbcIT`. |
| RF-MA-06 Turmas | UC-MA-06 | Ofertas por disciplina, professor, período, capacidade e horários. | `CadastroAcademicoJdbcIT`, `HorarioAcademicoJdbcIT`. |
| RF-MA-07 Salas | UC-MA-07 | Cadastro, capacidade, atividade e compromissos existentes. | `SalaJdbcIT`, `AlocacaoJdbcIT`. |
| RF-MA-08 Alocação | UC-MA-08 | Tempos antes da sala; alternativas, alocação posterior e histórico. | `AlocacaoJdbcIT`, `HorarioAcademicoJdbcIT`. |
| RF-MA-09 Matrícula | UC-MA-09 | Ingresso, histórico, correções, trancamento, reativação e cancelamento. | `MatriculaJdbcIT`. |
| RF-MA-10 Inscrição | UC-MA-10, UC-MA-30 | Elegibilidade, currículo, pré-requisitos, vagas, horários e confirmação do próprio aluno. | `InscricaoJdbcIT`, `AjustesAcademicosJdbcIT`, `ParametrosAcademicosJdbcIT`. |
| RF-MA-11 Rematrícula automática | UC-MA-11 | Lote com prévia, plano atual, resultados, biblioteca, propostas e ocorrências. Sem oferta: análise manual integral do candidato; sem inscrição automática. | `RematriculaLoteJdbcIT`, `MatriculaJdbcIT`. |
| RF-MA-12 Transferências | UC-MA-12 | Interna efetiva; entrada/saída externa com processo, documentos e mudança explícita de vínculo. | `TransferenciaInternaJdbcIT`, `TransferenciaExternaJdbcIT`. |
| RF-MA-13 Aproveitamento/equivalência | UC-MA-13 | Análise formal; automática somente para a mesma disciplina aprovada formalmente na transferência interna. | `EquivalenciaJdbcIT`, `ProcessoAcademicoJdbcIT`, `TransferenciaInternaJdbcIT`. |
| RF-MA-14 Plano de estudo | UC-MA-14, UC-MA-29 | Geração institucional versionada, prioridades, evidências, ofertas e consulta própria sem atualizar automaticamente. | `PlanejamentoJdbcIT`, `PlanoInstitucionalJdbcIT`. |
| RF-MA-15 Horários | UC-MA-15 | Consulta própria das inscrições ativas, inclusive sala pendente. | `ConsultasAlunoJdbcIT`, `HorarioAcademicoJdbcIT`. |
| RF-MA-16 Avaliações/notas | UC-MA-16 | Professor: P1/P2/PF, lançamentos e correções versionadas, fechamento/retificação. | `DiarioAcademicoJdbcIT`, `AjustesAcademicosJdbcIT`, `ParametrosAcademicosJdbcIT`. |
| RF-MA-17 Frequência | UC-MA-17 | Chamada por tempo, tolerância, importação fictícia deduplicada e exclusão de tempos anteriores à inscrição. | `DiarioAcademicoJdbcIT`, `ParametrosAcademicosJdbcIT`. |
| RF-MA-18 Pauta/conteúdo | UC-MA-18 | Registro docente de aulas por tempos, pauta e conteúdo. | `DiarioAcademicoJdbcIT`. |
| RF-MA-19 Diário | UC-MA-19 | Consulta completa docente, consulta individual e exportação; originais separados de resultado vigente. | `DiarioAcademicoJdbcIT`, `RelatorioAcademicoJdbcIT`, `PortalAcademicoJdbcIT`. |
| RF-MA-20 Metas/prazos | UC-MA-20 | Coordenação/gestão: cadastro, versões, situação, prazo e histórico. | `GestaoAcademicaJdbcIT`. |
| RF-MA-21 Auditoria do diário | UC-MA-21 | Retrato conferido, conclusão, observação e autoria; sem alterar nota/frequência. | `GestaoAcademicaJdbcIT`. |
| RF-MA-22 Consultar avaliações | UC-MA-22 | Filtros no alcance de curso, período, disciplina e turma. | `GestaoAcademicaJdbcIT`. |
| RF-MA-23 Monitoramento | UC-MA-23 | Trajetória, resultados e análise curricular; dados insuficientes explícitos. | `GestaoAcademicaJdbcIT`. |
| RF-MA-24 Histórico escolar | UC-MA-24 | Consulta própria de cursadas, reconhecimentos e legado preservado. | `AtendimentoAlunoJdbcIT`, `RelatorioAcademicoJdbcIT`. |
| RF-MA-25 Notas/frequência próprias | UC-MA-25 | Boletim próprio com pendências, versões, tempos considerados e resultado separado. | `DiarioAcademicoJdbcIT`, `ConsultasAlunoJdbcIT`, `PortalAcademicoJdbcIT`. |
| RF-MA-26 Documentos | UC-MA-26 | Solicitar declaração de matrícula/histórico, acompanhar e atender na secretaria com referência de entrega. | `DocumentoAcademicoJdbcIT`, `PortalAcademicoJdbcIT`. |
| RF-MA-27 Sugestões/reclamações | UC-MA-27 | Registro da própria pessoa com autoria e consulta própria. | `AtendimentoAlunoJdbcIT`, `PortalAcademicoJdbcIT`. |
| RF-MA-28 MEC | UC-MA-28 | Censo/ENADE demonstrativos aprovados para o protótipo; prévia e arquivos PDF/XLSX preservados. | `RelatorioAcademicoJdbcIT`. |

Todas as classes citadas estão em `src/test/java/br/edu/cefetmg/scoa/persistencia`. [Resultados por classe](testes/publicados/execucao_codex_v0_14/RESULTADOS.md), [ambiente](testes/publicados/execucao_codex_v0_14/ambiente.txt) e [hashes das fontes](testes/publicados/execucao_codex_v0_14/FONTES_SHA256.json). As contagens de versões sucessivas não devem ser somadas como testes distintos.

## Regras e controles transversais

| Frente | Comportamento e evidência | Situação de aceitação |
| --- | --- | --- |
| RF-GE-01/02/03 no Acadêmico | Autenticação, vínculo versus concessão técnica, perfis/contas, geração de sessão, proteção do último administrador. `SegurancaAcademicaJdbcIT`, `AdministracaoAcessoAcademicoJdbcIT`, testes HTTP. | Automática; manual integrada pendente. |
| RnF-03/05/07 | Propriedade/alcance, preservação de IDs/histórico, conflitos, repetição e rollback de falha de auditoria. | Cobertura nas baterias indicadas; não constitui certificação geral. |
| RnF-10 | Concorrência de vagas e sessões com conexões distintas; confirmação repetida não duplica gravação. | Cenários testados; carga institucional máxima não medida. |
| RnF-12 | Portal HTML/CSS local, sem scripts/CDNs externos; renderização selecionada com Edge. | Percurso manual em Chrome/Edge/Firefox ainda pendente. |
| RnF-13/14 e RI-01 | Contrato de chegadas fictícias; eventos repetidos/recusados não corrompem chamadas anteriores; biblioteca impeditiva consumida pelos fluxos acadêmicos. | Simulação/contrato; equipamento e fornecedor reais não conectados. |
| RnF-15 | Arquivos PDF e XLSX reais, autoria, retrato, hashes, limites e download preservado. | Automática/visual selecionada; conferência manual pendente. |
| RnF-16 | Doze parâmetros em versões imutáveis e seleção somente para períodos futuros. `ParametrosAcademicosJdbcIT`. | Automática; conferência manual pendente. |
| RnF-08/09/11 | Disponibilidade, prazo de resposta e backup/recuperação institucional. | Não declarados como integralmente verificados pelo protótipo local. |

## Alcance aprovado e pendências reais

O equipamento é fictício. Os documentos aprovados neste recorte são solicitação/acompanhamento de declaração de matrícula e histórico escolar com atendimento da secretaria. Os relatórios MEC são demonstrativos. Emissão/assinatura oficial, envio ao MEC, diploma, conclusão automática de curso e desenvolvimento integral de Biblioteca/Financeiro/RH/Compras/Patrimônio não são presumidos a partir desta matriz.

Faltam a rodada manual integrada do autor, a conferência de divergências, a revisão final do Word e o fechamento da v0.14. Qualquer falha encontrada reabre o item correspondente. A aceitação deve registrar os casos efetivamente executados, sem repetir operações para fabricar capturas.

Para **30/11/2026**, o percurso de apresentação deve mostrar conta única, oferta/inscrição, diário/resultado, gestão e consulta/documento/exportação, usando apenas o que estiver validado. Para **07/12/2026**, fechar divergências, rastreabilidade, documentação e pacote final do escopo aceito. O planejamento da próxima iteração precederá o guia de continuidade, conforme solicitado pelo autor.
