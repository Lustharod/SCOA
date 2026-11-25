# Validação Codex do incremento acadêmico v0.14

Em 25/11/2026, o incremento com horários definidos antes da sala e alocação posterior passou em **180 testes unitários e 348 testes de integração**, sem falhas, erros ou ignorados. O Maven exibiu BUILD SUCCESS às 14:55:44 UTC-03:00, com duração de 02:01 min. A rodada manual do autor será integrada ao final dos blocos; ainda não foi realizada na v0.14.

## Horários independentes da sala

Dezoito casos JDBC verificam planejamento em grupo por tempos oficiais de 50 minutos, intervalos sem reserva automática, sala pendente explícita, autoria, versões e histórico, conflito com rollback do grupo, atribuição e retirada posterior de sala preservando ID/dias/tempos, cancelamento sem exclusão do registro, capacidade e disponibilidade revalidadas, conflito de versão, inscrição do aluno e consulta própria com sala pendente, manutenção da sala após inscrição/aula sem alterar lançamentos, bloqueio de mudança de tempos/vínculos da turma comprometida, permissões atuais, revogação e rollback em falha de auditoria. O caminho autenticado de alocação antigo também preserva histórico e o legado mantém autoria desconhecida. Dois casos de domínio verificam sala opcional sem ID zero e seleção de tempos/dias válidos.

Consultas de aluno, elegibilidade, planejamento, diário e verificação de salas usam os horários ativos. Uma sala pendente não apaga o horário nem impede sua consulta; cancelar um horário não exclui seu ID. Mudanças de dias/tempos de turma com inscrições ou aulas exigem tratamento acadêmico próprio e são recusadas neste fluxo. As verificações de concorrência de alocação/capacidade anteriores permanecem na regressão.

## Regressão e rastreabilidade

Todos os testes anteriores de acesso, cadastros, matrícula, inscrição, reconhecimento, planejamento, diário, transferências internas, documentos e atendimento permanecem na bateria. [Resultados por classe](RESULTADOS.md).

As contagens de versões sucessivas não devem ser somadas como casos distintos. O incremento anterior publicado em 87582e95fd6192a8be45286a0945ee99675c7c80 tinha 178 unitários e 330 integrações. Os originais de cada execução, inclusive tentativas que falharam, permanecem privados e identificados separadamente.

Instância MySQL 8.0.46 nova, scoa_java_teste, porta local 33386; DDL de testes v7.5 e V006 a V021. Nenhuma migração de scoa. As fixtures SQL são preparação/restauração dos testes, sem atribuição a atos auditados do console. Log completo, XML/TXT e manifesto permanecem privados. Os [hashes das fontes](FONTES_SHA256.json) foram conferidos após a execução. [Ambiente](ambiente.txt).

Este ponto amplia o ramo desenvolvimento/academico-completo. Main permanece na v0.13 fechada. A v0.14 e o módulo acadêmico continuam em desenvolvimento. Relatórios/exportações, administração operacional de concessões, parametrização e interface web seguem pendentes. [Regras e limites](../../../SCOA_Implementacao_v0_14.md).
