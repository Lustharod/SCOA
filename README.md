# SCOA — Sistema de Controle Acadêmico

Projeto acadêmico desenvolvido para a disciplina de Engenharia de Software do CEFET-MG. O SCOA reúne a modelagem de processos acadêmicos e administrativos e sua implementação incremental em Java e MySQL.

## Estado da implementação

A versão 0.2.0-SNAPSHOT reúne o gerenciamento de salas e sua alocação a turmas por dia e horário, com persistência no MySQL.

O cadastro permite incluir, consultar, listar, atualizar e inativar salas. A alocação verifica capacidade, situação da sala, sobreposição de horários e períodos letivos, incluindo conflitos da própria turma. Horários adjacentes são permitidos. A disponibilidade é revalidada no momento da gravação.

As alterações concorrentes são coordenadas por transações e bloqueios de registros. Salas com alocações em períodos atuais ou futuros não podem ser inativadas nem ter a capacidade reduzida abaixo da capacidade das turmas comprometidas.

A entrega avança RF-MA-07 e RF-MA-08, com as regras de RN-MA-03 na interface de console. Autenticação, autorização, interface web, cadastros completos de turmas e períodos e os demais módulos permanecem em desenvolvimento.

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

Os arquivos `.drawio` em `docs` contêm as vistas editáveis dos casos de uso, classes, atividades, sequências e dos modelos conceitual, lógico e físico do banco de dados. A [nota técnica da implementação v0.2](docs/SCOA_Implementacao_v0_2.md) descreve as novas classes, os critérios de alocação e a verificação desta entrega. O DDL v7.5 permanece compatível, sem novas tabelas ou colunas.

## Verificação

A implementação v0.2 foi verificada com 47 execuções unitárias e 23 de integração JDBC, sem falhas, erros ou testes ignorados. A bateria inclui conexões simultâneas para alteração de salas, disputa de alocação e preservação de capacidade e situação. O fluxo de alocação e desalocação também foi exercitado pelo console em uma instância MySQL isolada.

Os testes de integração utilizam um schema próprio. Os cenários sequenciais usam rollback; os cenários concorrentes confirmam fixtures sintéticas para permitir sua leitura por conexões independentes e removem os registros pelos IDs ao final. As credenciais locais não fazem parte do código-fonte.
