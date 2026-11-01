# Validação do SCOA v0.4 pelo autor

Versão **0.4.0-SNAPSHOT**. Código validado: **b714404cdc293f68dbd9f58b2a68ede13ff44ddc**. Responsável: **Rodrigo Lustosa**. Consolidação: **31/10/2026**.

A execução Maven terminou em **31/10/2026 às 16:03:46 UTC−03:00**, com **65 testes unitários e 49 de integração**, zero falhas, erros ou ignorados e **BUILD SUCCESS**. As 91 execuções da v0.3 estão incluídas nesses 114 resultados. O autor concluiu o roteiro manual no mesmo dia e exportou o estado do banco às **21:38:15 UTC−03:00**. O horário exato do roteiro manual não consta no PDF e não foi inferido da exportação.

## Identificação do código e escopo

O commit de implementação foi criado depois dos testes, sem modificação de código neste fechamento. Os 48 arquivos de fontes, testes, configuração Maven e SQL do manifesto da execução do Codex foram novamente conferidos por SHA-256, sem divergências. O [manifesto de fontes](FONTES_SHA256.json) preserva essa identificação; a3d0282 é somente a base da v0.3.

O escopo é RF-MA-02 / UC-MA-02, RF-MA-04 / UC-MA-04, coerência das relações de RN-MA-01 e integração com turmas de RF-MA-06. O [plano da v0.4](../../SCOA_Plano_Testes_Professores_Disciplinas_v0_4.pdf) foi registrado antes do código. A aprovação é restrita aos casos e condições desse plano; autenticação, autorização, interface web, contas, inscrições e elegibilidade do aluno continuam pendentes.

## Resultados automatizados

| Classe | Nível | Execuções | Falhas / erros / ignorados |
|---|---|---:|---|
| ServicoSalaTest | Unitário | 9 | 0 / 0 / 0 |
| AlocacaoTest | Unitário | 15 | 0 / 0 / 0 |
| CadastroAcademicoTest | Unitário | 9 | 0 / 0 / 0 |
| ProfessoresDisciplinasTest | Unitário | 9 | 0 / 0 / 0 |
| SalaTest | Unitário | 23 | 0 / 0 / 0 |
| AlocacaoJdbcIT | Integração | 17 | 0 / 0 / 0 |
| CadastroAcademicoJdbcIT | Integração | 12 | 0 / 0 / 0 |
| ProfessoresDisciplinasJdbcIT | Integração | 14 | 0 / 0 / 0 |
| SalaJdbcIT | Integração | 6 | 0 / 0 / 0 |
| **Total** | | **114** | **0 / 0 / 0** |

As listagens de [testes unitários](surefire-reports/RESULTADOS.md) e de [integração](failsafe-reports/RESULTADOS.md) foram extraídas dos nove XMLs originais, sem propriedades privadas. As três capturas automatizadas mostram a versão, o total unitário e o total de integração com BUILD SUCCESS.

## Rastreabilidade dos 15 casos planejados

| Caso | Métodos de comprovação nos XMLs do autor | Resultado |
|---|---|---|
| P04-01 | ProfessoresDisciplinasTest.p0401ProfessorNormalizaPreservaIdsEUnicode; p0401ProfessorRejeitaAusenciaExcessoEIdsInvalidos | Aprovado nos relatórios do autor |
| P04-02 | ProfessoresDisciplinasJdbcIT.p0402ProfessorMantemContaIdentidadeESituacao | Aprovado nos relatórios do autor |
| P04-03 | ProfessoresDisciplinasJdbcIT.p0403InativacaoPreservaTurmaAnteriorENaoReativa | Aprovado nos relatórios do autor |
| P04-04 | ProfessoresDisciplinasJdbcIT.p0404ProfessorComprometidoNaoInativa; p0404ConcorrenciaProfessorETurmaNasDuasOrdens, nas duas ordens | Aprovado nos relatórios do autor |
| P04-05 | ProfessoresDisciplinasTest.p0405DisciplinaAceitaLimites; p0405DisciplinaRecusaCargaInvalida; p0405DisciplinaRecusaDadosInvalidos, incluindo todas as invocações parametrizadas | Aprovado nos relatórios do autor |
| P04-06 | ProfessoresDisciplinasJdbcIT.p0406DisciplinaMantemIdentidadeESituacaoSemMudancas | Aprovado nos relatórios do autor |
| P04-07 | ProfessoresDisciplinasJdbcIT.p0407AusenciasNaoCriamRegistrosNemRelacoes | Aprovado nos relatórios do autor |
| P04-08 | ProfessoresDisciplinasJdbcIT.p0408AdicionaConsultaRemoveERecusaDuplicidade | Aprovado nos relatórios do autor |
| P04-09 | ProfessoresDisciplinasJdbcIT.p0409RecusaAutorreferenciaECiclosDiretosEIndiretos | Aprovado nos relatórios do autor |
| P04-10 | ProfessoresDisciplinasJdbcIT.p0410RelacoesConcorrentesNaoCriamCicloMesmoAposSnapshot | Aprovado nos relatórios do autor |
| P04-11 | ProfessoresDisciplinasJdbcIT.p0411TransacoesERollbackPreservamDados | Aprovado nos relatórios do autor |
| P04-12 | ProfessoresDisciplinasJdbcIT.p0412NovasReferenciasIntegramTurmas | Aprovado nos relatórios do autor |
| P04-13 | ProfessoresDisciplinasJdbcIT.p0413ConsoleConfirmaCadastrosERelacoes | Aprovado nos relatórios do autor |
| P04-14 | ProfessoresDisciplinasJdbcIT.p0414ConsoleCancelaERecusaSemGravar | Aprovado nos relatórios do autor |
| P04-15 | As sete classes anteriores da v0.3: 56 execuções unitárias e 35 JDBC, incluídas na bateria atual | Aprovado nos relatórios do autor |

P04-13 e P04-14 têm comprovação automatizada nos métodos de console, além do roteiro humano abaixo. Nem todas as condições dos casos de integração foram repetidas manualmente: concorrência, limites de dados, contas vinculadas, históricos e transações são comprovados pelos métodos automatizados indicados. Não se confunde a execução de um caso com a quantidade de métodos ou invocações parametrizadas.

## Evidência manual

O [PDF original do console](capturas/v0.4%20-%20Testes%20funcionais%20manuais.pdf) contém **17 páginas** e foi preservado sem edição. Sua orientação de impressão pode exigir rotação no leitor; a numeração de páginas abaixo é a original.

| Páginas | Conteúdo observado |
|---|---|
| 1–3 | Consulta e atualização do professor 18; criação das disciplinas 121, 122 e 123; listagem e consulta. |
| 4–5 | Atualização da disciplina 122 e inclusão/consulta de 122 → 121 e 122 → 123. |
| 6–7 | Recusas de 123 → 123 e 123 → 122; inclusão de 18 → 122; recusa de 121 → 18, que fecharia 18 → 122 → 121 → 18; recusa de duplicidade; retirada de 122 → 123. |
| 8–12 | Disciplinas preservadas após retirada; cancelamento de criação e atualização de professor e disciplina; cancelamento de inclusão e retirada de pré-requisito com consultas posteriores. |
| 12–14 | Recusa de inativação dos professores comprometidos 48 e 18; criação/inativação do professor 94; identificação atualizada sem reativação; criação do professor 95. |
| 15–17 | Criação do período 99 e turma 99 com disciplina 121/professor 95; recusa da inativação do professor 95; encerramento normal do console. |

## Conferência do estado final no banco

Foram preservados os seis CSVs originais do Workbench. Eles registram o estado final e complementam o PDF das operações; não reconstituem, por si, tentativas recusadas. Dados e datas futuras de períodos são sintéticos.

| Exportação | Estado observado |
|---|---|
| [ambiente](capturas/v0.4%20-%20Banco%20-%20ambiente.csv) | Data 31/10/2026 às 21:38:15, banco scoa_java_teste e servidor MySQL 8.0.46. |
| [Disciplinas](capturas/v0.4%20-%20Banco%20-%20Disciplinas.csv) | Disciplinas 121, 122 e 123 persistidas; disciplina 122 com código/nome atualizados e carga 55. |
| [períodos](capturas/v0.4%20-%20Banco%20-%20per%C3%ADodos.csv) | Período 99 preservado com início em 01/03/2027 e fim em 30/06/2027. |
| [Professores](capturas/v0.4%20-%20Banco%20-%20Professores.csv) | Professor 94 INATIVO com identificação atualizada; professor 95 ATIVO. |
| [pré-requisitos](capturas/v0.4%20-%20Banco%20-%20pr%C3%A9-requisitos.csv) | Restam 18 → 122 e 122 → 121; relação retirada 122 → 123 ausente. |
| [Turmas](capturas/v0.4%20-%20Banco%20-%20Turmas.csv) | Turma 99 ATIVO, capacidade 40, professor 95, disciplina 121 e período 99. |

## Integridade e fechamento

O [ambiente](ambiente.txt) diferencia servidor e driver. O [manifesto](MANIFESTO.md) registra hashes dos originais utilizados e da seleção revisada. O ambiente preenchido antes do fechamento foi preservado no arquivo local; seu texto residual da v0.3 foi corrigido a partir das evidências atuais. XMLs completos e configurações continuam privados.

Não houve nova execução dos testes nem acesso ao banco para produzir esta consolidação. A v0.4 está validada pelo autor e documentalmente fechada para este escopo. O Word v7.7 atualiza o capítulo 9; código e DDL permanecem os já testados. A criação destes commits locais não representa envio ao GitHub.
