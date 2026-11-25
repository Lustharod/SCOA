# Validação Codex do incremento acadêmico v0.14

Em 25/11/2026, o incremento com transferências externas de entrada e saída passou em **178 testes unitários e 330 testes de integração**, sem falhas, erros ou ignorados. O Maven exibiu BUILD SUCCESS às 11:23:59 UTC-03:00, com duração de 01:36 min. A rodada manual do autor será integrada ao final dos blocos; ainda não foi realizada na v0.14.

## Transferências externas

Vinte e três casos JDBC verificam registro e acompanhamento sem alteração de vínculo, documentos e histórico com autoria/versões, entrada com vínculo curricular no destino e preservação da pessoa/conta e vínculos legítimos, saída com origem TRANSFERIDO e cancelamento das inscrições, preservação de resultados e histórico próprio, limites D+13/D+14/D+45, impedimento após D+45 ou fim do período, pendência bibliotecária e regularização sem exclusão, biblioteca indisponível, contexto alterado, destino já ativo, pedido de cancelamento pendente, repetição sem duplicar efeitos, colisão de chave, conflito de versão, estados terminais, permissões independentes e atuais e rollback integral em falha de auditoria no registro, entrada e saída. Três casos de domínio verificam referências acadêmicas, documentos, fundamento, versões e limites de entrada.

A efetivação requer decisão explícita da secretaria. O processo não gera credenciais nem aproveita disciplinas externas por semelhança de nome: o reconhecimento externo continua dependente do processo formal de equivalência existente. A saída bloqueia até regularizar a biblioteca, conforme decisão do autor; não altera a pendência bibliotecária. O registro e a mera consulta não efetivam transferências. Não há comunicação com instituições externas.

## Regressão e rastreabilidade

Todos os testes anteriores de acesso, cadastros, matrícula, inscrição, reconhecimento, planejamento, diário, transferências internas, documentos e atendimento permanecem na bateria. [Resultados por classe](RESULTADOS.md).

As contagens de versões sucessivas não devem ser somadas como casos distintos. O incremento anterior publicado em 0ee49fdbe83f150a7b8061d3b155fa33d5e98d02 tinha 175 unitários e 307 integrações. Os originais de cada execução, inclusive tentativas que falharam, permanecem privados e identificados separadamente.

Instância MySQL 8.0.46 nova, scoa_java_teste, porta local 33386; DDL de testes v7.5 e V006 a V020. Nenhuma migração de scoa. As fixtures SQL são preparação/restauração dos testes, sem atribuição a atos auditados do console. Log completo, XML/TXT e manifesto permanecem privados. Os [hashes das fontes](FONTES_SHA256.json) foram conferidos após a execução. [Ambiente](ambiente.txt).

Este ponto amplia o ramo desenvolvimento/academico-completo. Main permanece na v0.13 fechada. A v0.14 e o módulo acadêmico continuam em desenvolvimento. Relatórios/exportações, horários sem sala, administração operacional de concessões, parametrização e interface web seguem pendentes. [Regras e limites](../../../SCOA_Implementacao_v0_14.md).
