# CLAUDE.md — Huginn Mobile (NFC)

Este arquivo orienta o Claude Code ao trabalhar neste repositório.

## Comandos de Build e Teste

```bash
# Build
./gradlew assembleDebug          # APK de debug
./gradlew assembleRelease        # APK de release (com ProGuard)
./gradlew build                  # Todos os variants

# Testes
./gradlew test                   # Todos os unit tests
./gradlew testDebugUnitTest      # Só variant debug
./gradlew connectedAndroidTest   # Testes de instrumentação (requer emulador/device)

# Lint
./gradlew lint
```

## Configuração de Segredos

Antes de compilar, configure as chaves HMAC em `local.properties` (nunca commitar este arquivo):

```properties
HUGINN_QR_HMAC_KEY=sua_chave_aqui
HUGINN_TOKEN_HMAC_KEY=sua_chave_token_aqui
```

Sem essas propriedades, o build usa os valores padrão de desenvolvimento
(`SRBR_HUGINN_ODIN_SECRET_2024` e `SRBR_HEIMDALL_TOKEN_SECRET_2024`).

## Arquitetura

**Stack**: Kotlin + Jetpack Compose + MVVM + Hilt
**SDK mínimo**: 26
**Propósito**: App de credencial de acesso corporativo via **NFC HCE** (Host Card Emulation).

### Fluxo principal

1. **Onboarding**: Usuário escaneia QR de registro emitido pelo Odin (sistema admin) → valida assinatura HMAC → salva credencial criptografada
2. **Card**: Usuário autentica com biometria → NFC HCE ativado por 30 s → leitor NFC do Heimdall lê token dinâmico

### Estrutura de pacotes

```
core/security/     → Criptografia e modelos de domínio
  HuginnCard         Data class da credencial
  QRValidator        Valida QRs de registro (HMAC-SHA256, expiração, nonce)
  NfcTokenGenerator  Gera tokens NFC: deviceId|empId|sysId|ts|nonce.sig
  DeviceIdentity     SHA-256(salt + ANDROID_ID), exibido como "SRBR-XXXX-YYYY"
  HuginnHCEService   Serviço NFC HCE; estado em companion object com @Volatile

core/storage/      → Persistência criptografada
  CardStorage        EncryptedSharedPreferences (AES-256-GCM) + rastreamento de nonces
  CardRepository     Fachada sobre CardStorage (facilita mock nos testes)

feature/onboarding/→ Fluxo de cadastro
  OnboardingViewModel  sealed class OnboardingStep: Welcome → Scanning → Validating → Error | Success
  OnboardingScreen     Câmera ML Kit, callbacks de permissão/erro da câmera

feature/card/      → Exibição da credencial
  CardViewModel      locked → (biometria) → unlocked 30 s + auto-lock ao expirar/background
  CardScreen         DisposableEffect observa ON_STOP → onAppBackground()

ui/components/     → Composables reutilizáveis
  HuginnCardComposable  Card 3D com flip
  NfcRippleComposable   Animação de ondas (1 rememberInfiniteTransition com múltiplos valores)

di/AppModule.kt    → Hilt: provê QRValidator, NfcTokenGenerator, CardRepository, DeviceIdentity
MainActivity.kt    → Orquestra câmera (ML Kit), BiometricPrompt e navegação
NavGraph.kt        → Compose Navigation; passa callbacks de câmera/biometria
```

## Decisões Técnicas Importantes

| Decisão | Motivo |
|---|---|
| `Base64.URL_SAFE or NO_WRAP or NO_PADDING` | JS strip padding; ambos os lados devem produzir 43 chars sem `=` |
| `MessageDigest.isEqual()` para HMAC | Comparação em tempo constante; previne timing attacks |
| `@Volatile` em todos os campos do companion object de `HuginnHCEService` | Thread NFC lê; main thread escreve — sem @Volatile há race condition |
| `SecureRandom` para nonces | `kotlin.random.Random` não é criptograficamente seguro |
| `DeviceIdentity` cached com `by lazy` | Evita SHA-256 + Settings read a cada chamada |
| `FLAG_SECURE` no Window | Impede screenshots e gravações de tela |
| `repository.hasCard()` em `Dispatchers.IO` via `LaunchedEffect` | `EncryptedSharedPreferences` não deve bloquear a main thread |
| `requireDeviceUnlock="true"` no apduservice.xml | NFC HCE só responde com dispositivo desbloqueado |
| `NfcTokenGenerator` extraído de `HuginnHCEService` | Testabilidade; HuginnHCEService usa Android framework, não testável em unit tests |
| `hasCard()` = `contains(KEY) && loadCard() != null` | Evita falso-positivo se a chave existe mas a deserialização falha |
| `deleteCard()` remove também `KEY_NONCES` | Evita vazamento de nonces de credenciais anteriores |

## Formato do Token NFC

```
deviceId|employeeId|systemId|timestamp_unix|nonce.hmac-sha256-base64url
```
- `timestamp` em segundos (Unix)
- `nonce` = `SecureRandom().nextLong() and 0xFFFFFFL` (6 hex digits)
- `hmac` = HMAC-SHA256 do payload antes do `.`, Base64url sem padding (43 chars)
- Chave: `BuildConfig.TOKEN_HMAC_KEY`

## Testes

Organização por pacote (espelha a estrutura de produção):

```
com.srbr.huginn.core.security/
  QRValidatorTest         — payload, adulteração, HMAC, formato Base64url, vetor cross-platform
  NfcTokenGeneratorTest   — formato, unicidade, timestamp

com.srbr.huginn.core.storage/
  CardStorageTest         — CRUD, FIFO de nonces (máx. 50) — usa Robolectric

com.srbr.huginn.feature.card/
  CardViewModelTest       — estados (lock/unlock/expire/background), countdown, auto-lock

com.srbr.huginn.feature.onboarding/
  OnboardingViewModelTest — fluxo completo, nonce reutilizado, permissão/câmera negada
```

**Ferramentas**: JUnit 4 + MockK + Turbine + `StandardTestDispatcher` + Robolectric (para CardStorage)

### Vetor de teste cross-platform (QRValidatorTest)

O valor esperado foi derivado via Node.js:
```bash
node -e "const c=require('crypto'); \
  console.log(c.createHmac('sha256','TEST_SECRET_KEY') \
  .update('1|REG|1700000000|1700003600|fixed-nonce|EMP001|Test User|SYS001') \
  .digest('base64').replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,''))"
# Resultado: eSJdyv9cvTX3kst9JbzhJJoiD_P60Svb2UhaXPgVbBE
```

## Diferença em relação ao Hugginn_mobile_qr_code

| Aspecto | Este app (NFC) | Hugginn_mobile_qr_code (QR) |
|---|---|---|
| Canal de saída | NFC HCE (`HuginnHCEService`) | QR code dinâmico na tela |
| Token gerado por | `NfcTokenGenerator` | `QrTokenGenerator` |
| Animação | `NfcRippleComposable` | `QrCodeComposable` |
| CardViewModel unlock | Autoriza `HuginnHCEService` | Gera e exibe `qrToken` |
