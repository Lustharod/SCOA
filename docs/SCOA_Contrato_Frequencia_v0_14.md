# Contrato da frequência simulada — v0.14

O equipamento de controle de acesso é fictício no protótipo. O SCOA recebe informações para frequência e não controla fisicamente portas ou catracas. Um adaptador futuro pode transformar eventos de outro sistema no contrato abaixo, preservando identificação, deduplicação, autorização e resultado de processamento.

## Entrada

`DiarioAcademico.Chegada` é a entrada tipada de `importarSimulados`. O professor responsável autentica a operação; cada aula/inscrição é conferida no alcance da turma. O console recebe os campos; no portal, são até 200 linhas por importação, separadas por ponto e vírgula:

```text
origem;chave;aula;inscricao;instanteLocal
SIMULADOR;evento-0001;101;202;2026-11-25T07:15:00
```

As referências `101` e `202` são ilustrativas; devem ser substituídas pelas existentes na instância de teste, sem confundir aula, inscrição e conta.

| Campo | Contrato |
| --- | --- |
| `origem` | Identificação da fonte, de 1 a 80 caracteres. |
| `chave` | Identificação do evento na fonte, de 1 a 100 caracteres. O par origem/chave é único. |
| `aula` | ID interno de uma aula por tempo letivo. Não é ID de sala, turma ou conta. |
| `inscricao` | ID interno da inscrição do aluno na turma. Não é ID de aluno ou matrícula institucional. |
| `instanteLocal` | Data e hora local acadêmica de São Paulo, sem deslocamento embutido: `AAAA-MM-DDTHH:mm:ss`. Precisão persistida em microssegundos. |

Uma integração real deve resolver a correspondência dos identificadores externos antes de enviar aula e inscrição; o protótipo não inventa correspondência por nome ou por coincidência de IDs.

## Processamento e repetição

O instante deve pertencer à data da aula. O tempo oficial determina seu início e a versão de regras selecionada para o período determina a tolerância. Na base aprovada, chegada até 15 minutos após o início, inclusive, resulta em presença integral de 50 minutos; após o limite resulta em falta naquele tempo. Os intervalos não geram tempos letivos. Uma disciplina pode ocupar vários tempos.

Repetir origem/chave com os mesmos dados retorna `REPETIDO`, sem acrescentar presença ou carga. Reutilizar a chave com outros dados é recusado. O adaptador não sobrescreve chamada manual ou outro evento já aceito. Ausência de evento não cria automaticamente falta. Tempos anteriores à data da inscrição são desconsiderados da frequência individual, preservando a carga integral da disciplina.

Cada item informa chave, situação e mensagem. Uma recusa de regra é identificada por item e os demais podem ser processados. Falha de persistência desfaz a transação do item; os itens anteriores já concluídos permanecem identificáveis por suas chaves. Para retomar, reenviar as mesmas chaves evita duplicações. Não gerar chaves novas apenas porque a resposta anterior não chegou.

Dados inválidos, aula/inscrição de outra turma, responsabilidade docente ausente, data incompatível, resultado encerrado e conflito com chamada existente são recusados. Nenhum evento autoriza alteração de resultado final: depois do fechamento, aplica-se o procedimento de retificação.

## Evidência e limite

`DiarioAcademicoJdbcIT`, `ParametrosAcademicosJdbcIT` e testes HTTP verificam recepção simulada, limites, repetição, autorização e preservação dos registros. [Resultados](testes/publicados/execucao_codex_v0_14/RESULTADOS.md).

Não há transporte de fornecedor, dispositivo físico, garantia de entrega externa ou serviço de reconexão a equipamento real implementado. O contrato permite desenvolver esse adaptador posteriormente sem mudar a regra acadêmica e sem atribuir à simulação uma integração real.
