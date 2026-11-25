# Validação Codex do incremento acadêmico v0.14

Em 25/11/2026, o incremento em desenvolvimento passou em **159 testes unitários e 241 testes de integração**, sem falhas, erros ou ignorados. O Maven exibiu BUILD SUCCESS às 08:42:18 UTC-03:00, com duração de 01:09 min. Esta execução é do Codex; a validação manual do autor da v0.14 continua pendente.

## Comportamentos verificados

- Sessão e permissões na fronteira dos nove contratos acadêmicos; revogação, vínculo/conta inativos, propriedade da matrícula e professor responsável pelo primeiro resultado.
- Histórico próprio com resultado ausente/divergente, reconhecimentos vigentes/suspensos/revogados, contexto curricular e legados; manifestações com autoria e isolamento entre pessoas.
- Grade de 17 tempos de 50 minutos, intervalos excluídos e limite inclusivo de 15 minutos de atraso. Este é o domínio da frequência; aulas e armazenamento por tempo ainda não foram integrados.
- Transferência interna efetiva e preservação da pessoa, conta e origem; prazo D+45, vagas D+13/D+14, impedimento bibliotecário, repetição sem duplicidade e aproveitamento somente da mesma disciplina com aprovação formal. O aproveitamento habilita pré-requisito com prova rastreável; uma retificação prejudicial exige análise de impacto.
- Solicitação dos dois documentos aprovados, acesso próprio, concessões independentes da secretaria, andamento com versão, estados finais, dados legados e falhas atômicas de auditoria.
- Regressão dos fluxos anteriores. [Contagens por classe](RESULTADOS.md).

## Ambiente e rastreabilidade

Instância nova de MySQL 8.0.46, schema scoa_java_teste, porta local 33386, DDL de testes v7.5 e migrações V006 a V015. O schema scoa não foi migrado. As fixtures SQL sintéticas são preparação/restauração de teste; não representam atos auditados executados por uma pessoa no console.

Os XML/TXT finais foram recuperados após o reinício do computador e preservados privadamente com manifesto. O log completo do terminal em diretório temporário não foi recuperado; o resumo registra o resultado observado, sem reconstruir um log bruto. Os [hashes das fontes](FONTES_SHA256.json) foram conferidos após a recuperação. [Ambiente e duração](ambiente.txt).

## Estado da atualização

Este é um ponto de desenvolvimento sobre a v0.13 fechada em f5107209c26aa80ca83ef7cf9b3f3477c5e1d899. Não é fechamento da v0.14 nem conclusão de todo o módulo acadêmico. Word v7.17 e versões anteriores permanecem preservados. Os [limites e próximos blocos](../../../SCOA_Implementacao_v0_14.md) incluem diário, frequência/avaliações por lançamento, transferências externas, planos/lote, gestão, relatórios e interface web.
