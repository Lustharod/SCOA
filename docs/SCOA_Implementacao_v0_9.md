# Distribuição do plano com ofertas cadastradas v0.9

O menu 17 oferece listagem de períodos e simulação de distribuição. O autor definiu considerar somente ofertas cadastradas e pré-requisitos já cumpridos formalmente. A matrícula e a estrutura vinculada são revalidadas; contextos inativos, inconsistentes ou legados não conciliados exigem análise explícita.

O usuário seleciona períodos e pode priorizar disciplinas; as restantes seguem por ID. Para cada pendente, a primeira oferta viável é buscada por início do período e ID da turma. A busca não garante combinação ótima. Conclusões, reconhecimentos e inscrições em andamento são preservados, sem redistribuição; sugestões e andamento não cumprem requisitos.

O resultado apresenta motivos por oferta tentada: situação, requisito não cumprido, horário ausente, conflito ou lotação. Ausência de oferta no horizonte é identificada. Conflitos incluem inscrições ativas do mesmo aluno em outras matrículas e sugestões anteriores, respeitando os períodos. Horas cadastradas das sugestões são somadas por período, excluindo inscrições em andamento. Não há créditos, teto institucional ou limite automático de carga.

Oferta futura pode ser simulada antes da janela; inscrição real continua no menu 15 com revalidação. Vagas observadas não são reservadas. A consulta não grava plano, item, inscrição, resultado ou equivalência. Não exige migração: permanecem as 76 tabelas após V008. A transação usa coordenação existente e savepoints, com leitura conservadora de turmas; não há medição de desempenho sob carga ou garantia contra escritores externos que ignorem o protocolo.

Planejamento contém dados e regra de distribuição; RepositorioPlanejamentoJdbc lê o contexto e as ofertas; ConsolePlanejamento apresenta o fluxo. Implementação validada: b0a39303f5a84981cfa70f752b13d1b2bfc2f874. Autor e Codex apresentaram 113 unitárias e 144 integração aprovadas, sem falhas/erros/ignorados, em execuções separadas. Ver relatórios em docs/testes/publicados/execucao_autor_v0_9 e execucao_codex_v0_9. Roteiro privado detalhado e métodos concretos foram preparados durante o desenvolvimento, sem alegar que todos antecederam o código.

O autor utiliza scoa_java_teste. Este fechamento não migrou scoa e não alterou fontes testadas. Word v7.12 permanece privado e preserva a v7.11. Revisão/revogação, retificação, importação de legados, rematrícula, integralização completa, autenticação/autorização, web e recuperação de conexão permanecem pendentes.
