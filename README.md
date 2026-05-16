# SecureMind Hub

A Spring Boot web application that combines four cybersecurity and privacy modules into a single platform. Users register with password + optional keystroke biometrics, and access a dashboard of AI-powered tools.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring Security 6 |
| Templates | FreeMarker (`.ftlh`) |
| Database | MySQL 8 (Hibernate / Spring Data JPA) |
| AI | Google Gemini 2.5 Flash (REST API) |
| Auth | Form login + Google OAuth2 + BCrypt |
| Build | Maven |
| Server | Embedded Tomcat, port `2690` |

---

## Prerequisites

- Java 21+
- MySQL 8 running locally
- A Gemini API key (already configured in `application.properties`)
- Gmail SMTP credentials (already configured)

---

## Setup & Run

**1. Create the database**

```sql
CREATE DATABASE hackathon_test_2;
```

**2. Configure credentials** in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hackathon_test_2
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
gemini.api.key=YOUR_GEMINI_KEY
```

**3. Run**

```bash
mvn spring-boot:run
```

App starts at `http://localhost:2690`

> Hibernate runs with `ddl-auto=update` — all tables are created automatically on first run.

---

## Registration & Login

### Registration (`/registration`)

- Fill in name, email, password (min 8 chars)
- **Optional:** enable the biometric toggle to record 5 typing sessions — the system averages your dwell/flight timings and stores a behavioral fingerprint
- On submit, a **10-word seed phrase** is generated and shown once — write it down, it is the recovery key
- A confirmation email is sent; click the link to activate the account

### Login (`/login`)

- Standard email + password form
- **Keystroke biometrics** (if enrolled): type the phrase shown, the system compares your rhythm against the stored profile and shows a similarity score
- **Seed phrase recovery**: if biometrics fails, enter your 10 words to confirm identity
- **Google OAuth2**: one-click sign-in via Google

---

## Modules

### 🛡 Module 1 — Media Shield (`/media-analysis`)

Detects AI-generated text, fake news, and propaganda in any article or post.

**How it works:**

1. Paste text or upload a `.txt` / `.html` / `.md` file
2. The backend sends it to Gemini 2.5 Flash with Google Search grounding enabled
3. Gemini applies a fixed scoring formula (no randomness):
   - **AI Bias** — +10 per signal (uniform sentences, no personal voice, generic transitions)
   - **Fake News** — +15 per issue (unverifiable claim, contradicts search results, missing source)
   - **Propaganda** — +12 per technique (us-vs-them, appeal to fear, bandwagon, cherry-picking)
4. Results are **cached by SHA-256 hash** of the input — the same text always returns the same scores

**Endpoints:**
```
GET  /media-analysis          → page
POST /media-analysis/analyze  → analyze plain text (JSON)
POST /media-analysis/upload   → analyze uploaded file (JSON)
```

---

### 🔒 Module 2 — Query Proxy (`/query-proxy`)

Lets users query AI without exposing personal data — PII is stripped before the request leaves the server.

**How it works:**

1. Type a query (may contain emails, phones, card numbers, etc.)
2. The proxy scans with regex patterns and replaces each match with a placeholder: `[EMAIL_1]`, `[PHONE_2]`, etc.
3. The sanitized query is sent to Gemini
4. Placeholders in the AI response are restored to original values
5. The UI shows: original query, sanitized query, detected PII tokens, and the AI response (with markdown rendering)

**Detected PII types:** email, phone, credit/debit card, IBAN, passport number, IP address

**Endpoints:**
```
GET  /query-proxy           → page
POST /query-proxy/ask       → full pipeline: sanitize → AI → restore (JSON)
POST /query-proxy/sanitize  → preview only: sanitize without calling AI (JSON)
```

---

### 🖼 Module 3 — Secret Frame (`/steganography`)

Hides encrypted text or files inside AI-generated images using AES-256 + LSB steganography.

**Encrypt flow:**

1. Enter secret text or upload a file
2. Choose an image style (Cyberpunk, Watercolor, Oil Painting, etc.) or upload your own cover image
3. The payload is AES-256-CBC encrypted with PBKDF2 key derivation
4. Gemini generates a themed cover image (or uses the uploaded one)
5. The ciphertext is embedded into the image via LSB (least significant bit) of each pixel's RGB channels
6. A **one-time key token** is issued — valid for **30 seconds only**, then destroyed forever
7. The stego-PNG is available to download

**Decrypt flow:**

1. Upload the stego-PNG
2. Enter the key token (must be used within 30 s of encryption)
3. The LSB payload is extracted and AES-decrypted

**Endpoints:**
```
GET  /steganography                → page
POST /steganography/encrypt        → encrypt + embed → returns stego image + key
POST /steganography/decrypt        → extract + decrypt → returns plaintext or file
GET  /steganography/key-status     → check key TTL countdown
```

---

### 🧬 Module 4 — Key Shield (`/biometrics`)

Behavioral biometrics based on keystroke dynamics — identifies users by *how* they type, not *what* they type.

**Metrics captured:**
- **Dwell time** — how long each key is held down
- **Flight time** — gap between releasing one key and pressing the next
- **Typing speed** (CPM), error rate (backspace ratio), burst ratio
- **Digraph timings** — timing patterns for the most frequent key pairs
- **Fingerprint hash** — SHA-256 of the combined timing profile (first 16 chars shown)

**Three tabs:**

| Tab | Description |
|---|---|
| Training | Type the same phrase 5 times. The system averages all sessions into a stable baseline profile and optionally saves it to the DB. |
| Verification | Type the phrase once. Compared against the baseline — returns `STRONG_MATCH`, `MATCH`, `WEAK_MATCH`, or `NO_MATCH` with a similarity %. |
| Analysis | Type anything freely and see your real-time biometric profile. |

**Verification on login:** when a user with a saved biometric profile logs in, the login page captures keystrokes from the bio typing area and automatically runs a verify call.

**Endpoints:**
```
GET  /biometrics                  → page
POST /biometrics/analyze          → compute profile from events (no storage)
POST /biometrics/enroll           → save baseline profile for current user
POST /biometrics/verify           → compare session against baseline
POST /biometrics/verify-seed      → verify 10-word seed phrase (public, no auth)
GET  /biometrics/profile/{userId} → get stored profile metadata
```

---

## Security

- **Passwords** hashed with BCrypt (strength 8)
- **CSRF** enabled with explicit exemptions for all JSON API endpoints
- **Session** stored server-side; `JSESSIONID` deleted on logout
- **Biometric profile** stored as JSON in `LONGTEXT` column in the `users` table
- **Seed phrase (mnemonic)** stored in `mnemonic_phrase` column (plaintext — treat as a recovery token, not a secret)
- **Gemini API key** — set in `application.properties`; do not commit to public repos
- **Steganography keys** — stored in-memory only, never persisted, auto-expire after 30 s

---

## Project Structure

```
src/main/java/com/example/shop/
├── configurations/
│   ├── AppConfig.java              # RestTemplate bean
│   ├── OAuth2UserServiceImpl.java  # Google OAuth2 user creation
│   └── SecurityConfig.java         # Spring Security rules
├── controller/
│   ├── KeystrokeBiometricsController.java
│   ├── MediaAnalysisController.java
│   ├── QueryProxyController.java
│   ├── SteganographyController.java
│   ├── UserController.java         # Registration, login, confirm
│   └── ...
├── models/
│   └── User.java                   # email, password, mnemonicPhrase,
│                                   # biometricProfileJson, roles, avatar
├── services/
│   ├── KeystrokeBiometricsService.java  # analyze, verify, reconstructFromJson
│   ├── MediaAnalysisService.java        # Gemini + Google Search + SHA-256 cache
│   ├── QueryProxyService.java           # PII regex sanitize + Gemini
│   ├── SteganographyService.java        # AES-256 + LSB + Gemini image gen
│   └── UserService.java                 # createUser, processAndSetBiometricProfile
└── repositories/
    └── UserRepository.java         # findByEmail, findByMnemonic, enableUser

src/main/resources/
├── application.properties
├── static/
│   ├── game.css                    # Main stylesheet (dark theme, design system)
│   └── script.js
└── templates/
    ├── login.ftlh
    ├── registration.ftlh
    ├── main.ftlh                   # Dashboard / home
    ├── media_analysis.ftlh         # Module 1
    ├── query_proxy.ftlh            # Module 2
    ├── steganography.ftlh          # Module 3
    └── biometrics.ftlh             # Module 4
```

---

## Known Limitations

- Biometric profiles are stored **both** in-memory (for fast verify) and in the DB. On server restart the in-memory store is lost; the service reconstructs the baseline from the DB JSON automatically on the next verify call.
- The steganography key store is **in-memory only** — a server restart during an active 30 s window will invalidate all pending keys.
- Media analysis results are **cached in-memory** (SHA-256 keyed). Cache is cleared on restart.
- `mnemonic_phrase` is stored in plaintext. If your threat model requires it encrypted, add a `@Convert` attribute encryption layer.
