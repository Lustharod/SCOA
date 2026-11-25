# Validação Codex do incremento acadêmico v0.14

Em 25/11/2026, o incremento com regras acadêmicas versionadas por período passou em **197 testes unitários e 411 testes de integração**, sem falhas, erros ou ignorados. O Maven exibiu BUILD SUCCESS às 16:13:35 UTC-03:00, com duração de 02:44 min. A rodada manual do autor será integrada ao final dos blocos; ainda não foi realizada na v0.14.

## Regras com vigência futura

Vinte e um casos JDBC verificam permissão específica/ADMIN, versões imutáveis sem adoção automática, período sem seleção usando sempre a base aprovada, seleção futura explícita com histórico, preservação das inscrições, recusa de período iniciado/hoje/inativo, conflito de prévia/versão, integridade dos parâmetros, recusa de período com resultado mesmo se datas da fixture forem tornadas futuras, rollback de criação/seleção em falha de auditoria, resultado e retificação com a regra selecionada apesar de versão global posterior, resultado anterior preservado, procedência nos novos resultados/pedidos, prazo docente e abertura configurada da próxima inscrição, janelas no planejamento/elegibilidade, cancelamento e liberação de vaga nos limites configurados, prazo de transferência, atraso em evento simulado e concordância entre prévia do diário e fechamento com frequência configurada. Sete casos de domínio verificam equivalência da base com as regras aprovadas, limiares sem mudar a fórmula, limite inclusivo de atraso e configurações incoerentes sem arredondamento silencioso.

A primeira regressão deste bloco executou 197 unitários e 409 integrações, com três erros na preparação de fixtures: descrições sintéticas ultrapassavam os 50 caracteres do cadastro de período. Os originais permanecem privados, identificados como tentativa com erro. As descrições de teste foram corrigidas; acrescentadas duas verificações de integração e conferência de avaliações existentes na seleção de regras futuras.

Criar uma versão não altera períodos. Selecionar exige período ativo ainda não iniciado; períodos iniciados preservam sua seleção. Novos resultados/retificações registram a procedência; registros antigos não são recalculados nem recebem versão histórica inventada. O calendário é simulado por SQL nas fixtures, sem alterar o relógio e sem atribuir essas preparações a operações auditadas do console.

A regressão inclui os formatos PDF/XLSX e seus limites, com 48 marcadores no PDF sintético e reabertura da planilha. As [amostras visuais](AMOSTRAS.md) continuam identificadas como dados sintéticos do bloco de relatórios, separadas dos testes manuais do autor.

## Regressão e rastreabilidade

Todos os testes anteriores de acesso, cadastros, matrícula, inscrição, reconhecimento, planejamento, diário, transferências internas, documentos e atendimento permanecem na bateria. [Resultados por classe](RESULTADOS.md).

As contagens de versões sucessivas não devem ser somadas como casos distintos. O incremento anterior publicado em ab2b65e369940167df78dca97395baf1ab68d941 tinha 190 unitários e 390 integrações. Os originais de cada execução, inclusive tentativas que falharam, permanecem privados e identificados separadamente.

Instância MySQL 8.0.46 nova, scoa_java_teste, porta local 33386; DDL de testes v7.5 e V006 a V024. Nenhuma migração de scoa. As fixtures SQL são preparação/restauração dos testes, sem atribuição a atos auditados do console. Log completo, XML/TXT e manifesto permanecem privados. Os [hashes das fontes](FONTES_SHA256.json) foram conferidos após a execução. [Ambiente](ambiente.txt).

Este ponto amplia o ramo desenvolvimento/academico-completo. Main permanece na v0.13 fechada. A v0.14 e o módulo acadêmico continuam em desenvolvimento. Interface web acadêmica e rodada manual integrada seguem pendentes. [Regras e limites](../../../SCOA_Implementacao_v0_14.md).
