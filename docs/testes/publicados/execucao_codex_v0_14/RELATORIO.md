# Validação Codex do incremento acadêmico v0.14

Em 25/11/2026, o incremento com relatórios acadêmicos e exportações PDF/XLSX passou em **188 testes unitários e 370 testes de integração**, sem falhas, erros ou ignorados. O Maven exibiu BUILD SUCCESS às 15:23:57 UTC-03:00, com duração de 02:14 min. A rodada manual do autor será integrada ao final dos blocos; ainda não foi realizada na v0.14.

## Relatórios e formatos

Vinte e dois casos JDBC verificam prévia sem gravação, confirmação com revalidação dos dados, PDF/XLSX reais do mesmo retrato, autoria, arquivamento imutável, repetição sem duplicidade, colisão de referência entre contas, alteração posterior da origem, consulta própria, professor responsável, alcance da coordenação em turma compartilhada, gestor sem poder técnico implícito para MEC, revogação de concessão/vínculo/conta, plano inexistente sem criação automática, plano desatualizado preservado, incoerências, reconhecimento futuro, filtros/população vazia, falta de dados institucionais, símbolo não suportado na prévia, integridade de dados/arquivos e rollback em falha de auditoria.

Seis casos dos formatos verificam XML sem entidades externas e com preservação de acentos/caracteres reservados/CRLF, extração de todos os registros do PDF até a última página, XLSX com texto que parece fórmula mantido como texto, zeros à esquerda e campos extensos, limites explícitos sem truncamento e cópia defensiva dos arquivos. Dois casos de domínio verificam filtros e confirmação. A primeira e a última página do PDF sintético foram conferidas visualmente; [amostras selecionadas](AMOSTRAS.md).

A primeira regressão tinha 188 unitários e 368 integrações, com uma falha de asserção que exigia 8.00 quando o banco preserva 8.0. A comparação passou a conferir o valor numérico; cálculo e nota armazenada não foram alterados. Essa execução permanece identificada separadamente. A versão final também inclui validação dos formatos na prévia, melhoria de paginação e provider de logging.

Dependências declaradas: Apache PDFBox 3.0.8, Apache POI 6.0.0 e Log4j Core 2.26.1. Artefatos públicos obtidos do Maven Central e conferidos no cache isolado de validação. A fonte incorporada do PDF suporta o repertório verificado de português; símbolos sem glifo e textos acima do limite do XLSX impedem concluir a geração, sem alterar ou truncar os dados.

Censo e ENADE são demonstrativos do protótipo, sem formato/envio oficial e sem presumir aptidão ao ENADE ou conclusão de curso. Diário exportado distingue lançamentos originais de resultado vigente. Histórico próprio não substitui documento oficial; plano próprio não é atualizado pela consulta/exportação.

## Regressão e rastreabilidade

Todos os testes anteriores de acesso, cadastros, matrícula, inscrição, reconhecimento, planejamento, diário, transferências internas, documentos e atendimento permanecem na bateria. [Resultados por classe](RESULTADOS.md).

As contagens de versões sucessivas não devem ser somadas como casos distintos. O incremento anterior publicado em b856ec7898a612234ea6186760eebe8489b7d206 tinha 180 unitários e 348 integrações. Os originais de cada execução, inclusive tentativas que falharam, permanecem privados e identificados separadamente.

Instância MySQL 8.0.46 nova, scoa_java_teste, porta local 33386; DDL de testes v7.5 e V006 a V022. Nenhuma migração de scoa. As fixtures SQL são preparação/restauração dos testes, sem atribuição a atos auditados do console. Log completo, XML/TXT e manifesto permanecem privados. Os [hashes das fontes](FONTES_SHA256.json) foram conferidos após a execução. [Ambiente](ambiente.txt).

Este ponto amplia o ramo desenvolvimento/academico-completo. Main permanece na v0.13 fechada. A v0.14 e o módulo acadêmico continuam em desenvolvimento. Administração operacional de concessões, parametrização e interface web seguem pendentes. [Regras e limites](../../../SCOA_Implementacao_v0_14.md).
