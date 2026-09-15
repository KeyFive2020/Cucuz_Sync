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
- instalação parcial segura dos arquivos encontrados, sem ocultar os que continuam sem fonte;
- tela vermelha separada e relatório `.txt` dos mods sem fonte;
- descoberta do JAR exato pela API pública da Modrinth usando SHA-1;
- fallback de descoberta no CurseForge usando fingerprint Murmur2 quando uma chave de API está configurada;
- manifesto reconstruído e republicado a cada cinco minutos;
- JARs separados para cliente e servidor;
- downloads somente de CDNs HTTPS permitidas da Modrinth e CurseForge;
- validação de tamanho e hashes SHA-1/SHA-512;
- staging dos downloads e quarentena recuperável dos extras;
- aplicação por helper PowerShell depois que o Minecraft fecha, evitando arquivos JAR travados;
- histórico do plano aplicado.

## Configuração do servidor

Na primeira inicialização, o servidor cria:

`config/cuscuz-sync/server-manifest.json`

O arquivo é reconstruído com os mods ativos. Para cada JAR novo ou alterado, o servidor procura primeiro o arquivo exato na Modrinth pelo SHA-1 e, se não encontrar, tenta o CurseForge pelo fingerprint Murmur2. Entradas antigas deixam de existir na próxima reconstrução. Há um modelo do formato em:

`versions/forge-1.20.1/config-examples/server-manifest.example.json`

O cliente faz primeiro um Server List Ping especial, identificado no hostname do handshake. O servidor responde com um JSON mínimo contendo somente o manifesto comprimido. O ping normal do Forge não é alterado nem ampliado. Isso funciona na mesma porta do Minecraft e não exige nova liberação no firewall ou painel da hospedagem.

O endpoint HTTP permanece como fallback e usa por padrão a porta do Minecraft mais um. Exemplo: `25565` vira `25566`. As propriedades JVM disponíveis são:

- `-DcuscuzSync.httpEnabled=false` para desativar;
- `-DcuscuzSync.httpPort=25570` para escolher outra porta;
- `-DcuscuzSync.bindAddress=127.0.0.1` para alterar o endereço de bind.

Se a porta ou URL pública for diferente da convenção, o cliente cria `.cuscuz-sync/servers.json`. Adicione em `overrides` uma entrada cuja chave seja exatamente o endereço salvo na lista de servidores.

## Modrinth

Não exige configuração. O servidor consulta a API pública pelo SHA-1, confirma Forge 1.20.1 e registra o arquivo exato correspondente ao hash, mesmo quando a versão possui vários arquivos.

## CurseForge

Como a API oficial exige chave, o cliente não recebe nem armazena segredo algum. Defina `CURSEFORGE_API_KEY`, a propriedade Java `cuscuzSync.curseForgeApiKey` ou o campo de servidor em `config/cuscuz-sync/server-settings.json`. O servidor calcula o fingerprint do JAR e consulta o arquivo exato. O cliente baixa somente de hosts HTTPS oficiais permitidos.

## Limitações conhecidas deste marco

- O manifesto ainda não possui assinatura criptográfica. A superfície de download já é limitada às plataformas oficiais e todo download é verificado, mas assinatura e confiança por servidor continuam na próxima fase.
- A chave CurseForge precisa ser provisionada no servidor e ter acesso válido à API; sem ela, Modrinth continua funcionando normalmente.
- Forge não carrega nem descarrega JARs em tempo de execução. Colocar ou remover um arquivo em `mods` exige reiniciar o servidor; depois do reinício, o manifesto é reconstruído e continua sendo republicado a cada cinco minutos.
- Manifestos grandes demais para o limite do status ping ainda exigem endpoint HTTP ou hospedagem externa.
- Proxies que recriam o ping (por exemplo, algumas configurações de Velocity/Bungee) podem remover o marcador ou a resposta. Nesses casos, use um `override` HTTPS; proxy TCP transparente funciona normalmente.
- `enable-status=false` desativa também o transporte pela porta do Minecraft.
