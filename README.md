# 🏦 PDF-Banking Assistant – KI-gestützte Finanzanalyse aus Bank-PDFs

[![Java](https://shields.io)](https://adoptium.net/)
[![Spring Boot](https://shields.io)](https://spring.io/)
[![Groq](https://shields.io)](https://groq.com/)
[![License](https://shields.io)](LICENSE)
[![PRs](https://shields.io)](https://github.com/your-username/pdf-banking-assistant/pulls)

Ein intelligenter, produktionsreifer Finanzassistent auf Basis einer LLM-first-Architektur. Das System extrahiert strukturierte Transaktionsdaten aus unstrukturierten Bank-Kontoauszügen (PDF) und ermöglicht komplexe, kontextbezogene Analysen über ein intuitives Web-Interface mittels natürlicher Sprache.

---

## 📌 Problemstellung

Klassische regel- oder Regex-basierte Parser scheitern systematisch an der Varianz moderner Bank-Kontoauszüge. Layout-Änderungen, mehrzeilige Verwendungszwecke und dynamische Tabellenstrukturen führen zu hohem Wartungsaufwand und volatilen Extraktionsraten. Die manuelle Aufbereitung ist zeitintensiv, fehleranfällig und nicht skalierbar.

**Die Lösung:** Dieses Projekt implementiert eine robuste **LLM-first-Pipeline**. Über ein seitenweises, intelligentes Chunking und semantisches Parsing werden volatile Textwüsten deterministisch in strukturierte JSON-Entitäten überführt. Ein integrierter Chatbot ermöglicht im Anschluss deklarative Abfragen über den gesamten Transaktionskorpus.

---

## ✨ Kernfunktionen

* **📄 Intelligente PDF-Extraktion** – Seitenweises, speicherschonendes Chunking via Apache PDFBox, gekoppelt mit deterministischem LLM-Parsing (Groq Llama 3.3 70B).
* **🤖 KI-gestützter Finanz-Chatbot** – Ad-hoc-Beantwortung komplexer Fragen (*„Wie hoch waren meine Lebensmittel-Ausgaben im März?“* oder *„Vergleiche meine Fixkosten zwischen Januar und Februar“*).
* **📊 Automatische Finanzkennzahlen** – On-the-fly-Berechnung von Gesamtausgaben, aggregierten Sparquoten und Kategorie-Metriken ohne Datenbank-Overhead.
* **🗂️ Smart Categorization** – Hybrides System aus regelbasiertem Keyword-Mapping und LLM-gestützter Klassifizierung (erweiterbar für benutzerdefinierte Regelwerke).
* **⚡ Hochperformer In-Memory-Storage** – Thread-sichere Datenhaltung mittels `CopyOnWriteArrayList` für schnellen Durchsatz im Demo-Betrieb, vollständig entkoppelt für einfache PostgreSQL-Migration.
* **🎨 Modern Responsive UI** – Schlankes, mobiles Interface auf Basis von Spring Thymeleaf, dynamisch gerendert mit TailwindCSS.
* **🔌 Pluggable LLM-Provider** – Abstrahiertes API-Interface, aktuell optimiert für Groq Cloud (Durchsatz ~840 Tokens/Sek), vorbereitet für Ollama (Local), OpenAI und Azure.
## 🧱 Tech Stack & Systemvoraussetzungen

### Technologische Architektur
* **Backend Core** – Java 21 (JDK), Spring Boot 3.2.3 (Web, Thymeleaf, Logging)
* **PDF-Processing** – Apache PDFBox 3.0.x (Stream-basiertes Content-Parsing)
* **LLM Engine** – Groq Cloud API (OpenAI-kompatibles REST-Interface)
* **UI Layer** – Thymeleaf Template Engine, TailwindCSS via CDN
* **Build System** – Apache Maven 3.8+ / Maven Wrapper
* **I/O Client** – RestTemplate mit konfigurierter Resilience (Retry-Mechanismen, Timeouts)
* **Serialization** – Jackson JSON Processor (Data Binding & ObjectMapper)

### Systemvoraussetzungen
* **Java Development Kit (JDK)** – Version 21 oder höher (z. B. Eclipse Temurin oder Oracle OpenJDK)
* **Build-Umgebung** – Lokale Maven-Installation oder Ausführung über den mitgelieferten Maven Wrapper (`mvnw`)
* **API-Zugang** – Ein gültiger Groq-API-Key ([Kostenlose Registrierung in der Groq Console](https://groq.com))
* **Optional** – Docker Engine (für containerisierte Builds und Deployments)

---

## ⚙️ Installation & Lokales Setup

### 1. Repository klonen und Verzeichnis wechseln
```bash
git clone https://github.com
cd pdf-banking-assistant
```

### 2. API-Key im Betriebssystem hinterlegen

#### Windows (CMD / Eingabeaufforderung)
```cmd
set GROQ_API_KEY=gsk_dein_tatsaechlicher_api_key_hier
```

#### Windows (PowerShell)
```powershell
\$env:GROQ_API_KEY="gsk_dein_tatsaechlicher_api_key_hier"
```

#### Linux / macOS (Bash / Zsh)
```bash
export GROQ_API_KEY="gsk_dein_tatsaechlicher_api_key_hier"
```

### 3. Applikation kompilieren und starten
```bash
# Verwendung der lokalen Maven-Installation
mvn spring-boot:run

# Alternativ über den integrierten Maven-Wrapper (Linux/macOS)
./mvnw spring-boot:run

# Alternativ über den integrierten Maven-Wrapper (Windows)
.\mvnw.cmd spring-boot:run
```

Nach erfolgreichem Boot-Vorgang des Tomcat-Servers ist die Anwendung im Browser erreichbar unter:
👉 **http://localhost:8080**

---

## 🔐 Umgebungsvariablen (.env & Application-Properties)

Die Steuerung sensitiver Credentials erfolgt strikt über Umgebungsvariablen, um Configuration-Leaks im Repository zu verhindern.

### Lokale Konfigurations-Struktur (`application.yml`)
```yaml
groq:
  api-key: \${GROQ_API_KEY}
  model: llama3-70b-8192 # Standardmäßig konfiguriertes High-Performance-Modell
  url: https://groq.com

server:
  port: 8080
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

logging:
  level:
    root: INFO
    com.banking.pdf_chat_spring: DEBUG
```

> ⚠️ **Produktions-Sicherheitshinweis:** Für den Produktivbetrieb muss zwingend das Spring-Profil `prod` aktiviert (`-Dspring.profiles.active=prod`) und eine persistente SQL-Datenbank (z. B. PostgreSQL) angebunden werden. In-Memory-Daten gehen bei jedem Server-Neustart verloren.
## 📁 Architektur & Ordnerstruktur

Das Projekt folgt einer klassischen, schichtbasierten Domain-Driven-Architektur innerhalb des Spring-Boot-Ökosystems. Die Trennung von Business-Logik, Datenhaltung und Präsentationsschicht sichert Wartbarkeit und Testbarkeit.

```text
pdf-banking-assistant/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/banking/pdf_chat_spring/
│   │   │       ├── config/
│   │   │       │   └── AppConfig.java              # Zentrale Bean-Konfiguration & Pfadvalidierung
│   │   │       ├── controller/
│   │   │       │   ├── WebController.java          # MVC-Controller für das Thymeleaf-Frontend
│   │   │       │   └── ChatController.java         # REST-Endpunkte für Upload und KI-Interaktion
│   │   │       ├── dto/
│   │   │       │   ├── ChatRequest.java            # Immutable Record für eingehende Fragen
│   │   │       │   └── ChatResponse.java           # Immutable Record für strukturierte LLM-Antworten
│   │   │       ├── model/
│   │   │       │   ├── Category.java               # Enum für Finanz-Kategorien mit Keyword-Zuweisung
│   │   │       │   ├── MonthlySummary.java         # DTO für aggregierte Monatsauswertungen
│   │   │       │   └── Transaction.java            # Core-Entität (Immutable, implementiert Builder-Pattern)
│   │   │       ├── service/
│   │   │       │   ├── FinanceAnalysisService.java # Berechnet Metriken wie Sparquote und Budgets
│   │   │       │   ├── GroqClient.java             # Low-Level-HTTP-Wrapper für Groq (Retry & Timeout)
│   │   │       │   ├── PdfParsingService.java      # Kernkomponente für Prompt-Engineering & JSON-Mapping
│   │   │       │   ├── PdfTextExtractor.java       # Apache PDFBox-Wrapper für seitenweises Chunking
│   │   │       │   └── TransactionStorageService.java # Thread-safe In-Memory-Repository (CopyOnWriteArrayList)
│   │   │       ├── util/
│   │   │       │   ├── DateUtil.java               # Zentraler Parser für diverse deutsche Datumsformate
│   │   │       │   └── InputSanitizer.java         # XSS-Schutz und String-Validierung vor LLM-Übergabe
│   │   │       └── PdfChatSpringApplication.java   # Haupt-Startklasse der Spring Boot Applikation
│   │   └── resources/
│   │       ├── templates/
│   │       │   └── index.html                  # Thymeleaf-UI-Template, gestylt mit TailwindCSS
│   │       └── application.yml                 # Zentrale Konfigurationsdatei (Profiles, Logging, Keys)
│   └── test/
│       └── java/com/banking/pdf_chat_spring/   # Unit- und Integrationstests (JUnit 5, Mockito)
├── pdf-documents/                              # Zielverzeichnis für Datei-Uploads (wird auto-generiert)
└── pom.xml                                     # Maven Projektkonfiguration & Dependency-Management
```

---

## 📡 API-Endpunkte & Nutzungsbeispiele

Die Applikation stellt sowohl eine Weboberfläche als auch eine programmatisch ansprechbare REST-Schnittstelle zur Verfügung.

### Endpunkt-Übersicht

| Methode | Endpunkt | Content-Type | Beschreibung |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | `text/html` | Liefert die interaktive Thymeleaf-Startseite aus. |
| `POST` | `/api/upload` | `multipart/form-data` | Nimmt ein Bank-PDF entgegen, extrahiert Transaktionen. |
| `POST` | `/api/chat` | `application/json` | Sendet eine Frage an das System (RAG-basiert auf Transaktionen). |
| `GET` | `/api/status` | `application/json` | Liefert Metadaten (z. B. Anzahl aktuell geladener Transaktionen). |
| `GET` | `/api/files` | `application/json` | Gibt eine Liste aller verarbeiteten PDF-Dateinamen zurück. |

### Integrations- und Testbeispiele via CLI

#### 1. PDF-Kontoauszug hochladen und parsen
```bash
curl -X POST http://localhost:8080/api/upload \
  -F "file=@/path/to/your/kontoauszug.pdf"
```
*Antwort (HTTP 200):*
```json
{
  "status": "success",
  "message": "PDF erfolgreich verarbeitet. 42 Transaktionen extrahiert."
}
```

#### 2. Natural-Language-Anfrage an den Finanz-Assistenten senden
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "Wie hoch waren meine Lebensmittel-Ausgaben im März?"}'
```
*Antwort (HTTP 200):*
```json
{
  "answer": "🛒 Deine Ausgaben für Lebensmittel beliefen sich im März auf insgesamt 345,67 €. Die größte Einzelbuchung war 'REWE SUPERMARKT' mit 82,40 € am 12.03.",
  "sources": ["kontoauszug_maerz_2026.pdf"]
}
```

#### 3. Systemstatus abfragen
```bash
curl -X GET http://localhost:8080/api/status
```
*Antwort (HTTP 200):*
```json
{
  "transactionCount": 184,
  "lastUpload": "2026-06-16T10:15:30Z",
  "activeProvider": "Groq (Llama 3.3 70B)"
}
```
## 🚀 Deployment & CI/CD-Hinweise

### Containerisierung mit Docker
Für den plattformunabhängigen Produktivbetrieb steht ein optimiertes, mehrstufiges Docker-Setup zur Verfügung.

#### 1. Dockerfile (`Dockerfile`)
```dockerfile
# Build-Stufe
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Laufzeit-Stufe
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 2. Image bauen und lokal ausführen
```bash
# Artefakt und Container-Image bauen
docker build -t pdf-banking-assistant .

# Container starten mit injiziertem API-Key
docker run -d \
  -p 8080:8080 \
  -e GROQ_API_KEY="gsk_dein_api_key_hier" \
  --name banking-assistant \
  pdf-banking-assistant
```

### GitHub Actions Pipeline (`.github/workflows/ci-cd.yml`)
Jeder Push oder Pull Request gegen den `main`- oder `develop`-Branch triggert die automatisierte Pipeline zur Qualitätssicherung.

```yaml
name: Java CI/CD mit Maven

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - name: Code auschecken
        uses: actions/checkout@v4

      - name: Java 21 (Temurin) aufsetzen
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Unit- und Integrationstests ausführen
        run: mvn clean test

      - name: Applikation kompilieren
        run: mvn package -DskipTests
```

### ⚠️ Architektur-Checkliste für den Produktivbetrieb
Bevor das System in einer produktiven Cloud-Umgebung (AWS, Azure, GCP) ausgerollt wird, müssen folgende Anpassungen vorgenommen werden:
* **Persistence Layer** – Der flüchtige In-Memory-Storage (`CopyOnWriteArrayList`) muss durch ein persistentes Datenbanksystem (z. B. PostgreSQL) via Spring Data JPA ersetzt werden.
* **Secrets-Management** – Der `GROQ_API_KEY` darf niemals im Filesystem persistiert werden. Nutzen Sie AWS Secrets Manager, HashiCorp Vault oder GitHub Encrypted Secrets.
* **Production-Profil aktivieren** – Starten Sie die Anwendung mit `-Dspring.profiles.active=prod`, um verfeinerte Sicherheitsrichtlinien und optimierte Caching-Mechanismen zu laden.
* **Rate-Limiting** – Schützen Sie den `/api/chat`-Endpunkt vor Denial-of-Service-Angriffen und unerwarteten API-Kosten durch den Einbau von Filtern (z. B. mittels `Bucket4j`).

---

## 🤝 Contributing-Richtlinien (Senior-Level)

Wir begrüßen Beiträge aus der Community. Um die Codequalität auf Senior-Niveau zu halten, halten Sie sich bitte strikt an folgende Software-Engineering-Standards:

### 🔱 Branch-Strategie & Git-Workflow
* **`main`** – Enthält ausschließlich stabilen, produktionsreifen und releasten Code (geschützt, Direct-Pushes sind blockiert).
* **`develop`** – Der zentrale Integrations-Branch für neue Features.
* **`feature/*`** / **`bugfix/*`** – Isolierte Branches für dedizierte Arbeiten, immer abzweigend von `develop`.

### 🔄 Pull-Request-Prozess
1. Erstellen Sie ein Issue, um das Feature oder den Bug zu beschreiben und Akzeptanzkriterien festzulegen.
2. Erstellen Sie Ihren lokalen Branch (`git checkout -b feature/mein-neues-feature`).
3. Verfassen Sie Code, welcher modernen Java 21-Paradigmen entspricht (bevorzugt Records, Text Blocks, Pattern Matching).
4. Vermeiden Sie ungekapselte Konsolenausgaben. Nutzen Sie konsistent strukturiertes Logging via SLF4J (`@Slf4j`).
5. Schreiben Sie Unit-Tests mit JUnit 5 und Mockito. Eine Testabdeckung von **>80%** der geänderten Logik ist erforderlich.
6. Erstellen Sie den PR gegen `develop` unter Verwendung der bereitgestellten Vorlage und verknüpfen Sie ihn mit dem Issue (`closes #123`).
7. Ein erfolgreicher CI-Build sowie mindestens ein Code-Review durch das Core-Team sind zwingend erforderlich für den Merge.

---

## 👥 Autoren & Lizenz

* **Entwickler / Maintainer** – [Dein Name] (<deine.email@example.com>)
* **GitHub Profil** – [@your-username](https://github.com)

### Lizenzierung
Dieses Projekt ist unter den Bedingungen der **MIT-Lizenz** lizenziert. Das bedeutet, dass Sie den Code ohne Einschränkungen für private, kommerzielle oder akademische Zwecke nutzen, modifizieren und verbreiten dürfen. Siehe die [LICENSE](LICENSE)-Datei für den vollständigen Lizenztext.

---
*Viel Erfolg bei der KI-gestützten Finanzanalyse mit dem PDF-Banking Assistant! 🚀*
