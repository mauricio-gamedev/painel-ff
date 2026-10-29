# Yoru Aim Lab — Galaxy A06 4G

Painel Android independente em português, com tema anime dark, para perfis de sensibilidade, diagnóstico orientativo de arrasto e preparação do Galaxy A06 antes de jogar.

## Recursos da versão 2.2

- Tema Yoru anime dark com neon rosa/violeta.
- Perfis locais Equilibrado, Capa suave, Precisão e Personalizado.
- Controles de Geral, Ponto vermelho, 2×, 4×, AWM, Olhadinha e botão de tiro.
- Assistente conservador para “mira pesada”, “passa do alvo” e “tremedeira”.
- Teste de traço/toque dentro do aplicativo.
- Assistente externo Android arrastável com sliders reais para Geral, Ponto vermelho, 2×, 4×, AWM, Olhadinha e tamanho do botão de tiro; salva perfis localmente e oferece atalho para abrir Free Fire/FF MAX.
- Diagnóstico de RAM disponível, bateria, estado térmico e armazenamento livre quando o Android oferece esses dados.
- Atalhos para configurações oficiais do Android (bateria, tela, notificações e detalhes do app do jogo).
- Importação/exportação JSON e persistência local.
- GitHub Actions valida JavaScript/XML e compila APK de depuração.

## Ativar painel flutuante

Toque em Ativar / atualizar painel flutuante. O Android poderá abrir Exibir sobre outros apps; conceda a permissão para o Yoru Aim Lab e volte ao aplicativo, depois toque em Ativar novamente. O serviço mantém uma notificação enquanto o painel estiver ativo. Arraste a bolha para reposicionar, toque para abrir, recolha ou pare o serviço dentro do painel.

## Limites técnicos

O assistente externo agora permite alterar e salvar os valores do próprio perfil com controles deslizantes, alternar predefinições, copiar os valores e abrir o jogo por cima do Android. Ele não escreve parâmetros internos do Free Fire, não injeta bibliotecas, não filtra eventos de toque e não envia movimentos automáticos; aplique os valores manualmente no jogo. A análise de traço mede somente gestos na área de treino do próprio Yoru Aim Lab. Para testar a calibração no jogo, aplique os valores manualmente e mude uma variável por vez.

O diagnóstico do sistema mostra sinais disponíveis no Android; não promete ganho artificial de FPS. Os atalhos abrem configurações oficiais e não encerram serviços nem limpam RAM agressivamente.

## Compilar sem Termux

1. Abra a aba Actions deste repositório.
2. Execute Build FF Aim Lab APK ou envie um commit para a branch main.
3. Baixe o artefato FF-Aim-Lab-A06-v2-debug-apk após o workflow concluir com sucesso.
4. Extraia o ZIP e instale app-debug.apk no Android para testar.

## Arte de fundo e créditos

- Wallpaper de fundo: **Neon Cyberpunk Anime Girl**, publicado por Backiee: https://backiee.com/wallpaper/neon-cyberpunk-anime-girl/363313
- A imagem é carregada pela URL HTTPS do provedor e exige permissão de internet; o app não envia perfis nem dados do dispositivo para esse site. Se a URL deixar de funcionar ou o dispositivo estiver offline, o fundo remoto não aparece e a interface escura continua utilizável.
- Consulte os termos do autor/provedor antes de redistribuir ou usar comercialmente essa imagem.

## Privacidade

Perfis ficam locais ou em arquivos escolhidos pelo usuário. A permissão de internet existe apenas para carregar o wallpaper remoto; a permissão de sobreposição só é usada após ação explícita do usuário. O app não envia os perfis para servidores. Projeto independente, sem afiliação com a Garena.
