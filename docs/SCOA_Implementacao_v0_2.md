# Implementação de salas e alocação no SCOA v0.2

A versão 0.2.0-SNAPSHOT amplia o cadastro de salas pela implementação da alocação de salas às turmas e corrige a perda de alterações concorrentes. A referência funcional é o documento v7.5, em especial RF-MA-07, RF-MA-08, UC-MA-07, UC-MA-08 e RN-MA-03.

## Responsabilidades das classes

| Componente | Responsabilidade |
|---|---|
| `Sala` | Validar e representar os dados da sala |
| `FaixaHorario` | Validar dia e horas e comparar intervalos |
| `PeriodoLetivo` | Representar datas e identificar a ocorrência de um dia na interseção de períodos |
| `TurmaResumo` | Projetar os dados de uma turma já cadastrada necessários à alocação |
| `HorarioAula` | Representar a associação entre turma, sala e horário semanal |
| `RegraAlocacao` | Verificar situação, capacidade, ocorrência do dia e conflitos |
| `ServicoSala` e `ServicoAlocacao` | Coordenar as operações da aplicação |
| Interfaces de repositório | Definir os contratos de persistência e operações atômicas |
| Implementações JDBC | Consultar, bloquear e persistir os registros no MySQL |
| `TransacaoJdbc` | Confirmar ou desfazer a operação, preservando a transação externa quando existir |

`TurmaResumo` é uma projeção de consulta; não representa a implementação completa do cadastro de turmas. Os records Java introduzidos nesta entrega representam dados imutáveis e validam seus valores na construção.

## Fluxo de alocação

O usuário seleciona uma turma; o console apresenta sua capacidade, período e horários existentes. Em seguida, o usuário informa o dia e o intervalo e consulta as salas disponíveis. Ao selecionar a sala, o sistema verifica novamente as condições e registra o horário. As turmas, salas e períodos devem estar previamente cadastrados, como exige UC-MA-08.

Uma sala deve estar ATIVO e ter capacidade igual ou superior à da turma. São recusados horários sobrepostos da mesma sala e da mesma turma. A verificação considera as datas dos períodos, mesmo quando seus IDs são diferentes.

Nesta implementação, dia 1 significa segunda-feira e dia 7, domingo, conforme a convenção ISO. Intervalos são tratados como [início, fim): 08:00–10:00 e 10:00–12:00 são adjacentes e permitidos. A precisão é de segundos, compatível com TIME do DDL. Intervalos que atravessam a meia-noite devem ser representados por horários separados; a estrutura atual exige fim maior que início no mesmo dia.

A interseção de períodos só produz conflito quando contém uma ocorrência do dia semanal escolhido. Isso evita recusar uma segunda-feira apenas porque os períodos se encontram em um domingo. Feriados, exceções de calendário e conflitos de professor não são modelados por esta entrega.

## Consistência entre instâncias

Uma alteração da sala lê o registro com `SELECT ... FOR UPDATE`, aplica a modificação aos dados atuais e grava antes de encerrar a transação. Uma atualização mantém a situação encontrada sob bloqueio; uma inativação mantém a capacidade e os demais dados atuais. Objetos antigos não podem ser enviados diretamente à operação de inserção para sobrescrever registros existentes.

A inclusão de um horário bloqueia a sala e depois a turma e o período, verifica novamente as regras e inclui o registro na mesma transação. A pesquisa de disponibilidade é informativa; o resultado é confirmado novamente ao alocar. Duas solicitações concorrentes não podem utilizar o mesmo resultado antigo para criar alocações conflitantes.

Alterações de sala verificam as alocações em períodos com data final igual ou posterior à data corrente do banco. Havendo compromissos nesses períodos, a inativação é recusada e a capacidade não pode ficar abaixo da de nenhuma turma alocada. A desalocação remove o vínculo de horário e permite reavaliar essas operações.

Quando a conexão usa autocommit, o repositório inicia e encerra a transação da operação. Se o chamador já mantém uma transação, são usados savepoints e o commit continua sendo responsabilidade do chamador. Os bloqueios permanecem até o encerramento da transação externa. Cada sessão utiliza sua própria conexão. Falhas de bloqueio ou deadlocks são apresentadas como falha da transação, sem anunciar sucesso; uma nova tentativa pode ser feita após consultar os dados.

Essas garantias se aplicam às operações implementadas nos repositórios Java. Alterações diretas no banco e futuras operações de manutenção de turma ou período precisam respeitar as mesmas regras e ordem de bloqueios.

Referência técnica: [leituras com bloqueio do InnoDB](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html).

## Relação com os modelos e verificação

A implementação utiliza as tabelas sala, turma, periodo_letivo e horario_aula do DDL v7.5, sem alterar colunas, chaves ou as 71 tabelas existentes. Os diagramas e o PDF v7.5 continuam como base aprovada de análise e modelagem; esta nota registra as classes e decisões específicas desta entrega.

Em 25/10/2026, a bateria foi executada com Java 25.0.1, Maven 3.9.11, JUnit 6.0.1, Connector/J 26.7.0 e MySQL 8.0.46: 47 execuções unitárias e 23 JDBC aprovadas. Foram verificadas duas ordens de atualização/inativação, disputa de alocação da mesma sala e da mesma turma, e disputa de alocação com redução de capacidade ou inativação. O fluxo do console também foi validado com dados sintéticos.

Autenticação, autorização, interface web e os demais cadastros e módulos continuam pendentes. A interface de console permanece como interface inicial de desenvolvimento e validação.
