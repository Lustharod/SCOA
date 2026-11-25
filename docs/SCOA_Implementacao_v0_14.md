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

A V014 preserva transferências legadas sem reconstruir destino/autor, acrescenta os campos do novo fluxo e a ligação de aproveitamento. A origem permanece consultável, inclusive quando transferida. Transferências externas e demais situações institucionais ainda dependem de especificação e implementação.

## Documentos acadêmicos

O menu 23 permite solicitar declaração de matrícula ou histórico escolar para uma matrícula pertencente à pessoa autenticada, consultar seus protocolos e acompanhar o atendimento. A secretaria consulta a fila e registra andamento, parecer visível ao solicitante e referência do documento entregue. O fluxo admite SOLICITADO, EM_ATENDIMENTO, ATENDIDO e INDEFERIDO; atendimento concluído exige referência de entrega. A geração do arquivo e sua assinatura oficial não estão implementadas neste incremento.

O atendimento e a consulta da fila exigem permissões técnicas próprias (`SCOA_DOCUMENTOS_ATENDER` e `SCOA_DOCUMENTOS_CONSULTAR`), além da conta ativa; ADMIN é autorizado. Aluno, professor, coordenador e as permissões genéricas de cadastro não recebem acesso à fila por esse motivo. Cada alteração revalida a concessão e a versão consultada; uma tela antiga não sobrescreve o andamento de outro operador. Solicitação, histórico e auditoria são gravados juntos, e a auditoria não duplica parecer ou referência privada.

A V015 acrescenta autoria, instantes UTC, versão e histórico de andamento. Solicitações legadas mantêm seus tipos e situações originais, autoria desconhecida e ausência de histórico detalhado; não são promovidas automaticamente ao novo fluxo. O aluno continua podendo solicitar histórico de seu vínculo transferido. Solicitar um documento não reativa nem altera matrícula.

O autor também definiu relatórios MEC como demonstrativos do protótipo e alcance do gestor acadêmico sobre todos os cursos da instituição, mantendo a coordenação por curso. Essas definições orientam os próximos blocos; relatórios e funções do gestor ainda não foram implementados.

## Diário, avaliações e frequência por tempo

O menu 24 registra aulas realizadas, agrupando tempos da mesma data com pauta e conteúdo. Cada tempo precisa estar coberto pelo horário da turma naquela data, dentro de seu período, sem duplicidade. Um grupo é salvo inteiro ou desfeito. A mesma turma pode ocupar vários tempos; os intervalos não viram aula nem minutos de presença.

A carga da disciplina continua cadastrada em horas de 60 minutos: **60 horas = 3.600 minutos = 72 tempos de 50 minutos**. O cadastro não é regravado como 72 horas. Cargas que não se dividem em tempos inteiros são apontadas para conferência, sem arredondamento. As datas continuam sendo as do período, sem estimar seu fim a partir de meses aproximados.

P1, P2 e PF são cadastradas separadamente. Cada nota e chamada revalida o professor responsável, o vínculo com a turma, a situação da inscrição e a versão consultada. Alterações antes do fechamento mantêm evento com valor anterior e motivo. Uma nota lançada por engano pode ser retirada da apuração sem excluir seu ID, valor armazenado ou histórico. Não se substituem notas ausentes por zero.

A frequência pode ser lançada manualmente como PRESENTE, FALTA ou SEM_REGISTRO. Na segunda fase, tempos anteriores à data da inscrição ficam fora do numerador e do denominador individuais, sem faltas automáticas; tempos na própria data da inscrição são considerados. A carga completa da disciplina e as aulas continuam visíveis. Cada presença considerada soma 50 minutos. Uma chamada ausente ou pendente não vira falta nem permite fechamento silencioso.

O professor pode conferir os lançamentos e fechar o resultado a partir deles após o fim do período. A carga registrada precisa corresponder à prevista e as notas/chamadas consideradas devem estar resolvidas. A regra de resultado vigente continua sendo usada, incluindo 75% de frequência e as médias P1/P2/PF já aprovadas. Depois de encerrado, a alteração do resultado segue o processo de retificação existente; não se reabrem os lançamentos por esse menu. O aluno consulta apenas os próprios lançamentos, distinguindo-os do resultado vigente após uma retificação.

O contrato de chegada simulada recebe origem, chave única, aula, inscrição e instante local. A importação fictícia aplica o limite inclusivo de 15 minutos, recusa colisões de chave e não duplica carga ou presença ao receber um evento novamente. Uma frequência manual ou já registrada não é sobrescrita pelo importador. Recusas de dados são identificadas por evento e o lote continua; falhas de persistência interrompem o processamento. Não há conexão com equipamento real.

A V016 mantém aulas/avaliações antigas sem tempo/tipo/autoria presumidos, acrescenta versões e autoria de lançamentos e guarda a deduplicação dos eventos simulados. O diário aponta dados legados para conferência, sem reinterpretá-los como lançamentos novos. Este fluxo operacional ainda não é a emissão e assinatura do diário institucional completo, nem substitui a gestão/auditoria docente prevista para os próximos blocos.

## Limites e próximos blocos

Validação Codex do incremento com diário em 25/11/2026: **166 unitários e 262 integrações aprovados**, sem falhas, erros ou ignorados, em instância nova de scoa_java_teste. O [relatório](testes/publicados/execucao_codex_v0_14/RELATORIO.md) detalha cobertura e rastreabilidade. A validação manual do autor da v0.14 ainda está pendente.

Continuam em desenvolvimento: emissão/exportação do diário institucional completo e gestão/auditoria docente; planos institucionais e processamento em lote; transferências externas; geração e assinatura de documentos; relatórios demonstrativos MEC; parametrização e interface web. O console ainda não é o portal web previsto no Word.

As camadas JDBC básicas mantêm as regras de persistência e são usadas pelas fixtures e pela regressão legada. Os serviços de entrada do executável usam `RepositoriosAcademicosAutorizados` ou repositórios que já exigem a sessão. Qualquer futura interface web deve reutilizar essa fronteira, sem expor diretamente os repositórios básicos.

Desenvolvimento e testes usam exclusivamente `scoa_java_teste`, em instância nova. As migrações V013 a V016 não devem ser aplicadas automaticamente à base do autor ou ao schema `scoa`. Word editável, decisões detalhadas, guias, configurações, credenciais e evidências brutas permanecem privados.
