# Plano P06 de matrícula institucional

Registrado em 01/11/2026 antes da implementação. Base validada: dc1a6f3; fechamento: 4d75429. Proposta aprovada pelo autor nesta conversa. Resultados ainda não executados neste registro.

## Recorte e decisões

RF-MA-09 / UC-MA-09 com consulta real UC-MB-10 / RN-MA-05. Criar, consultar, listar por aluno, corrigir ingresso, trancar, retornar e cancelar. Situações persistidas seguem a convenção existente ATIVO, TRANCADO, CANCELADO e CONCLUIDO. CONCLUIDO é somente leitura neste incremento. Trancado ainda reserva o vínculo: novo ingresso no mesmo curso só após encerramento. Reingresso é nova identidade e não concede equivalência automática. Nenhuma inscrição, plano ou crédito é criado.

Data de ingresso usa DATE (1000–9999). Período de ingresso deve existir e estar ATIVO na criação/correção; a data não precisa estar dentro das aulas, pois ingresso administrativo pode antecedê-las. Sem presumir janela institucional ainda não definida. Aluno e curso ativos para criação/retorno. Correção preserva aluno, curso, situação e histórico; pode ocorrer em vínculo encerrado. Motivo obrigatório, até 500 caracteres Unicode, em cada escrita. Histórico inclui criação e correções, data do servidor e valores anteriores/novos; não inventa ator autenticado.

Migração aditiva v0.6 preserva DDL v7.5 e registros antigos: período novo é anulável somente por compatibilidade histórica. Novas matrículas exigem período. Não preencher retroativamente nem renomear situações existentes. Situações desconhecidas impedem novas efetivações para o mesmo vínculo até saneamento autorizado.

Biblioteca: identificar aluno por conta já vinculada e usuário da biblioteca. Ausência de conta, usuário bibliotecário ausente/inativo ou situação de pendência desconhecida significa INDISPONIVEL. Nenhuma ausência de vínculo representa regularidade. Pendências ATIVO com impeditiva=1 bloqueiam; REGULARIZADO e LIBERADO são encerramentos reconhecidos neste contrato. Outros valores falham de modo conservador. Confirmar convenções antes de integrar escritores futuros da biblioteca. Consulta sem impedimentos só é válida com identidade resolvida. Não criar contas automaticamente nem oferecer liberação manual.

Consulta e correção não exigem biblioteca. Criação e retorno consultam dentro da transação; trava do usuário bibliotecário coordena a consulta com futuros escritores, que deverão obter a mesma trava antes de inserir/alterar pendências. Esta versão não implementa produtores da biblioteca. Escritas externas que ignorem o protocolo ficam fora da garantia concorrente. Trava de aluno serializa vínculos e inativação; trava de curso/período protege referências. Matrícula e histórico são atômicos, inclusive sob transação externa com savepoint. Período referenciado fica protegido contra alteração de datas/situação, admitindo correção de descrição, assim como ocorre com turmas.

## Casos e resultados esperados

| Caso | Verificação | Resultado esperado |
|---|---|---|
| P06-01 | IDs, datas, período, motivos Unicode e limites | Recusa inválidos sem escrita |
| P06-02 | Criação regular, consulta e listagem | ATIVO, período e histórico inicial preservados |
| P06-03 | Pendência ativa impeditiva/não impeditiva e encerrada | Só impeditiva ativa bloqueia |
| P06-04 | Conta ausente, biblioteca ausente/inativa, situação desconhecida, falha SQL | Efetivação não concluída; sem matrícula/histórico parcial |
| P06-05 | Duplicidade ATIVO/TRANCADO e situação legada desconhecida | Recusa; apresenta orientação para vínculo existente |
| P06-06 | Cancelamento e reingresso | Novo ID; vínculo anterior preservado; sem créditos automáticos |
| P06-07 | Trancar/retornar/cancelar e transições proibidas | Somente transições previstas; CONCLUIDO sem escrita |
| P06-08 | Retorno bloqueado/indisponível; aluno/curso inativo | TRANCADO preservado; nenhum evento parcial |
| P06-09 | Correção inclusive sem biblioteca e sem mudança de valores | Identidade/situação preservadas; motivo e valores auditados |
| P06-10 | Referências ausentes/inativas e datas SQL limite | Recusa segura; período não implica inscrição |
| P06-11 | Histórico, rollback próprio/externo, falha de histórico | Matrícula e eventos atômicos; transação externa preservada |
| P06-12 | Duas criações simultâneas e protocolo biblioteca | Só um vínculo vigente; leitura espera escritor coordenado |
| P06-13 | Período com matrícula | Só descrição corrigível; matrícula legada com período NULL consultável |
| P06-14 | Console confirma, cancela, entrada inválida e EOF | Nenhuma escrita não confirmada |
| P06-15 | Regressão completa e migração | Casos anteriores passam; migração não altera registros antigos |

## Execução

Preparar instância MySQL isolada do Codex; criar nela a base de testes a partir do DDL v7.5 e aplicar migração v0.6. Nunca recriar banco do autor. Executar Maven verify -Pmysql-it e preservar XMLs/console separadamente das evidências do autor. Para o autor, fornecer migração aditiva e roteiro antes da execução; não aplicar automaticamente em seus bancos. Não alegar validação do autor com resultados do Codex.
