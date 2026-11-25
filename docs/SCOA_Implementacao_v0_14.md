# Atualização acadêmica v0.14 — em desenvolvimento

Base: v0.13 fechada em `f5107209c26aa80ca83ef7cf9b3f3477c5e1d899`. Esta atualização amplia somente o Módulo Acadêmico. Ainda não representa o módulo completo nem o fechamento da v0.14; a validação manual do autor e os demais blocos continuam pendentes.

## Primeiro incremento implementado

- Fronteira autenticada dos serviços usados pelos menus legados de salas, alocação, cadastros, matrícula, inscrição, equivalência e planejamento. As operações verificam sessão, atividade e permissão no acesso aos dados; a auditoria das novas manutenções integra a transação.
- Permissões técnicas acadêmicas de consulta e manutenção separadas. ADMIN técnico pode executar esses cadastros; vínculo de aluno, docente ou coordenação não concede essas permissões. Nenhuma concessão existente foi alterada ou criada automaticamente. A administração completa das concessões ainda é trabalho pendente.
- Primeiro fechamento de resultado exige o professor ativo responsável pela turma. ADMIN, aluno, coordenação ou outro professor não substituem esse vínculo. São preservados P1/P2/PF, minutos apurados, resultado único e retificação posterior. Turmas que usam o diário do menu 24 encerram a partir de seus lançamentos; o fechamento antigo com totais independentes fica bloqueado para elas.
- Menu 21: histórico escolar do próprio aluno e registro/consulta de sugestões e reclamações. O histórico reúne matrículas, períodos, disciplinas, resultados, reconhecimentos e fontes legadas, sem presumir notas ausentes ou promover legado a reconhecimento. Revisão, revogação, contexto e datas limitam a indicação de vigência. Trata-se de consulta, não documento oficial.
- Manifestações identificam aluno, conta autora e instante. O conteúdo fica no registro; a auditoria registra ação e referência, sem duplicar o texto privado. A V013 mantém autoria desconhecida nas manifestações antigas.
- Grade de 50 minutos e tolerância de atraso definidas pelo autor: seis tempos de manhã, seis à tarde e cinco à noite; intervalos não letivos. Presença integral até 15 minutos após o início, inclusive; depois, falta naquele tempo. Ausência de informação continua distinta de falta. A regra está conectada ao diário e à importação de eventos simulados no menu 24.

## Transferência interna

O menu 22 efetiva a mudança de curso em uma transação: preserva a matrícula de origem como TRANSFERIDO, abre a matrícula de destino com a mesma pessoa/conta, vincula o currículo escolhido, cancela inscrições ainda ativas e registra processo, histórico e autoria. A biblioteca é consultada antes da criação do destino; uma falha desfaz o conjunto.

A transferência está limitada a D+45 e ao fim do período. Cancelamentos por transferência até D+13 liberam vaga; a partir de D+14 mantêm vaga retida, conforme a regra vigente. Pedidos de cancelamento pendentes ou inscrições ativas incoerentes exigem tratamento anterior. Repetir o mesmo pedido retorna o protocolo existente, sem duplicar vínculo ou reconhecimento; uma chamada autorizada continua sujeita à auditoria técnica.

Foi aprovada pelo autor a regra de aproveitar automaticamente disciplinas idênticas. A implementação usa identidade pelo mesmo `id_disciplina`, aprovação formal APROVADO na origem e período encerrado, com componente ativo na estrutura de destino. Igualdade de nome ou código não basta. Um pedido formal identificado como automático e uma ligação ao resultado original preservam fundamento e instantâneo; não se trata de análise manual inventada. Equivalências entre disciplinas distintas mantêm seu fluxo existente. Uma retificação que derrube resultado usado nessa transferência exige análise de impacto.

A V014 preserva transferências legadas sem reconstruir destino/autor, acrescenta os campos do novo fluxo e a ligação de aproveitamento. A origem permanece consultável, inclusive quando transferida. Transferências externas possuem processo próprio descrito abaixo; outras situações institucionais não são presumidas.

## Transferências externas

O menu 28 registra entrada ou saída com referência única, aluno/curso ou matrícula de origem, período, instituição externa, documentos e fundamento. A saída obtém pessoa e curso da matrícula, sem pedir IDs independentes incompatíveis. O registro abre EM_ANALISE; acompanhamentos preservam documentos anteriores, observação, autor e instante UTC. A secretaria confirma explicitamente EFETIVADA, INDEFERIDA ou CANCELADA, usando a versão consultada. Estados terminais não são reabertos.

Na entrada efetivada, o sistema abre matrícula e vínculo curricular no destino para o aluno já cadastrado, preservando sua conta e vínculos legítimos. Não cria credenciais nem aproveita disciplinas externas por nome/código; o reconhecimento depende da equivalência formal existente. Na saída, a matrícula de origem fica TRANSFERIDO, inscrições ativas são canceladas e a regra de vagas D+13/D+14 é mantida. Resultados anteriores e histórico continuam consultáveis; não é aberta uma matrícula fictícia para outra instituição dentro do SCOA.

A efetivação exige período ativo, até D+45 e sem ultrapassar o fim do período; inscrições ativas também são conferidas. A saída bloqueia até regularizar a biblioteca, como definiu o autor, e mantém a pendência preservada. Biblioteca ausente/inválida, contexto alterado, destino já ativo ou cancelamento pendente impedem efeitos sem concluir o processo. Registro e consulta não efetuam transferência. Não há comunicação com instituição externa.

A V020 mantém os registros legados e acrescenta processo e andamento externos. Repetir a mesma referência e pedido retorna o protocolo; uma colisão é recusada. Repetir a mesma decisão não duplica matrícula, transferência ou histórico. Processo, efeitos acadêmicos, andamento e auditoria são transacionais; uma falha desfaz o conjunto. São 99 tabelas previstas após V020.

## Documentos acadêmicos

O menu 23 permite solicitar declaração de matrícula ou histórico escolar para uma matrícula pertencente à pessoa autenticada, consultar seus protocolos e acompanhar o atendimento. A secretaria consulta a fila e registra andamento, parecer visível ao solicitante e referência do documento entregue. O fluxo admite SOLICITADO, EM_ATENDIMENTO, ATENDIDO e INDEFERIDO; atendimento concluído exige referência de entrega. A geração do arquivo e sua assinatura oficial não estão implementadas neste incremento.

O atendimento e a consulta da fila exigem permissões técnicas próprias (`SCOA_DOCUMENTOS_ATENDER` e `SCOA_DOCUMENTOS_CONSULTAR`), além da conta ativa; ADMIN é autorizado. Aluno, professor, coordenador e as permissões genéricas de cadastro não recebem acesso à fila por esse motivo. Cada alteração revalida a concessão e a versão consultada; uma tela antiga não sobrescreve o andamento de outro operador. Solicitação, histórico e auditoria são gravados juntos, e a auditoria não duplica parecer ou referência privada.

A V015 acrescenta autoria, instantes UTC, versão e histórico de andamento. Solicitações legadas mantêm seus tipos e situações originais, autoria desconhecida e ausência de histórico detalhado; não são promovidas automaticamente ao novo fluxo. O aluno continua podendo solicitar histórico de seu vínculo transferido. Solicitar um documento não reativa nem altera matrícula.

O autor também definiu relatórios MEC como demonstrativos do protótipo e alcance do gestor acadêmico sobre todos os cursos da instituição, mantendo a coordenação por curso. A gestão está implementada abaixo; os relatórios demonstrativos seguem pendentes.

## Diário, avaliações e frequência por tempo

O menu 24 registra aulas realizadas, agrupando tempos da mesma data com pauta e conteúdo. Cada tempo precisa estar coberto pelo horário da turma naquela data, dentro de seu período, sem duplicidade. Um grupo é salvo inteiro ou desfeito. A mesma turma pode ocupar vários tempos; os intervalos não viram aula nem minutos de presença.

A carga da disciplina continua cadastrada em horas de 60 minutos: **60 horas = 3.600 minutos = 72 tempos de 50 minutos**. O cadastro não é regravado como 72 horas. Cargas que não se dividem em tempos inteiros são apontadas para conferência, sem arredondamento. As datas continuam sendo as do período, sem estimar seu fim a partir de meses aproximados.

P1, P2 e PF são cadastradas separadamente. Cada nota e chamada revalida o professor responsável, o vínculo com a turma, a situação da inscrição e a versão consultada. Alterações antes do fechamento mantêm evento com valor anterior e motivo. Uma nota lançada por engano pode ser retirada da apuração sem excluir seu ID, valor armazenado ou histórico. Não se substituem notas ausentes por zero.

A frequência pode ser lançada manualmente como PRESENTE, FALTA ou SEM_REGISTRO. Na segunda fase, tempos anteriores à data da inscrição ficam fora do numerador e do denominador individuais, sem faltas automáticas; tempos na própria data da inscrição são considerados. A carga completa da disciplina e as aulas continuam visíveis. Cada presença considerada soma 50 minutos. Uma chamada ausente ou pendente não vira falta nem permite fechamento silencioso.

O professor pode conferir os lançamentos e fechar o resultado a partir deles após o fim do período. A carga registrada precisa corresponder à prevista e as notas/chamadas consideradas devem estar resolvidas. A regra de resultado vigente continua sendo usada, incluindo 75% de frequência e as médias P1/P2/PF já aprovadas. Depois de encerrado, a alteração do resultado segue o processo de retificação existente; não se reabrem os lançamentos por esse menu. O aluno consulta apenas os próprios lançamentos, distinguindo-os do resultado vigente após uma retificação.

O contrato de chegada simulada recebe origem, chave única, aula, inscrição e instante local. A importação fictícia aplica o limite inclusivo de 15 minutos, recusa colisões de chave e não duplica carga ou presença ao receber um evento novamente. Uma frequência manual ou já registrada não é sobrescrita pelo importador. Recusas de dados são identificadas por evento e o lote continua; falhas de persistência interrompem o processamento. Não há conexão com equipamento real.

A V016 mantém aulas/avaliações antigas sem tempo/tipo/autoria presumidos, acrescenta versões e autoria de lançamentos e guarda a deduplicação dos eventos simulados. O diário aponta dados legados para conferência, sem reinterpretá-los como lançamentos novos. Este fluxo operacional ainda não é a emissão e assinatura do diário institucional completo; a consulta e auditoria da gestão estão descritas abaixo.

## Plano institucional versionado

O menu 25 prepara e registra o plano por matrícula, estrutura, períodos selecionados e prioridades. Reutiliza o plano derivado e a simulação de ofertas, com resultados formais, reconhecimentos e pré-requisitos; uma disciplina sugerida não passa a ser concluída por isso. A secretaria confere a prévia antes de confirmar. Se os dados ou a versão mudarem nesse intervalo, o registro é recusado para nova conferência.

Cada atualização cria uma versão com autoria, instante UTC, motivo e componentes preservados, incluindo nome, carga, situação, fundamento, requisitos e sugestão de oferta. Versões anteriores não são reescritas quando mudam nomes, resultados, reconhecimentos ou vagas. A consulta do aluno é restrita às próprias matrículas e não gera/atualiza plano automaticamente: mostra o registrado e informa ATUAL, PENDENTE_ATUALIZACAO ou INCONSISTENTE conforme a conferência possível.

A V017 guarda as versões novas separadamente dos planos legados, que mantêm seu processo de conciliação. Estrutura ausente/vazia, legado não conciliado ou dados acadêmicos incoerentes impedem geração consistente. Um plano registra a situação conhecida e sugestões, sem reservar vaga ou efetivar inscrição.

## Rematrícula institucional em lote

O menu 26 prepara o processamento por curso, período anterior encerrado e período de destino ativo posterior. A secretaria seleciona matrículas explicitamente ou confere todas as ativas do curso. Cada candidato é avaliado quanto a ingresso, vínculo, resultados anteriores, retificação pendente, plano atual para o destino, biblioteca e elegibilidade das propostas. A confirmação revalida os dados da prévia.

O lote renova os mesmos vínculos institucionais e guarda propostas e ocorrências. Não cria inscrições em turmas; cada confirmação posterior continua sujeita a elegibilidade e vagas reais. Se nenhuma turma sugerida estiver disponível, **toda a rematrícula daquele candidato segue para análise manual**, sem renovação automática, conforme decisão do autor. Plano ausente/desatualizado, dados insuficientes ou consulta bibliotecária inválida também geram ocorrência; os demais candidatos podem ser processados.

Repetir a mesma chave com os mesmos dados retorna o lote existente. Outro processamento reconhece as renovações já feitas e não as duplica. Colisão de chave ou mudança de dados desde a prévia exige conferência. Lote, renovações, propostas, histórico e auditoria integram a transação; uma falha de persistência/auditoria desfaz o processamento. A V018 acrescenta os três registros de lote, candidatos e propostas. A validação individual de rematrícula foi reutilizada, preservando o caminho do próprio aluno.

## Gestão acadêmica e auditoria docente

O menu 27 permite acompanhar metas/prazos docentes, avaliações, diário e trajetória discente. O gestor acadêmico possui vínculo explícito em `gestao_academica`, com alcance sobre todos os cursos. O coordenador continua restrito aos cursos que coordena. ADMIN técnico, professor ou aluno não recebem esse alcance por sua permissão ou papel isolado. Conta, vínculo e alcance são revalidados em cada operação.

No provisionamento 18 → 11, GESTOR vincula a conta existente diretamente, sem pedir ID de aluno, professor ou curso. A referência exibida é `gestao_academica.id_gestor`. A opção 18 → 14 altera a atividade desse vínculo, preservando sua identidade, conta, credenciais, permissões e demais vínculos. Nenhuma permissão técnica é criada por vincular gestor.

Metas e prazos identificam curso, professor, período opcional, tipo, descrição, vencimento e situação. Cada alteração exige versão consultada e motivo, conserva valores anteriores, autoria e instante UTC. Conclusões e cancelamentos não são reabertos por esse fluxo. A indicação de atraso é calculada na consulta, sem mudar automaticamente a situação. Metas legadas sem curso/autoria permanecem preservadas para conciliação.

As consultas de avaliações e diários aceitam filtros de curso, período, disciplina e turma. Em turmas compartilhadas, aulas e avaliações pertencem à turma, mas notas, chamadas e resultados de alunos são limitados ao curso selecionado. O diário distingue lançamentos originais do resultado vigente após retificação. A auditoria exige prévia conferida, conclusão e observação; revalida a assinatura dos dados e guarda o retrato consultado, autor e instante. Ela não muda notas, frequência ou resultado e não encerra o diário acadêmico.

O monitoramento informa situação curricular, inscrições e atualidade do plano. Dados insuficientes são apontados sem substituir contagens desconhecidas por zero, criar plano ou declarar conclusão de curso. A V019 acrescenta o vínculo de gestor e o histórico de metas, estende metas/auditorias existentes com campos opcionais para preservar registros anteriores e leva a 97 tabelas previstas no DDL.

## Limites e próximos blocos

Validação Codex do incremento com transferências externas em 25/11/2026: **178 unitários e 330 integrações aprovados**, sem falhas, erros ou ignorados, em instância nova de scoa_java_teste. O [relatório](testes/publicados/execucao_codex_v0_14/RELATORIO.md) detalha cobertura e rastreabilidade. A pedido do autor, a rodada manual será integrada ao fim dos blocos; os testes automáticos continuam durante a implementação.

Continuam em desenvolvimento: emissão/exportação do diário institucional completo; relatórios demonstrativos MEC; horários definidos antes da sala; administração operacional de concessões; parametrização e interface web. Geração/assinatura oficial de documentos não está implementada; o fluxo aprovado atual é solicitação/acompanhamento e atendimento. O console ainda não é o portal web previsto no Word.

As camadas JDBC básicas mantêm as regras de persistência e são usadas pelas fixtures e pela regressão legada. Os serviços de entrada do executável usam `RepositoriosAcademicosAutorizados` ou repositórios que já exigem a sessão. Qualquer futura interface web deve reutilizar essa fronteira, sem expor diretamente os repositórios básicos.

Desenvolvimento e testes usam exclusivamente `scoa_java_teste`, em instância nova. As migrações V013 a V020 não devem ser aplicadas automaticamente à base do autor ou ao schema `scoa`. Word editável, decisões detalhadas, guias, configurações, credenciais e evidências brutas permanecem privados.
