# Atualização acadêmica v0.14 — em desenvolvimento

Base: v0.13 fechada em `f5107209c26aa80ca83ef7cf9b3f3477c5e1d899`. Esta atualização amplia somente o Módulo Acadêmico. Ainda não representa o módulo completo nem o fechamento da v0.14; a validação manual do autor e os demais blocos continuam pendentes.

## Primeiro incremento implementado

- Fronteira autenticada dos serviços usados pelos menus legados de salas, alocação, cadastros, matrícula, inscrição, equivalência e planejamento. As operações verificam sessão, atividade e permissão no acesso aos dados; a auditoria das novas manutenções integra a transação.
- Permissões técnicas acadêmicas de consulta e manutenção separadas. ADMIN técnico pode executar esses cadastros; vínculo de aluno, docente ou coordenação não concede essas permissões. Nenhuma concessão existente foi alterada ou criada automaticamente. A administração completa das concessões ainda é trabalho pendente.
- Primeiro fechamento de resultado exige o professor ativo responsável pela turma. ADMIN, aluno, coordenação ou outro professor não substituem esse vínculo. São preservados P1/P2/PF, minutos apurados, resultado único e retificação posterior. O cadastro/lançamento por avaliação e a totalização a partir de aulas ainda serão integrados.
- Menu 21: histórico escolar do próprio aluno e registro/consulta de sugestões e reclamações. O histórico reúne matrículas, períodos, disciplinas, resultados, reconhecimentos e fontes legadas, sem presumir notas ausentes ou promover legado a reconhecimento. Revisão, revogação, contexto e datas limitam a indicação de vigência. Trata-se de consulta, não documento oficial.
- Manifestações identificam aluno, conta autora e instante. O conteúdo fica no registro; a auditoria registra ação e referência, sem duplicar o texto privado. A V013 mantém autoria desconhecida nas manifestações antigas.
- Base de domínio para os tempos de 50 minutos e a tolerância de atraso definidos pelo autor: seis tempos de manhã, seis à tarde e cinco à noite; intervalos não letivos. Presença integral até 15 minutos após o início, inclusive; depois, falta naquele tempo. Ausência de informação continua distinta de falta. Essa regra ainda será conectada ao cadastro de aulas e à integração fictícia.

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

## Limites e próximos blocos

Validação Codex do incremento em 25/11/2026: **159 unitários e 241 integrações aprovados**, sem falhas, erros ou ignorados, em instância nova de scoa_java_teste. O [relatório](testes/publicados/execucao_codex_v0_14/RELATORIO.md) detalha cobertura e rastreabilidade. A validação manual do autor da v0.14 ainda está pendente.

Continuam em desenvolvimento: avaliações P1/P2/PF separadas; aulas, pauta, conteúdo e frequência por tempo; contrato de eventos fictícios; diário e gestão docente; planos institucionais e processamento em lote; geração e assinatura de documentos; relatórios demonstrativos MEC; parametrização e interface web. O console ainda não é o portal web previsto no Word.

As camadas JDBC básicas mantêm as regras de persistência e são usadas pelas fixtures e pela regressão legada. Os serviços de entrada do executável usam `RepositoriosAcademicosAutorizados` ou repositórios que já exigem a sessão. Qualquer futura interface web deve reutilizar essa fronteira, sem expor diretamente os repositórios básicos.

Desenvolvimento e testes usam exclusivamente `scoa_java_teste`, em instância nova. As migrações V013 a V015 não devem ser aplicadas automaticamente à base do autor ou ao schema `scoa`. Word editável, decisões detalhadas, guias, configurações, credenciais e evidências brutas permanecem privados.
