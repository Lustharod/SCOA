# Plano de testes de professores disciplinas e pré requisitos

Planejado em 28/10/2026, antes da codificação desta entrega. Base: documentação SCOA v7.5; implementação de partida v0.3. O plano da v0.3 continua como consolidação histórica e regressão.

## Escopo da entrega

RF-MA-02 / UC-MA-02: criar, consultar, listar, atualizar e inativar professores, preservando o cadastro e seus vínculos anteriores. RF-MA-04 / UC-MA-04: criar, consultar, listar e atualizar disciplinas e acrescentar, consultar e retirar pré-requisitos, sem autorreferência ou ciclos. RN-MA-01 fundamenta a coerência dos pré-requisitos; verificar elegibilidade e gravar inscrições permanece fora desta entrega. Autenticação, autorização e interface web continuam pendentes, portanto não se declara aprovação integral dos casos de uso.

Os dados respeitam o DDL v7.5. O vínculo opcional do professor com usuário é preservado, sem criar ou alterar contas de acesso. Não se presume unicidade de identificação de professor ou código de disciplina onde o modelo aprovado não a estabelece. A disciplina não recebe nova operação de inativação nesta entrega, pois UC-MA-04 prevê cadastro, consulta e atualização. A situação existente é preservada.

Restrição inicial de implementação: não inativar professor com turma ativa em período atual ou futuro, para preservar o planejamento. Turmas anteriores permanecem vinculadas e consultáveis. Essa proteção é documentada como limite técnico da entrega, não como nova regra institucional. Alterações de pré-requisitos são transacionais e coordenadas para impedir que operações simultâneas formem ciclos; não alteram inscrições ou resultados anteriores.

## Casos e resultados esperados

| Caso | Origem e nível | Condição e ação | Resultado esperado |
|---|---|---|---|
| P04-01 | RF-MA-02; unitário | Construir professor com identificação obrigatória, limites de texto, Unicode, espaços e identificadores válidos e inválidos | Aceitar e normalizar dados válidos; recusar dados inválidos sem truncamento |
| P04-02 | RF-MA-02; integração | Criar, consultar, listar e atualizar professor existente, inclusive com vínculo opcional de usuário previamente registrado | Manter identidade, situação e vínculo de usuário; recuperar o cadastro atualizado |
| P04-03 | RF-MA-02; integração | Inativar professor sem compromisso impeditivo, repetir a operação e atualizar dados; manter turma anterior vinculada | Preservar registro, vínculos anteriores e situação inativa sem reativação implícita |
| P04-04 | RF-MA-02 e RF-MA-06; integração e concorrência | Inativar professor com turma ativa atual ou futura; disputar a inativação e a criação de turma nas duas ordens | Recusar inativação incompatível; após confirmação concorrente, revalidar e impedir novo vínculo ativo com professor inativo |
| P04-05 | RF-MA-04; unitário | Construir disciplina com dados obrigatórios, limites, Unicode, carga horária e identificadores válidos e inválidos | Aceitar dados válidos, normalizar espaços e recusar dados inválidos |
| P04-06 | RF-MA-04; integração | Cadastrar, consultar, listar e atualizar disciplina, inclusive sem mudanças e com situação inativa previamente registrada | Preservar identidade e situação e recuperar todos os campos; não confundir atualização sem mudanças com ausência |
| P04-07 | RF-MA-02 e RF-MA-04; integração | Consultar, alterar ou inativar professor ausente; consultar ou alterar disciplina ausente; relacionar disciplina ou pré-requisito ausente | Informar ausência e não criar registros substitutos nem relações parciais |
| P04-08 | RF-MA-04; integração | Adicionar, consultar e retirar pré-requisito válido; repetir inclusão e retirar relação inexistente | Manter relação orientada correta, rejeitar duplicidade e informar relação ausente; não apagar disciplinas |
| P04-09 | UC-MA-04 e RN-MA-01; integração | Tentar autorreferência, ciclo direto e ciclo em cadeia de pré-requisitos | Rejeitar relação inconsistente e preservar a cadeia válida anterior |
| P04-10 | UC-MA-04 e RN-MA-01; integração e concorrência | Conexões independentes adicionam relações que, juntas, formariam ciclo | Serializar e revalidar as relações; confirmar somente o conjunto sem ciclo |
| P04-11 | RF-MA-02 e RF-MA-04; integração | Executar alterações válidas e inválidas em transação do chamador e desfazer; executar operação própria com falha | Preservar dados anteriores na recusa, manter transação externa sob controle do chamador e restaurar confirmação automática na própria |
| P04-12 | RF-MA-02, RF-MA-04 e RF-MA-06; integração | Usar novos professores e disciplinas no cadastro de turma; consultar referências ativas e tentar vincular professor inativo | Referências ativas disponíveis e turma válida persistida; professor inativo fora da seleção e rejeitado no novo vínculo |
| P04-13 | UC-MA-02 e UC-MA-04; sistema pelo console | Confirmar criação, consulta, atualização e inativação de professor livre; confirmar disciplina e manutenção de pré-requisito | Apresentar dados, pedir confirmação antes de gravar e persistir somente operações confirmadas |
| P04-14 | UC-MA-02 e UC-MA-04; sistema pelo console | Cancelar criação e alteração de professor e disciplina e manutenção de pré-requisitos; submeter dados inválidos | Informar cancelamento ou recusa, manter estado anterior e permitir continuar pelo menu |
| P04-15 | RF-MA-06, RF-MA-07 e RF-MA-08; regressão | Executar toda a bateria automatizada existente da v0.3 junto dos novos testes | Preservar os resultados de domínio, serviços, transações, manutenção e concorrência já verificados |

Nenhum caso acima está aprovado por antecipação. Antes do encerramento, executar todos os casos, vincular métodos e evidências a cada identificador e registrar resultados reais. Valores concretos pertencem à implementação dos testes e aos registros de execução, não às condições conceituais deste plano.

## Ambiente critérios e evidências

Usar Java e Maven definidos no projeto e schema scoa_java_teste em instância MySQL isolada. Executar verify com o perfil mysql-it, incluindo as regressões. A validação pelo console deve conferir o estado persistido e as operações canceladas. Usar dados sintéticos, limpar somente os registros da execução e separar evidências do Codex das do autor. Credenciais não integram código, documentos públicos ou saídas.

Registrar versão, identificação do código efetivamente testado, data e hora reais, responsável, servidor e driver em campos distintos, resultados e limitações. Aplicar o padrão de evidências do projeto. Uma falha exige correção e nova execução pertinente; caso bloqueado ou ignorado não é aprovado. A entrega só encerra quando todos os casos incluídos tiverem comprovação; a validação do autor será registrada separadamente.
