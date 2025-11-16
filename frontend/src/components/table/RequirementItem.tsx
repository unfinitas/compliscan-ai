"use client";

import { useState } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { ChevronDown } from "lucide-react";
import type { Requirement } from "@/types/compliance";

interface RequirementItemProps {
  requirement: Requirement;
  isViewed: boolean;
  onToggleViewed: () => void;
}

const statusConfig = {
  full: {
    bgColor: "bg-emerald-50",
    hoverBgColor: "hover:bg-emerald-50",
    dotColor: "bg-emerald-600",
    badgeBg: "bg-emerald-100",
    badgeText: "text-emerald-700",
    expandedBg: "bg-slate-900",
    label: "Compliant",
  },
  partial: {
    bgColor: "bg-amber-50",
    hoverBgColor: "hover:bg-amber-50",
    dotColor: "bg-amber-500",
    badgeBg: "bg-amber-100",
    badgeText: "text-amber-700",
    expandedBg: "bg-slate-900",
    label: "Partial",
  },
  non: {
    bgColor: "bg-rose-50",
    hoverBgColor: "hover:bg-rose-50",
    dotColor: "bg-rose-600",
    badgeBg: "bg-rose-100",
    badgeText: "text-rose-700",
    expandedBg: "bg-slate-900",
    label: "Non-compliant",
  },
};

export function RequirementItem({
  requirement,
  isViewed,
  onToggleViewed,
}: RequirementItemProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const status = statusConfig[requirement.compliance_status];

  return (
    <div className="rounded-lg border border-slate-200 bg-white overflow-hidden mb-3 transition-all hover:shadow-md">
      {/* Header */}
      <div
        className={`flex items-center gap-4 px-6 py-4 ${
          isExpanded ? status.expandedBg : status.bgColor
        } ${
          isExpanded ? "" : status.hoverBgColor
        } transition-all duration-150 cursor-pointer`}
        onClick={() => setIsExpanded(!isExpanded)}
      >
        {/* Expand/Collapse chevron */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            setIsExpanded(!isExpanded);
          }}
          className={`flex items-center justify-center w-6 h-6 transition-colors ${
            isExpanded ? "text-white" : "text-slate-400 hover:text-slate-600"
          }`}
          aria-label="Toggle details"
        >
          <ChevronDown
            size={18}
            className={`transition-transform duration-200 ${
              isExpanded ? "rotate-180" : ""
            }`}
          />
        </button>

        {/* Status indicator dot */}
        <div
          className={`w-2.5 h-2.5 rounded-full ${status.dotColor} shrink-0 border border-slate-300 shadow-sm`}
        />

        {/* Requirement ID and Status */}
        <div className="flex items-center gap-3 flex-1">
          <code
            className={`text-sm font-semibold font-mono tracking-wide ${
              isExpanded ? "text-white" : "text-slate-900"
            }`}
          >
            {requirement.requirement_id}
          </code>
          <span
            className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${status.badgeBg} ${status.badgeText}`}
          >
            {status.label}
          </span>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <Checkbox
            checked={isViewed}
            onCheckedChange={() => {
              onToggleViewed();
            }}
            onClick={(e) => e.stopPropagation()}
            className={`w-5 h-5 cursor-pointer ${
              isExpanded ? "border-white" : ""
            }`}
            aria-label="Mark as viewed"
          />
          <label
            onClick={(e) => {
              e.stopPropagation();
              onToggleViewed();
            }}
            className={`text-sm font-semibold cursor-pointer hover:opacity-80 transition-opacity whitespace-nowrap ${
              isExpanded ? "text-white" : "text-slate-700"
            }`}
          >
            Viewed
          </label>
        </div>
      </div>

      {isExpanded && (
        <div className="border-t border-slate-200 bg-white p-6 space-y-6">
          {/* Justification */}
          {requirement.justification && (
            <section>
              <h3 className="text-xs font-semibold text-slate-600 uppercase tracking-widest mb-2">
                Justification
              </h3>
              <p className="text-sm text-slate-900 leading-relaxed bg-white p-3 rounded border border-slate-200">
                {requirement.justification}
              </p>
            </section>
          )}

          {/* Missing Elements */}
          {requirement.missing_elements.length > 0 && (
            <section>
              <h3 className="text-xs font-semibold text-slate-600 uppercase tracking-widest mb-2">
                Missing Elements
              </h3>
              <ul className="space-y-2">
                {requirement.missing_elements.map((element, idx) => (
                  <li
                    key={idx}
                    className="flex items-start gap-2 text-sm text-slate-900"
                  >
                    <span className="text-rose-500 font-bold mt-0.5 shrink-0">
                      •
                    </span>
                    <span>{element}</span>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {/* Recommended Actions */}
          {requirement.recommended_actions.length > 0 && (
            <section>
              <h3 className="text-xs font-semibold text-slate-600 uppercase tracking-widest mb-2">
                Recommended Actions
              </h3>
              <ul className="space-y-2">
                {requirement.recommended_actions.map((action, idx) => (
                  <li
                    key={idx}
                    className="flex items-start gap-2 text-sm text-slate-900"
                  >
                    <span className="text-emerald-600 font-bold mt-0.5 shrink-0">
                      →
                    </span>
                    <span>{action}</span>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {/* Evidence */}
          {requirement.evidence.length > 0 && (
            <section>
              <h3 className="text-xs font-semibold text-slate-600 uppercase tracking-widest mb-3">
                Evidence
              </h3>
              <div className="space-y-3">
                {requirement.evidence.map((ev, idx) => (
                  <div
                    key={idx}
                    className="p-4 bg-slate-50 rounded-lg border-2 border-slate-200"
                  >
                    <div className="mb-3 pb-3 border-b border-slate-300">
                      <p className="text-xs font-mono text-slate-700 font-semibold">
                        Paragraph ID: {ev.moe_paragraph_id}
                      </p>
                    </div>
                    <div className="space-y-2">
                      <div>
                        <p className="text-xs text-slate-600 font-semibold uppercase tracking-wider mb-1">
                          Relevant
                        </p>
                        <p className="text-sm text-slate-900 font-medium bg-white p-2 rounded border border-slate-200">
                          &quot;{ev.relevant_excerpt}&quot;
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-slate-600 font-semibold uppercase tracking-wider mb-1">
                          Full Paragraph
                        </p>
                        <p className="text-sm text-slate-800 leading-relaxed bg-white p-2 rounded border border-slate-200">
                          {ev.full_paragraph_excerpt}
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      )}
    </div>
  );
}
