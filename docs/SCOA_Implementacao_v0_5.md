# Alunos cursos e estrutura curricular do SCOA v0.5

A versão 0.5.0-SNAPSHOT acrescenta o cadastro básico de alunos e cursos e a manutenção das estruturas curriculares e de seus componentes, na opção 13 do menu principal. O recorte foi aprovado pelo autor antes da implementação e avança RF-MA-01, RF-MA-03 e RF-MA-05 / UC-MA-01, UC-MA-03 e UC-MA-05, conforme a documentação v7.7. Não conclui as informações acadêmicas agregadas desses casos de uso: matrícula, histórico consultável, análise curricular e plano de estudo permanecem pendentes.

## Organização e uso

Aluno, Curso e EstruturaCurricular representam os campos do DDL v7.5 e validam identificadores, textos obrigatórios e limites por pontos de código Unicode. RepositorioBaseAcademica define o contrato; ServicoBaseAcademica conduz as operações e preserva identidade e situação; RepositorioBaseAcademicaJdbc persiste com consultas parametrizadas e TransacaoJdbc; ConsoleBaseAcademica apresenta o submenu com confirmação e cancelamento de cada gravação. O fim da entrada encerra o submenu sem confirmar uma operação incompleta.

Alunos permitem criação, listagem, consulta, correção de identificação e inativação lógica. A conta opcional previamente vinculada é preservada. Inativar não exclui o aluno, não altera matrículas nem inscrições e não apaga histórico. Atualizar a identificação não reativa. A futura elegibilidade deverá verificar também a situação do aluno, sem inferir que uma matrícula isoladamente torne o aluno apto.

Cursos permitem criação, listagem, consulta e correção de nome. Identidade e situação existentes são preservadas. A entrega não acrescenta inativação de curso ou gestão de contas. Nomes e identificações não receberam restrições de unicidade ausentes no modelo físico.

Estruturas são criadas para cursos ativos e podem ser consultadas por ID ou listadas por curso. A correção de identificação preserva curso e situação. A composição permite inclusão de disciplinas ativas, consulta e retirada; não exclui a disciplina e não modifica seu grafo de pré-requisitos. A montagem inicial pode permanecer vazia e não significa que uma análise curricular esteja completa. Não se presume uma única estrutura por curso, nem são inventados semestre, carga mínima ou ordem de integralização ausentes nessas tabelas.

## Consistência e proteção do histórico

Alterações bloqueiam o registro antes de ler o estado e gravar, evitando a perda de atualizações. A composição bloqueia primeiro a estrutura, depois o curso, consulta vínculos de plano com leitura atual e, na inclusão, bloqueia a disciplina. Repetição de componente é recusada e a chave composta do DDL permanece como proteção adicional. Duas inclusões concorrentes na mesma estrutura são serializadas e revalidadas. Escritas externas ou futuras devem respeitar o protocolo.

Como restrição inicial da aplicação, estruturas vinculadas a qualquer plano_estudo não permitem alterar componentes, inclusive se o plano não estiver ativo. A correção da identificação continua permitida. Uma nova composição exige outra estrutura; o versionamento curricular e a migração de planos não foram implementados. Futuras criações ou mudanças de vínculo de plano devem bloquear a estrutura antes de referenciá-la. Esta proteção não transforma o cadastro em um motor completo de análise curricular.

Operações próprias confirmam ou desfazem integralmente a transação e restauram autocommit. Dentro de transação externa, os savepoints preservam o controle do chamador. Erros não são informados como sucesso. Contenção e deadlock continuam sujeitos ao tratamento já adotado: consultar os dados e repetir a operação quando necessário.

## Planejamento e resultados

O plano P05 contém 14 casos registrados antes da codificação. A verificação do Codex terminou em 31/10/2026 às 22:42:50 UTC−03:00: 74 testes unitários e 66 de integração, sem falhas, erros ou ignorados, BUILD SUCCESS. São 26 novas execuções (9 unitárias e 17 de integração); as 114 execuções da v0.4 estão incluídas nos totais. O ambiente foi uma instância MySQL 8.0.46 isolada da instalação de uso do autor, com schema scoa_java_teste e DDL v7.5.

O relatório em testes/publicados/execucao_codex_v0_5 identifica as classes e métodos, a correspondência com os casos e os hashes das fontes. Os XMLs e o console completo são preservados privadamente em testes/evidencias/execucao_codex_v0_5; podem conter caminhos e propriedades locais. A validação do autor foi concluída separadamente: 74 testes unitários e 66 de integração aprovados em 31/10/2026 às 22:52:45 UTC−03:00, com 11 XMLs originais, PDF automatizado de duas páginas, PDF manual de 12 páginas, três capturas e seis CSVs. O relatório em testes/publicados/execucao_autor_v0_5 registra as páginas, os IDs e os limites. O código validado corresponde retrospectivamente ao commit dc1a6f3, sem alteração das 53 fontes/Maven/DDL conferidas neste fechamento.

O DDL, suas 71 tabelas e os diagramas permanecem inalterados. A revisão Word v7.8 amplia o capítulo 9 com o incremento e a validação do autor; a v7.7 permanece preservada. Autenticação, autorização, interface web, matrícula, inscrição, resultados, equivalências, planos e integração com biblioteca permanecem pendentes.
