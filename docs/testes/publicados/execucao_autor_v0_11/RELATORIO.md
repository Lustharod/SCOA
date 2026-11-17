# Validação da v0.11

## Execução do autor

O Maven gerou a versão 0.11.0-SNAPSHOT e concluiu o objetivo `verify -Pmysql-it` em 10/11/2026 às 14:56:36 (UTC−03:00). Os 21 relatórios registram 116 testes unitários e 167 de integração, com zero falhas, erros ou ignorados. O ambiente consta em [ambiente.txt](ambiente.txt).

| Caso manual | Resultado conferido |
| --- | --- |
| PA01 e PA02 | Consulta bloqueada sem sessão e para o solicitante sem a permissão de decisão. |
| PA03 e PA04 | Analista consultou duas revisões e duas conciliações. As mesmas linhas apareceram ao voltar ao submenu sem novo login. |
| PA05 e PA06 | Consulta bloqueada após logout. ID zero rejeitado com mensagem de entrada inválida. |
| PL01 e PL02 | Período ativo listado. A simulação preservou duas disciplinas em andamento e registrou o impedimento por pré-requisito de uma oferta. Total sugerido vazio, conforme o cenário. |
| PL03 | A captura confirma erro de prioridade. O período foi informado uma vez e repetido no campo de prioridade. A entrada com dois IDs do período no mesmo campo tem cobertura no teste automatizado `PlanejamentoJdbcIT#selecaoDePeriodosEValidada`. |
| PL04 | Retorno ao menu principal confirmado. |

As cinco grades de matrícula, revisões, conciliações, contagens e inscrições coincidem byte a byte entre as exportações anteriores e posteriores ao percurso.

## Execução do Codex

Em 10/11/2026 às 14:52:45 (UTC−03:00), `mvn -o -B -Pmysql-it verify` terminou com BUILD SUCCESS em MySQL 8.0.46 isolado, no schema de teste, com 116 testes unitários e 167 de integração aprovados. O primeiro recorte também foi comparado de forma automatizada com a v0.10 no submenu de planejamento, com normalização dos números e da etiqueta aleatória da fixture.
