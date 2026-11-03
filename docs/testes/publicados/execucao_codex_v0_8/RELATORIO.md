# Validação isolada do Codex — SCOA v0.8

Maven verify -Pmysql-it em modo offline concluiu com BUILD SUCCESS em 01/11/2026 às 23:35:10 UTC−03:00, duração exibida 01:31 min. Foram **103 execuções unitárias e 130 de integração**, sem falhas, erros ou ignorados: **233 execuções**, sendo 206 regressões da v0.7 e 27 novas (6 unitárias e 21 JDBC). Não se trata de validação do autor.

Implementação sobre d081bcdb72a8c16a5ea10a29e67066f6157e62d6, software 0.8.0-SNAPSHOT. A matriz SCOA_P08_Implementado_v0_8.md foi detalhada durante o desenvolvimento; a proposta inicial antecedeu a codificação. Não se afirma que todos os métodos de teste antecederam o código.

## Ambiente e isolamento

Windows, OpenJDK 25.0.1, Maven 3.9.11, JUnit Jupiter 6.0.1, Connector/J 26.7.0 e MySQL 8.0.46. Instância temporária exclusiva ligada a 127.0.0.1, porta 33318; schema scoa_java_teste criado nessa instância. Não foram usados ou migrados os bancos do autor.

DDL v7.5 e V006/V007/V008 foram aplicados integralmente na instância isolada: 76 tabelas. V008 acrescenta apenas pedido_equivalencia e não promove linhas legadas. Essa aplicação não prova migração em base do autor nem restauração de backup.

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

Os novos JDBC verificam deferimento/indeferimento, efeitos derivados no histórico/plano, pré-requisitos, matrícula de reingresso, preservação de reprovação anterior, aprovação formal, inscrição em andamento, destino curricular, mudança de carga, imutabilidade da decisão, legados, rollback após UPDATE, transação externa/savepoints, concorrência de decisões e concorrência entre deferimento e inscrição no destino. Console com desistência/EOF também é exercitado.

## Exercício adicional do console

O console principal foi executado em duas sessões com dados sintéticos da instância isolada. A preparação SQL cria o contexto; não constitui execução de matrícula institucional. Etiqueta V08MAN-6a5ff18ed666: matrícula 114, estrutura 106, destino 285, seguinte 286, turma 146, pedido 29 e inscrição 31.

O exercício vinculou estrutura pelo menu 15, verificou a falta de pré-requisito, registrou pedido em análise pelo menu 16, consultou histórico/plano, deferiu manualmente, confirmou a inscrição dependente e consultou o plano atualizado. Destino 285 RECONHECIDA; seguinte 286 EM_ANDAMENTO. Consulta real do banco confirmou pedido 29 DEFERIDO e inscrição 31 ATIVO; nenhum resultado_inscricao foi criado para a matrícula.

O pedido foi registrado em 01/11/2026 23:38:02.108814 e decidido em 23:38:05.905853, conforme timestamps do servidor isolado. Os logs são saída textual real, com acentos conferidos; não foi produzido PDF nem reconstruída captura. O exercício adicional não é somado às execuções JUnit e não substitui a validação manual do autor. Indeferimento, concorrência e rollback são atribuídos aos testes automatizados, não a esse exercício adicional.

## Rastreabilidade e limites

FONTES_SHA256.json identifica os bytes executados de Java, testes, Maven e banco. Na transferência, arquivos históricos não modificados permanecem com os bytes do autor; eventuais diferenças CRLF/LF com o checkout isolado não são normalizadas no original. Para arquivos alterados, o manifesto identifica também os bytes entregues quando houver diferença de quebra de linha. Evidências e logs brutos ficam privados em docs/testes/evidencias/execucao_codex_v0_8; não publicar propriedades dos XMLs.

Uma execução preliminar completa também passou, com 103 unitárias e 123 de integração, antes do acréscimo de sete testes de impacto e do detalhamento da situação da matrícula no plano. Uma execução anterior apenas unitária passou com 97 regressões. Essas execuções não são somadas à final. As tentativas iniciais de conexão da instância temporária falharam antes da bateria JDBC; a instância foi iniciada em sessão persistente antes da preparação bem-sucedida. Não houve execução de testes contra banco do autor.

O recorte não inclui armazenamento de anexos, validação material dos documentos, autenticação/autorização, conciliação importadora de legados, combinações de disciplinas, revisão/revogação, distribuição automática por períodos, retificação de notas, integralização completa, rematrícula, web ou recuperação de conexão. A decisão administrativa é declaratória na interface atual.

Validação do autor e atualização do Word continuam pendentes. Word v7.10, demais documentos históricos e evidências v0.7 foram preservados; nenhum commit ou envio ao GitHub integra esta execução.
