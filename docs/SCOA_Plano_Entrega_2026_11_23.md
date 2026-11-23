# Prioridades do SCOA para apresentação e entrega

Plano revisto em 23/11/2026, após a v0.12 e a implementação de OC-V12-01 na v0.13. Apresentação em 30/11/2026 e entrega em 07/12/2026, referência America/Sao_Paulo. As datas abaixo são uma organização proposta; não constituem promessa de implementar todo o escopo documentado ou alteração dos requisitos aprovados.

## Próximo recorte funcional proposto

Priorizar o primeiro lançamento autenticado de resultados, antes de expandir o diário. RF-MA-16 e RF-MA-17 têm apuração final e retificação, mas o menu 15.4 ainda é legado. O recorte deve encaminhar a sessão ao serviço/repositório responsável, verificar o professor da turma e preservar a regra P1/P2/PF, minutos, resultado único e retificação posterior do menu 20. Uma conta sem vínculo docente ou professor de outra turma deve ser recusada. Ser ADMIN ou coordenador não deve conceder implicitamente a autoria do primeiro lançamento.

Antes de implementar, conferir a regra no documento aprovado e acertar eventual exceção para substituição docente, coordenação ou secretaria. A proposta é responsabilidade do professor da turma; se a documentação não resolver as exceções, elas precisam de decisão explícita. Não inventar delegação institucional para cumprir prazo. O incremento deve incluir recusa sem sessão, sessão inválida, outro docente, permissão administrativa isolada, primeira gravação autorizada, duplicidade, efeito em pré-requisitos e preservação do caminho de retificação.

Como segundo recorte, após o primeiro estar estável, propor RF-MA-24: consulta autenticada do próprio histórico escolar. Reutilizar resultados e reconhecimentos existentes e apresentar origem e situação, inclusive revisão/revogação, sem tratar histórico de matrícula como histórico escolar nem transformar plano legado em conclusão. Definir previamente a apresentação e o que constitui documento oficial; uma consulta de console não é emissão de documento oficial.

## Matriz acadêmica atual

As prioridades Essencial, Importante e Desejável foram conferidas nos RF-MA-01 a RF-MA-28 do Word v7.16. Elas são prioridades aprovadas dos requisitos; a ordem de execução proposta considera também dependências e tempo de validação. Esta matriz distingue comportamento presente de requisito integralmente atendido. A correção de acesso da v0.13 é transversal e não transforma requisitos acadêmicos pendentes em concluídos. Menus e testes são referências de implementação, não resultados de execução manual.

| Requisito | Prioridade aprovada | Comportamento presente | Pendência ou limite |
| --- | --- | --- | --- |
| RF-MA-01 Alunos | Essencial | Cadastro, consulta, atualização e inativação no menu 13. | Autorização global dos cadastros legados. |
| RF-MA-02 Professores | Essencial | Cadastro, consulta, atualização e inativação no menu 12. | Autorização global dos cadastros legados. |
| RF-MA-03 Cursos | Essencial | Cadastro, consulta e atualização no menu 13. | Autorização global dos cadastros legados. |
| RF-MA-04 Disciplinas | Essencial | Cadastros e pré-requisitos com proteção de ciclos; uso na elegibilidade. | Autorização global dos cadastros legados. |
| RF-MA-05 Estrutura curricular | Essencial | Composição, vínculo e análise das disciplinas. | Transferência do vínculo e integralização. |
| RF-MA-06 Turmas | Essencial | Período, docente, disciplina, capacidade e horários; proteção do calendário. | Autorização global do cadastro; datas compartilhadas com o período. |
| RF-MA-07 Salas | Essencial | Cadastro, alterações, inativação e disponibilidade. | Autorização global do cadastro. |
| RF-MA-08 Alocação | Essencial | Capacidade e conflitos; proteção de turmas com inscrições. | Autorização global dos menus legados. |
| RF-MA-09 Matrícula | Essencial | Registro, estados, histórico institucional e bloqueio bibliotecário. | Autorização global dos fluxos legados; integração bibliotecária limitada. |
| RF-MA-10 Inscrição | Essencial | Duas fases, confirmação do aluno, cancelamento e controle de vagas. | Via técnica legada ainda exige revisão de autorização. |
| RF-MA-11 Rematrícula automática | Importante | Renovação institucional explícita preserva matrícula; simulação de plano. | Automatismo integral não entregue; turmas exigem confirmação separada. |
| RF-MA-12 Transferências | Essencial | Sem fluxo de transferência. | Documento, responsável, vínculo curricular e tratamento do histórico. |
| RF-MA-13 Aproveitamento | Essencial | Pedido, decisão, conciliação e revisão com preservação. | Encaminhamento acadêmico completo de casos específicos. |
| RF-MA-14 Plano de estudo | Essencial | Plano derivado e simulação de ofertas com motivos e horas. | Manutenção do plano institucional e integralização. |
| RF-MA-15 Horários do aluno | Importante | Próprias inscrições ativas no menu 19.1. | Oferta sem horário é apresentada sem inventar faixa ou sala. |
| RF-MA-16 Avaliações e notas | Essencial | Resultado P1/P2/PF e retificação com histórico. | Primeiro lançamento autenticado; cadastro e notas por avaliação. |
| RF-MA-17 Frequência | Essencial | Apuração final manual em minutos e retificação. | Registro e totalização por aula; integração de acesso. |
| RF-MA-18 Conteúdo e pauta | Essencial | Sem fluxo. | Aula identificada, autoria docente e prazos. |
| RF-MA-19 Diário | Essencial | Sem fluxo de diário. | Aulas, conteúdo, avaliações e frequência por aula. |
| RF-MA-20 Prazos docentes | Importante | Sem acompanhamento docente. | Critérios, metas e escopo da coordenação. |
| RF-MA-21 Auditoria do diário | Importante | Histórico de retificações existe. | Diário e sua auditoria integral. |
| RF-MA-22 Avaliações pela gestão | Importante | Sem consulta específica. | Avaliações por turma e escopo autorizado da gestão. |
| RF-MA-23 Monitoramento | Importante | Plano e histórico técnicos por matrícula. | Visão autorizada da coordenação e análise curricular abrangente. |
| RF-MA-24 Histórico escolar | Essencial | Histórico técnico no menu 16.5. | Consulta escolar consolidada do próprio aluno. |
| RF-MA-25 Notas e frequência | Essencial | Próprio resultado final no menu 19.2. | Avaliações intermediárias e registros por aula. |
| RF-MA-26 Documentos | Importante | Sem fluxo de solicitação. | Tipos, encaminhamento e estados. |
| RF-MA-27 Manifestações | Desejável | Sem fluxo de sugestões ou reclamações. | Responsáveis e visibilidade. |
| RF-MA-28 MEC | Essencial | Sem relatório implementado. | Campos, periodicidade, acesso e contrato da simulação. |

## Organização até as entregas

| Período | Prioridade | Critério para avançar |
| --- | --- | --- |
| 23 a 25/11 | Validar v0.13 e conferir regras do primeiro lançamento. | Regressão aprovada, roteiro manual real, evidências e documentação coerentes. |
| 25 a 27/11 | Executar o recorte de lançamento autenticado, se as regras estiverem resolvidas. | Recusas e sucesso cobertos, resultado e retificação preservados. |
| Até 27/11 | Avaliar histórico próprio como incremento seguinte. | Só incluir se houver tempo de validar; caso contrário, manter como evolução. |
| 28 e 29/11 | Preparar e ensaiar demonstração; revisar documentação e ambiente. | Percurso reproduzível, dados sintéticos e limitações declaradas. |
| 30/11 | Apresentação. | Demonstrar apenas o recorte executado e validado. |
| 01 a 04/12 | Corrigir achados e concluir recorte já iniciado. | Evitar iniciar frentes sem regras e tempo de teste. |
| 05 e 06/12 | Congelar conteúdo, regressão e revisão final. | Fontes identificadas, evidências reais, documento e repositório correspondentes. |
| 07/12 | Entrega. | Escopo implementado e evolução futura identificados com precisão. |

Diário completo, transferências, integralização, web e demais módulos não entram automaticamente nesse compromisso. Os nove pacotes acadêmicos e as demais frentes da estimativa de 29/10 continuam sendo escopo de evolução; esta organização prioriza um executável demonstrável.

## Percurso proposto para apresentação

Usar uma conta por pessoa para demonstrar aluno, docente e coordenação, mostrando a diferença entre ID da conta e referência acadêmica. Percorrer renovação, verificação bibliotecária, elegibilidade, confirmação, cancelamento e retificação usando cenários de calendário próprios. Uma instância não representa simultaneamente segunda fase, cancelamento tardio e retificação após o fim do período: manter as fixtures e não mudar o relógio para forçar resultados.

Apresentar a regressão e um exemplo de recusa de acesso, além do sucesso. Mostrar histórico próprio ou primeiro lançamento autenticado apenas se tiverem sido implementados e validados até o ensaio. Separar a evidência histórica da v0.12 dos resultados novos; não atribuir ao autor testes executados pelo Codex.
