# Resultados unitários da execução do autor v0.4

Extraídos dos XMLs originais da execução encerrada em 31/10/2026 às 16:03:46 UTC−03:00. Nenhuma nova execução foi realizada para gerar este resumo. Propriedades locais e saídas de configuração não são reproduzidas.

| Classe | Execuções | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| ServicoSalaTest | 9 | 0 | 0 | 0 |
| AlocacaoTest | 15 | 0 | 0 | 0 |
| CadastroAcademicoTest | 9 | 0 | 0 | 0 |
| ProfessoresDisciplinasTest | 9 | 0 | 0 | 0 |
| SalaTest | 23 | 0 | 0 | 0 |
| **Total** | 65 | 0 | 0 | 0 |

## br.edu.cefetmg.scoa.aplicacao.ServicoSalaTest

| Execução | Resultado |
|---|---|
| ctJuSala018ListagemNaoPermiteAlterarOsCadastrosPeloRetorno | Aprovado |
| ctJuSala016RepetirInativacaoMantemOEstado | Aprovado |
| ctJuSala014AtualizacaoInvalidaPreservaCadastroAnterior | Aprovado |
| ctJuSala019AtualizarSalaInativaNaoReativaImplicitamente | Aprovado |
| ctJuSala011CadastraSalaEDevolveIdentificador | Aprovado |
| ctJuSala017OperacoesSobreSalaAusenteSaoRejeitadas | Aprovado |
| ctJuSala015InativaSemExcluirHistoricoDoCadastro | Aprovado |
| ctJuSala013AtualizaMantendoIdentidadeESituacao | Aprovado |
| ctJuSala012DadoInvalidoNaoChegaAoRepositorio | Aprovado |

## br.edu.cefetmg.scoa.dominio.AlocacaoTest

| Execução | Resultado |
|---|---|
| datasInvalidasEIdsInvalidosSaoRejeitados | Aprovado |
| intervaloInvalidoERejeitado | Aprovado |
| salaInativaERejeitada | Aprovado |
| intersecaoDeDatasSemODiaEscolhidoNaoConflita | Aprovado |
| diaSemOcorrenciaNoPeriodoERejeitado | Aprovado |
| periodosComIdsDiferentesMasDatasSobrepostasConflitam | Aprovado |
| capacidadeIgualADaTurmaEPermitida | Aprovado |
| salaMenorQueTurmaERejeitada | Aprovado |
| diaInvalidoERejeitado(int)[1] | Aprovado |
| diaInvalidoERejeitado(int)[2] | Aprovado |
| diaInvalidoERejeitado(int)[3] | Aprovado |
| intersecaoEContencaoDeHorariosConflitam | Aprovado |
| periodosDistintosNaoConflitam | Aprovado |
| diasDiferentesNaoConflitam | Aprovado |
| horariosAdjacentesNaoConflitam | Aprovado |

## br.edu.cefetmg.scoa.dominio.CadastroAcademicoTest

| Execução | Resultado |
|---|---|
| camposObrigatorios(String)[1] | Aprovado |
| camposObrigatorios(String)[2] | Aprovado |
| cadastraPeriodoETurmaSemIdERemoveEspacos | Aprovado |
| capacidadeInvalida(int)[1] | Aprovado |
| capacidadeInvalida(int)[2] | Aprovado |
| capacidadeInvalida(int)[3] | Aprovado |
| idsInvalidos | Aprovado |
| datasForaDoTipoDateSaoRejeitadas | Aprovado |
| limitesDeTextoEUnicode | Aprovado |

## br.edu.cefetmg.scoa.dominio.ProfessoresDisciplinasTest

| Execução | Resultado |
|---|---|
| p0405DisciplinaRecusaDadosInvalidos | Aprovado |
| p0405DisciplinaAceitaLimites(int)[1] | Aprovado |
| p0405DisciplinaAceitaLimites(int)[2] | Aprovado |
| p0405DisciplinaAceitaLimites(int)[3] | Aprovado |
| p0401ProfessorNormalizaPreservaIdsEUnicode | Aprovado |
| p0401ProfessorRejeitaAusenciaExcessoEIdsInvalidos | Aprovado |
| p0405DisciplinaRecusaCargaInvalida(int)[1] | Aprovado |
| p0405DisciplinaRecusaCargaInvalida(int)[2] | Aprovado |
| p0405DisciplinaRecusaCargaInvalida(int)[3] | Aprovado |

## br.edu.cefetmg.scoa.dominio.SalaTest

| Execução | Resultado |
|---|---|
| ctJuSala005RejeitaTextosQueExcedemOsCamposDoBanco | Aprovado |
| aceitaCapacidadeDentroDosLimites(int)[1] | Aprovado |
| aceitaCapacidadeDentroDosLimites(int)[2] | Aprovado |
| aceitaCapacidadeDentroDosLimites(int)[3] | Aprovado |
| ctJuSala006AceitaLimitesDeTextoECaracteresUnicode | Aprovado |
| ctJuSala009PreservaIdAcimaDoLimiteDeIntDoJava | Aprovado |
| rejeitaLocalizacaoAusente(String)[1] | Aprovado |
| rejeitaLocalizacaoAusente(String)[2] | Aprovado |
| rejeitaLocalizacaoAusente(String)[3] | Aprovado |
| rejeitaIdForaDoIntervaloDoBanco(long)[1] | Aprovado |
| rejeitaIdForaDoIntervaloDoBanco(long)[2] | Aprovado |
| rejeitaIdForaDoIntervaloDoBanco(long)[3] | Aprovado |
| rejeitaCapacidadeForaDosLimites(int)[1] | Aprovado |
| rejeitaCapacidadeForaDosLimites(int)[2] | Aprovado |
| rejeitaCapacidadeForaDosLimites(int)[3] | Aprovado |
| ctJuSala007RemoveEspacosDasExtremidades | Aprovado |
| rejeitaSituacaoAusente(String)[1] | Aprovado |
| rejeitaSituacaoAusente(String)[2] | Aprovado |
| rejeitaSituacaoAusente(String)[3] | Aprovado |
| rejeitaIdentificacaoAusente(String)[1] | Aprovado |
| rejeitaIdentificacaoAusente(String)[2] | Aprovado |
| rejeitaIdentificacaoAusente(String)[3] | Aprovado |
| rejeitaIdentificacaoAusente(String)[4] | Aprovado |
