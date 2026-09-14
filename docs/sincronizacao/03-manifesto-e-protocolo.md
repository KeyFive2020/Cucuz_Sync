# 3. Manifesto e protocolo

## Objetivo do manifesto

O manifesto é um lockfile assinado do servidor. Ele descreve arquivos exatos e políticas; não é uma lista de consultas “latest”. O mesmo manifesto deve sempre produzir o mesmo conjunto de JARs enquanto suas origens estiverem disponíveis.

## Modelo conceitual

```json
{
  "schemaVersion": 1,
  "server": {
    "id": "uuid-estavel",
    "name": "Nome visivel",
    "revision": 42
  },
  "runtime": {
    "minecraft": "1.20.1",
    "loader": "forge",
    "loaderVersion": "47.4.23",
    "minimumSyncProtocol": 1
  },
  "policy": {
    "unknownExtras": "ask",
    "maxDownloadBytes": 2147483648
  },
  "mods": [
    {
      "logicalId": "create",
      "displayName": "Create",
      "providedModIds": ["create"],
      "requirement": "required",
      "environment": "client_and_server",
      "file": {
        "filename": "create-1.20.1-x.y.z.jar",
        "size": 12345678,
        "sha512": "hexadecimal"
      },
      "sources": [
        {
          "platform": "modrinth",
          "projectId": "id-estavel",
          "versionId": "id-versao",
          "fileHash": "sha512"
        },
        {
          "platform": "curseforge",
          "projectId": 328085,
          "fileId": 1234567,
          "fileHashSha1": "hexadecimal"
        }
      ]
    }
  ],
  "issuedAt": "ISO-8601",
  "expiresAt": "ISO-8601",
  "signature": {
    "algorithm": "Ed25519",
    "keyId": "identificador",
    "value": "base64"
  }
}
```

O exemplo define formato e intenção, não congela os nomes finais das classes Java.

## Campos obrigatórios por entrada

- Identificador lógico estável.
- Nome exibido ao usuário.
- Mod IDs fornecidos pelo JAR; um arquivo pode conter mais de um.
- Regra: `required`, `optional`, `client_allowed` ou `forbidden`.
- Ambiente/lado esperado.
- Nome de arquivo sanitizado.
- Tamanho exato.
- SHA-512 calculado pelo administrador/gerador do lockfile.
- Ao menos uma origem oficial permitida.
- Identificador imutável de projeto e versão/arquivo da plataforma.

## Canonicalização e assinatura

- JSON será canonicalizado antes do hash/assinatura.
- A assinatura cobrirá todos os campos, exceto o próprio valor da assinatura.
- Ed25519 é a opção inicial por ter chaves/assinaturas pequenas e implementação disponível em Java moderno; compatibilidade Java 17 deve ser confirmada no protótipo.
- Na primeira conexão, o cliente mostrará a identidade/chave do servidor e pedirá confiança.
- Conexões posteriores usarão TOFU: mudança de chave exigirá confirmação destacada.
- Assinatura garante que o manifesto não mudou no caminho; ela não transforma um servidor desconhecido em confiável.

## Mensagens conceituais do protocolo

- `ServerHello`: protocolo, server ID, revisão, hash e método de obtenção.
- `ManifestRequest`: revisão/cache que o cliente já possui.
- `ManifestResponse`: manifesto ou indicação de cache válido.
- `ClientInventorySummary`: hash do estado efetivo, sem transmitir dados além do necessário.
- `ClientReady`: confirmação de que o estado local corresponde ao lockfile.
- `ClientDeclined`: cancelamento sem punição ou alteração.
- `SyncError`: código estável e mensagem localizada.

O inventário completo do cliente não será enviado automaticamente ao servidor. A comparação ocorre localmente; o servidor recebe apenas o hash do estado compatível e os identificadores estritamente necessários para diagnóstico.

## Compatibilidade do protocolo

- O canal terá versão própria, independente da versão do mod.
- Versões incompatíveis interrompem o preflight com instrução para atualizar o Cuscuz Sync.
- Campos desconhecidos serão ignorados somente quando o schema permitir.
- Mudança incompatível incrementará `schemaVersion` ou protocolo.
- Tamanho máximo de manifesto e de cada mensagem será imposto antes de alocar memória.

## Geração do lockfile

O servidor não conseguirá mapear todos os JARs para CurseForge/Modrinth apenas pelo `mod_id`. O fluxo de administração será:

1. Escanear JARs presentes no servidor.
2. Calcular hashes e fingerprints.
3. Consultar as plataformas para correspondências exatas.
4. Resolver dependências obrigatórias.
5. Pedir ao administrador para completar arquivos não reconhecidos ou ambíguos.
6. Definir políticas de lado e extras.
7. Validar que cada entrada tem arquivo exato e origem permitida.
8. Gerar, canonicalizar e assinar o manifesto.

O manifesto publicado não será alterado automaticamente a cada atualização disponível. O administrador deverá gerar uma nova revisão e poderá revisar o diff antes de publicá-la.

## Cache

- Manifestos serão armazenados por `serverId + revision + hash`.
- Respostas das plataformas respeitarão cache/expiração e rate limits.
- Hashes locais serão cacheados por caminho normalizado, tamanho e data de modificação, mas recalculados antes de uma transação crítica.
- URLs temporárias de download não serão persistidas além do necessário.

