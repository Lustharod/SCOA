# Validação do autor do SCOA v0.8

Rodrigo Lustosa concluiu Maven verify -Pmysql-it em **02/11/2026 às 16:22:06 UTC−03:00**, duração exibida **48.069 s**, BUILD SUCCESS. Os 17 XMLs JUnit confirmam **103 execuções unitárias e 130 de integração, sem falhas, erros ou ignorados**: 233 execuções, incluindo as 206 regressões da v0.7 e 27 novas. Métodos e parametrizações coincidem com a bateria isolada do Codex, sem somar as duas execuções como casos distintos.

As fontes do manifesto de entrega v0.8 conferem integralmente. O autor compilou 59 fontes principais, contra 58 no conjunto isolado: ExemploSala local foi preservado e identificado por hash, sem teste adicional ou incorporação automática ao Git. Código validado identificado retrospectivamente por **8793cabb06464dfd2457a1e2ee18e070da8d99da**, criado após as execuções. Este fechamento não alterou Java, testes, Maven ou DDL.

## Evidências e horários

Foram preservados **41 originais**: dois PDFs, quatro CSVs, 17 XMLs JUnit, 17 TXT e failsafe-summary.xml. PDF automatizado: três páginas, emitido em 02/11/2026 às **16:41:02 UTC−03:00**. PDF manual: sete páginas, emitido às **21:36:49 UTC−03:00**. Emissão não é horário de execução de cada ação.

Os PDFs e os CSVs públicos são cópias byte a byte revisadas. XMLs brutos e suas propriedades permanecem privados. Linhas longas no manual estão cortadas à direita; não foram reconstruídas capturas. A extração textual auxilia a consulta, mas valores completos persistidos são corroborados pelos CSVs. O automatizado apresenta substituição de alguns acentos do console IBM850; totais, BUILD SUCCESS e horário permanecem legíveis.

| Comportamento | Evidência manual |
|---|---|
| Vínculo matrícula 73 / estrutura 80 e plano inicialmente pendente | PDF manual p. 1 |
| Falta de pré-requisito 412, desistência e criação do pedido 17 | p. 2 |
| EM_ANALISE sem cumprimento; nova recusa da inscrição dependente | p. 3 |
| Pedido 17 indeferido; histórico com abertura/decisão e A pendente | pp. 3–4; CSV de pedidos |
| Criação do pedido 18 e decisão de deferimento | pp. 4–5; CSV de pedidos |
| Histórico com ambas as decisões e A reconhecida | p. 5 |
| Inscrição confirmada na turma 232 e listagem | p. 5; CSV de inscrições |
| A RECONHECIDA e B EM_ANDAMENTO | pp. 6–7 |
| Recusa de redecidir pedido 18 e de duplicar o destino | p. 6; listagem posterior pp. 6–7 |
| Saída pelos menus 0 e encerramento com exit code 0 | p. 7 |

Os pareceres e o responsável são textos declarados para o teste sintético, não identificação autenticada. As referências documentais não representam upload, autenticidade verificada ou análise acadêmica automatizada. Concorrência, rollback, transação externa, legados, preservação de reprovação e reingresso têm cobertura automatizada; não são atribuídos ao manual acima.

## Estado final persistido

Etiqueta **V08MAN-81a42485d6f0**, matrícula **73**, estrutura **80**, destino A **412**, seguinte B **413**, turma **232**. IDs de preparação informados pelo autor nesta conversa: aluno 150, curso 148, professor 246, sala 262, período 300 e horário 123; os quatro CSVs finais não consultam todos esses cadastros.

| Registro | Valor real |
|---|---|
| Vínculo curricular 73/80 | 02/11/2026 20:34:37.505701 no servidor |
| Pedido 17 | INDEFERIDO; abertura 20:53:23.801229; decisão 21:07:12.973134 |
| Pedido 18 | DEFERIDO; abertura 21:10:50.340886; decisão 21:11:56.733428 |
| Origem / cargas analisadas | Conclusão 10/12/2025; origem 60 horas e destino 60 horas |
| Inscrição | 38; matrícula 73; turma 232; data 02/11/2026; ATIVO |
| Resultados finais da matrícula | Contagem igual a zero |

Horários de vínculo/pedidos acima são registros do servidor em 02/11/2026. O CSV de inscrição informa data, não o instante completo da confirmação; não se atribui a ele horário de execução. A contagem zero de resultados comprova ausência de resultado_inscricao da matrícula consultada, sem inferir inspeção de todas as tabelas de diário/notas.

## Resultados por classe

| Classe | Execuções |
|---|---:|
| ServicoSalaTest | 9 |
| AlocacaoTest | 15 |
| BaseAcademicaTest | 9 |
| CadastroAcademicoTest | 9 |
| EquivalenciaTest | 6 |
| InscricaoTest | 20 |
| MatriculaTest | 3 |
| ProfessoresDisciplinasTest | 9 |
| SalaTest | 23 |
| AlocacaoJdbcIT | 17 |
| BaseAcademicaJdbcIT | 17 |
| CadastroAcademicoJdbcIT | 12 |
| EquivalenciaJdbcIT | 21 |
| InscricaoJdbcIT | 14 |
| MatriculaJdbcIT | 29 |
| ProfessoresDisciplinasJdbcIT | 14 |
| SalaJdbcIT | 6 |

## Escopo encerrado e limites

A v0.8 fica validada para equivalência manual de uma origem para um destino, consulta de histórico acadêmico, plano básico derivado e utilização do reconhecimento em pré-requisitos da mesma matrícula. O plano não distribui por períodos ou ofertas e não atesta integralização completa. Aprovações por nota e reconhecimentos permanecem distintos. Decisões finais não são sobrescritas pelo fluxo; casos de impacto exigem tratamento específico.

O autor informou que trabalha em scoa_java_teste e deixa scoa para após validação. Isso é compatível com a estratégia de testes. Não houve consulta direta da versão MySQL do autor, inspeção/migração do banco scoa ou demonstração de restauração de backup neste fechamento. A execução do Codex usou MySQL 8.0.46 isolado, em outra instância. A presença da tabela v0.8 é exercitada pelos testes, mas não prova que scoa foi migrado.

Permanecem pendentes distribuição automática do plano por períodos/ofertas, conciliação importadora de legados, revisão/revogação, combinações de disciplinas, retificação de notas, rematrícula, transferência de estrutura, integralização completa, autenticação/autorização, web e recuperação de conexão. Roteiros futuros devem detalhar menus, opções, valores, consultas SQL, tabelas/colunas esperadas, resultados e nomes dos arquivos de evidência.

A revisão acadêmica v7.11 registra a seção 9.13 e os Quadros 41/42, mantendo a v7.10 preservada. O relatório de fechamento privado registra a conferência do Word e o envio dos commits; o Word editável não é publicado no GitHub. Originais: docs/testes/evidencias/execucao_autor_v0_8; seleção pública: este diretório. Não foi feita nova execução de testes neste fechamento.


Word privado v7.11: **497 páginas**, SHA-256 **1a999b526153d1e007c895bea2a1bf0a72b9276ed7ac7a041bb4212e4c84498c**. Estilos, texto/formatação dos capítulos 1–8 e do apêndice, imagens e 151 desenhos conferidos. Comparação visual: 478 corpos de página idênticos e 19 páginas revistas. Word instalado substituiu LibreOffice indisponível. V7.10 preservada.
