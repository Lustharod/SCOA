# Alterações do SCOA

## 0.3.0-SNAPSHOT

- Incluído relatório da validação do autor em 27–28/10/2026, com ambiente, resultados extraídos dos relatórios JUnit e seleção de capturas originais.

- Incluídos cadastro, consulta, listagem, atualização e inativação de períodos letivos e turmas, com confirmação das operações no console.
- Turmas vinculam disciplina e professor previamente cadastrados e ativos e um período letivo ativo.
- Protegidas datas e situação de períodos vinculados a turmas, preservados os horários e os vínculos das turmas e validada a capacidade frente às salas alocadas e inscrições registradas.
- Alocação passou a recusar turmas e períodos inativos, mantendo as consultas históricas.
- Validação: 56 execuções unitárias e 35 JDBC aprovadas, incluindo regressões e concorrência; fluxo do console validado. DDL v7.5 preservado.

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
