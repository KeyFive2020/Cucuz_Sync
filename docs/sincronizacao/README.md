# Plano do Cuscuz Sync

## Estado deste documento

Esta pasta reúne o plano aprovado e o estado da implementação do mod de sincronização. O primeiro MVP executável para Forge 1.20.1 já foi criado; os documentos 01 a 08 permanecem como especificação e o documento 09 registra o que está funcionando.

Nome: **Cuscuz Sync**. O identificador adotado é `cuscuz_sync`.

## Objetivo

Criar um mod instalado no cliente e no servidor que, antes da entrada do jogador, compare os mods locais com a lista exata aprovada pelo servidor. Quando houver diferenças, o jogador verá tudo o que será alterado e escolherá se deseja continuar.

O primeiro alvo será **Minecraft 1.20.1 com Forge 47.x e Java 17**. Depois de a versão 1.20.1 estar estável, a solução será portada para as demais linhas existentes no workspace.

## Comportamento pretendido

- Detectar mods obrigatórios ausentes ou com versão/arquivo diferente.
- Resolver arquivos exatos no Modrinth ou CurseForge, sem escolher automaticamente “a versão mais nova”.
- Mostrar nome, versão, origem, tamanho e motivo de cada alteração.
- Pedir consentimento antes de qualquer download ou mudança local.
- Reconhecer mods extras puramente client-side e mantê-los.
- Identificar extras incompatíveis ou proibidos pela política do servidor.
- Tratar mods extras desconhecidos como “incertos”, nunca como seguros por adivinhação.
- Desativar extras incompatíveis movendo-os para quarentena reversível; exclusão permanente não será o padrão.
- Verificar tamanho, hash e identidade do arquivo antes da instalação.
- Aplicar as mudanças como uma transação, com rollback se algo falhar.
- Solicitar reinício do jogo antes de uma nova tentativa de conexão.

## Limitação técnica importante

O Forge descobre e carrega os mods durante a inicialização do jogo. Um JAR baixado enquanto o Minecraft já está aberto não pode ser carregado com segurança na mesma execução, e um mod já carregado não pode ser descarregado. Portanto, toda sincronização que adicionar, trocar ou desativar mods exigirá reinício.

No MVP, o fluxo seguro será: analisar, confirmar, baixar para uma área temporária, fechar o jogo, aplicar a transação e pedir ao usuário que abra novamente a instância. Relançamento automático poderá ser estudado depois, sem ser requisito da primeira versão.

## Índice

1. [Requisitos e limites](01-requisitos-e-escopo.md)
2. [Arquitetura técnica](02-arquitetura.md)
3. [Manifesto e protocolo](03-manifesto-e-protocolo.md)
4. [Experiência do usuário](04-experiencia-do-usuario.md)
5. [Integração com plataformas](05-integracao-plataformas.md)
6. [Segurança, privacidade e recuperação](06-seguranca-e-recuperacao.md)
7. [Fases, testes e critérios de aceite](07-fases-e-testes.md)
8. [Decisões pendentes](08-decisoes-pendentes.md)
9. [Implementação inicial — Forge 1.20.1](09-implementacao-forge-1.20.1.md)

## Princípios do produto

1. **Consentimento informado:** nenhuma alteração sem uma tela clara e confirmação explícita.
2. **Servidor não é confiável por padrão:** o servidor informa o conjunto desejado, mas o cliente valida origem, identidade, limites e hashes.
3. **Mudanças reversíveis:** extras são movidos para quarentena; instalações substituídas recebem backup até a conexão ser confirmada.
4. **Versões determinísticas:** o servidor publica um lockfile com arquivos exatos.
5. **Compatibilidade honesta:** classificação client-side incerta será exibida como incerta.
6. **Sem segredo no cliente:** chaves privadas e chaves da API CurseForge nunca serão embutidas no JAR.

## Fontes técnicas oficiais

- [Forge 1.20.x — SimpleImpl](https://docs.minecraftforge.net/en/1.20.x/networking/simpleimpl/)
- [CurseForge API](https://docs.curseforge.com/rest-api/)
- [CurseForge — Project Distribution Toggle](https://support.curseforge.com/en/support/solutions/articles/9000207877)
- [Modrinth API](https://docs.modrinth.com/api/)
- [Modrinth — identificação de versão por hash](https://docs.modrinth.com/api/operations/versionfromhash/)
