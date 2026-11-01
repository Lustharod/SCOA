# Matrícula institucional na v0.6

O incremento implementa parte de RF-MA-09 / UC-MA-09 e a consulta UC-MB-10 necessária a RN-MA-05. O menu principal 14 permite criar, consultar, listar por aluno, corrigir ingresso, trancar, retornar, cancelar e consultar histórico e situação da biblioteca. Mantém os menus 11, 12 e 13.

## Responsabilidades

Matricula valida identidade, limites DATE, motivos e transições. ServicoMatricula expõe as operações de aplicação. RepositorioMatricula define o contrato; RepositorioMatriculaJdbc coordena referências, duplicidade, consulta bibliotecária e histórico na mesma transação. EventoMatricula representa os eventos persistidos; SituacaoBiblioteca distingue REGULAR, IMPEDITIVA e INDISPONIVEL. ConsoleMatricula apresenta resumos e exige confirmação em cada escrita. ApoioJdbc verifica a presença da migração antes da integração.

## Regras e limites

ATIVO pode ir para TRANCADO ou CANCELADO; TRANCADO pode voltar para ATIVO ou ir para CANCELADO. CANCELADO e CONCLUIDO não são reabertos. Não há comando para concluir curso: análise curricular ainda não implementada. As grafias seguem o padrão persistido existente, independentemente do gênero usado na interface.

Aluno e curso ativos são necessários ao ingresso e retorno. ATIVO, TRANCADO ou situação desconhecida do mesmo aluno/curso impedem novo vínculo. Encerramento permite outra matrícula com outro ID, preservando a anterior; não concede créditos. Uma correção não troca aluno, curso ou situação. Data e período são dados distintos; matrícula administrativa pode anteceder as aulas. Correção exige período existente e ativo, inclusive para preencher período ausente de registro legado. Período de ingresso não distribui disciplinas automaticamente. Seu encerramento posterior não é motivo para recusar retorno de trancamento.

Conta vinculada e usuário bibliotecário ativo permitem resolver a identidade. Sua ausência não significa regularidade. Neste contrato, pendência ATIVO com impeditiva=1 bloqueia; ATIVO não impeditiva, REGULARIZADO e LIBERADO não bloqueiam. Situação desconhecida torna a consulta indisponível. Falha SQL interrompe a operação; não é convertida em resposta regular. Biblioteca não recebe comando de matrícula e não há liberação manual. Leitura e correção não dependem de regularidade.

Sem autenticação/autorização nesta aplicação de console, não se atribui um autor fictício ao histórico. Datas dos eventos vêm do servidor, motivos e valores anteriores/novos são persistidos. Cancelar/trancar não altera inscrições ou planos existentes; esses fluxos continuam próprios e seus futuros validadores devem considerar a situação da matrícula. A consulta de biblioteca é uma integração de leitura, não a implementação dos cadastros e processos de empréstimo/devolução.

## Persistência e concorrência

O DDL original v7.5 e os diagramas permanecem preservados como base histórica. A migração V006 acrescenta id_periodo_ingresso anulável para preservar matrículas antigas e a tabela historico_matricula. A estrutura resultante possui 72 tabelas. A aplicação exige período em novas escritas de ingresso. Não cria eventos retroativos nem preenche dados legados por inferência. A migração deve ser aplicada uma vez e não integra automaticamente a inicialização da aplicação.

A trava de aluno serializa inscrições institucionais e concorre com a inativação do cadastro. Curso e período são revalidados sob bloqueio. A manutenção de período agora preserva datas/situação se houver matrícula, além da proteção anterior por turmas. Há preservação de transação externa por savepoint; matrícula e evento são confirmados ou desfeitos juntos. A regra de duplicidade é protegida pelo protocolo da aplicação, não por uma restrição UNIQUE que impediria reingressos.

Futuros escritores da biblioteca devem bloquear usuario_biblioteca antes de inserir/alterar empréstimos ou pendências. A leitura corrente FOR UPDATE aguarda esse bloqueio e observa mudanças confirmadas. Escritas SQL externas que ignorem esse protocolo não possuem garantia de serialização com a matrícula. A classificação institucional continua sendo o campo impeditiva, sem inventar multas ou impedimentos por valor. Situações bibliotecárias futuras devem estender explicitamente o contrato e seus testes.

## Verificação

Plano P06 registrado antes da implementação, com quinze grupos de casos. MatriculaTest verifica limites e matriz de transições; MatriculaJdbcIT cobre persistência, biblioteca, reingresso, histórico, falhas, transações, concorrência, período legado e console. As regressões anteriores permanecem na bateria. Resultados do Codex são separados da execução futura do autor. O Word v7.8 não foi alterado neste preparo.


## Fechamento da validação do autor

Rodrigo Lustosa concluiu verify -Pmysql-it em 01/11/2026 às 15:04:41 UTC−03:00, com 77 testes unitários e 95 de integração aprovados. O PDF automatizado tem três páginas. A validação manual está preservada em duas partes de seis e oito páginas, com sete CSVs (preparação e seis consultas finais). Matrícula 32 cancelada e matrícula 33 ativa preservam aluno 79, curso 79 e período 211; eventos 35–41 correspondem às sete escritas. Alunos 80 e 81 não receberam matrícula.

O erro 1175 do SQL auxiliar foi corrigido com atualização pela chave id_pendencia e conferência sob SQL_SAFE_UPDATES=1. Os originais conservam duas falhas de consulta e a retomada após ROLLBACK orientado no Workbench e reinício da aplicação. Os PDFs não expõem a causa SQL interna; não se afirma um timeout comprovado. A aplicação não reconecta automaticamente e apresenta orientação genérica nessas falhas. O relatório do autor descreve a limitação e separa os cenários manuais da cobertura automatizada.

O código validado corresponde retrospectivamente a 87fb486ea15fefa0c1f2ddc796472ddc9a169f55. As 64 fontes/Maven/DDL da preparação permanecem idênticas; não foi repetida a bateria durante o fechamento documental. A revisão Word v7.9 incorpora a seção 9.11 e os Quadros 37 e 38, preservando a v7.8. A seleção pública em testes/publicados/execucao_autor_v0_6 reúne originais revisados, resumos e manifestos; XMLs brutos e guias permanecem privados. A validação da v0.6 encerra somente este recorte.
