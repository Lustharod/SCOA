# Validação Codex do incremento acadêmico v0.14

Em 25/11/2026, o incremento com diário passou em **166 testes unitários e 262 testes de integração**, sem falhas, erros ou ignorados. O Maven exibiu BUILD SUCCESS às 10:16:12 UTC-03:00, com duração de 01:11 min. É execução Codex; a validação manual do autor da v0.14 continua pendente.

## Incremento do diário verificado

- Carga de 60 horas preservada no cadastro e convertida em 72 tempos/3.600 minutos; fechamento real do resultado com esses totais.
- Aulas por tempos oficiais cobertos pelos horários, pauta/conteúdo, grupo transacional, duplicidade, datas e dados legados sem interpretação presumida.
- P1/P2/PF separadas, notas ausentes distintas de zero, edição com versão e histórico, retirada de PF indevida sem excluir ID/valor anterior e fechamento com a fórmula já aprovada.
- Chamada por tempo, pendência sem falta automática, limite de 75% e exclusão dos tempos anteriores à data da inscrição, preservando a carga integral da disciplina.
- Autoria do professor responsável revalidada; ADMIN não o substitui. Consulta própria do aluno e bloqueio de alterações após o fechamento, preservando a retificação existente. O fechamento antigo com totais independentes é recusado em turmas com novo diário.
- Eventos fictícios de chegada: 15 minutos inclusive, duplicidade, colisão de chave, continuidade após recusas de dados e preservação de correções manuais. Nenhum equipamento real foi conectado.
- Falha de auditoria desfaz lançamentos, histórico e evento simulado. O console exige confirmação e preserva a versão consultada.

## Regressão dos blocos anteriores

Permanecem aprovados os testes de acesso dos nove contratos acadêmicos, histórico próprio/manifestações, transferência interna com aproveitamento rastreável, documentos/secretaria e todos os fluxos anteriores. [Contagens por classe](RESULTADOS.md).

| Execução Codex em 25/11/2026 | Unitários | Integração | Situação |
| --- | ---: | ---: | --- |
| Primeiro incremento, 08:42:18 UTC-03 | 159 | 241 | Aprovado; publicado em 457f159. |
| Diário antes da retirada de nota, 10:12:37 UTC-03 | 163 | 261 | Aprovado. |
| Incremento atual, 10:16:12 UTC-03 | 166 | 262 | Aprovado. |

As contagens descrevem baterias de versões sucessivas; não devem ser somadas como testes distintos. A primeira tentativa do diário falhou na fixture de horários, por falta da referência de sala; essa preparação foi corrigida antes das rodadas aprovadas. Os relatórios dessa tentativa foram preservados privadamente.

## Ambiente, evidências e limites

Instância nova de MySQL 8.0.46, schema scoa_java_teste, porta local 33386, DDL de testes v7.5 e migrações V006 a V016. O schema scoa não foi migrado. As fixtures SQL são preparação/restauração de teste, não atos auditados executados por uma pessoa no console.

O log completo e os XML/TXT desta execução foram preservados diretamente no projeto, em área privada, com manifesto SHA-256. Os [hashes das fontes](FONTES_SHA256.json) foram conferidos após a execução. [Ambiente e duração](ambiente.txt).

Este ponto amplia o ramo desenvolvimento/academico-completo sobre 457f159. Main permanece na v0.13 fechada. Não é fechamento da v0.14 nem conclusão de todo o módulo: emissão/exportação do diário completo, gestão/auditoria docente, planos/lote, transferências externas, relatórios, parametrização e web seguem pendentes. Consulte as [notas da implementação](../../../SCOA_Implementacao_v0_14.md).
