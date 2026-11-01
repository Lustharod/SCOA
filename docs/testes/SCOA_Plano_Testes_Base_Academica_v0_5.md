# Plano proposto para o SCOA v0.5

Preparado em 31/10/2026, antes da implementação. Base local conferida: 9c0f2e1, com código da v0.4 em b714404 e árvore de trabalho limpa. Recorte submetido ao autor: alunos, cursos e estrutura curricular, dependências do fluxo de matrícula e inscrição.

## Origem e limites

Referência: documentação aprovada v7.7, RF-MA-01, RF-MA-03 e RF-MA-05; UC-MA-01, UC-MA-03 e UC-MA-05; tabelas aluno, curso, estrutura_curricular e estrutura_disciplina do DDL v7.5.

O cadastro de alunos abrange identificação, consulta, atualização e inativação lógica. Preserva a conta opcional previamente vinculada, a identidade e todos os vínculos acadêmicos; não modifica matrículas ou inscrições ao inativar o aluno. A atualização cadastral não reativa registros. Nome de identificação não é uma chave única no DDL.

Cursos permitem criação, listagem, consulta e correção do nome, preservando a situação existente. Estruturas permitem criação para um curso ativo, consulta, correção da identificação e manutenção das disciplinas componentes. O curso de uma estrutura existente permanece fixo para preservar seus vínculos. Não se presume unicidade dos nomes, nem uma única estrutura por curso. A inclusão exige disciplina ativa e não repetida; a retirada preserva a disciplina. Como proteção inicial, estruturas já referenciadas por plano_estudo não terão composição alterada nesta entrega; versionamento curricular e migração de planos exigirão definição posterior.

Não serão criados dados acadêmicos fictícios para simular elegibilidade. Matrícula (RF-MA-09) depende da integração com a verificação de pendências da biblioteca, inclusive tratamento de indisponibilidade. Inscrição (RF-MA-10 e UC-MA-30) depende de matrícula ativa, regras de situação, janela de inscrição, vagas, pré-requisitos, resultados e equivalências. Essas operações permanecem pendentes. A autenticação e autorização também permanecem pendentes na aplicação de console.

## Casos e resultados esperados

| Caso | Condição e ação | Resultado esperado |
|---|---|---|
| P05-01 | Identificação de aluno, nome de curso e identificação curricular vazios, Unicode, espaços e limites do DDL | Normalização consistente; recusa de campos inválidos sem truncamento |
| P05-02 | Criar, listar, consultar e atualizar aluno | Recuperar dados; preservar ID, conta e situação |
| P05-03 | Inativar aluno e repetir; atualizar após inativação | Preservar aluno, vínculos e situação INATIVO |
| P05-04 | Criar, consultar e atualizar curso, inclusive sem mudanças | Recuperar dados e preservar situação e ID |
| P05-05 | Criar estrutura para curso existente e ativo; consultar e alterar identificação | Preservar curso, identidade e situação |
| P05-06 | Incluir, listar e retirar disciplina componente | Persistir relação correta sem excluir disciplina |
| P05-07 | Repetir inclusão ou retirar relação ausente | Recusar e manter composição anterior |
| P05-08 | Referências ausentes, inativas e identificadores inválidos | Informar erro sem gravação parcial |
| P05-09 | Alterar composição de estrutura vinculada a plano | Recusar sem modificar plano ou componentes |
| P05-10 | Operação própria e operação em transação externa; falha e rollback | Preservar atomicidade e controle da transação pelo chamador |
| P05-11 | Duas conexões incluindo o mesmo componente | Uma relação persistida, outra tentativa recusada |
| P05-12 | Console: confirmar criações, consultas, atualizações, inativação e componentes | Persistir somente operações confirmadas |
| P05-13 | Console: cancelar operações, informar campo ou ID inválido e encerrar entrada | Preservar dados, mostrar motivo e permitir saída sem repetição infinita |
| P05-14 | Reexecutar a bateria existente e os novos testes | Nenhuma regressão; registrar totais reais e XMLs |

## Execução e evidências

O schema do autor scoa_java_teste será preservado, sem recriar o DDL. Os testes do Codex devem usar ambiente isolado quando disponível; qualquer limitação será registrada. O autor executará a configuração Application SCOA — Alocação de testes e Maven SCOA — Testes MySQL após a preparação do módulo. Os números das opções e a sequência final serão registrados no guia de validação depois da implementação.

Aceitar PDF original do console, capturas dos resultados automatizados e CSVs das consultas, além dos XMLs JUnit. Não exigir uma imagem por caso. Registrar IDs dos dados sintéticos e relacionar casos às páginas ou registros correspondentes. Manter execucao_codex_v0_5 e execucao_autor_v0_5 separadas; não marcar a validação do autor como concluída antes de sua execução.

## Situação

Recorte aprovado pelo autor nesta conversa: alunos, cursos e estrutura curricular. Implementação e validação em preparação. Este plano foi registrado antes da codificação; os resultados serão documentados separadamente.
