# 1. Requisitos e limites

## Atores

- **Administrador do servidor:** define e publica o conjunto oficial de mods.
- **Servidor Forge:** anuncia o manifesto e valida o estado declarado pelo cliente.
- **Jogador:** revisa e autoriza cada transação local.
- **Cliente Forge:** inventaria os JARs, calcula diferenças, baixa, valida e prepara alterações.
- **CurseForge e Modrinth:** origens permitidas para arquivos oficiais.

## Requisitos funcionais do MVP

### No servidor

- Gerar ou carregar um manifesto determinístico na inicialização.
- Fixar Minecraft `1.20.1`, Forge compatível e arquivo exato de cada mod.
- Associar cada arquivo a um projeto/versão do Modrinth, a um projeto/arquivo do CurseForge ou a ambas as origens.
- Marcar cada entrada como obrigatória, opcional, client-side permitida ou incompatível.
- Publicar hash, tamanho, nome e fontes aceitas.
- Anunciar versão do protocolo, revisão e hash do manifesto.
- Rejeitar clientes ainda incompatíveis após a sincronização, com mensagem compreensível.
- Nunca transmitir uma chave da API CurseForge ao cliente.

### No cliente

- Executar a verificação antes de tentar concluir o login.
- Ler os metadados de todos os JARs ativos na pasta `mods`.
- Calcular SHA-512 e, quando necessário, SHA-1/fingerprint para identificação nas plataformas.
- Comparar por conteúdo e identidade, não apenas pelo nome do arquivo.
- Separar o resultado em: ausentes, versão errada, extras permitidos, extras incompatíveis e desconhecidos.
- Exibir uma prévia completa das ações.
- Permitir cancelar sem alterar a instância.
- Baixar apenas depois do consentimento.
- Validar origem, tamanho e hashes antes de mover qualquer arquivo.
- Criar plano transacional e journal de recuperação.
- Aplicar mudanças somente após o encerramento do Minecraft.
- Preservar uma quarentena por servidor/perfil.
- Informar claramente quando um download precisar ser manual.

## Requisitos não funcionais

- Funcionar em Windows inicialmente; manter o desenho portável para Linux e macOS.
- Não bloquear a thread principal durante leitura de disco, hash ou download.
- Não registrar tokens, chaves, URLs assinadas ou caminhos pessoais completos nos logs públicos.
- Ter limites configuráveis de número de arquivos, tamanho individual e tamanho total.
- Aceitar retomada de download quando a origem oferecer suporte.
- Produzir logs de diagnóstico sem incluir dados sensíveis.
- Ser testável sem acessar APIs reais, usando respostas simuladas.

## Fora do escopo inicial

- Instalar ou trocar Forge, Java ou a versão do Minecraft.
- Carregar/descarregar mods sem reiniciar.
- Sincronizar saves, opções pessoais, shaders ou screenshots.
- Copiar configurações (`config/`, `defaultconfigs/`) no MVP.
- Redistribuir JARs diretamente pelo servidor.
- Contornar bloqueios de distribuição definidos por autores no CurseForge.
- Baixar de links arbitrários fornecidos por qualquer servidor.
- Resolver automaticamente “qualquer versão compatível”; o servidor deve fixar arquivos.
- Suportar Fabric/NeoForge antes da entrega estável da linha Forge 1.20.1.

## Definição das diferenças

| Estado | Significado | Ação proposta |
| --- | --- | --- |
| Ausente | Entrada obrigatória do manifesto não encontrada | Baixar e instalar após consentimento |
| Divergente | Mesmo projeto/mod ID, porém hash diferente | Substituir com backup |
| Extra client-side seguro | Não está no manifesto e é declarado/confirmado como client-side | Manter |
| Extra incompatível | Proibido no manifesto ou incompatível com o servidor | Oferecer desativação em quarentena |
| Extra desconhecido | Lado/impacto não pôde ser provado | Mostrar aviso; nunca remover automaticamente |
| Origem indisponível | Plataforma recusou ou não forneceu download | Orientar download manual e não alterar parcialmente |

## Regras para mods extras

A classificação não deve depender apenas do nome do JAR. Serão consideradas, nesta ordem:

1. Regra explícita do manifesto do servidor.
2. Identidade/hash reconhecido pela plataforma e metadado de ambiente disponível.
3. Metadados internos do JAR e compatibilidade de canais Forge.
4. Lista local de decisões anteriores do usuário para aquele servidor.
5. Resultado `UNKNOWN` quando não houver evidência suficiente.

Um mod desconhecido não será excluído só porque não aparece no servidor. A tela poderá informar que o Forge talvez rejeite a conexão e oferecer desativação manualmente confirmada.

## Perfis por servidor

Cada servidor terá um identificador estável e um perfil local. O perfil guardará:

- último manifesto aceito;
- chave pública confiada ao servidor;
- mods que foram colocados em quarentena;
- transação pendente ou concluída;
- preferências de origem do usuário;
- decisões explícitas para extras desconhecidos.

Isso evita perder mods ao alternar entre servidores e permite restaurar o estado anterior.

