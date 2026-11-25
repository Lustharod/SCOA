# Documentação e evidências do SCOA

**Neste ramo: v0.14 em desenvolvimento. Última versão fechada: v0.13, em 24/11/2026.** As [notas da atualização acadêmica](SCOA_Implementacao_v0_14.md) identificam os incrementos implementados e os blocos pendentes. Os testes manuais do autor da v0.14 ainda não foram realizados.

O [relatório Codex da v0.14](testes/publicados/execucao_codex_v0_14/RELATORIO.md) registra 175 unitários e 307 integrações aprovados em 25/11/2026, incluindo diário, plano institucional versionado, rematrícula em lote e gestão acadêmica. Contém contagens por classe, ambiente e hashes das fontes. A rodada manual integrada do autor será realizada ao fim dos blocos.

## Comece pela entrega atual

| Conteúdo | Arquivo |
| --- | --- |
| Regras e limites da implementação | [SCOA_Implementacao_v0_13.md](SCOA_Implementacao_v0_13.md) |
| Validação do autor | [Relatório da v0.13](testes/publicados/execucao_autor_v0_13/RELATORIO.md) |
| Imagens reais e CSVs de apoio | [Galeria de capturas](testes/publicados/execucao_autor_v0_13/CAPTURAS.md) |
| Contagens por classe | [Resultados automáticos](testes/publicados/execucao_autor_v0_13/RESULTADOS.md) |
| Ambiente e datas | [ambiente.txt](testes/publicados/execucao_autor_v0_13/ambiente.txt) |
| Rechecagem independente | [Relatório Codex](testes/publicados/execucao_codex_v0_13/RELATORIO.md) |
| Matriz acadêmica e prioridades | [Plano de apresentação e entrega](SCOA_Plano_Entrega_2026_11_23.md) |

## Evolução recente

| Versão | Incremento | Evidência pública |
| --- | --- | --- |
| v0.13 | Contas, vários vínculos legítimos e separação de permissões. | [Relatório](testes/publicados/execucao_autor_v0_13/RELATORIO.md), seis recortes e 20 CSVs selecionados. |
| v0.12 | Consultas do aluno, calendário, inscrição, cancelamento, renovação e retificação. | [Relatório e manifestos](testes/publicados/execucao_autor_v0_12/RELATORIO.md); capturas originais mantidas privadas. |
| v0.11 | Encaminhamento por serviços de planejamento e consulta de processos. | [Relatório](testes/publicados/execucao_autor_v0_11/RELATORIO.md); originais privados. |
| v0.10 | Autenticação, conciliação e revisão de reconhecimentos. | [Relatório](testes/publicados/execucao_autor_v0_10/RELATORIO.md) e CSVs selecionados. |
| v0.9 | Planejamento por ofertas e impedimentos acadêmicos. | [Relatório](testes/publicados/execucao_autor_v0_9/RELATORIO.md) e capturas selecionadas. |

As demais rodadas estão em [testes/publicados](testes/publicados). O [CHANGELOG](../CHANGELOG.md) registra os incrementos anteriores.

## Por que nem toda captura está no GitHub

Cada caso executado precisa de evidência, mas isso pode ser um resultado automatizado, uma exportação de banco ou uma captura agrupada de console. Publicar todas as telas repetidas não acrescenta uma verificação nova. O conjunto integral de originais é preservado privadamente e identificado por manifesto SHA-256; a seleção pública mostra exemplos relevantes e os dados que sustentam a conclusão.

Na v0.13, foram preservados 90 arquivos originais: 26 CSVs, oito PDFs, 27 XMLs, 28 TXT e um TSV. Grande parte foi gerada automaticamente. A publicação seleciona seis recortes visuais e 20 CSVs com dados sintéticos. Os recortes removem margens, a linha inicial de execução local e trechos de histórico repetido; não alteram mensagens ou resultados. Os [manifestos](testes/publicados/execucao_autor_v0_13/SELECAO_PUBLICA.json) ligam a seleção aos originais.

Para novas rodadas, reunir etapas relacionadas em uma captura por comportamento, exportar consultas de apoio em conjunto quando possível e guardar os relatórios automáticos. Uma recusa de acesso e um sucesso relevante merecem registro; repetir a mesma tela ou refazer a operação só para outra imagem não é necessário. Falhas e tentativas anteriores devem ser preservadas com sua origem.

## Modelagem de referência

O [PDF v7.5](SCOA%20-%20Sistema%20de%20Controle%20Acad%C3%AAmico%20v7.5.pdf) é uma base histórica de requisitos e modelagem; não representa a implementação atual inteira. Diagramas `.drawio` nesta pasta contêm as vistas editáveis disponíveis. As notas de implementação e relatórios mais recentes identificam o comportamento efetivamente entregue.

Word editável atualizado, guias de continuidade, credenciais, configurações e relatórios brutos permanecem privados.
