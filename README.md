# Hugginn Mobile (NFC)

App Android de **credencial de acesso corporativo via NFC HCE** (Host Card Emulation). Após autenticação biométrica, emite por NFC um token dinâmico assinado que o leitor de portaria (**Heimdall**) valida.

```
Odin Admin → Hugginn Mobile (NFC) → [NFC HCE] → Heimdall → Backend REST
```

Parte do ecossistema de controle de acesso SRBR (3 apps + módulo compartilhado).

---

## Documentação do ecossistema

| Documento | Conteúdo |
|---|---|
| [INTEGRATION_GUIDE.md](../INTEGRATION_GUIDE.md) | Contrato de comunicação: token, Base64Url, nonce, HMAC, vetores de teste |
| [BUILD_CICD.md](../BUILD_CICD.md) | Build, injeção de chaves HMAC, CI/CD, certificate pinning |
| [ARCHITECTURE.md](../ARCHITECTURE.md) | Arquitetura, módulo `:core-credential`, design system de Slots |

> O guia local [CLAUDE.md](CLAUDE.md) contém restrições de build, mas a seção de formato de token/nonce está **desatualizada** — prevalecem os docs do ecossistema acima.

---

## Papel deste repositório

| Aspecto | Valor |
|---|---|
| Stack | Kotlin + Jetpack Compose + MVVM + Hilt |
| minSdk / target | 26 / 34 |
| Canal de saída | NFC HCE (`HuginnHCEService`, AID `F0 53 52 42 52 00`) |
| Gerador de token | `NfcTokenGenerator` |
| Slot visual / motion | `NfcRippleComposable` / `ROTATE_TO_PORTRAIT` |
| Módulo compartilhado | `com.srbr.huginn:credential:0.3.0-SNAPSHOT` (mavenLocal) |

---

## Build rápido

**1.** Publique o módulo compartilhado (no repo `huginn-core-credential`):

```bash
./gradlew :core-credential:publishToMavenLocal
```

**2.** Configure as chaves em `local.properties` (nunca commitar):

```properties
HUGINN_QR_HMAC_KEY=...
HUGINN_TOKEN_HMAC_KEY=...
```

> Precedência `-P → local.properties → env var`. Sem a chave, **release aborta**; debug usa `DEV_ONLY_FALLBACK_NEVER_RELEASE`. Detalhes em [BUILD_CICD.md §3](../BUILD_CICD.md#3-injeção-de-chaves-hmac).

**3.** Compile e teste:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew testDebugUnitTest
```
