# Equivalência manual, histórico e plano básico — v0.8

Implementação sobre o fechamento d081bcd da v0.7. Software 0.8.0-SNAPSHOT. Este recorte avança RF-MA-13/RN-MA-08, RF-MA-14/RN-MA-09 e a utilização dos reconhecimentos por UC-MA-30. Não encerra todos os processos acadêmicos.

## Definições do autor e fluxo

O autor definiu uma origem para um destino por pedido. O aluno apresenta documentos; o funcionário administrativo analisa manualmente ementa e carga horária e defere ou indefere. O SCOA registra o processo, sem calcular percentuais de compatibilidade ou decidir automaticamente. São regras do projeto, sem alegação de consulta a regulamento institucional.

O menu 16 registra instituição e nome da origem, carga horária em horas, ementa considerada, data de conclusão, referências dos documentos de conclusão/ementa/carga e ementa do destino considerada. A origem é uma descrição documental preservada, inclusive para estudo externo; não se cria disciplina curricular fictícia. As referências não são upload, assinatura, verificação automática de autenticidade ou armazenamento dos anexos. O administrativo deve conferir os documentos fora do sistema.

O destino precisa estar ativo e integrar a estrutura explicitamente vinculada à matrícula. O pedido guarda a estrutura e a carga do destino consideradas. Pedido e decisão exigem aluno, matrícula, curso e estrutura ativos. A data da origem não pode ser futura; usa-se a data UTC do servidor convertida para America/Sao_Paulo, como na v0.7.

EM_ANALISE não comprova cumprimento. DEFERIDO ou INDEFERIDO exige parecer sobre ementa/carga e funcionário responsável declarado. Não há ator autenticado nem controle de acesso. Confirmação é exigida no console; desistência ou EOF não grava a operação não confirmada. A decisão revalida contexto e impactos. Sua gravação final é imutável neste fluxo. Novo pedido após indeferimento preserva o anterior; pedido em análise ou deferido impede duplicação do destino na mesma matrícula.

## Histórico e plano

O histórico acadêmico lista resultados finais, abertura de pedidos, decisões e referências de procedência. Não é o histórico administrativo de mudanças da matrícula do menu 14. Registros legados são identificados como não conciliados, sem atribuir reconhecimento nem inventar instante de registro. Não se cria aprovação por nota, frequência, turma ou inscrição para representar equivalência. O histórico exibido é uma consulta acadêmica do recorte, não documento oficial completo de histórico escolar.

O plano básico é gerado a cada consulta sob transação e leituras correntes, sem persistir cópias divergentes em plano_estudo/item_plano_estudo. Inclui todas as disciplinas da estrutura vinculada e a situação atual da matrícula. CONCLUIDA depende de resultado formal APROVADO com período encerrado; RECONHECIDA depende de pedido DEFERIDO; EM_ANDAMENTO indica inscrição ATIVO e não comprova conclusão; PENDENTE mantém disciplinas sem cumprimento, inclusive após reprovação. Reprovações e tentativas continuam no histórico. O plano não distribui por períodos, não seleciona ofertas e não calcula créditos ou integralização completa. Consulta de matrícula trancada/cancelada preserva cumprimentos; não autoriza inscrição.

Conclusão e decisão persistidas são fontes das consultas derivadas: um deferimento não exige atualizar uma cópia de histórico/plano. Falha da transação preserva a situação anterior; não existe uma janela em que uma cópia de plano esteja pendente de atualização após commit.

## Inscrição e impactos

O reconhecimento formal v0.8 pode cumprir pré-requisito direto da mesma matrícula e estrutura, ao lado dos resultados formais v0.7. Data/hora da decisão deve ser não futura no servidor. A confirmação da inscrição revalida o reconhecimento; não há transporte entre reingressos, equivalência transitiva ou revisão retroativa da elegibilidade de inscrições anteriores.

Destino já reconhecido não admite nova inscrição pelo fluxo simples. Destino com inscrição ativa não admite deferimento; permanece inscrito e o pedido permanece em análise. O indeferimento pode encerrar esse pedido sem cancelar a inscrição. Aprovação anterior impede duplicar cumprimento; reprovação formal anterior pode coexistir com reconhecimento posterior e permanece preservada. Estados desconhecidos ou inscrições encerradas sem resultado formal coerente exigem análise de impacto.

Revisar/revogar decisão, apagar resultado, cancelar inscrição por equivalência ou resolver efeitos encadeados em inscrições anteriores não são operações oferecidas. Mudança da carga do destino entre pedido e decisão impede fechamento até análise específica. A decisão conserva a carga analisada mesmo se o cadastro mudar depois. Escritores externos que ignorem o protocolo não têm garantia de consistência; não há alegação de imutabilidade contra SQL administrativo direto.

## Legados

O fluxo novo não altera aproveitamento_equivalencia, plano_estudo ou item_plano_estudo. Qualquer processo/plano legado da matrícula exige conciliação explícita antes de registrar pedidos ou gerar o novo plano. Essa conciliação ainda não tem ferramenta de importação nesta versão: o sistema diagnostica e preserva, sem escolher estrutura/plano por inferência. O vínculo curricular imutável v0.7 continua preservado. Um DEFERIDO legado isolado continua sem liberar pré-requisito; conclusões formais válidas seguem o contrato anterior.

## Banco e concorrência

V008__equivalencia_manual.sql acrescenta pedido_equivalencia: 76 tabelas sobre as 75 da v0.7. DDL v7.5, V006 e V007 permanecem intactos. A migração é manual, aditiva e de aplicação única, após backup, seleção explícita do schema e conferência das migrações anteriores. Não reaplicar parcialmente nem recriar banco. DDL MySQL não torna toda a migração uma transação atômica.

Chaves estrangeiras preservam referências. A unicidade do destino em uso impede dois pedidos simultaneamente em análise/deferidos na mesma matrícula; indeferidos são conservados. A decisão e sua data são escritas juntas. O protocolo reutiliza controle_inscricao, aluno, matrícula e leituras bloqueadas de disciplinas/estrutura. Decisão e inscrição do mesmo contexto são coordenadas; resultado final usa a coordenação por aluno existente. Transações externas permanecem com o chamador por savepoints. O plano derivado usa esse mesmo contexto de leitura.

## Validação e pendências

Consultar docs/testes/publicados/execucao_codex_v0_8/RELATORIO.md para execução efetiva. A proposta inicial antecedeu a implementação; contratos detalhados e testes concretos foram elaborados durante o desenvolvimento. Não se afirma que todos os testes antecederam o código.

Validação do autor e atualização acadêmica do Word permanecem etapas separadas. Word v7.10, evidências v0.7 e bancos do autor não foram usados como ambiente de teste. Distribuição do plano por períodos, combinações de origens/destinos, conciliação importadora do legado, revisão/revogação, retificação de notas, rematrícula automática, transferência de estrutura, integralização, autenticação/autorização, web e recuperação de conexão continuam pendentes.


## Fechamento da validação do autor

O autor concluiu verify -Pmysql-it em 02/11/2026 às 16:22:06 UTC−03:00, duração 48.069 s: 103 unitárias e 130 de integração, sem falhas, erros ou ignorados. Os 17 XMLs corroboram as 233 execuções, com os mesmos métodos/parametrizações do Codex. O manual de sete páginas e os quatro CSVs demonstram pedido 17 indeferido, pedido 18 deferido, inscrição 38 ATIVO, matrícula 73/estrutura 80, A 412 RECONHECIDA e B 413 EM_ANDAMENTO, sem resultado final da matrícula. Recusas de redecisão/duplicidade e saída normal são demonstradas. Linhas longas estão cortadas à direita; os originais foram preservados, sem reconstrução.

O commit 8793cabb06464dfd2457a1e2ee18e070da8d99da identifica retrospectivamente as fontes validadas, sem alteração de código no fechamento. Os 41 originais permanecem privados; relatório, ambiente e manifestos com seis cópias revisadas ficam em docs/testes/publicados/execucao_autor_v0_8. O autor utiliza scoa_java_teste; a migração de scoa fica para uma etapa posterior. O Word v7.11 incorpora esta validação em revisão própria, preservando a v7.10. Consulte o relatório do autor para horários e limites; não houve nova execução de testes ou migração do banco do autor neste fechamento.
