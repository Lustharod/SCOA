# SCOA — Sistema de Controle Acadêmico

Projeto acadêmico desenvolvido para a disciplina de Engenharia de Software do CEFET-MG. O SCOA reúne a modelagem de processos acadêmicos e administrativos e sua implementação incremental em Java e MySQL.

## Estado da implementação

A versão 0.9.0-SNAPSHOT acrescenta o menu 17 para simular a distribuição do plano por períodos e ofertas cadastrados. O usuário escolhe períodos e prioridades; somente pré-requisitos já cumpridos formalmente liberam sugestões. O resultado explica impedimentos e soma horas sugeridas, sem gravar plano ou inscrição, reservar vagas ou presumir créditos e limites institucionais. Consulte [as regras e limites da v0.9](docs/SCOA_Implementacao_v0_9.md). O menu 16 mantém equivalência manual, histórico acadêmico e plano básico da [v0.8](docs/SCOA_Implementacao_v0_8.md).

Antes de usar, faça backup, confira o schema selecionado e o estado de cada migração. Em base v0.7 já preparada, aplique uma vez `database/migrations/V008__equivalencia_manual.sql`; em base anterior, respeite DDL v7.5, V006, V007 e V008 nessa ordem. Não recrie bases existentes nem reaplique migrações parcialmente executadas. Não há migração automática.

O menu 15 preserva inscrição com contexto curricular, janela, horários, pré-requisitos, duplicidade e vagas, revalidando na confirmação, e o primeiro fechamento de resultado P1/P2/PF com frequência mínima de 75%. Consulte [o recorte da v0.7](docs/SCOA_Implementacao_v0_7.md). Projeção condicionada de dependências futuras, revisão de decisões, retificação de notas, autenticação/autorização e web continuam pendentes.

O cadastro permite incluir, consultar, listar, atualizar e inativar salas. A alocação verifica capacidade, situação da sala, sobreposição de horários e períodos letivos, incluindo conflitos da própria turma. Horários adjacentes são permitidos. A disponibilidade é revalidada no momento da gravação.

As alterações concorrentes são coordenadas por transações e bloqueios de registros. Salas com alocações em períodos atuais ou futuros não podem ser inativadas nem ter a capacidade reduzida abaixo da capacidade das turmas comprometidas.

A manutenção de turmas associa disciplina, professor, período e capacidade, avançando RF-MA-06 / UC-MA-06. Os horários são mantidos pela alocação. Disciplinas e professores devem estar previamente cadastrados e ativos. A implementação preserva os vínculos com inscrições existentes e impede mudanças incompatíveis com as alocações.

A entrega avança RF-MA-02, RF-MA-04, RF-MA-06, RF-MA-07 e RF-MA-08 na interface de console. Autenticação, autorização, interface web e os demais cadastros e módulos permanecem em desenvolvimento. A opção 12 do menu principal acessa professores, disciplinas e pré-requisitos; a opção 11 mantém períodos e turmas.

A v0.8 foi validada pelo autor em 02/11/2026 com 103 execuções unitárias e 130 de integração, sem falhas, erros ou ignorados. Consulte o [relatório e limites da validação](docs/testes/publicados/execucao_autor_v0_8/RELATORIO.md) e o [ambiente identificado](docs/testes/publicados/execucao_autor_v0_8/ambiente.txt). A validação em scoa_java_teste não implica migração de scoa.

A v0.9 foi validada pelo autor em 03/11/2026 às 21:57:27 UTC−03:00: 113 unitárias e 144 integração, zero falhas/erros/ignorados. Consulte o [relatório do autor](docs/testes/publicados/execucao_autor_v0_9/RELATORIO.md), o [ambiente](docs/testes/publicados/execucao_autor_v0_9/ambiente.txt) e a [execução separada do Codex](docs/testes/publicados/execucao_codex_v0_9/RELATORIO.md). Não há migração nova na v0.9; a validação em scoa_java_teste não implica migração de scoa. Word v7.12 e guias permanecem privados.

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

A [documentação do SCOA v7.5](docs/SCOA%20-%20Sistema%20de%20Controle%20Acad%C3%AAmico%20v7.5.pdf) apresenta os requisitos, a modelagem e o escopo planejado do sistema. A implementação disponível nesta versão corresponde ao recorte executável descrito acima; o documento apresenta também funcionalidades futuras.

Os arquivos `.drawio` em `docs` contêm as vistas editáveis dos casos de uso, classes, atividades, sequências e dos modelos conceitual, lógico e físico do banco de dados. As notas técnicas da [implementação v0.2](docs/SCOA_Implementacao_v0_2.md) e dos [cadastros v0.3](docs/SCOA_Implementacao_v0_3.md) descrevem as classes, regras e verificações incrementais. O [plano conceitual de testes da v0.3](docs/testes/SCOA_Plano_Conceitual_Testes_v0_3.pdf), também disponível em [texto](docs/testes/SCOA_Plano_Conceitual_Testes_v0_3.md), complementa a documentação v7.5 com 28 casos ligados aos requisitos e às evidências. Elaborado após a execução, registra essa origem e delimita a cobertura parcial dos casos de uso. Até a v0.5, o DDL v7.5 era suficiente sem alterações. A v0.6 exige a migração aditiva descrita acima; o arquivo original permanece preservado.

A [nota técnica da v0.4](docs/SCOA_Implementacao_v0_4.md) descreve o incremento e seus limites. O [plano da v0.4](docs/testes/SCOA_Plano_Testes_Professores_Disciplinas_v0_4.pdf), preparado antes da codificação, complementa a documentação v7.5 com os casos de professores, disciplinas e pré-requisitos.

A [nota da v0.5](docs/SCOA_Implementacao_v0_5.md) detalha o recorte e suas limitações. O [plano P05](docs/testes/SCOA_Plano_Testes_Base_Academica_v0_5.md) foi preparado antes da codificação e aprovado pelo autor quanto à prioridade das bases acadêmicas.

## Verificação

A v0.5 passou por 74 testes unitários e 66 de integração, sem falhas, erros ou ignorados, em instância MySQL isolada. Inclui as 114 regressões da v0.4. O [relatório do Codex](docs/testes/publicados/execucao_codex_v0_5/RELATORIO.md) relaciona os 14 casos aos testes e identifica as fontes executadas. A validação do autor da v0.5 foi concluída: 74 testes unitários e 66 de integração aprovados, encerramento em 31/10/2026 às 22:52:45 UTC−03:00; PDF manual de 12 páginas e seis CSVs do banco registrados em 01/11/2026. O [relatório do autor](docs/testes/publicados/execucao_autor_v0_5/RELATORIO.md) detalha a cobertura, os formatos e os horários. Código validado: dc1a6f3. A revisão Word v7.8 permanece privada e amplia o capítulo 9.

A v0.4 foi verificada pelo Codex em instância MySQL isolada: 65 execuções unitárias e 49 de integração, sem falhas, erros ou ignorados, incluindo todas as regressões da v0.3 e os fluxos de console com conferência da persistência. Os 15 casos planejados foram executados e estão rastreados no [relatório da v0.4](docs/testes/publicados/execucao_codex_v0_4/RELATORIO.md). A validação do autor foi concluída em 31/10/2026: 65 testes unitários e 49 de integração aprovados, roteiro manual em PDF e estado final do banco em seis CSVs. O [relatório do autor da v0.4](docs/testes/publicados/execucao_autor_v0_4/RELATORIO.md) reúne os resultados e as evidências revisadas. Código validado: b714404.

A implementação v0.3 foi verificada com 56 execuções unitárias e 35 de integração JDBC, sem falhas, erros ou testes ignorados. A bateria inclui as regressões de salas e alocação, manutenção de períodos e turmas e conexões simultâneas para as operações que afetam a disponibilidade. Os fluxos de cadastros e alocação também foram exercitados pelo console em uma instância MySQL isolada.

Os testes de integração utilizam um schema próprio. Os cenários sequenciais usam rollback; os cenários concorrentes confirmam fixtures sintéticas para permitir sua leitura por conexões independentes e removem os registros pelos IDs ao final. As credenciais locais não fazem parte do código-fonte.

A validação anterior do autor está documentada no [relatório da v0.3](docs/testes/publicados/execucao_autor_v0_3/RELATORIO.md), com ambiente, resultados por classe, rastreabilidade dos comportamentos verificados e capturas selecionadas do console e do banco.


A [nota da v0.6](docs/SCOA_Implementacao_v0_6.md) descreve as decisões e limites. O [plano P06](docs/testes/SCOA_Plano_Testes_Matricula_v0_6.md) antecede a implementação. Validação do Codex: 77 testes unitários e 95 de integração aprovados em MySQL isolado. Validação do autor concluída em 01/11/2026 às 15:04:41 UTC−03:00, com os mesmos totais; dois PDFs manuais e sete CSVs preservados. O [relatório do autor da v0.6](docs/testes/publicados/execucao_autor_v0_6/RELATORIO.md) identifica a cobertura e as ocorrências. Word v7.9 atualiza o capítulo 9 e preserva a v7.8. Código validado: 87fb486.


### Validação da v0.7

Rodrigo Lustosa concluiu verify -Pmysql-it em 01/11/2026 às 21:07:52 UTC−03:00: **97 testes unitários e 109 de integração**, sem falhas, erros ou ignorados. O [relatório do autor](docs/testes/publicados/execucao_autor_v0_7/RELATORIO.md) registra os PDFs, CSVs, IDs reais e limites da cobertura manual. As evidências do Codex permanecem separadas. O fechamento não migra bancos do autor nem altera as fontes já validadas.
