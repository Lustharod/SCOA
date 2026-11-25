# Portal acadêmico local — v0.14 em aceitação

O portal disponibiliza os percursos acadêmicos no navegador, utilizando a mesma conta, os mesmos vínculos e as mesmas regras do console. A rodada manual integrada do autor ainda é necessária para o fechamento da v0.14.

## Como executar

Use Java 25, as dependências do `pom.xml` e uma instância preparada exclusivamente de `scoa_java_teste`, com o DDL de testes e V006 a V024. O portal não cria a base nem aplica migrações. A preparação e as credenciais do ambiente do autor permanecem privadas.

Na configuração de execução do IntelliJ, informe as variáveis locais:

| Variável | Valor/configuração |
| --- | --- |
| `SCOA_DB_URL` | URL JDBC explícita de `scoa_java_teste`; por exemplo, `jdbc:mysql://127.0.0.1:33386/scoa_java_teste`. |
| `SCOA_DB_USUARIO` | Usuário local da instância de teste. |
| `SCOA_DB_SENHA` | Senha local, mantida fora do repositório e das capturas. |
| `SCOA_HTTP_PORT` | Opcional; padrão `8080`. Use outra porta livre se necessário. |

Execute `br.edu.cefetmg.scoa.web.PortalAcademico`, ou `br.edu.cefetmg.scoa.ScoaAplicacao` com o argumento `--web`. Abra `http://127.0.0.1:8080/`, ajustando a porta se configurada. A execução aceita apenas `scoa_java_teste` e escuta somente na interface local. Encerre a execução para parar o portal; encerre a instância MySQL somente depois de guardar as evidências.

Entre com a conta já provisionada. A inicialização administrativa permanece no procedimento privado de preparação/console. O portal não possui cadastro administrativo anônimo. Criar conta pela administração não solicita papel acadêmico: os vínculos com aluno, professor, curso coordenado ou gestão são explícitos e as concessões técnicas são separadas.

## Áreas e referências

| Área | Percursos |
| --- | --- |
| Aluno | Calendário, matrículas, ofertas/elegibilidade, inscrição e renovação próprias; horários, boletim, resultados, histórico e plano; cancelamento, documentos e manifestações. |
| Professor | Turmas/inscrições próprias; aulas por tempos, pauta/conteúdo, P1/P2/PF, notas, chamadas, importação simulada, diário completo, fechamento e proposta de retificação. |
| Secretaria | Alunos, professores, cursos, disciplinas/requisitos, currículos, períodos/turmas, salas/horários; matrícula, inscrição, equivalência, plano versionado, lote, transferências, documentos e processos legados. |
| Coordenação/gestão | Cursos autorizados, docentes, metas/prazos, avaliações, diário/auditoria e trajetória discente. Decisões de cancelamento e retificação continuam exigindo coordenação do curso. |
| Administração técnica | Contas, vínculos, perfis e concessões; atividade da conta; versões de regras e seleção para períodos futuros. ADMIN não assume responsabilidade docente ou acadêmica por seu perfil. |
| Relatórios | Demonstrativos Censo/ENADE autorizados, diário docente/gestão, histórico/plano próprios, arquivos PDF/XLSX preservados. |

O ID da conta identifica o acesso da pessoa. A referência de aluno/professor identifica o cadastro acadêmico; a de curso identifica o curso coordenado. Matrícula institucional identifica o vínculo aluno–curso. Inscrição identifica a participação na turma. Avaliação, aula, horário, perfil e protocolo têm suas próprias referências. As consultas correspondentes exibem os IDs antes do preenchimento dos formulários.

A mesma pessoa pode reunir várias áreas legítimas. O portal exibe as atividades dos vínculos/concessões atuais e o serviço confere novamente o alcance de cada operação. Alterar a URL ou enviar outro ID não autoriza acesso alheio.

## Consulta, conferência e gravação

Formulários de consulta não alteram os dados. Nos demais, o primeiro envio prepara uma conferência; a gravação acontece em **Confirmar operação**. Versões e assinaturas de origem são revalidadas onde aplicáveis. Dados alterados desde a conferência exigem nova preparação.

Repetir a confirmação não repete a operação. A resposta remete à consulta atual, sem reutilizar conteúdo de um vínculo depois retirado. Se a resposta da gravação for interrompida, consulte os registros antes de preparar outra operação. Eventos simulados têm resultado por linha; repetir a chave do mesmo evento não duplica frequência.

Em cadastro básico, referência vazia significa criar registro; uma referência existente significa atualizar preservando seu ID. Campos de versão da nota/chamada usam `-1` no primeiro lançamento; correções usam a versão exibida no boletim. Pauta/conteúdo e tempos de aula podem ser registrados em grupo. Sessenta horas correspondem a 72 tempos de 50 minutos; o campo de carga continua em horas de 60 minutos.

O boletim distingue ausência de lançamento, pendências e apuração dos registros originais. Resultado vigente após retificação é consultado separadamente. A consulta de diário completo não cria exportação. Solicitação de documento e atendimento da secretaria não equivalem à emissão de um documento oficial assinado.

## Relatórios e frequência simulada

A conferência do relatório apresenta o mesmo retrato que será arquivado em PDF/XLSX. Após gerar, os links baixam os bytes preservados, com nova verificação de acesso e integridade. **Minhas exportações arquivadas** permite recuperar o arquivo sem gerar outro. Uma alteração posterior na origem não reescreve o arquivo anterior.

Os demonstrativos MEC seguem o escopo aprovado para o protótipo: sem formato oficial fornecido, classificação presumida de concluinte/aptidão ou envio ao MEC. Histórico e plano próprios não substituem documento oficial solicitado à secretaria.

O [contrato de frequência simulada](SCOA_Contrato_Frequencia_v0_14.md) descreve campos, horário local, deduplicação e recusas. O equipamento é fictício; não se declara conexão com fornecedor real.

## Validação e limites

Os testes HTTP usam as regras JDBC reais; há também testes com conexões MySQL distintas para sessões simultâneas e revogação por outra conexão. [Resultados e ambiente](testes/publicados/execucao_codex_v0_14/RELATORIO.md) e [matriz acadêmica](SCOA_Matriz_Academica_v0_14.md).

O portal usa HTML e CSS sem dependência de scripts/CDNs externos. As amostras de renderização selecionadas são sintéticas e identificadas no [manifesto visual](testes/publicados/execucao_codex_v0_14/AMOSTRAS.md). Conferência com Edge não é registrada como teste de Chrome ou Firefox; a compatibilidade manual nesses navegadores permanece pendente.

Sessão: até duas horas, com expiração após 30 minutos sem uso. Sair fecha sua conexão. Inativação/revogação bloqueia o acesso seguinte; reativar exige novo login. As confirmações pendentes expiram em dez minutos. Dados, senhas, configuração e evidências brutas permanecem privados. O portal local não constitui implantação institucional nem comprovação de disponibilidade, carga máxima, prazo de resposta ou backup institucional.
