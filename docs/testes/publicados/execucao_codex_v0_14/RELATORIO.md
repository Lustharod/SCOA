# Validação Codex do incremento acadêmico v0.14

Em 25/11/2026, o incremento com administração de acesso acadêmico passou em **190 testes unitários e 390 testes de integração**, sem falhas, erros ou ignorados. O Maven exibiu BUILD SUCCESS às 15:41:01 UTC-03:00, com duração de 02:38 min. A rodada manual do autor será integrada ao final dos blocos; ainda não foi realizada na v0.14.

## Administração técnica de acesso

Vinte casos JDBC verificam exigência de ADMIN atual, criação de perfil sem concessão automática, autoria/histórico, concessão e revogação em sessão existente, preservação da conta/credencial/vínculo acadêmico, efeitos de perfis compartilhados, preservação de outras concessões legítimas, conflitos de versão, proteção do último administrador por situação da conta, revogação ou alteração de perfil, transição com outro administrador ativo, inativação e reativação sem reutilizar a sessão antiga, perfis fora do catálogo preservados, rollback de criação/edição/concessão/revogação/situação em falha de auditoria, códigos duplicados no legado e ausência de histórico/credenciais inventados ou expostos. Dois casos de domínio verificam catálogo, versões, situações e motivos.

O primeiro ensaio deste bloco parou na compilação de um teste que usava um acessor de ID incorreto; nenhum teste foi executado naquela chamada. O log e manifesto privados a identificam como falha de compilação, sem atribuir os XMLs remanescentes da rodada anterior a uma nova execução. O teste foi corrigido para o contrato existente e a regressão completa seguinte passou.

A V023 mantém contas, hashes de credenciais, perfis e concessões existentes. Acrescenta versões, histórico e geração de sessão; inativação revoga as sessões anteriores. O catálogo acadêmico não altera concessões de outros módulos. ADMIN técnico não substitui professor, coordenação ou gestor nos fluxos de responsabilidade acadêmica.

A regressão inclui os formatos PDF/XLSX e seus limites, com 48 marcadores no PDF sintético e reabertura da planilha. As [amostras visuais](AMOSTRAS.md) são da execução identificada do bloco de relatórios e continuam separadas dos testes manuais do autor.

## Regressão e rastreabilidade

Todos os testes anteriores de acesso, cadastros, matrícula, inscrição, reconhecimento, planejamento, diário, transferências internas, documentos e atendimento permanecem na bateria. [Resultados por classe](RESULTADOS.md).

As contagens de versões sucessivas não devem ser somadas como casos distintos. O incremento anterior publicado em c44b4247758cbe4d7f65f0c4e3fcf8871b0ea940 tinha 188 unitários e 370 integrações. Os originais de cada execução, inclusive tentativas que falharam, permanecem privados e identificados separadamente.

Instância MySQL 8.0.46 nova, scoa_java_teste, porta local 33386; DDL de testes v7.5 e V006 a V023. Nenhuma migração de scoa. As fixtures SQL são preparação/restauração dos testes, sem atribuição a atos auditados do console. Log completo, XML/TXT e manifesto permanecem privados. Os [hashes das fontes](FONTES_SHA256.json) foram conferidos após a execução. [Ambiente](ambiente.txt).

Este ponto amplia o ramo desenvolvimento/academico-completo. Main permanece na v0.13 fechada. A v0.14 e o módulo acadêmico continuam em desenvolvimento. Parametrização com vigência somente em períodos futuros e interface web acadêmica seguem pendentes. [Regras e limites](../../../SCOA_Implementacao_v0_14.md).
