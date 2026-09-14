# 4. Experiência do usuário

## Entrada sem diferenças

O jogador clica em entrar, vê uma indicação breve “Verificando mods…” e a conexão continua sem outra ação. A verificação não deve piscar uma tela quando durar pouco.

## Tela de revisão

Quando houver diferenças, a conexão será pausada e aparecerá uma tela com:

- nome e endereço do servidor;
- revisão do conjunto solicitado;
- quantidade e tamanho total dos downloads;
- aviso de que será necessário reiniciar;
- abas/listas de ausentes, atualizações, extras e problemas;
- fonte de cada download (Modrinth ou CurseForge);
- nome, versão atual, versão solicitada, tamanho e motivo;
- estado de confiança: verificado, incerto ou manual;
- botões `Cancelar`, `Ver detalhes` e `Sincronizar e fechar`.

Nenhuma caixa virá escondida ou pré-confirmada. O botão de sincronizar só ficará ativo quando o plano for completo e válido.

## Tratamento de categorias

### Mods ausentes

Exibir:

- nome e ícone quando obtidos com segurança;
- arquivo exato solicitado;
- por que é obrigatório;
- origem escolhida e alternativa disponível;
- dependências incluídas no plano.

### Versões divergentes

Exibir versão/arquivo atual e destino. A versão atual será movida para backup, não sobrescrita irreversivelmente.

### Extras client-side

Serão mostrados como “Mantidos — somente cliente”. Nenhuma ação será marcada.

### Extras incompatíveis

Serão mostrados como “Precisam ser desativados para este servidor”. O usuário verá evidência e destino da quarentena antes de confirmar.

### Extras desconhecidos

Serão mostrados separadamente, com linguagem honesta: “Não foi possível provar se este mod é somente cliente”. A política inicial recomendada é perguntar e não selecionar desativação por padrão.

## Confirmação final

Antes do download, a tela final mostrará um resumo imutável:

- arquivos que serão adicionados;
- arquivos que serão substituídos;
- arquivos que serão movidos para quarentena;
- espaço necessário e tamanho do download;
- pasta de backup;
- origem de cada arquivo;
- necessidade de reinício.

Se o manifesto mudar enquanto a tela estiver aberta, o plano será invalidado e recalculado; o usuário terá de revisar novamente.

## Progresso

- Progresso por arquivo e total.
- Estado atual: resolvendo, baixando, verificando ou preparando reinício.
- Botão de cancelar até o journal entrar na fase de aplicação.
- Mensagens específicas para falta de espaço, indisponibilidade, hash incorreto e limitação da plataforma.
- Downloads completos e verificados podem permanecer em cache; arquivos parciais não entram em `mods/`.

## Reinício e retorno

No MVP:

1. O usuário confirma `Fechar o jogo e aplicar`.
2. O Minecraft encerra normalmente.
3. O aplicador externo executa a transação.
4. Na próxima abertura, aparece o resultado.
5. Um botão `Reconectar ao servidor` usa o endereço salvo.

Relançar automaticamente CurseForge, Modrinth App ou outro launcher tem comportamentos diferentes e ficará para uma fase posterior.

## Falhas e recuperação

- Download recusado: nada é aplicado; indicar qual arquivo precisa ser obtido manualmente.
- Hash incorreto: apagar o arquivo temporário, bloquear a transação e registrar origem.
- Falta de espaço: interromper antes de fechar o jogo.
- Aplicação interrompida: detectar journal na próxima abertura e oferecer rollback.
- Servidor ficou offline: manter os downloads verificados, sem mudar a lista novamente.
- Conexão ainda rejeitada: mostrar comparação pós-reinício e opção de restaurar o perfil anterior.

## Acessibilidade e localização

- Textos em `pt_br` e `en_us` desde o MVP.
- Não depender apenas de cor para indicar risco.
- Listas navegáveis por teclado.
- Botões com rótulos completos e tooltips.
- Mensagens copiáveis para suporte.
- Valores de tamanho formatados e nomes longos truncados apenas visualmente.

