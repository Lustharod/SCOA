# Professores disciplinas e pré-requisitos do SCOA v0.4

A versão 0.4.0-SNAPSHOT acrescenta criação, consulta, listagem, atualização e inativação de professores e criação, consulta, listagem e atualização de disciplinas, com manutenção dos pré-requisitos. Avança RF-MA-02 / UC-MA-02 e RF-MA-04 / UC-MA-04 na interface de console, conforme a documentação v7.5. O cumprimento de pré-requisitos no ato da inscrição de RN-MA-01 ainda depende do futuro fluxo de inscrições.

## Uso e preservação dos dados

No menu principal de ScoaAplicacao, a opção 12 abre Professores, disciplinas e pré-requisitos. O submenu apresenta cadastro e manutenção, consulta dos relacionamentos e inclusão ou retirada de pré-requisitos. Toda gravação nova desse submenu exige confirmação S; outra resposta cancela a operação. As opções anteriores, incluindo 11 para períodos e turmas, foram preservadas.

O professor usa identificação e situação do modelo físico. O vínculo opcional com conta de acesso previamente existente é preservado; este cadastro não cria, altera ou inativa contas. A atualização não reativa professor inativo. A inativação preserva a linha e as turmas anteriores. Como restrição inicial de implementação, professor com turma ativa em período atual ou futuro não pode ser inativado até a reorganização desses vínculos. Essa proteção não substitui uma futura política institucional de substituição docente.

Disciplinas mantêm código, nome, carga horária e situação existente, sem reativação implícita. Não foi acrescentada uma operação de inativação de disciplina a UC-MA-04. O sistema não presume unicidade de identificação de professor ou código de disciplina onde o DDL aprovado não a estabelece. Os identificadores do banco distinguem os registros. Novos professores e disciplinas ativos passam a aparecer nas referências do cadastro de turmas.

## Pré-requisitos e concorrência

As relações identificam a disciplina dependente e seu pré-requisito. A gravação exige ambos os registros existentes, rejeita relação repetida, autorreferência e ciclos diretos ou indiretos. Retirar um pré-requisito não exclui as disciplinas. O cadastro da relação não avalia o histórico do aluno nem efetiva inscrições.

O repositório coordena as alterações do grafo por bloqueios das disciplinas em ordem de identificador e leitura com bloqueio das relações. A verificação usa os registros atuais, inclusive quando a transação já possuía uma visão anterior do banco. Os bloqueios permanecem até o encerramento da transação própria ou do chamador. O procedimento serializa as escritas de pré-requisitos nesta entrega; uma futura solução para volumes maiores deve manter a mesma proteção contra ciclos concorrentes.

A inativação de professor bloqueia seu registro, coordenando-se com a validação do vínculo na criação de turma. Contenção ou deadlock pode exigir consultar o estado e repetir a operação. O sistema não informa sucesso quando a transação falha. Alterações SQL externas ou novos repositórios devem respeitar os mesmos protocolos.

## Planejamento e verificação

O [plano da v0.4](testes/SCOA_Plano_Testes_Professores_Disciplinas_v0_4.pdf), também disponível em [texto](testes/SCOA_Plano_Testes_Professores_Disciplinas_v0_4.md), foi registrado antes da codificação e define 15 casos sem valores concretos de entrada. Todos foram executados pelo Codex, com 65 execuções unitárias e 49 de integração, sem falhas, erros ou ignorados. As 91 execuções da v0.3 estão incluídas nesses totais. Os fluxos de console foram verificados por entrada controlada com conferência dos dados persistidos; a validação do autor foi concluída em 31/10/2026, com 65 testes unitários e 49 de integração aprovados, PDF manual de 17 páginas e seis CSVs do banco. O [relatório do autor](testes/publicados/execucao_autor_v0_4/RELATORIO.md) distingue resultados automatizados, manual e estado final.

O [relatório da execução do Codex](testes/publicados/execucao_codex_v0_4/RELATORIO.md) registra os vínculos de cada caso, as classes executadas e os limites. Os arquivos de fontes testados são identificados por SHA-256, e coincidem com o código registrado posteriormente no commit b714404, sem mudanças de código no fechamento. Não se atribui a execução da v0.4 ao commit base a3d0282.

Foram preservados os diagramas e as 71 tabelas do DDL v7.5. O Word v7.7 atualiza o capítulo 9 com a validação do autor. O plano é um complemento da documentação aprovada. Autenticação, autorização, interface web, gestão de contas, inscrições e os demais módulos não foram validados por esta entrega. A execução do Codex usou instância isolada; a validação do autor usou seu schema scoa_java_teste.
