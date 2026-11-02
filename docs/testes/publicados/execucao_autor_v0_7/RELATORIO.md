# Validação do autor do SCOA v0.7

Rodrigo Lustosa concluiu Maven verify -Pmysql-it em **01/11/2026 às 21:07:52 UTC−03:00**, com **97 execuções unitárias e 109 de integração**, sem falhas, erros ou ignorados, **BUILD SUCCESS**. A duração exibida foi 01:16 min. Os 15 XMLs JUnit e failsafe-summary.xml confirmam as contagens. São 206 execuções, incluindo as 172 regressões da v0.6 e 34 novas. Os métodos e parametrizações coincidem com os da execução isolada do Codex, sem somar as duas execuções como casos distintos.

O código preparado sobre 7357409 foi conferido com o manifesto de entrega da v0.7, sem divergências nos arquivos nele relacionados. O autor também compilou o exemplo local ExemploSala: 54 fontes principais, enquanto a cópia do Codex compilou 53. O exemplo não está no manifesto anterior nem acrescenta testes; foi preservado e sua identificação consta do manifesto atual, sem incorporá-lo automaticamente ao código público. Este fechamento não alterou Java, testes, Maven ou DDL.

## Evidências e limites de horário

O PDF automatizado tem três páginas, emitido às **21:13:39 UTC−03:00**; o manual tem três páginas, emitido às **21:59:57 UTC−03:00**, ambos em 01/11/2026. Emissão não é início ou fim exato do roteiro. São quatro CSVs: preparação e três consultas finais. O vínculo curricular registra no servidor **21:36:56.441185**; as demais linhas não fornecem horário completo das ações ou exportações.

Foram preservados **37 originais**: dois PDFs, quatro CSVs, quinze XMLs JUnit, quinze TXT e failsafe-summary.xml. XMLs brutos e propriedades locais permanecem privados. O relatório é derivado dos originais; não representa nova execução.

| Comportamento | Evidência manual |
|---|---|
| Menu 15 e vínculo matrícula 72 / estrutura 79 | PDF manual, p. 1 |
| Desistência e consulta sem inscrição | p. 1 |
| Confirmação da inscrição 20 e listagem | p. 1; p. 2; CSV de inscrição |
| Duplicidade e falta de vaga | p. 1 |
| Consulta da alocação 122, turma 231 | p. 2 |
| Tentativas de remoção com IDs 20, 231 e 79 inexistentes | p. 2; não comprovam proteção da alocação 122 |
| Tentativa de resultado para inscrição inexistente 231 | pp. 2–3 |
| Recusa de resultado na inscrição 20 por período ainda não encerrado | p. 3 |

A retirada do horário existente pelo ID 122 não foi tentada no PDF fornecido. A proteção é coberta pelo método automatizado conflitoDeHorarioEProtecaoDasAlocacoes, aprovado no XML do autor, e não é atribuída à cobertura manual. A saída normal pelo menu 0 também não foi localizada no final do PDF. Não se exige nova captura para substituir cobertura automatizada já existente.

O resumo Resultado[...APROVADO] apresentado antes da confirmação é uma simulação do cálculo; a escrita da tentativa correta foi recusada. Não houve aprovação persistida demonstrada pelo manual. Fórmulas, PF, frequência exata, fechamento em período encerrado, pré-requisitos, rollback, concorrência e estados desconhecidos têm cobertura automatizada específica.

O PDF manual possui cortes à direita em linhas longas, mas IDs, entradas e mensagens decisivas permanecem legíveis. O PDF automatizado conserva problemas de representação de acentos do console IBM850; totais, BUILD SUCCESS e horário são legíveis. Não foram reconstruídos ou corrigidos os originais.

## Estado final conferido

Etiqueta **V07MAN-67713a9e-d65**: aluno 149, curso 147, disciplina 411, período 299, turma 231, horário 122, estrutura 79 e matrícula 72. O menu 7 registra sala 261 e segunda-feira das 08:00 às 09:00.

| Registro | Valor real |
|---|---|
| Vínculo curricular | Matrícula 72 / estrutura 79; registrado em 01/11/2026 21:36:56.441185 |
| Inscrição | 20; matrícula 72; turma 231; data 01/11/2026; ATIVO |
| Capacidade e ocupação | Turma 231; capacidade 1; ocupadas 1 |
| Janela apresentada | 28/10/2026 a 03/11/2026 inclusive |

Os nomes dos CSVs “inscricao disciplina 29” e “vinculo curricular matricula 28” não correspondem aos IDs das linhas. Os nomes e bytes originais foram preservados; prevalecem os IDs 20 e 72 contidos nos arquivos. A preparação cria uma matrícula sintética diretamente em SQL: isso não constitui execução de UC-MA-09 nem fabricação de regularidade bibliotecária. Os resultados não comprovam restauração de backup ou migração do banco scoa; não houve migração do servidor do autor pelo Codex neste fechamento.

## Resultados por classe

| Classe | Execuções |
|---|---:|
| ServicoSalaTest | 9 |
| AlocacaoTest | 15 |
| BaseAcademicaTest | 9 |
| CadastroAcademicoTest | 9 |
| InscricaoTest | 20 |
| MatriculaTest | 3 |
| ProfessoresDisciplinasTest | 9 |
| SalaTest | 23 |
| AlocacaoJdbcIT | 17 |
| BaseAcademicaJdbcIT | 17 |
| CadastroAcademicoJdbcIT | 12 |
| InscricaoJdbcIT | 14 |
| MatriculaJdbcIT | 29 |
| ProfessoresDisciplinasJdbcIT | 14 |
| SalaJdbcIT | 6 |

## Escopo encerrado

A v0.7 fica validada para inscrição com contexto curricular explícito, janela, situação acadêmica, pré-requisitos por resultados finais, vagas e horários, e para primeiro fechamento de resultado segundo as regras do projeto. Equivalências sem integração formal continuam impedindo os casos que delas dependem. Plano de estudo, gestão de equivalências, retificação, autenticação/autorização, interface web e recuperação de conexão permanecem pendentes.

Os originais ficam em docs/testes/evidencias/execucao_autor_v0_7. A seleção pública contém este relatório, ambiente, manifestos e cópias byte a byte dos PDFs/CSVs revisados. A execução isolada do Codex permanece separada. A revisão acadêmica v7.10 acrescenta a seção 9.12 e os Quadros 39/40, preservando a v7.9. Os estilos são idênticos byte a byte; texto e formatação dos capítulos 1 a 8 e texto do apêndice foram conferidos, com as mesmas 151 imagens/desenhos. Commit posterior identifica retrospectivamente o conjunto conferido, sem alterar os horários das execuções.


Código validado identificado retrospectivamente pelo commit **fde76dbe28dfb81634b6ec06a0bb3499b204ab35**, criado após os testes. Esse registro não altera datas das execuções.


Word privado v7.10: **491 páginas**. SHA-256: 4249796ef3b1ffc2c1cbf2c7ced3ede825a06b04ded92e8623b90725a45fd425. A v7.9 permanece preservada.
