# Validação do autor do SCOA v0.6

Rodrigo Lustosa concluiu Maven `verify -Pmysql-it` com **77 testes unitários e 95 de integração**, sem falhas, erros ou ignorados, **BUILD SUCCESS**, em **01/11/2026 às 15:04:41 UTC−03:00**. A duração Maven foi 57.585 s. Os 13 XMLs JUnit originais e o `failsafe-summary.xml` confirmam as contagens. São 172 execuções, incluindo as 140 regressões da v0.5 e 32 novas execuções; os quinze grupos P06 não são contagem de métodos.

Código validado: **87fb486ea15fefa0c1f2ddc796472ddc9a169f55**, criado após as execuções, sobre o fechamento documental 4d75429. As 64 fontes Java, testes, Maven e DDL do manifesto da preparação foram comparadas por SHA-256 e permaneceram idênticas. O commit identifica retrospectivamente o código executado, sem alterar datas dos testes. O SQL auxiliar do roteiro manual foi corrigido separadamente; não houve alteração do Java ou da bateria para fechar a entrega.

## Evidências e horários

O PDF automatizado tem três páginas: versão e comando na primeira, totais e BUILD SUCCESS na segunda, duração e horário final na terceira. Foi emitido às 15:15:45 UTC−03:00, depois da execução. O console registra IBM850; os consoles manuais usam UTF-8. Não são publicadas propriedades completas dos XMLs ou configurações locais.

O roteiro manual foi preservado em dois PDFs originais, de **seis e oito páginas**, emitidos em 01/11/2026 às **18:04:10** e **18:26:33 UTC−03:00**. São horários de emissão, não início e fim exatos do teste. Os sete eventos persistidos possuem horários do servidor entre **17:37:45.103522** e **18:19:09.303256**; isso data as escritas, não todas as ações ou consultas manuais. A preparação registra **16:21:22** e a consulta final dos seis CSVs registra **18:51:59**, no horário do servidor, com sessão SYSTEM e fuso do sistema Hora oficial do Brasil.

Foram preservados sete CSVs: a preparação e seis resultados finais. Os PDFs manuais possuem cortes à direita em comandos e linhas longas de menu ou resumo. As entradas e mensagens decisivas permanecem legíveis e foram conferidas visualmente; os CSVs e XMLs complementam a evidência. Não foram reconstruídas telas nem alterados os originais.

| Comportamento observado | Evidência manual |
|---|---|
| Acesso ao menu 14; respostas REGULAR, IMPEDITIVA e INDISPONIVEL | Parte 1, p. 1 |
| Recusa de matrícula por pendência e identidade bibliotecária ausente | Parte 1, pp. 1–2 |
| Alunos 80 e 81 continuam sem matrícula | Parte 1, p. 2; parte 2, p. 7 |
| Criação e consulta da matrícula 32 e evento inicial 35 | Parte 1, p. 2 |
| Duplicidade ativa; consulta de histórico com ID de matrícula ausente 79 | Parte 1, p. 3 |
| Correção cancelada, data preservada e correção confirmada | Parte 1, pp. 3–4 |
| Trancamento cancelado e confirmado; histórico e duplicidade durante trancamento | Parte 1, pp. 4–5 |
| Duas falhas de consulta antes da retomada | Parte 1, pp. 5–6 |
| Retomada com pendência 8; retorno bloqueado e preservação de TRANCADO/histórico | Parte 2, p. 1 |
| Correção cadastral aceita sem efetivar matrícula; evento 38 | Parte 2, p. 2 |
| Regularidade da fixture e retorno autorizado; retorno repetido recusado | Parte 2, pp. 2–3 |
| Cancelamento desistido, dados preservados e cancelamento confirmado | Parte 2, p. 4 |
| Reabertura recusada; reingresso como matrícula 33, sem substituir 32 | Parte 2, p. 5 |
| IDs abc/0 e data 2026-02-30 recusados | Parte 2, p. 6 |
| Motivo vazio recusado; matrícula 33 e evento 41 preservados | Parte 2, p. 7 |
| Consultas finais de matrículas e histórico | Parte 2, pp. 7–8 |

O cancelamento de criação do cenário M06-02 não foi localizado nos PDFs fornecidos; possui cobertura automatizada em `p0614ConsoleConfirmacaoCancelamentoEEof`. Também não se atribui ao PDF uma saída normal pelo menu 0, ausente ao final dos originais. Não são exigidas capturas adicionais para esses comportamentos já cobertos automaticamente. A consulta final não reconstrói tentativas recusadas: essas dependem do console registrado.

## Ocorrência durante a preparação do impedimento

O autor relatou erro **1175** do Workbench ao executar o UPDATE do SQL auxiliar inicial, por modo seguro ativado. O script foi corrigido para resolver uma única pendência da fixture e atualizar por `id_pendencia`, com limite e conferência. Ambos os blocos foram validados pelo Codex em instância isolada com `SQL_SAFE_UPDATES=1`; a correção não desabilita essa proteção.

A parte 1 preserva as mensagens “Não foi possível consultar matrícula/biblioteca” e “Não foi possível concluir a transação”. Foi orientado ROLLBACK na conexão do Workbench e reinício da aplicação, preservando os registros. Transação aberta e conexão invalidada foram hipóteses compatíveis com a ocorrência e o código, mas os PDFs não registram a causa SQL interna nem provam especificamente um timeout. A parte 2 comprova que, após a retomada, a consulta retornou IMPEDITIVA e o retorno foi recusado corretamente.

Esta ocorrência não foi tratada como sucesso do cenário de impedimento. Não houve alteração do código para sua recuperação. A aplicação mantém uma conexão por sessão, não reconecta automaticamente e apresenta mensagem genérica em falhas desse tipo; a orientação de recuperação poderá ser aprimorada em incremento próprio. A classificação bibliotecária indisponível e a falha de consulta continuam sem autorizar matrícula.

## Estado final conferido nos CSVs

Etiqueta sintética: `V06MAN-6c5ed890d629`. Os IDs são os realmente observados, não os sugeridos genericamente pelo guia.

| Registro | Resultado |
|---|---|
| Aluno 79 | ATIVO, conta 38 |
| Aluno 80 | ATIVO, conta 39; sem matrícula |
| Aluno 81 | ATIVO, conta NULL; sem matrícula |
| Curso 79 e período 211 | ATIVO; período 2026-02-01 a 2026-06-30 |
| Matrícula 32 | Aluno 79, curso 79, período 211; CANCELADO; ingresso 2026-01-17 |
| Matrícula 33 | Aluno 79, curso 79, período 211; ATIVO; ingresso 2026-01-18 |
| Eventos 35–40 | Seis eventos da matrícula 32: criação, correção, trancamento, correção sem efetivação, retorno e cancelamento |
| Evento 41 | Criação da matrícula 33 no reingresso |
| Pendência 8 | REGULARIZADO, impeditiva=1; usuário bibliotecário 32, empréstimo 32 |
| Pendência 9 | ATIVO, impeditiva=1; usuário bibliotecário 33, empréstimo 33 |

Os motivos e valores anteriores/novos nos sete eventos correspondem ao roteiro observado. Não há eventos para as tentativas recusadas. As alterações de biblioteca são fixtures sintéticas em SQL; não representam gerenciamento de pendências implementado no console. Não há concessão automática de créditos pelo reingresso.

## Correspondência com o plano P06

| Grupo | Cobertura e delimitação |
|---|---|
| P06-01 | `MatriculaTest.p0601LimitesEMotivos` e persistência DATE/Unicode; manual confirma IDs/data/motivo inválidos |
| P06-02 | `p0602CriacaoEHistorico`; manual parte 1 p. 2 e CSVs |
| P06-03 | Três variantes de `p0603PendenciaEncerradaOuImpeditiva` e `p0603NaoImpeditiva`; manual confirma impedimento e regularização da fixture |
| P06-04 | Quatro variantes de `p0604IdentificacaoOuSituacaoIndisponivel` e falha SQL sintética; manual confirma conta ausente e ocorrência de consulta malsucedida, sem provar sua causa interna |
| P06-05/06 | `p0605DuplicidadeEReingresso`, situação legada desconhecida; manual partes 1 pp. 3/5 e 2 p. 5 |
| P06-07 | Matriz completa no domínio e `p0607TransicoesEHistorico`; manual trancamento, retorno, cancelamento e recusas |
| P06-08 | Retorno bloqueado, referências inativas e indisponibilidade automatizados; manual parte 2 p. 1 |
| P06-09 | Duas variantes de correção, inclusive sem mudança e sem biblioteca; manual correção com impedimento na parte 2 p. 2 |
| P06-10 | Quatro variantes de referências inválidas e datas SQL limite; entradas concretas inválidas no manual não substituem todos os limites automatizados |
| P06-11 | Rollback externo e falha de histórico, em transações próprias/externas, automatizados; sem atribuição ao manual |
| P06-12 | Criação concorrente e escritor bibliotecário coordenado, automatizados; sem atribuição ao manual |
| P06-13 | Período protegido e matrícula legada, automatizados; não simulados com registros reais do autor |
| P06-14 | Confirmação, cancelamento, consultas, entradas inválidas e EOF automatizados; o manual cobre os fluxos observados na tabela de páginas |
| P06-15 | Bateria completa com regressões. A migração preservando fixture legada foi verificada pelo Codex em instância isolada; não equivale a teste de restauração do backup do autor |

## Fechamento e limites

A v0.6 está validada pelo autor no recorte de matrícula institucional e consulta bibliotecária. A execução do Codex permanece separada e terminou às **14:43:43 UTC−03:00 de 01/11/2026**, com os mesmos totais. Não foram somadas como testes distintos. Nenhuma fonte do manifesto mudou neste fechamento; não foi necessária outra bateria sobre código idêntico.

O DDL v7.5 com 71 tabelas permanece preservado. A migração aditiva V006 acrescenta período de ingresso e histórico, resultando em 72 tabelas; o Apêndice A e os diagramas anteriores continuam identificados como base histórica. A revisão Word v7.9 atualiza o capítulo 9 e preserva a v7.8. Word editável, XMLs brutos, roteiros privados e configurações locais permanecem fora da publicação.

Inscrição, elegibilidade completa, resultados/equivalências, plano de estudo, rematrícula, conclusão de curso, autenticação, autorização, interface web e os demais módulos continuam pendentes. Os escritores bibliotecários futuros devem respeitar o protocolo de bloqueio; a leitura atual não garante coordenação com SQL externo que o ignore. O fechamento não conclui o SCOA nem comprova pré-condições de acesso ainda não implementadas.
