# Changelog

## 0.3.0 - 2026-09-14

- Separa os artefatos Forge 1.20.1 em JAR de cliente e JAR de servidor.
- Reconstrói e substitui o manifesto do servidor a cada cinco minutos.
- Descobre arquivos pela Modrinth usando SHA-1 e pelo CurseForge usando fingerprint Murmur2 como fallback.
- Exibe botão explícito para instalar mods e mostra a plataforma de cada alteração.
- Mantém a última publicação válida se uma atualização exceder o limite do Server List Ping.

## 0.2.0 - 2026-09-14

- Transporta o manifesto comprimido por um Server List Ping especial e mínimo, usando a porta do Minecraft sem alterar o ping normal do Forge.
- Mantém o endpoint HTTP como fallback opcional.
- Valida limite descompactado e SHA-256 antes de analisar o manifesto.
- Melhora o diagnóstico quando nenhum transporte está disponível.

## 0.1.0 - 2026-09-13

- Primeiro MVP Forge 1.20.1.
