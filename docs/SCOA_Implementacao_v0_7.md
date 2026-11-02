# Inscrição em disciplinas na v0.7

Implementação preparada em 01/11/2026 sobre 7357409. Software 0.7.0-SNAPSHOT. Esta entrega avança RF-MA-10 / UC-MA-10 e a validação incluída UC-MA-30; não encerra todos os requisitos acadêmicos.

## Comportamento

O menu 15 permite inscrever, listar inscrições de uma matrícula, vincular sua estrutura curricular e registrar primeiro resultado final. O menu 6 já apresenta turmas; os menus 11–14 mantêm as bases existentes.

A inscrição exige aluno, curso, matrícula, turma, disciplina, período e estrutura ativos, com disciplina pertencente à estrutura explicitamente vinculada. O vínculo não cria plano nem créditos. Se já houver plano legado, sua conciliação fica pendente; o fluxo não escolhe uma estrutura por inferência. O vínculo curricular deste incremento é imutável; transferência de estrutura exige fluxo futuro. Sua composição fica protegida depois do vínculo.

Para aulas iniciadas na data D, a janela é [D−14, D−7), em dias corridos: sete dias de inscrição e sete dias para preparação. A referência é a data do servidor obtida em UTC e convertida para America/Sao_Paulo. Não há exceção por feriado, curso ou reabertura. Período com turma já possui proteção de datas na base anterior.

A proposta de inscrição apresenta IDs, vagas e janela. A validação é interna ao fluxo; não cria vínculo nem reserva. Confirmação reexecuta as condições dentro da transação. O sistema verifica duplicidade da mesma turma por aluno, inclusive entre matrículas, e horários sobrepostos nos períodos coincidentes. Horários adjacentes são permitidos. Ausência de horário em oferta ou inscrição vigente com período coincidente impede declarar compatibilidade. Não há regra adicional de proibição da mesma disciplina em turmas distintas sem conflito.

ATIVO é inscrição vigente. CANCELADO é encerramento que libera vaga. APROVADO e REPROVADO são encerramentos acadêmicos e continuam contabilizados na ocupação histórica da oferta. Valor desconhecido impede novas inscrições no contexto afetado; não recebe interpretação permissiva. Esta entrega não oferece cancelamento de inscrição persistida, rematrícula automática ou mudança retroativa dos estados legados. Desistir da confirmação é diferente de cancelar um vínculo existente.

Pré-requisitos diretos cadastrados são exigidos conjuntamente. Seu cumprimento depende de resultado final APROVADO da mesma matrícula, em período já encerrado. Não reutilizar automaticamente resultados de matrícula anterior após reingresso. Inscrições antigas apenas marcadas como concluídas não comprovam resultado calculado por esta versão.

## Resultados finais e limites

Notas entre 0 e 10, com no máximo uma casa decimal. Média inicial M=(P1+P2)/2, arredondada em decimal HALF_UP para uma casa antes da decisão. M>=7 aprova por notas e dispensa PF. Para M<7, MF=(M+PF)/2 precisa alcançar 5, também com HALF_UP para uma casa. PF desnecessária é recusada; PF pendente não fecha resultado por notas. Frequência inferior a 75% reprova mesmo com nota suficiente.

A frequência é informada por minutos totais e minutos presentes, com mesma unidade e origem de apuração indicada no motivo. A comparação é exata, sem arredondar percentuais. Total zero, negativos e presença maior que o total são recusados. Essa apuração final informada não é integração automática com o diário, nem implementação completa do lançamento de avaliações/frequência.

O primeiro fechamento exige inscrição ATIVO, período terminado e motivo com até 500 pontos Unicode. Resultado e mudança da situação da inscrição são atômicos. Registro final é imutável nesta versão: correção/retificação exige fluxo futuro. O aluno inativo não impede registrar resultado de uma inscrição histórica válida. Não se inventa ator autenticado, e não há autenticação/autorização nesta interface de console.

Equivalências permanecem uma dependência parcial: não há aprovação automática por ementa, nome ou carga horária. A aplicação não implementa gestão de decisões ou seus efeitos em histórico e plano (RF-MA-13 / RN-MA-08). Se o cumprimento depender de processo presente em aproveitamento_equivalencia sem integração formal, a inscrição é recusada com orientação explícita. Não declarar UC-MA-30 integralmente atendido para alunos dependentes desse reconhecimento. Disciplinas sem pré-requisitos e requisitos satisfeitos por resultados registrados são executáveis.

Também permanecem pendentes gerador de plano, retificação de resultados, critérios adicionais de pendências acadêmicas, interface web e recuperação de conexão. Biblioteca não é consultada para inscrição: RN-MA-05 continua em matrícula/rematrícula.

## Banco e concorrência

Aplicar uma vez V007__inscricao_resultado_contexto.sql depois do DDL v7.5 e V006, com backup e seleção explícita do schema. A migração acrescenta controle_inscricao, vinculo_curricular_matricula e resultado_inscricao: 75 tabelas sobre as 72 da v0.6. Não há alteração do DDL histórico, carga retroativa ou aplicação automática de migração. Não reaplicar cegamente se a execução parar no meio; DDL MySQL não constitui uma transação atômica de migração.

A linha técnica de controle serializa avaliações e efetivações nesta interface de console, evitando impasses de intervalos vazios entre alunos. É uma opção conservadora para o volume atual, com menor paralelismo. Aluno coordena duplicidade/horários com matrícula e resultados; turmas protegem capacidade e alocações; disciplinas e estrutura são revalidadas sob bloqueio. Bloqueios permanecem até o fim da transação externa, quando houver. Savepoints preservam a titularidade da transação do chamador.

Turma com qualquer inscrição não admite inclusão ou retirada de horário por este fluxo: preserva-se a oferta já utilizada até existir processo de alteração com análise de impacto. Manutenção de capacidade e referências mantém as proteções conservadoras anteriores. Escritores externos que ignorem o protocolo não têm garantia de serialização. Impasses possíveis com outros escritores são falhas transacionais com rollback, nunca autorização para ignorar as regras.

## Validação e documentação

O plano preliminar P07 foi produzido na conversa antes do código. Os testes concretos e os ajustes de concorrência foram escritos durante a implementação, sem alegação de que todos os métodos antecederam o código. A execução isolada do Codex e as tentativas preliminares ficam separadas de qualquer futura execução do autor. Consulte o relatório de execução para resultados efetivos. O Word v7.9 e as evidências v0.6 não foram alterados.


## Fechamento da validação do autor

Rodrigo Lustosa concluiu verify -Pmysql-it em 01/11/2026 às 21:07:52 UTC−03:00, com 97 execuções unitárias e 109 de integração, sem falhas, erros ou ignorados. A duração exibida foi 01:16 min. São 206 execuções, incluindo as 172 regressões da v0.6 e 34 novas, sem somar as execuções do autor e do Codex. Os métodos e parametrizações conferem com o conjunto isolado.

O manual de três páginas demonstra vínculo curricular, desistência sem inscrição, criação da inscrição 20, consulta, duplicidade e falta de vaga, consulta da alocação 122 e recusa de resultado antes do encerramento do período. As tentativas de remoção usaram IDs 20, 231 e 79 inexistentes; a proteção da alocação existente não é atribuída ao manual, mas ao teste automatizado aprovado. O resumo de cálculo APROVADO antes da confirmação não é um resultado persistido.

Os quatro CSVs identificam etiqueta V07MAN-67713a9e-d65, matrícula 72, estrutura 79, turma 231 e inscrição 20 ATIVO; capacidade 1 e ocupadas 1. O vínculo registra 21:36:56.441185 no servidor. Os nomes de dois CSVs divergem dos IDs das linhas e foram preservados. O PDF automatizado foi emitido às 21:13:39 e o manual às 21:59:57; emissão não data as ações. Os 37 originais permanecem privados. Consulte o relatório público do autor para rastreabilidade por página e limites.

Java, testes, Maven e DDL não foram alterados no fechamento. A documentação acadêmica incorpora a v0.7 em revisão própria, preservando a v7.9. A versão permanece 0.7.0-SNAPSHOT, conforme o padrão incremental do projeto; o recorte está validado sem declarar concluídas equivalências, plano, retificação, autenticação ou os demais módulos.
