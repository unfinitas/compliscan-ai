# CompliScan-AI
## EASA Part-145 MOE Automated Compliance Analysis System

### Mission
Automate EASA Part-145 MOE compliance verification for Traficom aviation inspectors, reducing manual audit time from days to minutes.

---
## Key Features

### 1. AI-Powered Analysis
- **Semantic Matching**: Compares MOE content against all EASA Part-145 clauses
- **Intelligent Judgment**: Google Gemini AI evaluates complex compliance cases
- **Evidence Mapping**: Links specific MOE paragraphs to each requirement

### 2. Structured Reporting
Each requirement analyzed with comprehensive JSON response data:

**API Response Structure:**
```json
{
  "compliance_status": "non",        // full | partial | non
  "requirement_id": "AMC_145_A_10",
  "finding_level": "Level 2",        // Level 1 | Level 2 | Observation | Recommendation
  "justification": "The provided MOE excerpts do not contain the definition of 'Line maintenance'",
  "missing_elements": [
    "Definition of 'Line maintenance'",
    "Specific activities included in line maintenance"
  ],
  "recommended_actions": [
    "Update MOE to include the definition of 'Line maintenance' and its scope as per AMC 145.A.10"
  ],
  "evidence": [
    {
      "moe_paragraph_id": 692,
      "relevant_excerpt": "This section specifies the categories and requirements...",
      "similarity_score": 0.87,
      "rerank_score": 0.87
    }
  ]
}
```

**UI Display (Dropdown Format):**
1. **Compliance Status** - Visual badge (Full/Partial/Non)
2. **Requirement** - ID & title (e.g., AMC_145_A_10: Line Maintenance)
3. **Justification** - AI reasoning for the judgment
4. **Missing Elements** - List of gaps found
5. **Recommended Actions** - Specific remediation steps
6. **Evidence** - MOE paragraphs with similarity scores (expandable)

### 3. Inspector-Optimized UI
- **Dropdown Design**: Clean overview with expandable details
- **Priority Ordering**: Critical findings first
- **Finding Levels**: Level 1/2, Observations, Recommendations
- **Pagination**: 10 requirements per page for easy review

---

## Architecture

<img width="1117" height="832" alt="Image" src="https://github.com/user-attachments/assets/d41c0439-58d2-4a0a-9569-c5c906ac831b" />

### Tech Stack
- **Backend**: Java 21, Spring Boot 3.5.7
- **Frontend**: Next.js 16, React 19
- **AI**: Google Gemini (embeddings + judgment)
- **Database**: PostgreSQL 17

---

## Project Structure
```
compliscan-ai/
├── backend/          # Spring Boot API
├── frontend/         # Next.js UI
├── uploads/moe/      # Document storage
└── docker-compose.yml
```

---

## Traficom's Challenge Solution

**Our Solution**: AI automation providing:
- Consistent evaluation standards
- Comprehensive compliance assessment
- Data-driven decision support
- Reduced inspector workload

---
## License

This project is open-sourced under the Apache 2.0 License.

Primary design, backend architecture, compliance analysis engine, embeddings pipeline, and UI/UX concept were created by Tan Anh Nguyen during the Traficom Challenge at Junction 2025.

The core concept, system outline, and technical direction of CompliScan-AI originated from and were led by Tan Anh Nguyen.

See [LICENSE](./LICENSE) for full legal terms.

## Contributors

Frontend implementation:
- Luu Hoang Nhat Vi

Additional contributions in presentation and documentation by:
- Chambit Oh
