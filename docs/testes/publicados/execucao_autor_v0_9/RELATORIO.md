# Validação do autor do SCOA v0.9

O autor concluiu verify -Pmysql-it em **03/11/2026 às 21:57:27 UTC−03:00**, duração **17.900 s**, BUILD SUCCESS. Os 19 XMLs confirmam **113 execuções unitárias e 144 de integração, zero falhas, erros e ignorados**. São 257 execuções: 233 regressões da v0.8 e 24 novas, com os mesmos métodos e parametrizações do Codex. Não somar baterias ou execuções preliminares como casos distintos.

As fontes conferidas coincidem com o conjunto validado. Commit de implementação identificado retrospectivamente: **b0a39303f5a84981cfa70f752b13d1b2bfc2f874**. O fechamento não altera Java, testes, Maven ou DDL. ExemploSala local permanece preservado, sem incorporação automática ao Git.

## Evidências e ambiente

62 originais privados: seis PDFs, 16 CSVs, um TXT de console, 19 XMLs JUnit, 19 TXT de relatórios e failsafe-summary.xml. A seleção pública contém seis PDFs e 15 CSVs, com hashes coincidentes. O CSV de conexão e o TXT do console ficam privados, assim como XMLs e configurações. Relatório e ambiente são derivados, não novas medições.

CSV de conexão: scoa_java_teste, MySQL 8.0.46, porta 3306. CSV 02: 76 tabelas. Não houve conexão do Codex ao banco do autor ou migração de scoa. OpenJDK 25.0.1, Maven 3.9.11, IntelliJ 2025.3, Connector/J 26.7.0; console automatizado IBM850 e manual UTF-8. Há substituições de acentos no automatizado e linhas cortadas à direita nas capturas manuais; originais preservados, sem reconstrução.

O PDF automatizado anterior registra término às 21:16:36, duração de 01:50 min, emissão às 21:17:51. A execução final é corroborada pelo TXT verify e pelo PDF 21, emitido às 21:59:04. Os PDFs 10/14/15/19 foram emitidos às 21:37:09, 21:52:08, 21:54:21 e 21:56:41, respectivamente, em 03/11/2026 UTC−03:00. Emissão não data cada ação. Os PDFs manuais são cumulativos: quatro capturas não representam quatro execuções completas independentes.

## Roteiro e estado observado

Etiqueta V09MAN-8691c049d7e3. Matrícula 134, estrutura 137; disciplinas 563 (origem curricular), 564 (dependente), 565 (conflitante), 566 (sem oferta); períodos 409/410; turmas 309/310/311. Cada disciplina possui 60h. Pré-requisito: 564 depende de 563.

| Comportamento | Evidência |
|---|---|
| Estrutura, ofertas e requisito | CSVs 03 a 06 |
| Vínculo 134/137 | CSV 08; PDF 10 p. 1 |
| Origem sugerida, dependente bloqueada, conflito e ausência de oferta | PDF 10 p. 2 |
| Prioridade de 565 muda a sugestão para turma 311 | PDF 10 p. 2 |
| Simulações iniciais sem gravações | CSVs 09 e 11 idênticos |
| Pedido 43 e decisão manual de deferimento | PDF 14 p. 3; CSV 12 |
| Origem reconhecida e distribuição final | PDF 15 p. 4; PDF 19 p. 4 |
| 60h por período; sem plano ou inscrição persistidos | PDF 19 p. 4; CSVs 13/16/17/18 |
| Saída normal pelo menu 0, exit code 0 | PDF 19 p. 4 |

O arquivo 09 conserva o nome “contagens_antes_simulacao - Copia.csv”; o 11 usa “contagens_apos_simulacao.csv”. Ambos registram um vínculo e zero pedidos, inscrições, resultados, planos e itens. Os arquivos 13/16 registram um vínculo, um pedido e os demais valores iguais a zero. As contagens específicas 17/18 confirmam zero inscrições e resultados. Não se conclui ausência em todas as tabelas do sistema a partir dessas consultas.

Pedido 43: abertura **03/11/2026 21:43:35.754550**, deferimento **21:45:32.288395**, conforme registros do servidor; matrícula 134, estrutura 137, destino 563, cargas 60/60h. A conclusão foi digitada como **03/11/2023** no console; o CSV de preparação sugeria 03/11/2025. O campo de conclusão não integra o CSV 12; o valor digitado é atribuído à captura, sem inferir nova consulta. Parecer e responsável são declarações sintéticas, sem autenticação ou verificação material de documentos.

Depois do reconhecimento, 563 permanece RECONHECIDA; 564 é sugerida na turma 310/período 410 e 565 na turma 311/período 409. A 566 permanece sem oferta. São 60h de sugestões por período, sem reserva de vagas e sem efetivação de inscrição.

## Resultados por classe

| Classe | Execuções | Falhas erros ignorados |
|---|---:|---|
| ServicoSalaTest | 9 | 0 / 0 / 0 |
| AlocacaoTest | 15 | 0 / 0 / 0 |
| BaseAcademicaTest | 9 | 0 / 0 / 0 |
| CadastroAcademicoTest | 9 | 0 / 0 / 0 |
| EquivalenciaTest | 6 | 0 / 0 / 0 |
| InscricaoTest | 20 | 0 / 0 / 0 |
| MatriculaTest | 3 | 0 / 0 / 0 |
| PlanejamentoTest | 10 | 0 / 0 / 0 |
| ProfessoresDisciplinasTest | 9 | 0 / 0 / 0 |
| SalaTest | 23 | 0 / 0 / 0 |
| AlocacaoJdbcIT | 17 | 0 / 0 / 0 |
| BaseAcademicaJdbcIT | 17 | 0 / 0 / 0 |
| CadastroAcademicoJdbcIT | 12 | 0 / 0 / 0 |
| EquivalenciaJdbcIT | 21 | 0 / 0 / 0 |
| InscricaoJdbcIT | 14 | 0 / 0 / 0 |
| MatriculaJdbcIT | 29 | 0 / 0 / 0 |
| PlanejamentoJdbcIT | 14 | 0 / 0 / 0 |
| ProfessoresDisciplinasJdbcIT | 14 | 0 / 0 / 0 |
| SalaJdbcIT | 6 | 0 / 0 / 0 |
| Total | 257 | 0 / 0 / 0 |

Transação externa, conflito entre matrículas, lotação por outro aluno, reconhecimento futuro e oferta fora do horizonte têm cobertura automatizada; não são atribuídos ao manual. Não se alega novo teste específico de concorrência do planejador.

## Escopo encerrado e limites

A v0.9 fica validada para simulação com ofertas cadastradas, períodos escolhidos, prioridade explícita, pré-requisitos já cumpridos, diagnóstico de impedimentos e soma das horas sugeridas. Sugestão ou andamento não cumprem requisitos. Não há oferta presumida, teto institucional, créditos, otimização global, integralização ou previsão de formatura. Uma oferta futura pode ser simulada antes da janela; inscrição real permanece no menu 15 com revalidação.

O Word v7.12 registra a seção 9.14 e os Quadros 43/44, em revisão privada que preserva a v7.11. Revisão/revogação, retificação, importação de legados, rematrícula, autenticação/autorização, web e recuperação de conexão seguem pendentes. Word editável e guias não são publicados. A execução do Codex permanece separada. Não houve nova bateria neste fechamento.

Word privado v7.12: 501 páginas, SHA-256 60556b4c35d7904a8d5b14fc921d4e7c9e42d9f447ca2e9547deedf3dd60eac8. Estilos, capítulos1–8, apêndice, imagens e151 desenhos preservados; 485 corpos de página idênticos e16 páginas revistas.
