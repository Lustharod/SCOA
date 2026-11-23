# Validação e fechamento da v0.12

A v0.12 foi concluída para o recorte de console em 23/11/2026. O autor informou a execução dos casos manuais obrigatórios e as exportações do banco foram conferidas. A regressão do autor registrou **128 testes unitários e 179 de integração aprovados**, com zero falhas, erros ou ignorados. O [ambiente](ambiente.txt) distingue as datas e as execuções.

## Comportamento entregue

- Menu 19: horários e resultado final do próprio aluno autenticado.
- Duas fases de inscrição: D-14 a D-8 e D+7 a D+13. Confirmação por aluno revalida identidade, renovação, elegibilidade e vagas.
- Cancelamento com solicitação, decisão e histórico: automático até D+13; tardio depende da coordenação e retém a vaga. Pedido pode ser solicitado até D+45 e antes do fim do período. Disciplina cancelada não admite reinscrição no mesmo período.
- Retificação solicitada pelo professor da turma nos sete dias seguintes ao fim do período e decidida pela coordenação antes da abertura da inscrição do sucessor. Retratos e decisões preservam o resultado anterior. Nova proposta indeferida conserva o resultado vigente.
- Renovação institucional preserva a matrícula e verifica a biblioteca; cada turma exige confirmação própria. Bibliotecário registra pendência pelo console.
- Calendário recusa sobreposição; turmas compartilham as datas do período. Sucessor é encontrado automaticamente e sua confirmação explícita exige coordenação.
- Menu 18.11 associa conta existente ao aluno, professor ou curso coordenado, com autorização administrativa.

## Evidência manual

| Rodada | Casos | Resultado conferido |
| --- | --- | --- |
| SEGUNDA_FASE | SF01–SF11 e calendário | Identidade, renovação, biblioteca, cancelamento automático, liberação/reutilização da vaga, bloqueio de reinscrição e recusa de sobreposição. |
| TARDIO | TA01–TA06 | Pedido pendente, recusa de decisão pelo aluno, indeferimento e deferimento em pedidos separados, retenção da vaga e retificação fora do prazo recusada. |
| RETIFICACAO | RT01–RT07 | Recusas por responsabilidade, confirmação N/S, deferimento com efeito acadêmico, plano sem inscrição, segundo pedido indeferido e sucessor protegido. |

Na retificação, o primeiro pedido passou de PENDENTE a DEFERIDA; o resultado mudou de REPROVADO com presença 70/120 para APROVADO com 110/120. O segundo pedido foi INDEFERIDA e manteve a aprovação. A exportação após o plano é idêntica byte a byte à anterior: uma inscrição, zero cancelamentos e zero renovações. O sucessor final é curso 1, período 2, sucessor 3 na fixture sintética.

O relato do autor sustenta os resultados de console. Os CSVs foram lidos e conferidos; os PDFs foram inventariados com SHA-256, sem inspeção visual de seu conteúdo nesta conferência. Os originais, XMLs brutos, Word e guias permanecem privados. O [manifesto](MANIFESTO_SHA256.json) permite identificar os originais sem publicá-los. O campo JSON das exportações Workbench foi normalizado apenas em memória para leitura correta, preservando os arquivos originais.

## Execuções automatizadas

Em 18/11/2026 às 18:54:07 UTC-03:00, o autor concluiu `verify -Pmysql-it` em 02:08 min, com BUILD SUCCESS e 307 testes aprovados. A tentativa anterior, das 18:46, permanece separada. Os [resultados por classe](RESULTADOS.md) foram extraídos dos XMLs da execução aprovada.

Em 23/11/2026 às 00:52:01 UTC-03:00, a rechecagem Codex `mvn -o -B test` compilou as fontes e aprovou 128 testes unitários em 22.847 s. A integração não foi repetida nessa rechecagem; seus 179 resultados são da execução do autor. As [fontes](FONTES_SHA256.json) foram identificadas no fechamento, sem atribuir o manifesto retroativamente à execução de 18/11.

## Limites e próxima iteração

OC-V12-01 foi encontrada pelo autor durante o teste manual: escolha redundante de ALUNO/PROFESSOR/COORDENADOR na criação da conta e no vínculo, sem confronto entre as escolhas. O código confirma a possibilidade de divergência do nome do perfil. Não foi executado um caso com escolhas divergentes nem relatado acesso indevido. O autor decidiu concluir a rodada e corrigir o fluxo na próxima implementação.

PRIMEIRA_FASE permaneceu opcional e não foi executada manualmente. Autorização global dos menus legados, diário, avaliações por aula, frequência por aula, transferências, integralização, interface web e demais módulos continuam pendentes. Este fechamento não declara o módulo acadêmico completo. A migração de `scoa` exige uma etapa própria; este trabalho utilizou `scoa_java_teste`.
