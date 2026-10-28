# Resultados extraídos dos relatórios surefire-reports

Execução do autor em 27/10/2026. Código testado: e9efc1b. Extração em 28/10/2026.

Este documento foi gerado dos XMLs originais, sem executar novamente os testes. Foram omitidas as propriedades do ambiente, caminhos locais e saída de console. Os originais permanecem preservados localmente; o manifesto registra seus hashes. Cada linha abaixo representa uma execução JUnit, inclusive as repetições parametrizadas.

| Classe | Executados | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| br.edu.cefetmg.scoa.aplicacao.ServicoSalaTest | 9 | 0 | 0 | 0 |
| br.edu.cefetmg.scoa.dominio.AlocacaoTest | 15 | 0 | 0 | 0 |
| br.edu.cefetmg.scoa.dominio.CadastroAcademicoTest | 9 | 0 | 0 | 0 |
| br.edu.cefetmg.scoa.dominio.SalaTest | 23 | 0 | 0 | 0 |
| **Total** | **56** | **0** | **0** | **0** |

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
