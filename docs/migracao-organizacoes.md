# Migração dos vínculos de organizações

1. Suspenda escritas da API durante a migração e faça um backup do banco. Teste o procedimento em uma cópia antes de aplicá-lo ao banco usado pela equipe.
2. Inventarie os projetos legados com a consulta abaixo. Identifique a organização correta de cada projeto. Nomes iguais podem representar organizações distintas. Confirme a descrição e o e-mail de contato da organização antes de cadastrá-la. O e-mail de contato do projeto permanece no próprio projeto.
3. Cadastre as organizações pela API em um ambiente com acesso controlado ou insira os dados já validados em `organizacoes` com o `mongosh`. Registre o mapeamento de cada `_id` de projeto para o `_id` da organização escolhida.
4. Grave os vínculos usando o mapeamento revisado e inicialize os históricos que estiverem ausentes. Verifique todos os vínculos antes de remover o nome legado.
5. Depois das verificações, remova `organizacaoResponsavel` dos projetos migrados e reabra o acesso de escrita.

No `mongosh`, selecione o banco da aplicação e inventarie os projetos sem vínculo:

```javascript
db.projetos_sociais.find(
  { organizacaoId: null },
  { nome: 1, organizacaoResponsavel: 1, emailContato: 1 }
)
```

Use os identificadores reais do mapeamento para repetir o seguinte comando por projeto. O exemplo assume um projeto com `_id` do tipo `ObjectId`. Se o identificador antigo estiver gravado como string, use a string no filtro.

```javascript
const projetoId = ObjectId("66db67818e72c12f8a3c9020")
const organizacaoId = ObjectId("66db67818e72c12f8a3c9021")

if (!db.organizacoes.findOne({ _id: organizacaoId })) {
  throw new Error("Organização ausente. Revise o mapeamento antes de continuar.")
}

db.projetos_sociais.updateOne(
  { _id: projetoId, organizacaoId: null },
  { $set: { organizacaoId: organizacaoId.toHexString() } }
)
```

O filtro `organizacaoId: null` inclui campos ausentes e permite repetir a operação sem substituir vínculos já gravados. Após preencher os vínculos, inicialize os históricos ausentes ou nulos:

```javascript
db.projetos_sociais.updateMany(
  { atualizacoes: null },
  { $set: { atualizacoes: [] } }
)
```

A agregação abaixo deve retornar zero documentos. Ela detecta vínculos ausentes, com formato inválido ou sem organização correspondente.

```javascript
db.projetos_sociais.aggregate([
  { $set: {
      idOrganizacaoBson: {
        $convert: { input: "$organizacaoId", to: "objectId", onError: null, onNull: null }
      }
    }
  },
  { $lookup: {
      from: "organizacoes",
      localField: "idOrganizacaoBson",
      foreignField: "_id",
      as: "organizacao"
    }
  },
  { $match: {
      $or: [
        { "organizacao.0": { $exists: false } },
        { $expr: { $ne: ["$organizacaoId", { $toString: "$idOrganizacaoBson" }] } }
      ]
    }
  },
  { $project: { nome: 1, organizacaoId: 1, organizacaoResponsavel: 1 } }
])
```

Confirme que cada vínculo corresponde à organização revisada. Depois remova o campo legado e crie um índice para a verificação de vínculos:

```javascript
db.projetos_sociais.updateMany(
  { organizacaoResponsavel: { $exists: true } },
  { $unset: { organizacaoResponsavel: "" } }
)
db.projetos_sociais.createIndex({ organizacaoId: 1 })
```

Durante a transição, projetos legados podem ser consultados com `organizacao: null` e retornam `atualizacoes: []` se o histórico estiver ausente. Também é possível vincular um projeto individualmente pelo `PUT /api/projetos/{id}`, informando o novo `organizacaoId` e os demais campos editáveis. Esse caminho remove o nome legado daquele projeto. Para reverter uma migração em lote antes da reabertura de escritas, restaure o backup e volte à versão anterior da API.
