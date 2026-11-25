# Fechamento da validação do autor na v0.13

A v0.13 foi validada pelo autor e encerrada em 24/11/2026 para o recorte de contas e vínculos acadêmicos. OC-V12-01 foi corrigida: a criação da conta escolhe acesso técnico; aluno, professor e coordenação são obtidos dos vínculos. O módulo acadêmico integral continua em desenvolvimento.

## Regressão automatizada

| Execução | Unitários | Integração | Falhas, erros e ignorados | Encerramento UTC-03:00 | Duração |
| --- | ---: | ---: | --- | --- | --- |
| Autor | 130 | 194 | 0 | 23/11/2026 12:58:05 | 46.485 s |
| Codex, independente | 130 | 194 | 0 | 23/11/2026 12:13:22 | 45.036 s |

O log do autor registra BUILD SUCCESS. Os [resultados por classe](RESULTADOS.md) vieram dos XMLs dessa execução. Os [dados do ambiente](ambiente.txt) distinguem execução, calendário manual e encerramento da instância. O [manifesto de fontes](FONTES_SHA256.json) foi conferido no fechamento e não é atribuído retroativamente ao início da regressão.

## Casos manuais

| Caso | Verificação | Evidência conferida |
| --- | --- | --- |
| V13 01 | Contas sem vínculo; acesso técnico separado; campo de ID positivo. | A sem perfil; B com SCOA_PROCESSO_DECIDIR; referências acadêmicas independentes. |
| V13 02 | Aluno, professor e coordenação de dois cursos na mesma conta. | Quatro vínculos em A, sem permissão técnica; cinco eventos antes e depois da repetição. |
| V13 03 | Consulta própria, administração recusada e sessão encerrada. | Zero contas indevidas e zero concessões técnicas a A. |
| V13 04 | Cadastros já vinculados e preservação das concessões. | A e B conservam seus alunos e professores; credenciais preservadas=1 para ambas; eventos A=5/B=3. |
| V13 05 | Perfil legado com rótulo divergente. | Perfil COORDENADOR preservado; B continua com aluno/professor e coordena zero cursos. |
| V13 06 | Inativação, recusa de acesso e retorno. | Login/vínculo recusados para conta inativa; A e B ativas ao final; concessões, credenciais e eventos preservados. |

Os CSVs foram lidos; as 18 páginas dos oito PDFs foram extraídas e renderizadas para conferência. A [galeria de capturas](CAPTURAS.md) mostra seis recortes de páginas reais, com a origem e a página identificadas. A [seleção pública](SELECAO_PUBLICA.json) registra os hashes dos originais e das derivações. Os originais completos, incluindo 90 arquivos da rodada, estão identificados no [manifesto](MANIFESTO_SHA256.json).

V13_02_eventos.csv e a exportação após a repetição são idênticos byte a byte. Na primeira consulta de conta, o autor informou a identificação textual onde o console exigia ID; o sistema recusou a entrada e a consulta seguinte com ID positivo foi aceita. Isso foi uma recusa de entrada prevista, sem alteração de dados.

## Preparação SQL e limites da evidência

No V13 05, inserir o perfil legado e sua associação foi preparação SQL de fixture. No V13 06, os UPDATE para INATIVO e ATIVO foram preparação e restauração da conta sintética B. Não foram operações auditadas do console. A preservação de cinco eventos em A e três em B é compatível com essa distinção.

O autor salvou a regressão em TXT e preservou os XMLs; não foi exigida nova execução para obter um PDF do mesmo resultado. A captura da repetição usa o nome real V13_02_eventos_apos_repeticao.pdf. Linhas longas já aparecem limitadas pela largura da impressão original; por isso a galeria é complementada pelos CSVs. As imagens públicas não recriam mensagens nem resultados.

Não há credenciais, configurações de execução ou XML bruto nesta seleção pública. Word editável, guias e originais permanecem privados. A instância de scoa_java_teste foi encerrada em 24/11/2026 às 22:37:43 UTC-03:00, conforme o log. Não houve migração de scoa.

O fechamento cobre esta correção. Autorização global, primeiro lançamento autenticado, diário, avaliações/frequência por aula, transferências, integralização, histórico escolar próprio consolidado, web e demais módulos continuam com seus limites documentados.
