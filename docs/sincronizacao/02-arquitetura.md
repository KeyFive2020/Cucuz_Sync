# 2. Arquitetura técnica

## Visão geral

O mod usa dois JARs com o mesmo mod ID e protocolo: um componente de servidor e um componente de cliente. Código que toca telas ou arquivos locais existe somente no JAR cliente; endpoints e atualização do manifesto existem somente no JAR servidor.

### Componentes do servidor

- `ServerInventoryScanner`: lê a lista efetiva de mods e versões carregadas.
- `ManifestBuilder`: combina inventário, política do administrador e IDs das plataformas.
- `ManifestValidator`: impede duplicatas, hashes ausentes e combinações incompatíveis.
- `ManifestSigner`: assina a representação canônica do manifesto.
- `PreflightService`: anuncia protocolo, revisão e forma de obter o manifesto.
- `CurseForgeResolver`: usa credencial mantida somente no servidor/backend.
- `ConnectionPolicy`: compara a declaração final do cliente e permite/rejeita login.

### Componentes do cliente

- `LocalInventoryScanner`: enumera JARs ativos sem seguir links simbólicos.
- `JarMetadataReader`: extrai `mods.toml`, manifesto e mod IDs sem executar o JAR.
- `HashService`: calcula SHA-512/SHA-1 em background com cache por tamanho/data.
- `PlatformIdentificationService`: identifica arquivos conhecidos por hash/fingerprint.
- `DiffEngine`: produz uma lista imutável de diferenças e ações possíveis.
- `ExtraModClassifier`: classifica extras com evidências e nível de confiança.
- `SyncScreen`: mostra resumo, detalhes, riscos, fonte e consentimento.
- `DownloadManager`: baixa em área temporária com limites, retomada e progresso.
- `ArtifactVerifier`: valida domínio, resposta, tamanho, hash e estrutura do JAR.
- `TransactionPlanner`: prepara operações atômicas e rollback.
- `RestartCoordinator`: fecha o jogo e inicia o aplicador externo após autorização.
- `RecoveryService`: recupera transações interrompidas antes de nova sincronização.

## Fluxo nominal

1. Jogador seleciona “Entrar” em um servidor.
2. Cliente descobre que o servidor oferece o protocolo Cuscuz Sync.
3. Cliente recebe a revisão/hash e obtém o manifesto.
4. Assinatura, expiração, versão do protocolo e identidade do servidor são validadas.
5. Inventário local é calculado fora da thread de renderização.
6. `DiffEngine` compara o inventário com o manifesto bloqueado.
7. Se não há mudanças, a conexão prossegue normalmente.
8. Se há mudanças, a conexão é pausada/cancelada e a tela de revisão é aberta.
9. Jogador aceita ou cancela.
10. Arquivos aceitos são resolvidos nas plataformas, baixados e verificados em staging.
11. Um journal transacional é gravado e sincronizado no disco.
12. O jogo solicita encerramento.
13. Um pequeno aplicador externo espera o processo do Minecraft terminar.
14. O aplicador move arquivos antigos para backup/quarentena e ativa os novos.
15. Em caso de erro, o aplicador restaura o estado anterior.
16. Na próxima abertura, o mod valida a transação e oferece reconectar ao servidor.

## Descoberta antes do login

Este é o primeiro risco técnico a ser prototipado. O Forge já compara canais e versões durante o ping/login e pode rejeitar a conexão antes de um pacote de jogo comum. A [documentação do SimpleImpl](https://docs.minecraftforge.net/en/1.20.x/networking/simpleimpl/) confirma que os predicados de versão também são usados no ping da lista de servidores.

Serão avaliadas nesta ordem:

1. Extensão controlada da resposta de status do servidor, contendo apenas protocolo, hash e endpoint do manifesto.
2. Mensagem/login query do Forge enviada antes da validação estrita do conjunto de mods.
3. URL de manifesto configurada para o servidor como fallback explícito.

O protótipo só avançará para o restante do mod após demonstrar que o cliente consegue mostrar a tela antes da rejeição normal do Forge. Não será adotada uma solução que exija esconder dados no MOTD ou interpretar texto frágil.

## Aplicador externo e reinício

O aplicador será um utilitário Java mínimo extraído do próprio mod para uma pasta privada. Ele receberá um arquivo de plano já validado, o PID do Minecraft e caminhos relativos autorizados. Suas únicas operações serão:

- esperar o processo encerrar;
- confirmar que todos os caminhos continuam dentro da instância;
- mover arquivos para backup/quarentena;
- mover arquivos verificados de staging para `mods/`;
- registrar sucesso ou falha;
- executar rollback se uma etapa falhar.

Ele não aceitará URLs, não fará downloads e não executará JARs baixados. Relançar automaticamente o launcher não fará parte do MVP.

## Armazenamento local proposto

```text
<instancia>/
  mods/
  cuscuz-sync/
    profiles/<server-id>/
      accepted-manifest.json
      trust.json
      quarantine/
      backups/
    staging/<transaction-id>/
    transactions/
      <transaction-id>.json
    cache/
      hashes.json
    logs/
```

Nenhum caminho vindo do servidor será usado diretamente como caminho de arquivo. Nomes serão normalizados e toda operação será confinada à instância.

## Concorrência

- Rede, hashing e leitura de JARs usarão executor limitado.
- Atualizações de UI voltarão para a thread cliente.
- Handlers de rede usarão `enqueueWork`, conforme recomendado pelo Forge.
- Haverá cancelamento cooperativo ao sair da tela.
- Apenas uma transação poderá estar ativa por instância.

