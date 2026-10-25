# SCOA — Sistema de Controle Acadêmico

Projeto acadêmico desenvolvido para a disciplina de Engenharia de Software do CEFET-MG. O SCOA reúne a modelagem de processos acadêmicos e administrativos e sua implementação incremental em Java e MySQL.

## Estado da implementação

A versão 0.3.0-SNAPSHOT reúne os cadastros de salas, períodos letivos e turmas e a alocação por dia e horário, com persistência no MySQL.

O cadastro permite incluir, consultar, listar, atualizar e inativar salas. A alocação verifica capacidade, situação da sala, sobreposição de horários e períodos letivos, incluindo conflitos da própria turma. Horários adjacentes são permitidos. A disponibilidade é revalidada no momento da gravação.

As alterações concorrentes são coordenadas por transações e bloqueios de registros. Salas com alocações em períodos atuais ou futuros não podem ser inativadas nem ter a capacidade reduzida abaixo da capacidade das turmas comprometidas.

A manutenção de turmas associa disciplina, professor, período e capacidade, avançando RF-MA-06 / UC-MA-06. Os horários são mantidos pela alocação. Disciplinas e professores devem estar previamente cadastrados e ativos. A implementação preserva os vínculos com inscrições existentes e impede mudanças incompatíveis com as alocações.

A entrega avança RF-MA-06, RF-MA-07 e RF-MA-08 na interface de console. Autenticação, autorização, interface web, gerenciamento de inscrições e os demais cadastros e módulos permanecem em desenvolvimento.

## Tecnologias

| Componente | Versão |
|---|---|
| Java | 25 |
| Maven | 3.9.11 |
| JUnit Jupiter | 6.0.1 |
| MySQL Connector/J | 26.7.0 |
| Banco de dados de referência | MySQL 8.0.16 ou superior na série 8.0 |

## Organização

- `src/main/java`: implementação Java, organizada em domínio, aplicação e persistência.
- `src/test/java`: testes unitários e de integração.
- `database`: DDL e testes do banco de dados.
- `docs`: documentação técnica em PDF e diagramas editáveis do projeto.
- `pom.xml` e Maven Wrapper: definição das dependências e da compilação.

## Documentação técnica

A [documentação do SCOA v7.5](docs/SCOA%20-%20Sistema%20de%20Controle%20Acad%C3%AAmico%20v7.5.pdf) apresenta os requisitos, a modelagem e o escopo planejado do sistema. A implementação disponível nesta versão corresponde ao gerenciamento de salas descrito acima.

Os arquivos `.drawio` em `docs` contêm as vistas editáveis dos casos de uso, classes, atividades, sequências e dos modelos conceitual, lógico e físico do banco de dados. As notas técnicas da [implementação v0.2](docs/SCOA_Implementacao_v0_2.md) e dos [cadastros v0.3](docs/SCOA_Implementacao_v0_3.md) descrevem as classes, regras e verificações incrementais. O DDL v7.5 permanece compatível, sem novas tabelas ou colunas.

## Verificação

A implementação v0.3 foi verificada com 56 execuções unitárias e 35 de integração JDBC, sem falhas, erros ou testes ignorados. A bateria inclui as regressões de salas e alocação, manutenção de períodos e turmas e conexões simultâneas para as operações que afetam a disponibilidade. Os fluxos de cadastros e alocação também foram exercitados pelo console em uma instância MySQL isolada.

Os testes de integração utilizam um schema próprio. Os cenários sequenciais usam rollback; os cenários concorrentes confirmam fixtures sintéticas para permitir sua leitura por conexões independentes e removem os registros pelos IDs ao final. As credenciais locais não fazem parte do código-fonte.
