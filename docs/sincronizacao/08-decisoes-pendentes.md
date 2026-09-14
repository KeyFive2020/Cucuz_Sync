# 8. Decisões pendentes

Estas escolhas precisam ser aprovadas antes da implementação. As recomendações são pontos de partida, não decisões já tomadas.

## 1. Identidade do mod

Recomendação:

- Nome: `Cuscuz Sync`
- Mod ID: `cuscuz_sync`
- Pacote: `br.com.cuscuz.sync`

Isso substituiria a identidade provisória `cuscuz_multiversion` no workspace.

## 2. Onde guardar a chave da API CurseForge

Recomendação para o MVP: chave configurada no servidor dedicado por variável de ambiente ou arquivo secreto; o servidor resolve URLs para IDs exatos sem expor a chave.

Alternativas:

- backend central do Cuscuz;
- chave fornecida por cada jogador;
- relay dos arquivos pelo servidor.

É necessário confirmar também os termos aplicáveis ao modelo escolhido antes da distribuição pública.

## 3. Ordem preferida das plataformas

Recomendação: Modrinth primeiro quando o hash exato existir nas duas; CurseForge como segunda origem. O usuário poderá trocar a preferência.

Motivo: consultas públicas do Modrinth não exigem segredo para arquivos públicos, enquanto CurseForge exige arquitetura de credencial.

## 4. Desativar ou excluir extras

Recomendação: mover para quarentena reversível e usar o texto “Desativar para este servidor”. Não oferecer exclusão permanente no MVP.

## 5. Extras desconhecidos

Recomendação: perguntar sem selecionar ação por padrão. Modo estrito do servidor pode exigir desativação, mas o usuário ainda deve confirmar.

Opções de política:

- `allow`: mantém desconhecidos;
- `ask`: decisão explícita do usuário;
- `strict`: conexão requer perfil sem desconhecidos.

## 6. Manifesto automático ou curado

Recomendação: geração assistida. A ferramenta detecta arquivos/plataformas, mas o administrador revisa dependências, lados e políticas antes de publicar.

Geração totalmente automática pode classificar errado mods não reconhecidos e atualizar o conjunto sem revisão.

## 7. Forma de descoberta do manifesto

Recomendação: decidir somente depois do spike Forge da Fase 1. Priorizar integração no status/pre-login; manter URL configurada como fallback.

## 8. Reinício

Recomendação do MVP: aplicar automaticamente após fechar, mas pedir que o usuário reabra a instância. Relançamento automático varia entre launchers e ficará para uma fase posterior.

## 9. Perfis por servidor

Recomendação: um perfil por `serverId`, com backup/quarentena próprios. Ao trocar de servidor, mostrar o diff do perfil antes de qualquer mudança.

## 10. Configurações, resource packs e datapacks

Recomendação: fora do MVP. Primeiro sincronizar apenas JARs de mods. Depois criar um protocolo separado, com permissões diferentes, para configurações e recursos.

## 11. Mods privados ou arquivos manuais

Recomendação: bloquear no MVP. Todo arquivo automático deve existir em CurseForge/Modrinth e ter download autorizado. Suporte a repositório privado exigiria outro modelo de confiança/autenticação.

## 12. Confiança inicial

Recomendação: TOFU por servidor, mostrando chave, conjunto e tamanho na primeira vez; alertar fortemente se a chave mudar.

## 13. Definição de “client-side seguro”

Recomendação: exigir política explícita ou evidência de plataforma/metadado/canal. Manter uma lista de evidências no relatório para explicar cada classificação.

## 14. Atualização do próprio Cuscuz Sync

Recomendação: o próprio mod não deve substituir seu JAR na primeira versão. Se o protocolo estiver incompatível, mostrar links oficiais e instruções. Autoatualização aumenta muito a superfície de risco.

## Resumo das recomendações iniciais

| Tema | Recomendação |
| --- | --- |
| Nome | Cuscuz Sync |
| Primeiro alvo | Forge 1.20.1 |
| Fonte preferida | Modrinth, depois CurseForge |
| CurseForge key | Somente servidor/backend |
| Extras incompatíveis | Quarentena reversível |
| Extras desconhecidos | Perguntar |
| Mudanças | Sempre com reinício |
| Relançamento | Manual no MVP |
| Manifesto | Lockfile assinado e revisado |
| Download arbitrário | Nunca permitido |
| Autoatualização | Fora do MVP |

