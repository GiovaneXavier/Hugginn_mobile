# Roadmap — Huginn Mobile (NFC)

## P0 — Crítico (bloqueia uso em produção)

### Gestão de chaves HMAC
As chaves `QR_HMAC_KEY` e `TOKEN_HMAC_KEY` têm fallback hardcoded para dev. Em produção:
- Definir processo de rotação de chave (invalida todos os QRs ativos, requer redistribuição para apps)
- Documentar procedimento de emergência para revogação imediata

### Testes instrumentados de câmera e NFC
Os unit tests cobrem a lógica, mas o fluxo físico (câmera ML Kit, HCE respondendo ao leitor) nunca é exercido por um test runner. Adicionar ao menos 1 teste instrumentado de fumaça para cada fluxo.

---

## P1 — Alta prioridade (segurança e robustez)

### Splash screen com `core-splashscreen`
`installSplashScreen()` deve ser chamado antes de `setContent`. Atualmente o `LaunchedEffect` de Q32 deixa a tela preta por alguns frames enquanto `repository.hasCard()` roda em IO. Integrar `androidx.core:core-splashscreen` e manter o splash ativo até `startDestination != null`.

### Revogar HCE em `onDestroy`
`HuginnHCEService.isAuthorized` é limpo em `onExpire()` e `onAppBackground()`, mas não em `onDestroy()`. Se o processo for morto enquanto o HCE está ativo, o serviço pode continuar respondendo ao leitor.

### Limite de tentativas biométricas
`BiometricPrompt.onAuthenticationFailed()` está vazio. Adicionar contador com bloqueio temporário após N falhas consecutivas.

---

## P2 — Médio prazo (qualidade e experiência)

### Persistência de estado de erro em rotação de tela
`OnboardingStep.Error` é recriado ao rotacionar a tela. Usar `SavedStateHandle` no `OnboardingViewModel` para persistir a mensagem de erro.

### Monitoramento e observabilidade
Adicionar logging estruturado: falhas de HCE, erros de câmera, tentativas de validação de QR rejeitadas.

---

## P3 — Futuro / nice-to-have

### Multi-credencial
`CardStorage` suporta apenas 1 `HuginnCard`. Para funcionários com acesso a múltiplos sistemas, implementar lista de credenciais com alternância de ativo.

### Android 14+ NFC HCE improvements
Android 14 introduziu melhorias no sistema de prioridade de serviço HCE. Avaliar migração para o novo modelo de preferência.
