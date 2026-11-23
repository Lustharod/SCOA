# Contas e vínculos acadêmicos na v0.13

Em 23/11/2026, a correção OC-V12-01 foi implementada e verificada automaticamente. A validação manual do autor permanece pendente; esta nota não encerra a iteração nem declara o módulo acadêmico concluído. A base é a v0.12 publicada em `57255c89d50dfb30d6b2089bd84adff55163a67c`.

## Comportamento

O menu 18.4 cria uma conta de acesso por pessoa. O acesso técnico inicial pode ser SEM_PERMISSOES (padrão), ADMIN, SOLICITANTE, ANALISTA ou BIBLIOTECARIO. Os três nomes acadêmicos deixaram de ser opções independentes de criação. SEM_PERMISSOES permite autenticar e não cria perfil vazio ou concessão técnica.

Aluno, professor e coordenação são identificados pelos vínculos de `aluno.id_usuario`, `professor.id_usuario` e `coordenacao_curso`. A mesma conta pode ter um cadastro de aluno, um de professor e coordenar mais de um curso. Permanecem as recusas de tomada de cadastro de outra conta e de associação de dois cadastros do mesmo tipo à mesma conta. A conta existente deve ser reutilizada ao acrescentar outro vínculo.

O menu 18.11 exige SCOA_ACESSO_ADMIN e mostra a conta de destino antes do vínculo. O campo seguinte identifica especificamente `aluno.id_aluno`, `professor.id_professor` ou `curso.id_curso`. Antes da confirmação, o console mostra o ID da conta, o tipo e a referência acadêmica. Estabelecer um vínculo não cria conta nem concede permissões técnicas. Repetir o mesmo vínculo não duplica o evento.

O menu 18.12 consulta a própria conta autenticada. O menu 18.13 exige permissão administrativa para consultar outra conta, inclusive a situação de uma conta inativa. As consultas apresentam referências acadêmicas e suas situações, além dos códigos de permissões técnicas concedidas por perfis ativos. Uma concessão listada não habilita conta inativa. Não são apresentados hash de senha ou conteúdo de credenciais. Os vínculos são relidos a cada consulta, sem serem armazenados na sessão.

Os nomes de perfis antigos não são usados como fonte de identidade acadêmica. Um perfil legado chamado SCOA ALUNO não transforma uma pessoa em aluno na ausência do vínculo correspondente. Perfis existentes, IDs, credenciais, permissões e eventos anteriores são preservados. Nenhuma limpeza, renomeação ou migração de dados é executada.

## Verificação

Regressão Codex em instância descartável de `scoa_java_teste`, com DDL v7.5 e V006 a V012: **130 unitários e 194 de integração**, sem falhas, erros ou ignorados. `mvn -o -B verify -Pmysql-it` terminou com BUILD SUCCESS em 23/11/2026 às 12:13:22 UTC-03:00, duração 45.036 s. São dois testes unitários e quinze execuções de integração a mais que a base v0.12. Consulte o [relatório](testes/publicados/execucao_codex_v0_13/RELATORIO.md) e o [ambiente](testes/publicados/execucao_codex_v0_13/ambiente.txt).

A cobertura nova inclui conta sem vínculo, rejeição de papel acadêmico independente, vários vínculos na mesma conta, concessões técnicas preservadas, perfis legados divergentes, referência pertencente a outra conta, limite por tipo de cadastro, repetição idempotente, autorização da consulta, revogação de permissão, conta inativa, sessão encerrada ou de outra conexão, rollback por falha de auditoria e distinção dos IDs no console. As demais integrações da v0.12 foram executadas junto desta rodada. Isso não substitui a validação manual do autor.

## Limites

Não foi criada uma entidade de pessoa ou identificação civil. A aplicação mantém login único e as restrições dos vínculos; conferir que aluno e professor pertencem à mesma pessoa continua sendo responsabilidade de quem provisiona a conta. Ela não deduz identidade por igualdade de nome e não funde cadastros ou contas existentes automaticamente.

Esta entrega não altera a autorização dos menus legados nem o primeiro lançamento de resultados. Diário, avaliações, frequência por aula, transferências, integralização, histórico escolar consolidado do próprio aluno, web e demais módulos continuam com as pendências da matriz acadêmica. O próximo recorte proposto está no [plano de entrega](SCOA_Plano_Entrega_2026_11_23.md).

Não há V013 de banco: a estrutura permanece com 87 tabelas. Não houve migração de `scoa`.
