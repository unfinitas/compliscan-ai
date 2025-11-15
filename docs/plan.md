# AI Audit Aid (AAA) – Final Project Plan

## 1. Objective

Build an AI-assisted auditing assistant that helps Traficom auditors and EASA Part-145 maintenance organizations:

* Upload a MOE (Maintenance Organisation Exposition).
* Automatically cross-reference it against a subset of Part-145 regulation.
* Show coverage, gaps, and risks through a clear UI.
* Generate targeted auditor questions.
* Demonstrate regulation version comparison on a small subset.

The system must be explainable, traceable, and realistically deployable within Traficom’s workflow.

---

## 2. MVP Scope (Hackathon Deliverable)

### 1. Document Ingestion

* Upload MOE PDF/DOCX.
* Extract text and split into structured sections/paragraphs.

### 2. Regulation Corpus (Subset)

* JSON-based storage of a realistic subset of Part-145 clauses.
* Each clause includes ID, text, topic, and version.

### 3. Requirement–MOE Mapping

* Embedding-based similarity search.
* Classify each clause as Covered / Partially Covered / Not Covered.
* Show matched snippets, confidence scores, and citations.

### 4. Gap / Non-Conformity Detection

* Low similarity → potential gap.
* Contradictory language → potential NC.
* Provide text-based explanations and references.

### 5. Auditor Question Generator

* Generate targeted questions for each ambiguous or missing area.

### 6. Regulation Version Comparison

* Two JSON versions of Part-145.
* Compute added/removed/changed clauses.
* Highlight MOE sections affected.

### 7. UI

* Upload MOE.
* Coverage matrix.
* Clause drill-down view.
* Version comparison page.

---

## 3. Architecture Overview

### Components

* Backend (Java + Spring Boot)
* Core logic modules:

  * Ingestion
  * Regulation loader
  * Mapping engine
  * Analysis engine
  * LLM integration
  * Question generator
* Frontend (React/Next.js)
* Regulation data store (subset JSON)

### Data Flow

1. Upload MOE → ingestion → structured document.
2. Load Part-145 subset.
3. Compute embeddings for MOE paragraphs and regulation clauses.
4. Mapping engine assigns coverage status.
5. Analysis engine identifies gaps and contradictions.
6. LLM generates auditor questions.
7. UI displays results.

---

## 4. Implementation Plan (Hackathon Timeline)

### Phase 1 – Foundation

* Repo setup: backend/api, backend/core, ui/web.
* Add sample MOE and regulation subset.

### Phase 2 – Ingestion + Regulation Parser

* PDF/DOCX extraction.
* Normalize into paragraphs with IDs.
* Regulation loader and version diff.

### Phase 3 – Mapping Engine

* Embedding client implementation.
* Paragraph-to-clause similarity mapping.
* Coverage classification.

### Phase 4 – Analysis + Questioning

* Gap detection logic.
* Non-conformity heuristics.
* LLM-based question generator.

### Phase 5 – UI Integration

* Upload screen.
* Coverage table.
* Clause detail view.
* Version comparison viewer.

### Phase 6 – Polish

* Architecture documentation.
* Sample demo workflow.
* Prepare pitch narrative.

---

## 5. Demo Script

1. Describe the regulatory auditing problem.
2. Upload a MOE.
3. Show automated coverage matrix.
4. Deep dive into one clause showing:

   * Regulation text
   * MOE snippet
   * Coverage status
   * Explanations
   * Generated auditor questions
5. Show regulation version comparison.
6. Explain cross-sector extensibility.

---

## 6. Future Roadmap

* Full regulation corpus integration.
* Vector database for scalable embeddings.
* Improved contradiction reasoning.
* Secure deployment and RBAC.
* Multi-sector support (maritime, rail, communications).
