# Validação do autor do SCOA v0.5

Rodrigo Lustosa concluiu a bateria automatizada com **74 testes unitários e 66 de integração**, sem falhas, erros ou ignorados, BUILD SUCCESS, em **31/10/2026 às 22:52:45 UTC−03:00**. Os 11 XMLs originais foram conferidos e seus métodos coincidem com a bateria da preparação. Os resumos públicos preservam os resultados e tempos por execução sem as propriedades privadas dos XMLs.

O código foi registrado posteriormente no commit **dc1a6f36f6ec0f86aed7048be4e8ad846bb0dfd4**, sem modificações de código neste fechamento. Os 53 arquivos do manifesto da preparação foram comparados por SHA-256 e continuam idênticos. Base local: 9c0f2e1; código anterior: b714404. A criação de commit posterior não é apresentada como uma execução naquele commit. O envio ao GitHub será confirmado pelo fechamento e pela conferência do remoto.

## Evidências e datas

O PDF automatizado original tem duas páginas e reproduz a versão 0.5.0-SNAPSHOT, os resultados e o horário de encerramento. As três capturas originais mostram a mesma execução; o relógio da área de trabalho nas capturas é posterior ao encerramento registrado no Maven. Não usar esse relógio como horário dos testes.

O PDF manual original tem 12 páginas. Seus metadados registram emissão em 01/11/2026 às 13:09:36 UTC−03:00, mas o console não registra o horário exato da execução manual. A consulta do banco foi exportada em seis CSVs às 13:13:02 de 01/11/2026, no horário do servidor, em scoa_java_teste / MySQL 8.0.46. Esse horário pertence à consulta posterior.

| Comportamento observado | PDF manual |
|---|---|
| Acesso aos menus e criação das disciplinas 213 e 214 | p. 1 |
| Criação do aluno 26, listagem, consulta e correção; identificador A1 recusado | p. 2 |
| Atualização cancelada, inativação cancelada e inativação confirmada | p. 3 |
| Repetição da inativação, campo vazio recusado e atualização sem reativação; criação do curso 25 | p. 4 |
| Consulta e correção de curso, cancelamento de correção e preservação do nome | p. 5 |
| Criação de curso cancelada, nome vazio recusado e criação/consulta da estrutura 22 | p. 6 |
| Correção e cancelamentos da estrutura, preservando curso e ID | p. 7 |
| Inclusão das disciplinas existentes 121 e 122 e consulta da composição | p. 8 |
| Duplicidade recusada, retirada cancelada e retirada confirmada de 122 | p. 9 |
| Relação ausente recusada, inclusão cancelada e composição final somente com 121 | p. 10 |
| Disciplina 122 preservada; IDs abc e 0 recusados; aluno ausente | p. 11 |
| Disciplina 400 e curso 50 ausentes recusados; saída normal | p. 12 |

As linhas longas de menus e comandos ficam visualmente cortadas à direita em partes dos PDFs originais de impressão. As entradas e mensagens decisivas dos fluxos acima permanecem legíveis; os originais foram preservados sem recriar telas. Os XMLs e os CSVs complementam as evidências. O estado final não reconstrói por si só as tentativas recusadas: elas são identificadas nas páginas correspondentes.

## Estado final conferido nos CSVs

| Registro | Resultado |
|---|---|
| Aluno 26 | V05 Aluno inativo corrigido; INATIVO; conta opcional NULL |
| Curso 25 | V05 Curso corrigido; ATIVO |
| Estrutura 22 | V05 Estrutura corrigida; ATIVO; curso 25 |
| Componente 22 → 121 | Presente |
| Componente 22 → 122 | Ausente |
| Disciplinas 121 e 122 | Ambas preservadas e ATIVO; cargas 40 e 55 |

O roteiro criou 213 e 214, mas utilizou 121 e 122 como componentes. Isso é confirmado pelo PDF e pelos CSVs, sem substituir os identificadores por aqueles sugeridos no guia.

## Correspondência com o plano P05

| Caso | Evidência e delimitação |
|---|---|
| P05-01 | BaseAcademicaTest: obrigatoriedade, Unicode, espaços, limites e situações; PDF manual pp. 4 e 6 para campos vazios |
| P05-02 | BaseAcademicaJdbcIT.p0502AlunoMantemContaIdentidadeESituacao; manual p. 2 e CSV de alunos |
| P05-03 | BaseAcademicaJdbcIT.p0503InativacaoPreservaHistoricoENaoReativa; manual pp. 3–4; CSV de alunos. Preservação de matrícula é automatizada |
| P05-04 | BaseAcademicaJdbcIT.p0504CursoEStruturaAtualizamSemMudancas, com os dois modos de useAffectedRows; manual pp. 4–6 |
| P05-05 | BaseAcademicaJdbcIT.p0505EstruturaPreservaCursoESituacao; manual pp. 6–7 e CSV de estruturas |
| P05-06 e P05-07 | BaseAcademicaJdbcIT.p0506ComponentesEP0507DuplicidadeEAusencia; manual pp. 8–11 e CSVs de componentes/disciplinas |
| P05-08 | Testes p0508 de domínio e JDBC; manual pp. 2 e 10–12 para entradas e ausências. Referências inativas e preservação de identidades são verificadas automaticamente |
| P05-09 | BaseAcademicaJdbcIT.p0509PlanoProtegeInclusaoERetirada; cobertura automatizada, sem atribuir essa operação ao roteiro manual |
| P05-10 | Dois métodos p0510 de BaseAcademicaJdbcIT, transações próprias e externas; cobertura automatizada |
| P05-11 | BaseAcademicaJdbcIT.p0511InclusoesConcorrentesNaoDuplicam; cobertura automatizada com conexões independentes |
| P05-12 | BaseAcademicaJdbcIT.p0512ConsoleConfirmaFluxoCompleto e páginas manuais acima |
| P05-13 | Dois métodos p0513 de BaseAcademicaJdbcIT e cancelamentos/recusas do PDF. Encerramento inesperado de entrada é automatizado |
| P05-14 | Todos os 140 resultados, com regressões da v0.4 incluídas, nos dois resumos RESULTADOS.md |

## Escopo fechado e pendências

Fechada a validação do autor para as bases acadêmicas desta v0.5. A execução do Codex permanece separada em execucao_codex_v0_5, com encerramento às 22:42:50 de 31/10/2026 e ambiente isolado. Nenhuma fonte foi modificada para este fechamento, por isso não foi executada outra bateria sobre código idêntico.

Matrícula, inscrição, elegibilidade, equivalências, análise curricular, planos, autenticação, autorização e interface web continuam pendentes. A composição curricular vinculada a planos possui proteção inicial; futuros escritores devem respeitar o bloqueio da estrutura. As 71 tabelas e os diagramas do DDL v7.5 foram preservados. A revisão acadêmica v7.8 amplia o capítulo 9 sem mudar a versão do software.
