# Validação isolada do Codex da v0.7

Maven verify -Pmysql-it, em modo offline, concluiu com BUILD SUCCESS em 01/11/2026 às 20:19:26 UTC−03:00, duração exibida de 01:03 min. Resultado: **97 execuções unitárias e 109 de integração**, sem falhas, erros ou ignorados. São 206 execuções, incluindo as 172 regressões da v0.6 e 34 novas (20 unitárias e 14 JDBC). Não se trata de validação do autor.

Código preparado sobre o fechamento 7357409, versão 0.7.0-SNAPSHOT. As fontes executadas estão identificadas no manifesto FONTES_SHA256.json; não se atribui retrospectivamente um commit novo que ainda não foi criado.

Na transferência ao projeto original, 24 arquivos históricos não modificados apresentaram apenas diferença de finais de linha CRLF/LF em relação à cópia de testes. Seus bytes originais foram preservados, e o conteúdo foi comparado integralmente após normalização de finais de linha. No manifesto, sha256 identifica os bytes testados e sha256_entregue os bytes na pasta do projeto. Os arquivos novos e alterados da implementação foram copiados e conferidos por hash. Não se afirma igualdade byte a byte dos 24 históricos entre as duas pastas.

Ambiente: Windows, OpenJDK 25.0.1, Maven 3.9.11, JUnit Jupiter 6.0.1, Connector/J 26.7.0 e MySQL 8.0.46. Instância temporária exclusiva na porta 33317, ligada a 127.0.0.1. Os bancos do autor não foram migrados nem usados. O schema scoa_java_teste desta execução pertence exclusivamente à instância temporária.

## Resultado por classe

| Classe | Execuções |
|---|---:|
| ServicoSalaTest | 9 |
| AlocacaoTest | 15 |
| BaseAcademicaTest | 9 |
| CadastroAcademicoTest | 9 |
| InscricaoTest | 20 |
| MatriculaTest | 3 |
| ProfessoresDisciplinasTest | 9 |
| SalaTest | 23 |
| AlocacaoJdbcIT | 17 |
| BaseAcademicaJdbcIT | 17 |
| CadastroAcademicoJdbcIT | 12 |
| InscricaoJdbcIT | 14 |
| MatriculaJdbcIT | 29 |
| ProfessoresDisciplinasJdbcIT | 14 |
| SalaJdbcIT | 6 |

InscricaoJdbcIT verifica inscrição sem biblioteca, consulta sem reserva, duplicidade, revalidação de trancamento, janela fechada, estrutura e horários ausentes, estados desconhecidos, pré-requisito por resultado formal, fechamento e frequência, conflito e proteção dos horários, transação externa, console com desistência/EOF, concorrência da mesma matrícula e de dois alunos pela última vaga, proteção da estrutura, recusa de equivalência ainda não integrada e rollback após falha posterior à inserção do resultado.

## Conferências complementares

Após a bateria, o DDL v7.5 e as migrações V006/V007 foram aplicados integralmente em outro schema vazio da mesma instância temporária, scoa_v07_migracao. Tanto ele quanto a base isolada de testes apresentaram 75 tabelas; a linha de controle 1 estava presente. Essa conferência não executa migração no banco do autor nem prova restauração de seu backup.

A preparação manual sintética também foi executada na instância isolada. O console principal acessou o menu 15, vinculou estrutura, desistiu de uma confirmação, efetivou uma inscrição e listou o vínculo. IDs desse exercício Codex: matrícula 100, estrutura 92, turma 149, inscrição 25 ATIVO em 01/11/2026. São IDs reais daquela instância, não valores que o autor deve reproduzir. O registro textual capturado pelo pipe apresenta substituição de acentos; IDs e resultados decisivos permanecem legíveis. Não foram reconstruídas telas nem gerado PDF para representar execução inexistente.

## Tentativas preliminares e limites

A primeira execução JDBC encontrou uma etiqueta de fixture maior que o limite de codigo; o teste foi corrigido. A segunda identificou deadlock entre alunos disputando vaga e falta de privilégio administrativo no teste que tentava criar trigger. A implementação passou a serializar validações/efetivações por uma linha técnica transacional; a falha de atualização passou a ser simulada no JDBC, sem trigger. Uma tentativa posterior encontrou erro de compilação no nome de parâmetro do teste, corrigido antes da execução final. Logs preliminares permanecem privados, separados dos resultados aprovados.

O plano preliminar P07 antecede o código; a matriz de testes concretos foi detalhada durante a implementação. Não há alegação de cobertura integral de todas as combinações concorrentes do plano conceitual. Gestão de equivalências, plano de estudo, retificação, autenticação/autorização e integração automática com diário continuam pendentes, como descrito na nota técnica. Casos dependentes de equivalência sem integração formal são recusados, não dispensados.

XMLs originais, TXT e logs desta execução ficam privados em docs/testes/evidencias/execucao_codex_v0_7. A publicação contém somente relatório e manifesto de fontes revisados. O Word e as evidências históricas v0.6 foram preservados. A validação do autor permanece pendente.
