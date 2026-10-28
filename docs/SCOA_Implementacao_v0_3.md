# Cadastros acadêmicos do SCOA v0.3

A versão 0.3.0-SNAPSHOT acrescenta a manutenção de períodos letivos e turmas ao console e amplia RF-MA-06 / UC-MA-06. A documentação v7.5 exige disciplina e professor previamente cadastrados; esta entrega permite selecionar essas referências ativas e associá-las à turma. Horários e salas continuam sendo mantidos pelo fluxo de alocação da versão anterior.

## Classes e persistência

`CadastroPeriodoLetivo` representa ID, descrição, datas e situação do período. O objeto `PeriodoLetivo`, utilizado desde a versão 0.2, continua representando apenas o intervalo de datas, evitando confundir o cadastro com o valor utilizado na verificação de conflitos.

`Turma` representa ID, disciplina, professor, período, código, capacidade e situação. `TurmaResumo` permanece como projeção de consulta da alocação. As validações respeitam os tipos e limites do DDL v7.5, inclusive contagem de caracteres Unicode e datas compatíveis com DATE do MySQL.

`ServicoCadastroAcademico` coordena as operações por meio da interface `RepositorioCadastroAcademico`. A implementação JDBC utiliza consultas parametrizadas, transações e leituras com bloqueio para as alterações. `ConsoleCadastrosAcademicos` organiza as entradas, apresenta os dados e solicita confirmação antes de gravar.

## Consistência das alterações

- As datas e a situação de um período só podem mudar enquanto ele não possui turmas. A descrição pode ser corrigida mesmo após o vínculo. Trata-se de uma restrição inicial de implementação para preservar o planejamento existente; uma futura reprogramação de períodos deverá revalidar todas as turmas e alocações em uma operação própria.
- Uma turma ativa exige referências ativas. Os vínculos acadêmicos não podem ser substituídos quando a turma possui horários; a operação exige desalocação prévia.
- A capacidade da turma não pode superar a de uma sala alocada em período atual ou futuro. A inativação também é recusada enquanto existirem esses compromissos.
- A capacidade não pode ficar abaixo do número de inscrições registradas. Nesta etapa, a contagem é conservadora e inclui todos os registros de inscrição; a distinção entre inscrições vigentes, canceladas e históricas será formalizada na implementação do fluxo de inscrições. Havendo inscrições, mudanças de vínculos ou situação são recusadas para preservar a vida acadêmica.
- Atualizações preservam a situação atual do cadastro e inativações preservam as linhas existentes. Consultas históricas continuam disponíveis; a alocação exige turma e período ativos.

Os bloqueios são mantidos até o término da transação própria ou da transação externa do chamador. Contenção ou deadlock pode exigir nova tentativa após a consulta dos dados; o sistema não informa sucesso quando a transação falha. Alterações diretas no SQL e futuros cadastros que modifiquem as mesmas entidades precisam respeitar essas invariantes.

## Verificação e limites

Em 25/10/2026, foram aprovadas 56 execuções unitárias e 35 JDBC com Java 25.0.1, Maven 3.9.11, JUnit 6.0.1, Connector/J 26.7.0 e MySQL 8.0.46 em instância isolada. A bateria inclui os 70 casos executáveis da versão anterior, nove novas execuções de domínio e 12 JDBC de cadastros. Duas conexões verificam criação concorrente de turma durante alteração de período e alocação durante mudança de capacidade nas duas ordens.

O console foi exercitado para criar período e turma, consultar e atualizar turma, cancelar uma operação antes de gravar, recusar mudança de datas de período vinculado, inativar uma turma sem compromissos e recusar sua alocação posterior.

As 71 tabelas do DDL v7.5, o Word e os diagramas aprovados foram preservados. Esta nota registra a evolução da implementação. Autenticação, autorização, interface web, manutenção completa de disciplinas e professores, inscrições e demais módulos permanecem pendentes; os testes desta versão não representam aprovação desses fluxos.

O autor também concluiu a execução automatizada em 27/10/2026 e a conferência manual registrada em 28/10/2026. Os resultados e as evidências selecionadas estão no [relatório de validação](testes/publicados/execucao_autor_v0_3/RELATORIO.md).
