# Validação automática da correção de contas na v0.13

Em 23/11/2026, o Codex executou `mvn -o -B verify -Pmysql-it` com Java 25 e MySQL em uma instância nova e descartável de `scoa_java_teste`. A base foi o DDL v7.5 seguido de V006 a V012. O schema `scoa` não foi migrado ou usado nesta execução.

| Grupo | Execuções | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| Unitários | 130 | 0 | 0 | 0 |
| Integração | 194 | 0 | 0 | 0 |
| Total | 324 | 0 | 0 | 0 |

BUILD SUCCESS, encerramento às 12:13:22 UTC-03:00, duração 45.036 s. Os resultados por classe foram extraídos dos XMLs da própria execução; XMLs, logs e informações de conexão permanecem privados. Consulte [resultados](RESULTADOS.md), [ambiente](ambiente.txt) e [fontes identificadas](FONTES_SHA256.json).

A correção elimina a escolha acadêmica independente na criação da conta. Os vínculos determinam a identidade acadêmica apresentada; as concessões técnicas permanecem separadas. A regressão inclui os testes anteriores e as novas recusas e preservações descritas na [nota de implementação](../../../SCOA_Implementacao_v0_13.md).

Esta é uma execução do Codex. A execução automatizada do autor e a rodada manual da v0.13 ainda não foram realizadas ou informadas. A evidência aprovada da v0.12 permanece com suas datas, contagens e autoria próprias. Esta publicação não encerra a validação da v0.13.
