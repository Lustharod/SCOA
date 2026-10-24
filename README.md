# SCOA — Sistema de Controle Acadêmico

Projeto acadêmico desenvolvido para a disciplina de Engenharia de Software do CEFET-MG. O SCOA reúne a modelagem de processos acadêmicos e administrativos e sua implementação incremental em Java e MySQL.

## Estado da implementação

A versão 0.1.0 inicia o módulo acadêmico pelo gerenciamento de salas: cadastro, consulta, listagem, atualização e inativação, com persistência no MySQL.

Essa entrega implementa parte de RF-MA-07 / UC-MA-07. Disponibilidade por horário, alocação de salas às turmas, autenticação, autorização e os demais módulos fazem parte da evolução prevista do projeto.

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

Os arquivos `.drawio` em `docs` contêm as vistas editáveis dos casos de uso, classes, atividades, sequências e dos modelos conceitual, lógico e físico do banco de dados.

## Verificação

A implementação inicial foi verificada com 32 execuções unitárias e seis testes de integração JDBC, sem falhas ou erros. O cadastro, a consulta, a atualização e a inativação também foram conferidos pelo console e pelo MySQL Workbench.

Os testes de integração utilizam um schema próprio de testes e transações desfeitas ao final. O repositório contém a estrutura SQL; os dados locais e as credenciais de acesso não fazem parte do código-fonte.
