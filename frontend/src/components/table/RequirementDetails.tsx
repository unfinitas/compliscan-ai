"use client";

interface Requirement {
  requirement_id: string;
  compliance_status: "full" | "partial" | "non";
  description: string;
  sub_requirements: string[];
  evidence: Array<{ text: string; similarity: number; rerank_score: number }>;
  missing_elements: string[];
  recommended_actions: string[];
  finding_level: string;
}

interface RequirementDetailsProps {
  requirement: Requirement;
}

export function RequirementDetails({ requirement }: RequirementDetailsProps) {
  return (
    <div className="bg-muted/30 px-6 py-4 space-y-6 border-t border-border">
      {/* Finding Level */}
      <div>
        <h4 className="text-xs font-semibold text-foreground uppercase tracking-wide mb-2">
          Finding Level
        </h4>
        <p className="text-sm text-foreground">{requirement.finding_level}</p>
      </div>

      {/* Evidence */}
      <div>
        <h4 className="text-xs font-semibold text-foreground uppercase tracking-wide mb-2">
          Evidence
        </h4>
        <div className="space-y-2">
          {requirement.evidence.map((ev, idx) => (
            <div
              key={idx}
              className="bg-background rounded border border-border px-3 py-2"
            >
              <p className="text-sm text-foreground mb-2">{ev.text}</p>
              <div className="flex gap-4 text-xs text-muted-foreground"></div>
            </div>
          ))}
        </div>
      </div>

      {/* Missing elements */}
      {requirement.missing_elements.length > 0 && (
        <div>
          <h4 className="text-xs font-semibold text-foreground uppercase tracking-wide mb-2">
            Missing Elements
          </h4>
          <ul className="space-y-1">
            {requirement.missing_elements.map((element, idx) => (
              <li key={idx} className="text-sm text-red-600 flex gap-2">
                <span className="text-red-600">✕</span>
                <span>{element}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Recommended actions */}
      {requirement.recommended_actions.length > 0 && (
        <div>
          <h4 className="text-xs font-semibold text-foreground uppercase tracking-wide mb-2">
            Recommended Actions
          </h4>
          <ul className="space-y-1">
            {requirement.recommended_actions.map((action, idx) => (
              <li key={idx} className="text-sm text-amber-600 flex gap-2">
                <span className="text-amber-600">→</span>
                <span>{action}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
