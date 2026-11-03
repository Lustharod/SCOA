# Matriz implementada P08

A proposta inicial foi consolidada antes da codificação; esta matriz descreve os testes concretos escritos durante a implementação, sem alegação de precedência de todos os métodos.

## Unitários — EquivalenciaTest

Seis execuções: documentos obrigatórios, carga positiva, conclusão obrigatória, parecer inclusive para indeferimento, responsável obrigatório e limite Unicode sem truncamento.

## Integração — EquivalenciaJdbcIT

| Método | Verificação |
|---|---|
| deferimentoProduzHistoricoPlanoEPreRequisito | Pedido em análise não libera; deferimento reflete em histórico/plano e permite inscrição dependente. |
| indeferimentoPreservaPedidoSemCumprimentoEPermiteNovoPedido | Indeferimento não cumpre requisito; novo pedido preserva o anterior. |
| decisaoFinalNaoPodeSerReescrita | Decisão final imutável pelo serviço. |
| duplicidadeEFuturoNaoGravamPedido | Conclusão futura e duplicidade recusadas sem gravação indevida. |
| revalidaMatriculaEDestinoNaDecisao | Trancamento e mudança de carga entre pedido e confirmação impedem decisão. |
| inscricaoNoDestinoImpedeDeferimentoSemCancelar | Inscrição preservada; indeferimento ainda possível. |
| reconhecidaNaoAdmiteNovaInscricaoNoDestino | Destino reconhecido não é novamente inscrito. |
| reconhecimentoNaoTransportaParaReingresso | Reconhecimento não passa a outra matrícula do mesmo aluno. |
| legadoNaoViraReconhecimentoNemERegravado | Legado não libera pré-requisito e exige conciliação no plano/pedido. |
| planoLegadoNaoEAdotadoSilenciosamente | Plano legado preservado e diagnosticado. |
| transacaoExternaPodeDesfazerPedidoEDecisao | Pedido/decisão podem ser revertidos pelo chamador. |
| falhaDepoisDeUpdateDesfazDecisao | Falha JDBC após UPDATE reverte decisão e efeitos derivados. |
| duasDecisoesConcorrentesSomenteUmaFinaliza | Duas sessões não encerram o mesmo pedido duas vezes. |
| deferimentoConcorrenteComInscricaoNoDestinoNaoProduzAmbos | Concorrência preserva exclusão entre inscrição ativa e novo deferimento do destino. |
| reprovacaoAnteriorPermaneceAposReconhecimento | Histórico preserva reprovação; plano reconhece o destino sem apagar tentativas. |
| aprovacaoAnteriorEConclusaoNoPlanoSemPedidoArtificial | Resultado formal gera conclusão e impede duplicar cumprimento. |
| planoPreservaCumprimentoAoTrancarSemAutorizarNovasOperacoes | Consulta mostra situação atual e mantém cumprimento sem autorizar inscrição. |
| destinoForaDaEstruturaOuInativoNaoAdmitePedido | Destino curricular validado. |
| falhaDeDecisaoPreservaAlteracoesAnterioresDaTransacaoExterna | Recusa por validação preserva alterações anteriores do chamador. |
| estadoLegadoDeInscricaoNaoGeraConclusaoNoPlano | Estado APROVADO isolado não cria conclusão formal. |
| consoleDesistenciaEEofNaoGravam | Desistência e entrada interrompida não criam pedido. |

As 206 execuções existentes da v0.7 integram a regressão, sem somar execuções repetidas como novos casos. Esta matriz não prova todas as combinações concorrentes, importação do legado, autenticação ou validade material dos documentos.
