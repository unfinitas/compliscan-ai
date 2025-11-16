"use client";

import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { Requirement } from "@/types/compliance";

interface RequirementDetailsModalProps {
  requirement: Requirement;
  onClose: () => void;
}

const statusConfig = {
  full: {
    bgColor: "bg-emerald-50",
    textColor: "text-emerald-700",
    borderColor: "border-emerald-200",
    label: "Compliant",
  },
  partial: {
    bgColor: "bg-amber-50",
    textColor: "text-amber-700",
    borderColor: "border-amber-200",
    label: "Partial",
  },
  non: {
    bgColor: "bg-rose-50",
    textColor: "text-rose-700",
    borderColor: "border-rose-200",
    label: "Non-compliant",
  },
};

export function RequirementDetailsModal({
  requirement,
  onClose,
}: RequirementDetailsModalProps) {
  const status = statusConfig[requirement.compliance_status];

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div
          className={`sticky top-0 ${status.bgColor} border-b ${status.borderColor} px-6 py-4 flex items-center justify-between`}
        >
          <div>
            <div className="flex items-center gap-3 mb-2">
              <code className="text-lg font-bold font-mono text-slate-900">
                {requirement.requirement_id}
              </code>
              <span
                className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-semibold bg-white ${status.textColor}`}
              >
                {status.label}
              </span>
            </div>
            <p className="text-sm text-slate-600 font-medium">
              {requirement.finding_level}
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-slate-200 rounded-lg transition-colors"
            aria-label="Close"
          >
            <X size={20} className="text-slate-600" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {/* Evidence */}
          {requirement.evidence.length > 0 && (
            <section>
              <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wide mb-3">
                Evidence
              </h3>
              <div className="space-y-3">
                {requirement.evidence.map((ev, idx) => (
                  <div
                    key={idx}
                    className="p-3 bg-slate-50 rounded border border-slate-200"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <p className="text-xs font-mono text-slate-600">
                        ID: {ev.moe_paragraph_id}
                      </p>
                    </div>
                    <div className="space-y-1">
                      <p className="text-sm text-slate-700 font-medium">
                        {ev.relevant_excerpt}
                      </p>
                      <p className="text-xs text-slate-600 italic">
                        {ev.full_paragraph_excerpt}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Justification */}
          {requirement.justification && (
            <section>
              <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wide mb-2">
                Justification
              </h3>
              <p className="text-sm text-slate-900 leading-relaxed bg-slate-50 p-3 rounded border border-slate-200">
                {requirement.justification}
              </p>
            </section>
          )}

          {/* Missing Elements */}
          {requirement.missing_elements.length > 0 && (
            <section>
              <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wide mb-2">
                Missing Elements
              </h3>
              <ul className="space-y-2">
                {requirement.missing_elements.map((element, idx) => (
                  <li
                    key={idx}
                    className="flex items-start gap-2 text-sm text-slate-900"
                  >
                    <span className="text-rose-500 font-bold mt-1">•</span>
                    <span>{element}</span>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {/* Recommended Actions */}
          {requirement.recommended_actions.length > 0 && (
            <section>
              <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wide mb-2">
                Recommended Actions
              </h3>
              <ul className="space-y-2">
                {requirement.recommended_actions.map((action, idx) => (
                  <li
                    key={idx}
                    className="flex items-start gap-2 text-sm text-slate-900"
                  >
                    <span className="text-emerald-600 font-bold mt-1">→</span>
                    <span>{action}</span>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </div>

        {/* Footer */}
        <div className="sticky bottom-0 bg-slate-50 border-t border-slate-200 px-6 py-3 flex justify-end gap-2">
          <Button variant="outline" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    </div>
  );
}
