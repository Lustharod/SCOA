# SCOA — Sistema de Controle Acadêmico

Projeto acadêmico desenvolvido para a disciplina de Engenharia de Software do CEFET-MG. O SCOA reúne a modelagem de processos acadêmicos e administrativos e sua implementação incremental em Java e MySQL.

## Estado da implementação

A versão 0.4.0-SNAPSHOT reúne os cadastros de professores, disciplinas e pré-requisitos, salas, períodos letivos e turmas e a alocação por dia e horário, com persistência no MySQL. A manutenção de pré-requisitos rejeita autorreferência e ciclos, inclusive sob operações concorrentes. A inativação de professores preserva o histórico e protege turmas ativas em períodos atuais ou futuros.

O cadastro permite incluir, consultar, listar, atualizar e inativar salas. A alocação verifica capacidade, situação da sala, sobreposição de horários e períodos letivos, incluindo conflitos da própria turma. Horários adjacentes são permitidos. A disponibilidade é revalidada no momento da gravação.

As alterações concorrentes são coordenadas por transações e bloqueios de registros. Salas com alocações em períodos atuais ou futuros não podem ser inativadas nem ter a capacidade reduzida abaixo da capacidade das turmas comprometidas.

A manutenção de turmas associa disciplina, professor, período e capacidade, avançando RF-MA-06 / UC-MA-06. Os horários são mantidos pela alocação. Disciplinas e professores devem estar previamente cadastrados e ativos. A implementação preserva os vínculos com inscrições existentes e impede mudanças incompatíveis com as alocações.

A entrega avança RF-MA-02, RF-MA-04, RF-MA-06, RF-MA-07 e RF-MA-08 na interface de console. Autenticação, autorização, interface web, gerenciamento de inscrições e os demais cadastros e módulos permanecem em desenvolvimento. A opção 12 do menu principal acessa professores, disciplinas e pré-requisitos; a opção 11 mantém períodos e turmas.

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

A [documentação do SCOA v7.5](docs/SCOA%20-%20Sistema%20de%20Controle%20Acad%C3%AAmico%20v7.5.pdf) apresenta os requisitos, a modelagem e o escopo planejado do sistema. A implementação disponível nesta versão corresponde aos cadastros de salas, períodos e turmas e à alocação descritos acima.

Os arquivos `.drawio` em `docs` contêm as vistas editáveis dos casos de uso, classes, atividades, sequências e dos modelos conceitual, lógico e físico do banco de dados. As notas técnicas da [implementação v0.2](docs/SCOA_Implementacao_v0_2.md) e dos [cadastros v0.3](docs/SCOA_Implementacao_v0_3.md) descrevem as classes, regras e verificações incrementais. O [plano conceitual de testes da v0.3](docs/testes/SCOA_Plano_Conceitual_Testes_v0_3.pdf), também disponível em [texto](docs/testes/SCOA_Plano_Conceitual_Testes_v0_3.md), complementa a documentação v7.5 com 28 casos ligados aos requisitos e às evidências. Elaborado após a execução, registra essa origem e delimita a cobertura parcial dos casos de uso. O DDL v7.5 permanece compatível, sem novas tabelas ou colunas.

A [nota técnica da v0.4](docs/SCOA_Implementacao_v0_4.md) descreve o incremento e seus limites. O [plano da v0.4](docs/testes/SCOA_Plano_Testes_Professores_Disciplinas_v0_4.pdf), preparado antes da codificação, complementa a documentação v7.5 com os casos de professores, disciplinas e pré-requisitos.

## Verificação

A v0.4 foi verificada pelo Codex em instância MySQL isolada: 65 execuções unitárias e 49 de integração, sem falhas, erros ou ignorados, incluindo todas as regressões da v0.3 e os fluxos de console com conferência da persistência. Os 15 casos planejados foram executados e estão rastreados no [relatório da v0.4](docs/testes/publicados/execucao_codex_v0_4/RELATORIO.md). A validação do autor foi concluída em 31/10/2026: 65 testes unitários e 49 de integração aprovados, roteiro manual em PDF e estado final do banco em seis CSVs. O [relatório do autor da v0.4](docs/testes/publicados/execucao_autor_v0_4/RELATORIO.md) reúne os resultados e as evidências revisadas. Código validado: b714404.

A implementação v0.3 foi verificada com 56 execuções unitárias e 35 de integração JDBC, sem falhas, erros ou testes ignorados. A bateria inclui as regressões de salas e alocação, manutenção de períodos e turmas e conexões simultâneas para as operações que afetam a disponibilidade. Os fluxos de cadastros e alocação também foram exercitados pelo console em uma instância MySQL isolada.

Os testes de integração utilizam um schema próprio. Os cenários sequenciais usam rollback; os cenários concorrentes confirmam fixtures sintéticas para permitir sua leitura por conexões independentes e removem os registros pelos IDs ao final. As credenciais locais não fazem parte do código-fonte.

A validação anterior do autor está documentada no [relatório da v0.3](docs/testes/publicados/execucao_autor_v0_3/RELATORIO.md), com ambiente, resultados por classe, rastreabilidade dos comportamentos verificados e capturas selecionadas do console e do banco.
