# Validação Codex do incremento acadêmico v0.14

Em 25/11/2026, o incremento com plano institucional e rematrícula em lote passou em **172 testes unitários e 289 testes de integração**, sem falhas, erros ou ignorados. O Maven exibiu BUILD SUCCESS às 10:40:46 UTC-03:00, com duração de 01:14 min. A rodada manual do autor será integrada ao final dos blocos; ainda não foi realizada na v0.14.

## Plano institucional

Treze casos JDBC verificam prévia/consulta sem gravação, autoria, contexto, componentes e requisitos, versões imutáveis, detecção de dados/versão alterados antes da confirmação, efeito de resultado formal e reconhecimento sob revisão, acesso próprio, permissões independentes, inconsistência curricular, legado/data futura, estrutura ausente/vazia, vínculo/conta revogados e rollback em falha de auditoria. Domínio e console verificam seleção, prévia, confirmação/cancelamento e ausência de geração automática pela consulta.

O plano registra um retrato acadêmico e sugestões; não constitui reserva de vaga nem prova de conclusão de uma disciplina sugerida. Uma consulta mantém a versão existente e informa atualização necessária, quando os dados mudam.

## Rematrícula em lote

Catorze casos JDBC verificam prévia sem efeitos, renovação dos mesmos vínculos com autoria, propostas sem inscrição automática, encaminhamento integral para análise manual quando nenhuma oferta está disponível, biblioteca indisponível para um candidato sem interromper os demais, plano ausente/desatualizado e resultados pendentes, repetição de chave, novo processamento sem duplicidade, colisão de chave, prévia desatualizada, seleção de população, sessão/permissões revogadas, calendário e rollback integral em falha de auditoria.

O caminho individual de rematrícula permanece protegido para o próprio aluno. O lote usa a mesma validação de vínculo, curso, período e biblioteca, com autorização da secretaria. A confirmação de cada turma continua separada e sujeita a nova elegibilidade.

## Regressão e rastreabilidade

Todos os testes anteriores de acesso, cadastros, matrícula, inscrição, reconhecimento, planejamento, diário, transferências internas, documentos e atendimento permanecem na bateria. [Resultados por classe](RESULTADOS.md).

| Ponto validado em 25/11/2026 | Unitários | Integração |
| --- | ---: | ---: |
| Primeiro incremento, 08:42:18 UTC-03 | 159 | 241 |
| Diário, 10:16:12 UTC-03 | 166 | 262 |
| Plano versionado, 10:33:22 UTC-03 | 170 | 275 |
| Plano e lote, 10:40:46 UTC-03 | 172 | 289 |

São baterias de versões sucessivas; não somar as contagens como casos distintos. Uma tentativa inicial do plano falhou na preparação de período com descrição acima do limite existente; a fixture foi corrigida, preservando o limite do banco. Os relatórios anteriores permanecem privados e identificados separadamente.

Instância MySQL 8.0.46 nova, scoa_java_teste, porta local 33386; DDL de testes v7.5 e V006 a V018. Nenhuma migração de scoa. As fixtures SQL são preparação/restauração dos testes, sem atribuição a atos auditados do console. Log completo, XML/TXT e manifesto permanecem privados. Os [hashes das fontes](FONTES_SHA256.json) foram conferidos após a execução. [Ambiente](ambiente.txt).

Esta entrega amplia o ramo desenvolvimento/academico-completo sobre 2cea6b4. Main permanece na v0.13 fechada. A v0.14 e o módulo acadêmico continuam em desenvolvimento; gestão/auditoria docente, transferências externas, relatórios/exportações, horários sem sala, acesso operacional, parametrização e web seguem pendentes. [Regras e limites](../../../SCOA_Implementacao_v0_14.md).
