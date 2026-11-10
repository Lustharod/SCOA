# Fechamento do SCOA v0.10

A validação do autor foi concluída em 09/11/2026. A execução automatizada de referência do autor terminou em 08/11/2026 às 19:51:08 (UTC−03:00), em 37,729 s, com BUILD SUCCESS. Os 21 relatórios JUnit registram 116 testes unitários e 166 de integração, com zero falhas, erros ou ignorados. Os casos coincidem com os da execução Codex de 03/11/2026 às 23:21:01 (UTC−03:00), em instância isolada na porta 33320. São 257 regressões e 25 casos novos. As duas execuções são registradas separadamente.

O autor validou o incremento em MySQL 8.0.46, porta 3306, no schema de teste `scoa_java_teste`. A migração aditiva V009 levou o schema de 76 para 81 tabelas. As 96 evidências originais da v0.10 foram inventariadas por SHA-256. Os 62 originais da v0.9 conferem com o manifesto anterior. As fontes conferem com o manifesto Codex da implementação.

Na matrícula sintética 385, a conciliação de plano manteve os dados legados e a de equivalência formalizou o pedido 110. A inscrição 109 ficou vinculada ao reconhecimento como prova do requisito. A primeira revisão (18) passou por complementação e tratamento e foi rejeitada. O CSV 35_36 recupera esses encaminhamentos do histórico depois da execução. O CSV 37 registra o estado final da primeira revisão.

A segunda revisão (19) revogou a validade futura do reconhecimento e preservou as inscrições 108, 109 e 110. A 108 permaneceu CANCELADO. As 109 e 110 permaneceram ATIVO. A dependência da 109 é COMPROVADO e as outras duas são POTENCIAL_HISTORICO. O deferimento original permaneceu no histórico. O CSV 48 registra um vínculo, duas conciliações, um pedido, duas revisões, três inscrições, uma prova, zero resultados, um plano e um item. Os CSVs 49–51 coincidem com os legados exportados nos CSVs 09–11.

A v0.10 inclui autenticação e permissões para o recorte de processos acadêmicos, conciliação de planos e equivalências legados, rastreabilidade de pré-requisitos e revisão/revogação com decisão humana. A revisão pendente suspende novos usos do reconhecimento. A revogação preserva as inscrições indicadas e a decisão original. A análise de dependência histórica distingue prova de impacto potencial.

## Arquitetura observada

O SCOA é uma aplicação Java monolítica de console, organizada em camadas e integrada ao MySQL via JDBC. A divisão aparece nos pacotes `apresentacao`, `aplicacao`, `dominio` e `persistencia`. `ScoaAplicacao` é o ponto de entrada e monta as dependências. Há serviços que usam contratos de repositório no domínio e implementações JDBC na persistência. Também há dependências diretas da apresentação para tipos da persistência, inclusive nos fluxos de planejamento e processos acadêmicos. O diagrama de pacotes deve registrar essas dependências reais. A formalização arquitetural detalhada e o diagrama ficam para a próxima conversa.

Apresentação do projeto em 30/11/2026. Entrega final em 07/12/2026.
