# Alterações do SCOA

## 0.2.0-SNAPSHOT

- Corrigida a perda de alterações entre instâncias no cadastro de salas. Consulta para alteração e gravação passaram a ocorrer sob bloqueio na mesma transação.
- Incluídas consulta de turmas e horários, pesquisa de salas disponíveis, alocação e desalocação pelo console.
- Implementadas as verificações de capacidade e disponibilidade de RF-MA-08 / RN-MA-03, considerando horários semanais e sobreposição de períodos letivos.
- Impedidas a inativação e a redução incompatível de capacidade de salas com alocações em períodos atuais ou futuros.
- Incluídos testes de concorrência com conexões MySQL independentes e dados demonstrativos exclusivos do schema de testes.
- Validação: 47 execuções unitárias e 23 JDBC aprovadas. Estrutura SQL v7.5 preservada.

## 0.1.0-SNAPSHOT

- Primeira implementação do gerenciamento de salas em Java e MySQL, com cadastro, consulta, listagem, atualização e inativação.
- Validação inicial: 32 execuções unitárias e seis JDBC aprovadas.
