# Resultado dos testes de professores disciplinas e pré requisitos v0.4

Execução: Codex. Versão: 0.4.0-SNAPSHOT. Encerramento: 2026-10-28T15:19:28.416723-03:00.

O plano foi registrado antes da codificação desta entrega. Todos os 15 casos planejados foram executados. Resultado: 65 execuções unitárias e 49 de integração, sem falhas, erros ou ignorados; BUILD SUCCESS. As 91 execuções da v0.3 integram esses totais. Os testes de console foram automatizados com entrada controlada e conferência do estado persistido no MySQL, e não representam execução manual do autor.

## Rastreabilidade

| Caso | Comprovação | Resultado |
|---|---|---|
| P04-01 | ProfessoresDisciplinasTest.p0401ProfessorNormalizaPreservaIdsEUnicode; p0401ProfessorRejeitaAusenciaExcessoEIdsInvalidos | Aprovado na execução do Codex |
| P04-02 | ProfessoresDisciplinasJdbcIT.p0402ProfessorMantemContaIdentidadeESituacao | Aprovado na execução do Codex |
| P04-03 | ProfessoresDisciplinasJdbcIT.p0403InativacaoPreservaTurmaAnteriorENaoReativa | Aprovado na execução do Codex |
| P04-04 | ProfessoresDisciplinasJdbcIT.p0404ProfessorComprometidoNaoInativa; p0404ConcorrenciaProfessorETurmaNasDuasOrdens, nas duas ordens | Aprovado na execução do Codex |
| P04-05 | ProfessoresDisciplinasTest.p0405DisciplinaAceitaLimites; p0405DisciplinaRecusaCargaInvalida; p0405DisciplinaRecusaDadosInvalidos, incluindo todas as invocações parametrizadas | Aprovado na execução do Codex |
| P04-06 | ProfessoresDisciplinasJdbcIT.p0406DisciplinaMantemIdentidadeESituacaoSemMudancas | Aprovado na execução do Codex |
| P04-07 | ProfessoresDisciplinasJdbcIT.p0407AusenciasNaoCriamRegistrosNemRelacoes | Aprovado na execução do Codex |
| P04-08 | ProfessoresDisciplinasJdbcIT.p0408AdicionaConsultaRemoveERecusaDuplicidade | Aprovado na execução do Codex |
| P04-09 | ProfessoresDisciplinasJdbcIT.p0409RecusaAutorreferenciaECiclosDiretosEIndiretos | Aprovado na execução do Codex |
| P04-10 | ProfessoresDisciplinasJdbcIT.p0410RelacoesConcorrentesNaoCriamCicloMesmoAposSnapshot | Aprovado na execução do Codex |
| P04-11 | ProfessoresDisciplinasJdbcIT.p0411TransacoesERollbackPreservamDados | Aprovado na execução do Codex |
| P04-12 | ProfessoresDisciplinasJdbcIT.p0412NovasReferenciasIntegramTurmas | Aprovado na execução do Codex |
| P04-13 | ProfessoresDisciplinasJdbcIT.p0413ConsoleConfirmaCadastrosERelacoes; transcrição console-confirmacoes.txt | Aprovado na execução do Codex |
| P04-14 | ProfessoresDisciplinasJdbcIT.p0414ConsoleCancelaERecusaSemGravar; transcrição console-cancelamentos.txt | Aprovado na execução do Codex |
| P04-15 | As sete classes anteriores da v0.3: 56 execuções unitárias e 35 JDBC, incluídas na bateria atual | Aprovado na execução do Codex |

## Classes executadas

| Classe | Nível | Execuções | Falhas | Erros | Ignorados |
|---|---|---:|---:|---:|---:|
| ServicoSalaTest | Unitário | 9 | 0 | 0 | 0 |
| AlocacaoTest | Unitário | 15 | 0 | 0 | 0 |
| CadastroAcademicoTest | Unitário | 9 | 0 | 0 | 0 |
| ProfessoresDisciplinasTest | Unitário | 9 | 0 | 0 | 0 |
| SalaTest | Unitário | 23 | 0 | 0 | 0 |
| AlocacaoJdbcIT | Integração | 17 | 0 | 0 | 0 |
| CadastroAcademicoJdbcIT | Integração | 12 | 0 | 0 | 0 |
| ProfessoresDisciplinasJdbcIT | Integração | 14 | 0 | 0 | 0 |
| SalaJdbcIT | Integração | 6 | 0 | 0 | 0 |

## Ambiente e identificação do código

Java OpenJDK 25.0.1, Maven 3.9.11, JUnit Jupiter 6.0.1, servidor MySQL 8.0.46 e driver MySQL Connector/J 26.7.0. Instância isolada, schema scoa_java_teste. O banco pessoal do autor não foi usado.

Base Git: a3d0282. As alterações da v0.4 ainda não têm commit; FONTES_SHA256.json identifica os arquivos efetivamente testados. Não apresentar a3d0282 como commit da implementação v0.4. Os resultados foram extraídos somente das estruturas de testes dos XMLs, sem propriedades privadas.

## Limites

Aprovação restrita aos casos e condições descritos no plano da v0.4. Autenticação, autorização, interface web, gestão de contas, inscrições e elegibilidade acadêmica permanecem pendentes. A proteção de ciclos se aplica às escritas coordenadas pelo repositório; alterações SQL externas devem respeitar o mesmo protocolo. A validação do autor no IntelliJ ainda está pendente e deverá ter evidências próprias.


## Atualização de fechamento em 31/10/2026

O texto acima descreve a situação na execução de 28/10/2026. Posteriormente, o autor validou a v0.4 e suas evidências foram consolidadas separadamente em [execucao_autor_v0_4](../execucao_autor_v0_4/RELATORIO.md). Os 48 arquivos do manifesto foram conferidos sem divergências antes do commit b714404. Essa atualização não constitui nova execução do Codex.
