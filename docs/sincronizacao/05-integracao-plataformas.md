# 5. Integração com CurseForge e Modrinth

## Regra geral

As plataformas serão catálogos/origens de arquivos, não autoridades sobre o conjunto do servidor. O lockfile do servidor escolherá uma versão exata e o cliente confirmará que o conteúdo baixado corresponde ao manifesto.

O cliente nunca montará URLs por adivinhação e nunca fará fallback para sites de terceiros.

## Modrinth

A [API do Modrinth](https://docs.modrinth.com/api/) permite a maioria das consultas públicas sem token, exige um `User-Agent` identificável e publica limites por IP nos headers. A documentação atualmente informa limite de 300 requisições por minuto; o código deverá ler os headers em vez de assumir que esse valor será permanente.

### Identificação

- Calcular SHA-512 preferencialmente e SHA-1 quando necessário.
- Usar os endpoints de versão por hash para reconhecer arquivos locais.
- Armazenar IDs estáveis, não slugs mutáveis.
- Confirmar `game_versions`, loader e `environment` da versão.

A [resposta de versão por hash](https://docs.modrinth.com/api/operations/versionfromhash/) fornece projeto, versão, loaders, ambiente, arquivos, tamanho, URL e hashes. Isso será usado tanto na identificação quanto na verificação.

### Download

- Selecionar o arquivo marcado como `primary`; se nenhum for primário, tratar o primeiro somente conforme a regra documentada pela API.
- Aceitar apenas URL retornada para o arquivo exato do manifesto.
- Restringir HTTPS aos hosts oficiais esperados e validar redirecionamentos.
- Verificar tamanho e SHA-512 após baixar.
- Não exigir login/token para arquivos públicos.
- Tratar 404, 410, rate limit e versão da API como erros específicos.

### Ambiente/lado

O campo `environment` do Modrinth será evidência importante para classificar um extra como client-side, mas ainda será combinado com a política do servidor e metadados do JAR.

## CurseForge

A [API oficial do CurseForge](https://docs.curseforge.com/rest-api/) usa `x-api-key` para endpoints de arquivo, URL de download e fingerprints. O cliente distribuído não poderá conter essa chave, pois qualquer segredo embutido em um JAR pode ser extraído.

### Arquitetura recomendada para a chave

1. Administrador configura a chave no servidor ou em um pequeno backend confiável.
2. O servidor usa fingerprints/IDs para validar a entrada e obter metadados.
3. O manifesto contém somente `projectId`, `fileId`, hashes e dados públicos.
4. Quando o usuário aceita, o cliente pede ao serviço do servidor uma resolução daquele par exato.
5. O serviço usa a chave sem expô-la e retorna apenas o resultado permitido/URL temporária.

Alternativas possíveis, mas menos recomendadas:

- usuário fornecer sua própria chave localmente;
- servidor funcionar como relay do arquivo, sujeito a banda, termos e disponibilidade;
- usar um backend central do projeto Cuscuz.

A escolha final está registrada como decisão pendente.

### Identificação

- Calcular o fingerprint esperado pelo CurseForge para correspondência.
- Usar `POST /v1/fingerprints` para correspondência exata.
- Confirmar projeto, arquivo, loader, versão do Minecraft e hashes retornados.
- Usar SHA-1 publicado pela API e SHA-512 do lockfile para defesa adicional.

### Download e direito de distribuição

O endpoint oficial fornece a URL do arquivo exato. O campo `allowModDistribution` e a disponibilidade da URL devem ser respeitados. O [controle de distribuição do CurseForge](https://support.curseforge.com/en/support/solutions/articles/9000207877) permite que o autor bloqueie acesso por serviços de terceiros.

Quando a distribuição estiver desativada ou a URL não puder ser obtida:

- o mod não tentará reconstruir/burlar o endereço de download;
- o item será marcado como instalação manual;
- o usuário verá o projeto e arquivo necessários;
- a transação automática só continuará quando o arquivo correto for fornecido e validado.

### Lado do mod

A API CurseForge não deve ser tratada como fonte suficiente para afirmar que todo extra é client-side. Metadados do JAR, política do servidor e comportamento de canal Forge completarão a classificação.

## Mods presentes nas duas plataformas

Cada entrada pode oferecer mais de uma origem para o mesmo conteúdo. A seleção seguirá:

1. Preferência explícita do usuário/servidor.
2. Origem que contém o hash exato bloqueado.
3. Origem disponível e com distribuição autorizada.
4. Fallback para a outra origem apenas se o conteúdo tiver o mesmo hash esperado.

Arquivos diferentes publicados como a “mesma versão” não serão trocados silenciosamente. O lockfile deve tratar cada conteúdo como artefato distinto.

## Dependências

- O gerador do manifesto resolverá dependências obrigatórias antes da publicação.
- Dependências opcionais não serão instaladas automaticamente sem política explícita.
- Dependências incompatíveis serão registradas como conflito.
- O cliente executará o plano fechado; não fará resolução “latest” durante a conexão.
- Ciclos e duas versões do mesmo projeto bloquearão a publicação do manifesto.

## Disponibilidade e cache

- Cache de metadados separado por plataforma e ID imutável.
- Backoff exponencial com limite para 429/5xx.
- Respeito a `Retry-After` e headers de rate limit.
- Circuit breaker curto para evitar travar a interface quando uma plataforma estiver indisponível.
- Sem fallback para espelhos não autorizados.

## Configuração prevista do servidor

```text
manifestMode = generated | file
sourcePriority = modrinth, curseforge
curseforgeCredential = environment-variable-or-secret-file
allowManualFiles = false
unknownExtraPolicy = ask
manifestSigningKey = protected-file
```

Credenciais não serão aceitas no manifesto, enviadas pela rede ou exibidas em logs.

