# Resultados de integração da execução do autor v0.4

Extraídos dos XMLs originais da execução encerrada em 31/10/2026 às 16:03:46 UTC−03:00. Nenhuma nova execução foi realizada para gerar este resumo. Propriedades locais e saídas de configuração não são reproduzidas.

| Classe | Execuções | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| AlocacaoJdbcIT | 17 | 0 | 0 | 0 |
| CadastroAcademicoJdbcIT | 12 | 0 | 0 | 0 |
| ProfessoresDisciplinasJdbcIT | 14 | 0 | 0 | 0 |
| SalaJdbcIT | 6 | 0 | 0 | 0 |
| **Total** | 49 | 0 | 0 | 0 |

## br.edu.cefetmg.scoa.persistencia.AlocacaoJdbcIT

| Execução | Resultado |
|---|---|
| transacaoExternaContinuaAbertaEAutocommitProprioERestaurado | Aprovado |
| disponibilidadeFiltraCapacidadeSituacaoEConflitos | Aprovado |
| permiteAdjacenciaEDiasDiferentes | Aprovado |
| alocaListaEDesaloca | Aprovado |
| atualizacaoSemMudancasComAffectedRowsNaoAcusaAusencia | Aprovado |
| permitePeriodosSemSobreposicao | Aprovado |
| rejeitaMesmaTurmaEmDuasSalasNoMesmoHorario | Aprovado |
| alocacaoConcorrenteProtegeCapacidadeESituacaoDaSala(boolean)[1] | Aprovado |
| alocacaoConcorrenteProtegeCapacidadeESituacaoDaSala(boolean)[2] | Aprovado |
| preservaCapacidadeESituacaoDeSalaComprometida | Aprovado |
| rejeitaCapacidadeInsuficienteESalaInativaSemGravar | Aprovado |
| alteracoesConcorrentesPreservamInativacaoECapacidade(boolean)[1] | Aprovado |
| alteracoesConcorrentesPreservamInativacaoECapacidade(boolean)[2] | Aprovado |
| rejeitaConflitoDaSalaEntrePeriodosComDatasSobrepostas | Aprovado |
| alocacoesConcorrentesNaoDuplicamSalaNemTurma(boolean)[1] | Aprovado |
| alocacoesConcorrentesNaoDuplicamSalaNemTurma(boolean)[2] | Aprovado |
| recusaEntidadesAusentes | Aprovado |

## br.edu.cefetmg.scoa.persistencia.CadastroAcademicoJdbcIT

| Execução | Resultado |
|---|---|
| periodoVinculadoPreservaDatasESituacaoMasPermiteDescricao | Aprovado |
| cadastraConsultaListaEAtualizaTurma | Aprovado |
| manutencaoDeTurmaPreservaAlocacoes | Aprovado |
| recusaIdsAusentes | Aprovado |
| turmaConcorrenteImpedeMudancaDasDatasDoPeriodo | Aprovado |
| capacidadeEAlocacaoConcorrentesNaoQuebramLimiteDaSala(boolean)[1] | Aprovado |
| capacidadeEAlocacaoConcorrentesNaoQuebramLimiteDaSala(boolean)[2] | Aprovado |
| preservaInscricoesExistentesECapacidadeMinima | Aprovado |
| inativacaoPreservaRegistroEImpedeNovasAlocacoes | Aprovado |
| rejeitaVinculosInexistentesOuInativosSemCriarTurma | Aprovado |
| atualizaEInativaPeriodoSemTurmas | Aprovado |
| alteracaoInvalidaPreservaDadosERollbackDoChamador | Aprovado |

## br.edu.cefetmg.scoa.persistencia.ProfessoresDisciplinasJdbcIT

| Execução | Resultado |
|---|---|
| p0409RecusaAutorreferenciaECiclosDiretosEIndiretos | Aprovado |
| p0414ConsoleCancelaERecusaSemGravar | Aprovado |
| p0404ConcorrenciaProfessorETurmaNasDuasOrdens(boolean)[1] | Aprovado |
| p0404ConcorrenciaProfessorETurmaNasDuasOrdens(boolean)[2] | Aprovado |
| p0406DisciplinaMantemIdentidadeESituacaoSemMudancas | Aprovado |
| p0407AusenciasNaoCriamRegistrosNemRelacoes | Aprovado |
| p0408AdicionaConsultaRemoveERecusaDuplicidade | Aprovado |
| p0403InativacaoPreservaTurmaAnteriorENaoReativa | Aprovado |
| p0410RelacoesConcorrentesNaoCriamCicloMesmoAposSnapshot | Aprovado |
| p0411TransacoesERollbackPreservamDados | Aprovado |
| p0402ProfessorMantemContaIdentidadeESituacao | Aprovado |
| p0404ProfessorComprometidoNaoInativa | Aprovado |
| p0413ConsoleConfirmaCadastrosERelacoes | Aprovado |
| p0412NovasReferenciasIntegramTurmas | Aprovado |

## br.edu.cefetmg.scoa.persistencia.SalaJdbcIT

| Execução | Resultado |
|---|---|
| ctJiSala002AtualizaSemCriarOutraSala | Aprovado |
| ctJiSala004TextoComAspasEComandoSqlPermaneceDado | Aprovado |
| ctJiSala005SalaRemovidaNaoERecriadaPorAtualizacao | Aprovado |
| ctJiSala006SalvarSemAlteracoesNaoAcusaAusencia | Aprovado |
| ctJiSala001CadastraERecuperaTodosOsCampos | Aprovado |
| ctJiSala003InativaPreservandoRegistro | Aprovado |
