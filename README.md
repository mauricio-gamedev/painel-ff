# FF Aim Lab — Galaxy A06 4G

Painel Android independente, em português, para organizar perfis de sensibilidade, treinar movimentos de arrasto e preparar o Galaxy A06 antes de jogar.

## Recursos

- Perfis Equilibrado, Puxada de capa, Precisão e Personalizado.
- Ajustes locais de Geral, Ponto vermelho, 2×, 4×, AWM, Olhadinha e botão de tiro.
- Treino local de alvo e diagnóstico orientativo.
- Checklist de desempenho com persistência.
- Importação/exportação JSON de perfis por seletor de arquivos Android.
- Detecção e abertura das edições reconhecidas do Free Fire e Free Fire MAX.
- Resumo do dispositivo usando informações expostas pelo Android.
- Projeto Kotlin + Android WebView, sem login ou permissões de root.
- GitHub Actions para validar assets e compilar um APK de depuração.

## Limites de integração

O projeto não presume uma API pública do Free Fire para escrever opções internas de sensibilidade. A camada Android pode detectar a presença das edições conhecidas e solicitar que o Android as abra; não lê nem modifica a memória do jogo, não injeta bibliotecas, não edita arquivos do jogo e não executa macros.

## Compilar sem Termux

1. Abra a aba **Actions** deste repositório.
2. Execute **Build FF Aim Lab APK**, ou envie um commit para `main`.
3. Quando o workflow terminar, baixe o artefato `FF-Aim-Lab-A06-v2-debug-apk`.
4. Extraia o ZIP do artefato e instale `app-debug.apk` no Android para teste.

O APK de depuração é destinado a testes. Ainda precisa de validação em aparelho real antes de ser considerado estável.

## Privacidade

Os perfis são guardados localmente ou em arquivos escolhidos pelo usuário. O aplicativo não solicita root, acesso à internet, serviço de acessibilidade ou sobreposição de tela. Projeto independente, sem afiliação com a Garena.

## Estrutura

- `app/`: aplicativo Android nativo e assets locais.
- `index.html`, `manifest.json`, `sw.js`: versão web/PWA.
- `.github/workflows/build-apk.yml`: validação e build de APK.
- `CHANGELOG.md`: histórico de alterações.
