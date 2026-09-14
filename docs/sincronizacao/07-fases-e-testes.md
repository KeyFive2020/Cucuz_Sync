# 7. Fases, testes e critérios de aceite

## Fase 0 — aprovação do plano

- Revisar esta documentação.
- Escolher nome, `mod_id`, pacote e decisões pendentes.
- Não escrever código funcional antes da aprovação.

Saída: plano aprovado e decisões registradas.

## Fase 1 — spike de preflight no Forge 1.20.1

- Criar mod mínimo em cliente e servidor.
- Provar descoberta na lista de servidores/antes da rejeição normal do Forge.
- Testar servidor com mod obrigatório ausente no cliente.
- Medir limites de payload e ordem dos eventos.
- Escolher status extension, login query ou fallback de URL configurada.

Critério de aceite: uma tela local pode ser aberta com a conexão ainda não concluída, antes de o Forge encerrar a tentativa por incompatibilidade.

## Fase 2 — manifesto e ferramenta administrativa

- Definir classes/schema e canonicalização.
- Escanear mods carregados no servidor.
- Mapear hashes/fingerprints nas plataformas.
- Permitir overrides manuais para entradas ambíguas.
- Validar dependências, lados e duplicatas.
- Assinar e publicar revisão.

Critério de aceite: o mesmo diretório de mods produz lockfile determinístico e uma alteração gera diff legível.

## Fase 3 — inventário local e motor de diferenças

- Ler JARs sem executar código.
- Extrair mod IDs e metadados Forge.
- Calcular/cachear hashes.
- Identificar arquivos por plataforma.
- Classificar diferenças e extras com evidências.

Critério de aceite: testes unitários cobrem ausente, divergente, duplicado, extra client-side, incompatível e desconhecido.

## Fase 4 — telas e consentimento

- Tela de verificação.
- Tela de resumo/detalhes.
- Seleção de origem quando houver equivalência.
- Confirmação final e cancelamento.
- Tela de erro/manual.
- Português do Brasil e inglês.

Critério de aceite: nenhuma alteração ocorre ao cancelar; o plano aceito corresponde exatamente à lista exibida.

## Fase 5 — downloads das plataformas

- Cliente Modrinth com User-Agent, cache e rate limit.
- Resolvedor CurseForge sem chave no cliente.
- Download para staging.
- Verificação de domínio, redirecionamento, tamanho e hash.
- Retomada/cancelamento.
- Fluxo manual para distribuição bloqueada.

Critério de aceite: arquivos corrompidos, URL alterada, hash errado e indisponibilidade nunca chegam à pasta `mods`.

## Fase 6 — aplicador, reinício e rollback

- Journal transacional.
- Aplicador Java externo mínimo.
- Espera do PID e confinamento de caminhos.
- Backup/quarentena.
- Aplicação e rollback.
- Validação na próxima inicialização.
- Atalho para reconectar.

Critério de aceite: interrupção simulada em cada etapa retorna ao estado anterior ou termina de forma recuperável.

## Fase 7 — endurecimento e compatibilidade

- Servidores/JARs/manifests maliciosos.
- Links simbólicos/junctions e path traversal.
- Arquivos gigantes, ZIP inválido e colisões.
- Timeouts, rate limits e respostas de API incompletas.
- Troca de chave do servidor.
- Locks de arquivo do Windows.
- Servidor dedicado sem classes client-side.

Critério de aceite: suíte de segurança passa e servidor dedicado inicializa sem carregar classes do cliente.

## Fase 8 — beta Forge 1.20.1

- Documentação para administrador e jogador.
- Changelog e política de privacidade.
- Pacote de exemplo com poucos mods conhecidos.
- Teste em instância limpa e instância CurseForge existente.
- Teste com pelo menos dois servidores/perfis.
- Publicação beta separada de produção.

Critério de aceite: sincronização completa funciona em máquina limpa, rollback funciona e nenhum segredo aparece no artefato/log.

## Fase 9 — portas para outras versões

Ordem proposta após estabilizar 1.20.1:

1. NeoForge 1.21.1.
2. Forge 1.19.2.
3. Forge 1.18.2.
4. Forge 1.16.5.
5. NeoForge 26.2.

O protocolo e o manifesto devem permanecer comuns; hooks de rede, UI e carregador serão adaptadores por versão.

## Estratégia de testes

### Unitários

- Parse/canonicalização/schema de manifesto.
- Assinatura válida, inválida, expirada e downgrade.
- Diff determinístico.
- Classificação e níveis de confiança.
- Sanitização de filenames/caminhos.
- Seleção de fonte.
- Máquina de estados da transação.

### Integração sem Minecraft

- Servidores HTTP/API simulados.
- Redirects permitidos/proibidos.
- Download parcial, truncado e hash incorreto.
- 404, 410, 429 e 5xx.
- Queda durante cada operação do aplicador.
- Concorrência e cancelamento.

### Integração Forge

- Cliente e servidor iguais.
- Cliente sem mod obrigatório.
- Cliente com versão errada.
- Extra puramente client-side.
- Extra que registra canal obrigatório.
- Extra desconhecido.
- Cuscuz Sync ausente de um dos lados.
- Servidor vanilla.
- Servidor com protocolo antigo/novo.
- Servidor dedicado headless.

### Sistemas operacionais

- Windows 10/11 como alvo obrigatório do MVP.
- Linux antes do beta público.
- macOS quando houver ambiente de teste disponível.

## Critérios globais de pronto

- Usuário sempre vê cada arquivo afetado antes de consentir.
- Cancelar deixa o disco inalterado.
- Downloads vêm apenas de origem permitida e passam por hash.
- Mudanças são recuperáveis.
- Chave CurseForge e chave privada do servidor não aparecem no cliente.
- Nenhum mod é classificado como client-side sem evidência registrada.
- A conexão só prossegue quando o inventário efetivo coincide com a política.
- Todos os cenários críticos têm testes automatizados ou roteiro reproduzível.

