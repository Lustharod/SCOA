# Validação Codex do incremento acadêmico v0.14

Em 25/11/2026, o incremento com portal web acadêmico local passou em **206 testes unitários e 434 testes de integração**, sem falhas, erros ou ignorados. O Maven exibiu BUILD SUCCESS às 16:56:19 UTC-03:00, com duração de 02:53 min. A rodada manual do autor será integrada ao final dos blocos; ainda não foi realizada na v0.14.

## Portal web e fronteiras acadêmicas

O portal local usa formulários HTML, conta existente, sessão e os mesmos repositórios protegidos do console. O catálogo contempla aluno, diário docente, secretaria, coordenação/gestão, documentos, relatórios, perfis, vínculos e regras por período. ADMIN técnico não recebe responsabilidade acadêmica por seu perfil. A operação é conferida antes de gravar; o servidor revalida acesso e regras atuais. Repetir a confirmação não repete a operação e não reexibe dados antigos de um vínculo depois retirado.

Vinte e um casos HTTP/JDBC transacionais verificam entrada e cabeçalhos, recusa de acesso anônimo, login/CSRF/origem, navegação por papel, rotas administrativas diretas, dupla confirmação sequencial/simultânea, texto escapado sem perder a origem, campos forjados, matrícula alheia, solicitação de documento com autoria, revogação de sessão/permissão, turma alheia, avaliação docente, cadastro com conflito de conferência, download dos bytes arquivados com revalidação, encerramento da sessão/conexão, calendário do aluno, diário completo sem gerar arquivo, senha com espaços preservada e ausência de dados anteriores na confirmação já processada após mudança de vínculo. Essas fixtures emprestam conexão transacional que é restaurada por caso; não são usadas como prova de isolamento entre conexões.

Dois casos adicionais HTTP/MySQL usam conexões realmente distintas, dados sintéticos confirmados e limpeza somente dos IDs criados: consultas simultâneas, logout de uma conta sem encerrar outra e inativação/reativação por outra conexão sem reutilizar o cookie antigo. Nove casos de apresentação verificam UTF-8 e senha sem exposição, limites/duplicações/codificação, campos tipados, escape HTML, cadastro sem ID inventado, catálogo por vínculos, links dos arquivos preservados, boletim sem nota/falta inventada e campos legados desconhecidos.

O diário em tela distingue lançamentos originais de resultado vigente; fechamento conferido mantém a leitura e gravação na mesma transação. Transferência interna revalida origem e inscrições da conferência antes de efetivar. O portal não aplica migrações nem usa scoa: exige explicitamente scoa_java_teste e escuta somente em 127.0.0.1. Não se presume implantação institucional ou validação de todos os requisitos de disponibilidade, desempenho, backup e todos os navegadores.

O primeiro incremento do portal passou com 204 unitários e 430 integrações. A revisão acrescentou consultas, apresentação do boletim e verificações de senha, legado e confirmação já processada; os originais das rodadas intermediárias permanecem privados. As amostras visuais selecionadas são sintéticas e identificadas, sem atribuição a capturas manuais do autor.

## Regressão e rastreabilidade

Todos os testes anteriores de acesso, cadastros, matrícula, inscrição, reconhecimento, planejamento, diário, transferências internas, documentos e atendimento permanecem na bateria. [Resultados por classe](RESULTADOS.md).

As contagens de versões sucessivas não devem ser somadas como casos distintos. O incremento anterior publicado em ae13c5d2fbdec04b1c1b1aedf3b3c1c2ece220c8 tinha 197 unitários e 411 integrações. Os originais de cada execução, inclusive tentativas que falharam, permanecem privados e identificados separadamente.

Instância MySQL 8.0.46 nova, scoa_java_teste, porta local 33386; DDL de testes v7.5 e V006 a V024. Nenhuma migração de scoa. As fixtures SQL são preparação/restauração dos testes, sem atribuição a atos auditados do console. Log completo, XML/TXT e manifesto permanecem privados. Os [hashes das fontes](FONTES_SHA256.json) foram conferidos após a execução. [Ambiente](ambiente.txt).

Este ponto amplia o ramo desenvolvimento/academico-completo. Main permanece na v0.13 fechada. A v0.14 e o módulo acadêmico continuam em desenvolvimento. Rodada manual integrada do autor, revisão final da matriz e documentação Word ainda pendentes; a v0.14 não está fechada. [Regras e limites](../../../SCOA_Implementacao_v0_14.md).
