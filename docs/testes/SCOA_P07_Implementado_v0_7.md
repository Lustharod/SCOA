# Verificação executável da v0.7

Complemento elaborado durante a implementação sobre o plano preliminar P07, sem substituir seu histórico. Os grupos P07 não são contagem de métodos. Nenhum resultado do autor é atribuído a esta entrega antes de sua execução.

| Área do plano | Verificação implementada |
|---|---|
| P07-01/02/03 | IDs validados, referências sob bloqueio; testes de trancamento e janela; fronteiras de dias/ano bissexto unitárias |
| P07-04 | Vínculo explícito, composição protegida, recusa de vínculo repetido e estrutura ausente |
| P07-05/06 | Requisito sem evidência recusa; resultado formal aprovado permite; primeiro fechamento, PF pendente, frequência e período não encerrado |
| P07-07/08 | Equivalência sem integração formal recusa; cumprimento busca somente a mesma matrícula. Gestão de equivalências e importação entre reingressos não implementadas |
| P07-09/10 | Duplicidade e turma lotada; duas confirmações da mesma matrícula; dois alunos disputam uma vaga |
| P07-11 | Conflito de horários e preservação de alocações; turma sem horários recusa |
| P07-12/13 | Validação não reserva; inscrição sem conta bibliotecária permanece possível |
| P07-14/15/16 | Console com desistência/EOF; escrita e listagem; trancamento entre proposta e confirmação |
| P07-17/18 | Conexões independentes com barreira de início; exatamente uma efetivação |
| P07-19 | Proteções de estrutura/horários e regressões dos escritores existentes; não alegar teste concorrente de todas as combinações de escritores |
| P07-20 | Rollback da transação externa; falha SQL sintética posterior à inserção do resultado desfaz resultado e situação |
| P07-21/22 | Instância isolada, aplicação de migrações e bateria de regressão; não representa migração do banco do autor |

InscricaoTest cobre fórmulas, limites de nota/precisão, PF, frequência exata inclusive números grandes e janela. InscricaoJdbcIT cobre os fluxos de persistência, console e concorrência descritos acima. Casos institucionais ainda não definidos e funcionalidades fora do recorte permanecem pendentes; os testes existentes não comprovam cobertura integral de todos os exemplos do plano preliminar.
