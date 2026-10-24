# Diagramas Arquiteturais — Football AI · Social

Todos os diagramas estão em **PlantUML** (`.puml`). Para cada Requisito Funcional há cinco visões:
**classes**, **atividades**, **máquina de estados**, **sequência** e **componentes**.
A pasta `global/` reúne as três visões transversais pedidas na especificação.

## Como renderizar

```bash
# PNG/SVG de tudo (requer Java 17+ e o jar do PlantUML)
java -jar plantuml.jar -tsvg -o ../../build/diagrams "docs/diagrams/**/*.puml"

# Só validar sintaxe
java -jar plantuml.jar -checkonly "docs/diagrams/**/*.puml"
```

No VS Code, a extensão *PlantUML* (jebbs) renderiza com `Alt+D`. No GitHub, use o
[PlantUML Proxy](https://www.plantuml.com/plantuml/proxy) ou o preview do IntelliJ.

## Índice

| Pasta | RF | class | activity | state | sequence | component |
|---|---|---|---|---|---|---|
| `global/` | Visão geral | [`class_diagram.puml`](global/class_diagram.puml) | [`activity_lgpd_export_delete.puml`](activity_lgpd_export_delete.puml) (RF5: exportar · excluir) | [`twin_state_machine.puml`](global/twin_state_machine.puml) | — | [`component_diagram.puml`](global/component_diagram.puml) |
| `rf1-auth/` | **RF1** Autenticação JWT, sessão Redis, rate limit | [class](rf1-auth/class.puml) | [activity](rf1-auth/activity.puml) | [state](rf1-auth/state.puml) | [sequence](rf1-auth/sequence.puml) | [component](rf1-auth/component.puml) |
| `rf2-identity/` | **RF2** Cadastro polimórfico, termos Velion, anel por papel | [class](rf2-identity/class.puml) | [activity](rf2-identity/activity.puml) | [state](rf2-identity/state.puml) | [sequence](rf2-identity/sequence.puml) | [component](rf2-identity/component.puml) |
| `rf3-feeds-ui/` | **RF3** SPA, bottom nav 4 abas, classificação de cards | [class](rf3-feeds-ui/class.puml) | [activity](rf3-feeds-ui/activity.puml) | [state](rf3-feeds-ui/state.puml) | [sequence](rf3-feeds-ui/sequence.puml) | [component](rf3-feeds-ui/component.puml) |
| `rf4-rarity-twin/` | **RF4** Rarity Engine & Gêmeo Digital | [class](rf4-rarity-twin/class.puml) | [activity](rf4-rarity-twin/activity.puml) | [state](rf4-rarity-twin/state.puml) | [sequence](rf4-rarity-twin/sequence.puml) | [component](rf4-rarity-twin/component.puml) |
| `rf4_1-badges/` | **RF4.1** Athletic Persona: badges e status físico | [class](rf4_1-badges/class.puml) | [activity](rf4_1-badges/activity.puml) | [state](rf4_1-badges/state.puml) | [sequence](rf4_1-badges/sequence.puml) | [component](rf4_1-badges/component.puml) |
| `rf5-lgpd/` | **RF5** Central de Compliance LGPD / Privacy by Design | [class](rf5-lgpd/class.puml) | [activity](rf5-lgpd/activity.puml) | [state](rf5-lgpd/state.puml) | [sequence](rf5-lgpd/sequence.puml) | [component](rf5-lgpd/component.puml) |

## O que cada visão responde

| Visão | Pergunta que responde |
|---|---|
| **Classes** | Quais entidades JPA, enums, value objects, serviços de domínio e DTOs existem e como se relacionam (controllers → use cases → domínio → adapters). |
| **Atividades** | Qual é o fluxo de negócio ponta a ponta, com decisões, paralelismo (`fork`) e responsáveis (swimlanes). |
| **Máquina de estados** | Quais estados um agregado pode assumir, quais transições são válidas e qual efeito cada transição produz. |
| **Sequência** | Como os componentes conversam em tempo de execução num cenário concreto, incluindo os casos de erro/segurança. |
| **Componentes** | Quais módulos, filtros, repositórios e stores (MySQL, Redis, Cassandra, S3, agentes Python) participam e por onde os dados fluem. |

## Convenções usadas

- Cores do **anel do autor** (`UserRole.ringColor`) e da **borda do card** (`PostContentType.borderColor`) seguem a tabela da especificação (`docs/PROMPT_FOOTBALL_AI_SOCIAL.md`).
- Faixas de **RarityTier**: COMMON 0–400 · RARE 401–900 · EPIC 901–1600 · LEGENDARY 1601–2400 · MYTHIC 2401+.
- Toda preferência LGPD nasce `FALSE` (opt-in); dados médicos/biométricos são marcados `<<Art. 11>>`.
- Estados terminais (`ANONYMIZED`, `DELETED`, `REMOVED`) nunca têm transição de saída.
- Estereótipos `<<@Entity>>`, `<<Cassandra>>`, `<<Redis>>`, `<<S3>>` indicam a tecnologia de persistência de cada elemento.
