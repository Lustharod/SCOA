# Resultados failsafe-reports do autor v0.6

Execução encerrada em 01/11/2026 às 15:04:41 UTC−03:00. Somente dados de teste; propriedades locais omitidas.

## br.edu.cefetmg.scoa.persistencia.AlocacaoJdbcIT

Execuções: 17; falhas/erros/ignorados: 0/0/0; tempo da classe: 9.638 s.

| Método ou invocação | Tempo em s | Resultado |
|---|---:|---|
| transacaoExternaContinuaAbertaEAutocommitProprioERestaurado | 3.435 | Aprovado |
| disponibilidadeFiltraCapacidadeSituacaoEConflitos | 0.372 | Aprovado |
| permiteAdjacenciaEDiasDiferentes | 0.303 | Aprovado |
| alocaListaEDesaloca | 0.344 | Aprovado |
| atualizacaoSemMudancasComAffectedRowsNaoAcusaAusencia | 0.302 | Aprovado |
| permitePeriodosSemSobreposicao | 0.234 | Aprovado |
| rejeitaMesmaTurmaEmDuasSalasNoMesmoHorario | 0.22 | Aprovado |
| alocacaoConcorrenteProtegeCapacidadeESituacaoDaSala(boolean)[1] | 0.552 | Aprovado |
| alocacaoConcorrenteProtegeCapacidadeESituacaoDaSala(boolean)[2] | 0.479 | Aprovado |
| preservaCapacidadeESituacaoDeSalaComprometida | 0.203 | Aprovado |
| rejeitaCapacidadeInsuficienteESalaInativaSemGravar | 0.205 | Aprovado |
| alteracoesConcorrentesPreservamInativacaoECapacidade(boolean)[1] | 0.579 | Aprovado |
| alteracoesConcorrentesPreservamInativacaoECapacidade(boolean)[2] | 0.563 | Aprovado |
| rejeitaConflitoDaSalaEntrePeriodosComDatasSobrepostas | 0.19 | Aprovado |
| alocacoesConcorrentesNaoDuplicamSalaNemTurma(boolean)[1] | 0.499 | Aprovado |
| alocacoesConcorrentesNaoDuplicamSalaNemTurma(boolean)[2] | 0.463 | Aprovado |
| recusaEntidadesAusentes | 0.151 | Aprovado |

## br.edu.cefetmg.scoa.persistencia.BaseAcademicaJdbcIT

Execuções: 17; falhas/erros/ignorados: 0/0/0; tempo da classe: 4.805 s.

| Método ou invocação | Tempo em s | Resultado |
|---|---:|---|
| p0511InclusoesConcorrentesNaoDuplicam | 0.58 | Aprovado |
| p0504CursoEStruturaAtualizamSemMudancas(boolean)[1] | 0.352 | Aprovado |
| p0504CursoEStruturaAtualizamSemMudancas(boolean)[2] | 0.311 | Aprovado |
| p0508SemUnicidadeInventada | 0.171 | Aprovado |
| p0505EstruturaPreservaCursoESituacao | 0.172 | Aprovado |
| p0510TransacaoExternaDesfazOperacoesEFalhaNaoDesfazAnterior | 0.225 | Aprovado |
| p0506ComponentesEP0507DuplicidadeEAusencia | 0.285 | Aprovado |
| p0508ReferenciasAusentes | 0.267 | Aprovado |
| p0510TransacaoPropriaConfirmaERestauraAposFalha | 0.265 | Aprovado |
| p0512ConsoleConfirmaFluxoCompleto | 0.462 | Aprovado |
| p0513ConsoleCancelaTodasAsGravacoes | 0.242 | Aprovado |
| p0503InativacaoPreservaHistoricoENaoReativa | 0.222 | Aprovado |
| p0509PlanoProtegeInclusaoERetirada | 0.331 | Aprovado |
| p0508RepositorioPreservaIdentidadesESituacoes | 0.209 | Aprovado |
| p0513ConsoleRecusaInvalidosESaiNoFimDaEntrada | 0.166 | Aprovado |
| p0502AlunoMantemContaIdentidadeESituacao | 0.203 | Aprovado |
| p0508ReferenciasInativas | 0.27 | Aprovado |

## br.edu.cefetmg.scoa.persistencia.CadastroAcademicoJdbcIT

Execuções: 12; falhas/erros/ignorados: 0/0/0; tempo da classe: 4.157 s.

| Método ou invocação | Tempo em s | Resultado |
|---|---:|---|
| periodoVinculadoPreservaDatasESituacaoMasPermiteDescricao | 0.292 | Aprovado |
| cadastraConsultaListaEAtualizaTurma | 0.269 | Aprovado |
| manutencaoDeTurmaPreservaAlocacoes | 0.342 | Aprovado |
| recusaIdsAusentes | 0.155 | Aprovado |
| turmaConcorrenteImpedeMudancaDasDatasDoPeriodo | 0.52 | Aprovado |
| capacidadeEAlocacaoConcorrentesNaoQuebramLimiteDaSala(boolean)[1] | 0.6 | Aprovado |
| capacidadeEAlocacaoConcorrentesNaoQuebramLimiteDaSala(boolean)[2] | 0.554 | Aprovado |
| preservaInscricoesExistentesECapacidadeMinima | 0.386 | Aprovado |
| inativacaoPreservaRegistroEImpedeNovasAlocacoes | 0.3 | Aprovado |
| rejeitaVinculosInexistentesOuInativosSemCriarTurma | 0.295 | Aprovado |
| atualizaEInativaPeriodoSemTurmas | 0.236 | Aprovado |
| alteracaoInvalidaPreservaDadosERollbackDoChamador | 0.152 | Aprovado |

## br.edu.cefetmg.scoa.persistencia.MatriculaJdbcIT

Execuções: 29; falhas/erros/ignorados: 0/0/0; tempo da classe: 8.362 s.

| Método ou invocação | Tempo em s | Resultado |
|---|---:|---|
| p0612BibliotecaEscritorCoordenado | 0.801 | Aprovado |
| p0608RetornoIndisponivelMantemHistorico | 0.364 | Aprovado |
| p0604FalhaSqlBibliotecaNaoLibera | 0.266 | Aprovado |
| p0614ConsoleConfirmacaoCancelamentoEEof | 0.593 | Aprovado |
| p0605DuplicidadeEReingresso | 0.355 | Aprovado |
| p0603PendenciaEncerradaOuImpeditiva(String)[1] | 0.205 | Aprovado |
| p0603PendenciaEncerradaOuImpeditiva(String)[2] | 0.186 | Aprovado |
| p0603PendenciaEncerradaOuImpeditiva(String)[3] | 0.199 | Aprovado |
| p0610ReferenciasInvalidas(String)[1] | 0.19 | Aprovado |
| p0610ReferenciasInvalidas(String)[2] | 0.183 | Aprovado |
| p0610ReferenciasInvalidas(String)[3] | 0.202 | Aprovado |
| p0610ReferenciasInvalidas(String)[4] | 0.208 | Aprovado |
| p0611FalhaNoHistoricoDesfazMatricula(boolean)[1] | 0.219 | Aprovado |
| p0611FalhaNoHistoricoDesfazMatricula(boolean)[2] | 0.22 | Aprovado |
| p0610DatasLimiteEMotivoUnicode | 0.274 | Aprovado |
| p0611RollbackExterno | 0.228 | Aprovado |
| p0607TransicoesEHistorico | 0.386 | Aprovado |
| p0609CorrecaoSemBibliotecaESemMudanca(boolean)[1] | 0.502 | Aprovado |
| p0609CorrecaoSemBibliotecaESemMudanca(boolean)[2] | 0.408 | Aprovado |
| p0612CriacoesConcorrentes | 0.292 | Aprovado |
| p0608RetornoBloqueadoEPreservacao | 0.366 | Aprovado |
| p0613PeriodoProtegidoELegado | 0.265 | Aprovado |
| p0605LegadoDesconhecidoBloqueia | 0.158 | Aprovado |
| p0602CriacaoEHistorico | 0.207 | Aprovado |
| p0604IdentificacaoOuSituacaoIndisponivel(String)[1] | 0.198 | Aprovado |
| p0604IdentificacaoOuSituacaoIndisponivel(String)[2] | 0.203 | Aprovado |
| p0604IdentificacaoOuSituacaoIndisponivel(String)[3] | 0.189 | Aprovado |
| p0604IdentificacaoOuSituacaoIndisponivel(String)[4] | 0.169 | Aprovado |
| p0603NaoImpeditiva | 0.142 | Aprovado |

## br.edu.cefetmg.scoa.persistencia.ProfessoresDisciplinasJdbcIT

Execuções: 14; falhas/erros/ignorados: 0/0/0; tempo da classe: 3.853 s.

| Método ou invocação | Tempo em s | Resultado |
|---|---:|---|
| p0409RecusaAutorreferenciaECiclosDiretosEIndiretos | 0.213 | Aprovado |
| p0414ConsoleCancelaERecusaSemGravar | 0.319 | Aprovado |
| p0404ConcorrenciaProfessorETurmaNasDuasOrdens(boolean)[1] | 0.485 | Aprovado |
| p0404ConcorrenciaProfessorETurmaNasDuasOrdens(boolean)[2] | 0.429 | Aprovado |
| p0406DisciplinaMantemIdentidadeESituacaoSemMudancas | 0.254 | Aprovado |
| p0407AusenciasNaoCriamRegistrosNemRelacoes | 0.212 | Aprovado |
| p0408AdicionaConsultaRemoveERecusaDuplicidade | 0.189 | Aprovado |
| p0403InativacaoPreservaTurmaAnteriorENaoReativa | 0.195 | Aprovado |
| p0410RelacoesConcorrentesNaoCriamCicloMesmoAposSnapshot | 0.473 | Aprovado |
| p0411TransacoesERollbackPreservamDados | 0.239 | Aprovado |
| p0402ProfessorMantemContaIdentidadeESituacao | 0.149 | Aprovado |
| p0404ProfessorComprometidoNaoInativa | 0.151 | Aprovado |
| p0413ConsoleConfirmaCadastrosERelacoes | 0.306 | Aprovado |
| p0412NovasReferenciasIntegramTurmas | 0.192 | Aprovado |

## br.edu.cefetmg.scoa.persistencia.SalaJdbcIT

Execuções: 6; falhas/erros/ignorados: 0/0/0; tempo da classe: 0.522 s.

| Método ou invocação | Tempo em s | Resultado |
|---|---:|---|
| ctJiSala002AtualizaSemCriarOutraSala | 0.101 | Aprovado |
| ctJiSala004TextoComAspasEComandoSqlPermaneceDado | 0.065 | Aprovado |
| ctJiSala005SalaRemovidaNaoERecriadaPorAtualizacao | 0.072 | Aprovado |
| ctJiSala006SalvarSemAlteracoesNaoAcusaAusencia | 0.098 | Aprovado |
| ctJiSala001CadastraERecuperaTodosOsCampos | 0.08 | Aprovado |
| ctJiSala003InativaPreservandoRegistro | 0.086 | Aprovado |

Total de execuções: **95**, sem falhas, erros ou ignorados.
