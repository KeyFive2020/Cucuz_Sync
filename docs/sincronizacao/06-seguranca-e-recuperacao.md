# 6. Segurança, privacidade e recuperação

## Modelo de ameaça

Instalar um mod equivale a autorizar código Java a executar na próxima inicialização. Um servidor malicioso poderia tentar transformar sincronização em execução remota de código. Por isso, o cliente tratará manifesto, nomes, tamanhos e mensagens recebidas como dados não confiáveis.

O consentimento do usuário é necessário, mas sozinho não basta: a implementação também aplicará limites e verificações técnicas.

## Regras obrigatórias de download

- Aceitar somente `https`.
- Rejeitar credenciais embutidas na URL.
- Permitir somente domínios oficiais/resolvidos pelas integrações aprovadas.
- Validar cada redirecionamento; não apenas o host inicial.
- Não aceitar `file:`, `jar:`, UNC, localhost ou endereços de rede privada vindos do servidor.
- Impor timeout, tamanho máximo individual, tamanho total e número máximo de arquivos.
- Escrever apenas em staging com nome gerado localmente.
- Calcular hashes durante e após o download.
- Confirmar que o arquivo é ZIP/JAR válido antes de planejar ativação.
- Não executar classes, scripts ou instaladores baixados.

## Validação de caminhos

- Canonicalizar a raiz da instância uma vez.
- Resolver todos os destinos de forma relativa à raiz autorizada.
- Rejeitar `..`, caminhos absolutos, ADS do Windows e nomes reservados.
- Não seguir junctions ou links simbólicos em `mods`, staging, backup ou quarentena.
- Sanitizar nomes; a identidade real será o hash, não o filename.
- Usar criação exclusiva para evitar colisões e TOCTOU.

## Confiança no servidor

- Primeiro contato: exibir servidor, chave pública, número de mods e tamanho total.
- Após aprovação: guardar confiança por `serverId` e endereço.
- Mudança de chave ou `serverId`: bloquear automação e pedir nova confirmação.
- Manifesto expirado, assinatura inválida ou revisão regressiva: rejeitar.
- A assinatura protege integridade/continuidade, não prova reputação.

## Transparência para o usuário

Cada ação mostrará:

- nome e identificadores do projeto;
- autor/publicador quando fornecido pela plataforma;
- plataforma de origem;
- versão e arquivo exatos;
- hash abreviado com opção de copiar completo;
- por que o servidor exige ou bloqueia o mod;
- ação local e caminho relativo de backup/quarentena.

O botão final não permitirá aceitar uma lista diferente da exibida.

## Transações atômicas

Estados previstos:

1. `PLANNED`: plano validado, nenhuma mudança aplicada.
2. `DOWNLOADING`: arquivos somente em staging.
3. `VERIFIED`: todos os downloads foram verificados.
4. `READY_TO_APPLY`: journal persistido e jogo pode fechar.
5. `APPLYING`: aplicador move arquivos em ordem registrada.
6. `APPLIED`: novo conjunto está ativo, backups preservados.
7. `CONFIRMED`: cliente iniciou e o inventário corresponde ao manifesto.
8. `ROLLED_BACK`: estado anterior restaurado.

Somente após `CONFIRMED` backups antigos poderão ser limpos, mediante política de retenção.

## Quarentena em vez de exclusão

Mods extras incompatíveis serão movidos para uma pasta por servidor/perfil. A interface usará o termo “desativar” como ação padrão. Exclusão permanente, se algum dia adicionada, exigirá uma ação separada e confirmação específica.

Esse comportamento protege o jogador ao alternar de servidor, permite rollback e reduz risco de classificação errada.

## Recuperação

- Journal será atualizado por substituição atômica e `fsync` quando disponível.
- Antes de aplicar, o utilitário confirmará espaço livre e hashes novamente.
- Em falha intermediária, operações concluídas serão revertidas na ordem inversa.
- Se o computador desligar, a próxima execução detectará journal incompleto.
- O cliente nunca tentará entrar automaticamente enquanto houver recuperação pendente.
- Haverá opção de exportar um relatório sem segredos.

## Segredos

- Chave da API CurseForge somente no servidor/backend.
- Chave privada de assinatura somente no servidor.
- Cliente guarda apenas chave pública e preferências.
- Arquivos secretos terão permissão restrita quando o sistema operacional permitir.
- Logs mascararão tokens, query strings assinadas e headers.
- Nenhum token será transmitido no protocolo Minecraft.

## Privacidade

- Sem telemetria no MVP.
- O servidor não recebe a lista completa de mods extras por padrão.
- A comparação principal ocorre localmente.
- O Modrinth/CurseForge verá requisições normais de API/download e endereço IP, conforme seus próprios serviços.
- Histórico local poderá ser apagado pelo usuário sem afetar a instalação de mods.
- Relatórios de erro só serão enviados por ação explícita.

## Abusos e limites

- Limite inicial configurável sugerido: 500 entradas no manifesto.
- Tamanho máximo de manifesto antes da descompressão.
- Sem compressão de payload não limitada.
- Máximo total de download exibido e confirmado.
- Rejeição de duplicatas de mod ID e arquivos com nomes conflitantes.
- Rate limit para tentativas de preflight e resolução de URLs.
- Mensagens do servidor nunca serão renderizadas como HTML/links ativos sem sanitização.

