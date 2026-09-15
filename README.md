# Cuscuz Sync

Mod de sincronização consentida de perfis de servidores, com suporte planejado às principais versões usadas por modpacks a partir da 1.16.5.

A implementação funcional começou por Forge 1.20.1. As outras linhas permanecem como ambientes de portabilidade e recebem a funcionalidade depois que o núcleo desta versão estiver estabilizado.

## Matriz suportada

| Minecraft | Loader | Java | Projeto |
| --- | --- | ---: | --- |
| 1.16.5 | Forge 36.2.42 | 8 | `versions/forge-1.16.5` |
| 1.18.2 | Forge 40.3.3 | 17 | `versions/forge-1.18.2` |
| 1.19.2 | Forge 43.4.22 | 17 | `versions/forge-1.19.2` |
| 1.20.1 | Forge 47.4.23 | 17 | `versions/forge-1.20.1` |
| 1.21.1 | NeoForge 21.1.250 | 21 | `versions/neoforge-1.21.1` |
| 26.2 | NeoForge 26.2.0.87 | 25 | `versions/neoforge-26.2` |

A 26.2 e a linha mais recente. Para montar modpacks grandes, a 1.21.1 continua sendo a opcao moderna com o ecossistema de mods mais amplo; por isso as duas permanecem suportadas.

Cada pasta de `versions/` e um projeto Gradle independente, com o wrapper oficial adequado. Os wrappers usam toolchains e podem baixar automaticamente o JDK de compilacao que estiver faltando. E necessario ter ao menos Java 17 instalado para iniciar os wrappers; Java 21 e recomendado para a linha 1.21.1 e Java 25 para a linha 26.2.

## Estrutura

- `shared/src/main/java`: logica Java pura compartilhada por todas as versoes. Nao importe APIs do Minecraft ou dos loaders aqui.
- `versions/<loader-versao>/src/main/java`: adaptadores, registros, eventos e demais codigo especifico daquela versao.
- `scripts`: comandos unificados para compilar e executar clientes de desenvolvimento.
- `dist`: JARs finais reunidos por `build-all.ps1`.

As APIs do Minecraft mudaram bastante entre 1.16.5 e 1.21.1. Por isso o codigo que toca Minecraft fica em cada linha, enquanto regras de negocio independentes do jogo podem ser compartilhadas.

## Comandos rapidos

No PowerShell, a partir da raiz:

```powershell
.\scripts\check-environment.ps1
.\scripts\build-all.ps1
.\scripts\run-client.ps1 forge-1.20.1
.\scripts\run-client.ps1 neoforge-1.21.1
.\scripts\run-client.ps1 neoforge-26.2
```

Para compilar ou executar apenas uma linha:

```powershell
cd .\versions\forge-1.20.1
.\gradlew.bat build
.\gradlew.bat runClient
```

Os JARs de todas as linhas serao copiados para `dist/` ao final de uma compilacao completa.

## Identidade

- Mod ID: `cuscuz_sync`
- Nome: `Cuscuz Sync`
- Código novo: `br.com.cuscuz.sync`
- Versao Forge 1.20.1: `0.3.2`
- Licenca: MIT

Use `scripts/set-mod-properties.ps1` para alterar os metadados de todas as linhas de uma vez. A troca do pacote Java exige tambem mover/renomear os arquivos fonte.

## Fluxo recomendado para novas funcionalidades

1. Coloque regras sem dependencias do jogo em `shared`.
2. Implemente primeiro na versao principal desejada, normalmente 1.20.1 ou 1.21.1.
3. Porte a integracao para cada pasta de `versions`.
4. Execute `build-all.ps1` antes de distribuir.

## Implementação Forge 1.20.1

O plano completo está em [`docs/sincronizacao`](docs/sincronizacao/README.md) e o estado do primeiro marco executável está em [`09-implementacao-forge-1.20.1.md`](docs/sincronizacao/09-implementacao-forge-1.20.1.md).

A versão 1.20.1 agora gera dois arquivos: `*-client.jar` para a instância do jogador e `*-server.jar` para o servidor dedicado. Não instale os dois no mesmo lado.

O cliente permite instalar os arquivos encontrados mesmo quando parte da lista continua sem fonte. Esses arquivos pendentes permanecem bloqueados até o servidor conseguir identificá-los.

Mods sem fonte aparecem em uma tela separada, em vermelho. O cliente pode salvar uma lista UTF-8 em `.cuscuz-sync/reports` enquanto prepara os outros downloads.

O servidor reconstrói o manifesto a cada cinco minutos. Ele identifica cada JAR ativo pelo SHA-1 na Modrinth e usa o fingerprint oficial do CurseForge como fallback. Modrinth não exige chave. Para mods exclusivos do CurseForge, configure `CURSEFORGE_API_KEY`, `-DcuscuzSync.curseForgeApiKey=...` ou o campo privado em `config/cuscuz-sync/server-settings.json`.

Como o Forge não carrega JARs durante a execução, alterações na pasta `mods` exigem reiniciar o servidor. Depois disso, a lista ativa é reconstruída e continua sendo atualizada a cada cinco minutos.
