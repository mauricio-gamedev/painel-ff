# Yoru Aim Lab — Galaxy A06 4G

Painel Android independente em português, com tema anime dark, para perfis de sensibilidade, diagnóstico orientativo de arrasto e preparação do Galaxy A06 antes de jogar.

## Recursos da versão 2.1

- Tema Yoru anime dark com neon rosa/violeta.
- Perfis locais Equilibrado, Capa suave, Precisão e Personalizado.
- Controles de Geral, Ponto vermelho, 2×, 4×, AWM, Olhadinha e botão de tiro.
- Assistente conservador para “mira pesada”, “passa do alvo” e “tremedeira”.
- Teste de traço/toque dentro do aplicativo.
- Painel flutuante Android arrastável, com perfis rápidos, valores, copiar e recolher/parar.
- Diagnóstico de RAM disponível, bateria, estado térmico e armazenamento livre quando o Android oferece esses dados.
- Atalhos para configurações oficiais do Android (bateria, tela, notificações e detalhes do app do jogo).
- Importação/exportação JSON e persistência local.
- GitHub Actions valida JavaScript/XML e compila APK de depuração.

## Ativar painel flutuante

Toque em Ativar / atualizar painel flutuante. O Android poderá abrir Exibir sobre outros apps; conceda a permissão para o Yoru Aim Lab e volte ao aplicativo, depois toque em Ativar novamente. O serviço mantém uma notificação enquanto o painel estiver ativo. Arraste a bolha para reposicionar, toque para abrir, recolha ou pare o serviço dentro do painel.

## Limites técnicos

O mini painel mostra e copia perfis, mas não escreve parâmetros internos do Free Fire, não injeta bibliotecas, não filtra eventos de toque e não envia movimentos automáticos. A análise de traço mede somente gestos na área de treino do próprio Yoru Aim Lab. Para testar a calibração no jogo, aplique os valores manualmente e mude uma variável por vez.

O diagnóstico do sistema mostra sinais disponíveis no Android; não promete ganho artificial de FPS. Os atalhos abrem configurações oficiais e não encerram serviços nem limpam RAM agressivamente.

## Compilar sem Termux

1. Abra a aba Actions deste repositório.
2. Execute Build FF Aim Lab APK ou envie um commit para a branch main.
3. Baixe o artefato FF-Aim-Lab-A06-v2-debug-apk após o workflow concluir com sucesso.
4. Extraia o ZIP e instale app-debug.apk no Android para testar.

## Privacidade

Perfis ficam locais ou em arquivos escolhidos pelo usuário. O app não solicita root nem permissão de internet. A permissão de sobreposição só é usada após ação explícita do usuário. Projeto independente, sem afiliação com a Garena.
