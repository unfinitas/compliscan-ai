# CompliScan-AI

CompliScan-AI is an AI-assisted compliance auditing system designed to help aviation authorities and maintenance organizations analyze Maintenance Organisation Exposition (MOE) documents against EASA Part-145 regulatory requirements.

It ingests MOE manuals, maps them against structured regulation datasets, identifies compliance gaps, and generates targeted auditor questions, reducing weeks of manual work to minutes.

Built for the Junction 2025 challenge.

---

## Key Capabilities

* Upload MOE documents (PDF, DOCX)
* Automatic text extraction and normalization
* Cross-reference against EASA Part-145 clauses
* Semantic similarity mapping (embeddings)
* Compliance coverage classification
* Gap and non-conformity detection
* Regulation version comparison (v1 → v2)
* AI-generated auditor questions
* Web dashboard for auditors

---

# Project Structure

```
compliscan-ai/
└── src/
    ├── main/
    │   ├── java/com/compliscan/
    │   │   ├── api/                 # Controllers, DTOs, workflow orchestration
    │   │   ├── core/                # Compliance engine (framework-free)
    │   │   │   ├── ingestion/       # PDF/DOCX parsing
    │   │   │   ├── normalization/   # Text cleaning, section tagging
    │   │   │   ├── regulation/      # Regulation loader, version diff
    │   │   │   ├── mapping/         # Embedding-based matching
    │   │   │   ├── analysis/        # Coverage evaluation, gap detection
    │   │   │   ├── llm/             # LLM abstraction
    │   │   │   ├── questions/       # Auditor question generator
    │   │   │   └── model/           # Domain models
    │   │   └── CompliScanApplication.java
    │   │
    │   ├── resources/
    │   │   ├── application.yml
    │   │   └── regulation-data/
    │   │       ├── part145/         # Extracted clauses
    │   │       ├── v1/              # Regulation version 1
    │   │       └── v2/              # Regulation version 2
    │   │
    └── test/
        └── java/com/compliscan/
            ├── core/
            └── api/

frontend/
└── web/                             # Next.js/React UI

scripts/
└── data-prep/                       # Dataset preparation utilities

docker/
├── Dockerfile.backend
├── Dockerfile.frontend
└── docker-compose.yml

docs/
├── architecture.md
├── project-plan.md
├── api-spec.md
└── moe-samples/
```

---

# Architecture Overview

CompliScan-AI is organized into three layers:

1. **API Layer (`api/`)** – Handles REST endpoints and orchestrates workflows.
2. **Core Engine (`core/`)** – Pure domain logic implementing ingestion → mapping → analysis.
3. **Frontend (`frontend/web/`)** – Next.js dashboard for visualization.

---

# Processing Pipeline

1. MOE upload → PDF/DOCX extraction
2. Text normalization & structure
3. Regulation dataset loading
4. Embedding-based similarity mapping
5. Classification: Covered / Partial / Missing
6. Gap detection & explanation
7. Auditor question generation
8. Frontend dashboard visualization

---

# Local Development

### Backend

```
./gradlew bootRun
```

### Frontend

```
cd frontend/web
npm install
npm run dev
```

### Docker

```
docker-compose up --build
```

---

# Regulation Dataset

Machine-readable JSON regulation subsets are stored in:

```
src/main/resources/regulation-data/
```

Dataset preparation tools are in:

```
scripts/data-prep/
```

---

# Future Improvements

* Full EASA Part-145 coverage
* Support for maritime, rail, cybersecurity regulations
* RAG-based retrieval pipeline
* Vector database integration
* Multi-user access control

---

# License

Only Unfinitas team :))

LLM:

For each regulation clause (R1 ... Rn):
1. Embed regulation clause → vector
2. Vector search MOE paragraphs → top 20
3. LLM rerank → top 3–5 relevant paragraphs
4. LLM judge → compliance JSON
