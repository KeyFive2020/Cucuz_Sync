# Implementação inicial — Forge 1.20.1

## Estado deste marco

O primeiro MVP executável do Cuscuz Sync foi implementado em `versions/forge-1.20.1`.

Ele já realiza:

- interceptação central da tentativa de entrada pela lista de servidores, conexão direta e Quick Play;
- obtenção primária do manifesto pelo Server List Ping na própria porta do Minecraft;
- fallback HTTP em `http://host:porta+1/cuscuz-sync/manifest`;
- inventário dos mods carregados, suas versões e seus arquivos;
- identificação de mods que o próprio Forge declara compatíveis quando ausentes no servidor;
- comparação entre cliente e servidor;
- tela paginada mostrando todos os mods que seriam instalados, atualizados ou movidos;
- consentimento explícito antes de qualquer mudança;
- resolução de versões pela API pública da Modrinth;
- downloads somente de CDNs HTTPS permitidas da Modrinth e CurseForge;
- validação de tamanho e hashes SHA-1/SHA-512;
- staging dos downloads e quarentena recuperável dos extras;
- aplicação por helper PowerShell depois que o Minecraft fecha, evitando arquivos JAR travados;
- histórico do plano aplicado.

## Configuração do servidor

Na primeira inicialização, o servidor cria:

`config/cuscuz-sync/server-manifest.json`

O arquivo lista automaticamente os mods carregados, inicialmente com `platform: NONE`. O administrador precisa informar a origem verificável de cada mod que será gerenciado. Há um modelo comentado pelo próprio formato em:

`versions/forge-1.20.1/config-examples/server-manifest.example.json`

O cliente faz primeiro um Server List Ping especial, identificado no hostname do handshake. O servidor responde com um JSON mínimo contendo somente o manifesto comprimido. O ping normal do Forge não é alterado nem ampliado. Isso funciona na mesma porta do Minecraft e não exige nova liberação no firewall ou painel da hospedagem.

O endpoint HTTP permanece como fallback e usa por padrão a porta do Minecraft mais um. Exemplo: `25565` vira `25566`. As propriedades JVM disponíveis são:

- `-DcuscuzSync.httpEnabled=false` para desativar;
- `-DcuscuzSync.httpPort=25570` para escolher outra porta;
- `-DcuscuzSync.bindAddress=127.0.0.1` para alterar o endereço de bind.

Se a porta ou URL pública for diferente da convenção, o cliente cria `.cuscuz-sync/servers.json`. Adicione em `overrides` uma entrada cuja chave seja exatamente o endereço salvo na lista de servidores.

## Modrinth

Informe o ID canônico do projeto e o ID exato da versão. O cliente consulta a API pública, confirma que a versão pertence ao projeto, escolhe o arquivo primário e usa os hashes oficiais fornecidos pela plataforma.

## CurseForge

Como a API oficial exige chave, o cliente não recebe nem armazena segredo algum. Defina `CURSEFORGE_API_KEY` somente no processo do servidor: o servidor resolverá em memória o nome, tamanho, SHA-1 e URL oficial a partir de `projectId` e `fileId`. O arquivo de configuração e o manifesto não armazenam a chave. Como alternativa, o administrador pode preencher esses metadados previamente. O cliente aceita apenas `edge.forgecdn.net` e `mediafilez.forgecdn.net` em HTTPS.

## Limitações conhecidas deste marco

- O manifesto ainda não possui assinatura criptográfica. A superfície de download já é limitada às plataformas oficiais e todo download é verificado, mas assinatura e confiança por servidor continuam na próxima fase.
- A chave CurseForge precisa ser provisionada manualmente no ambiente do servidor e ter acesso válido à API.
- Manifestos grandes demais para o limite do status ping ainda exigem endpoint HTTP ou hospedagem externa.
- Proxies que recriam o ping (por exemplo, algumas configurações de Velocity/Bungee) podem remover o marcador ou a resposta. Nesses casos, use um `override` HTTPS; proxy TCP transparente funciona normalmente.
- `enable-status=false` desativa também o transporte pela porta do Minecraft.
