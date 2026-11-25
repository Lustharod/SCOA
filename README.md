# SCOA — Sistema de Controle Acadêmico

Protótipo acadêmico em **Java e MySQL**, desenvolvido para Engenharia de Software no Bacharelado em Sistemas de Informação do CEFET/RJ, campus Maria da Graça. O projeto reúne requisitos, modelos de software e uma implementação incremental dos processos acadêmicos em console e portal web local.

**Neste ramo: v0.14 em desenvolvimento, atualização exclusiva do módulo acadêmico.** Inclui proteção dos menus acadêmicos, histórico próprio/manifestações, transferência interna efetiva, documentos/secretaria, diário por tempos, plano institucional versionado, rematrícula em lote, gestão acadêmica, transferências externas, horários antes da sala, relatórios PDF/XLSX e administração das concessões técnicas, além de regras versionadas por período e portal web local. Consulte as [notas da implementação](docs/SCOA_Implementacao_v0_14.md) para conhecer regras e limites. A rodada manual integrada dos novos incrementos será feita ao final dos blocos.

**Última entrega fechada: v0.13, em 24/11/2026.** O autor aprovou **130 testes unitários, 194 de integração e seis casos manuais** dessa versão. O módulo acadêmico completo e a interface web continuam em desenvolvimento.

[Documentação atual](docs/README.md) · [Relatório da v0.13](docs/testes/publicados/execucao_autor_v0_13/RELATORIO.md) · [Capturas reais](docs/testes/publicados/execucao_autor_v0_13/CAPTURAS.md) · [Evolução por versão](CHANGELOG.md)

Correção durante a aceitação: o envio de formulários pelo navegador preserva a origem local. [OC-V14-01 e rechecagem de 206 unitários/436 integrações](docs/testes/publicados/execucao_codex_v0_14/OC_V14_01.md). O autor confirmou o login corrigido; a v0.14 ainda não está fechada.

## O que já funciona

| Frente | Comportamento disponível no protótipo |
| --- | --- |
| Base acadêmica | Alunos, professores, cursos, disciplinas, pré-requisitos, estrutura curricular, períodos e turmas. |
| Salas e horários | Cadastro, horários por tempos sem sala, alocação posterior com verificação de capacidade/conflitos e histórico de manutenção. |
| Matrícula e inscrição | Histórico institucional, elegibilidade, duas fases de inscrição, controle de vagas e confirmação do próprio aluno. |
| Ajustes acadêmicos | Cancelamento automático ou por decisão da coordenação, renovação institucional e retificação de resultado com histórico. |
| Reconhecimento e planejamento | Equivalência manual, conciliação de legado, revisão de reconhecimentos e simulação por ofertas. |
| Acesso e consultas | Autenticação, permissões dos fluxos protegidos, vínculos acadêmicos e consulta dos próprios horários e resultados finais. |
| Incrementos v0.14 | Histórico próprio/manifestações (21), transferência interna (22), solicitação e atendimento de documentos (23), autorização dos menus legados e primeiro fechamento docente. |
| Diário por tempos (24) | Aulas, pauta/conteúdo, notas por avaliação, frequência manual/simulada, consulta própria e fechamento docente a partir dos lançamentos. 60 horas correspondem a 72 tempos de 50 minutos; tempos anteriores à inscrição ficam fora da frequência individual. |
| Plano institucional e lote (25–26) | Versões preservadas, consulta própria sem atualização automática, conferência de mudanças e processamento com propostas/ocorrências. A inscrição nas turmas exige confirmação separada; sem oferta disponível, a rematrícula segue para análise manual. |
| Transferências externas (28) | Entrada abre vínculo para a mesma pessoa; saída preserva origem e cancela inscrições no prazo aprovado. Processo, documentos e decisões com histórico; biblioteca impeditiva bloqueia a saída até regularização. |
| Horários antes da sala (29) | Tempos de 50 minutos definidos primeiro; sala pendente explícita e escolha posterior. Retirar a sala preserva dias e tempos; cancelamento conserva ID e histórico. |
| Relatórios e exportações (30) | Censo/ENADE demonstrativos, diário do docente/gestão, histórico e plano próprios. Prévia confirmada, arquivos preservados com integridade e autorização revalidada. Sem classificação oficial presumida. |
| Administração de acesso (31) | Perfis técnicos, concessão/revogação e atividade da conta com versões, autoria e histórico. Preserva credenciais/vínculos e o último administrador ativo; sessão antiga não retorna após reativação. |
| Regras por período (32) | Versões imutáveis de prazos, percentuais e médias. Seleção explícita somente para períodos futuros; períodos iniciados preservam as regras e resultados anteriores. |
| Portal acadêmico local | Áreas de aluno, professor, secretaria, coordenação/gestão e administração usando as mesmas regras e autorizações. Conferência antes de gravar, sessão por conta e arquivos PDF/XLSX arquivados. [Como acessar](docs/SCOA_Portal_Academico_v0_14.md). |
| Gestão acadêmica (27) | Metas e prazos docentes com versões, avaliações, consulta e auditoria do diário e monitoramento do discente. Gestor acompanha todos os cursos; coordenador acompanha seus cursos. Auditoria não altera notas, frequência ou resultados. |

## O que mudou na v0.13

A escolha repetida de ALUNO/PROFESSOR/COORDENADOR na criação de conta foi eliminada. Esses papéis são obtidos dos vínculos; o acesso técnico é concedido separadamente. A mesma conta pode reunir aluno, professor e coordenação de vários cursos sem receber permissão administrativa por esse motivo. O provisionamento identifica claramente qual campo pede ID da conta, do aluno, do professor ou do curso.

![Conta sintética com quatro vínculos acadêmicos e permissões técnicas vazias](docs/testes/publicados/execucao_autor_v0_13/capturas/02_vinculos.png)

*Recorte de uma execução real do autor, com dados sintéticos. A [galeria](docs/testes/publicados/execucao_autor_v0_13/CAPTURAS.md) reúne as demais verificações e os CSVs correspondentes.*

## Como a entrega foi verificada

| Execução independente | Unitários | Integração | Resultado |
| --- | ---: | ---: | --- |
| Codex, correção OC-V14-01 dos formulários locais, 25/11/2026 | 206 | 436 | BUILD SUCCESS; zero falhas, erros ou ignorados. Entrada corrigida confirmada pelo autor; demais casos manuais em andamento. |
| Codex, incremento v0.14 com portal web, 25/11/2026 | 206 | 434 | BUILD SUCCESS; zero falhas, erros ou ignorados. Rodada manual integrada pendente. |
| Autor, 23/11/2026 | 130 | 194 | BUILD SUCCESS; zero falhas, erros ou ignorados. |
| Codex, 23/11/2026 | 130 | 194 | BUILD SUCCESS; zero falhas, erros ou ignorados. |
| Casos manuais do autor, 23 e 24/11 | — | — | Seis casos concluídos; 26 CSVs e oito PDFs preservados. |

As duas execuções da v0.13 usam a mesma bateria e são identificadas separadamente. A seleção pública dessa versão contém **seis imagens de console e 20 CSVs sintéticos**, acompanhados de relatório, resultados por classe e manifestos. [Veja as evidências da v0.13](docs/testes/publicados/execucao_autor_v0_13/CAPTURAS.md). O [relatório Codex da v0.14](docs/testes/publicados/execucao_codex_v0_14/RELATORIO.md) identifica os testes e os limites do incremento atual. As [amostras de exportação](docs/testes/publicados/execucao_codex_v0_14/AMOSTRAS.md) são dados sintéticos de testes automáticos do Codex, identificados separadamente dos testes manuais do autor.

## Executar e testar

O projeto utiliza Java 25, Maven 3.9.11, JUnit Jupiter 6.0.1, MySQL Connector/J 26.7.0 e MySQL 8.0. A execução validada usou MySQL 8.0.46.

```powershell
# Testes unitários
.\mvnw.cmd test

# Regressão com MySQL, após preparar a conexão de testes
.\mvnw.cmd verify -Pmysql-it
```

Os testes JDBC exigem `SCOA_TEST_DB_URL`, `SCOA_TEST_DB_USUARIO` e `SCOA_TEST_DB_SENHA`, configurados localmente, apontando exclusivamente para **scoa_java_teste**. Neste ramo, a base usa o [DDL de testes v7.5](database/tests/SCOA_MySQL8_schema_v7_5_java_teste.sql) e as [migrações V006 a V024](database/migrations), nessa ordem: 104 tabelas previstas após V024. A v0.13 estável usava V012 e 87 tabelas. Bases existentes precisam de conferência e backup; não recriar ou reaplicar migrações automaticamente. O schema `scoa` não foi migrado.

Para o console, execute `br.edu.cefetmg.scoa.ScoaAplicacao` com `SCOA_DB_URL`, `SCOA_DB_USUARIO` e `SCOA_DB_SENHA`. A inicialização da primeira conta é local e explícita, sem senha padrão. Dados de conexão, senhas e guias operacionais são privados.

## Organização e limites

`src/main/java` contém domínio, aplicação, persistência e apresentação; `src/test/java` reúne os testes. `database` contém DDL e migrações. O [índice de docs](docs/README.md) separa a modelagem de referência, as notas de implementação e as evidências por versão.

O portal web local e a parametrização por período estão implementados. A [matriz acadêmica](docs/SCOA_Matriz_Academica_v0_14.md) relaciona os 28 requisitos e 30 casos de uso aos percursos e testes; a aceitação manual integrada ainda está pendente. Os relatórios demonstrativos e o diário operacional já podem ser exportados; emissão e assinatura oficiais de documentos não são presumidas. A apresentação está prevista para **30/11/2026** e a entrega para **07/12/2026**. As [notas da v0.14](docs/SCOA_Implementacao_v0_14.md) registram o avanço sobre o plano anterior; o conjunto não inclui emissão oficial de diploma ou conclusão presumida de curso.

O PDF v7.5 em `docs` é a base histórica de requisitos e modelagem. Para saber o que está implementado neste ramo, consulte as [notas da v0.14](docs/SCOA_Implementacao_v0_14.md). O [relatório da v0.13](docs/testes/publicados/execucao_autor_v0_13/RELATORIO.md) documenta a última entrega fechada.
