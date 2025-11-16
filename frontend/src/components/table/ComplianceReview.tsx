"use client";

import { useState, useMemo, useEffect } from "react";
import {
  ChevronDown,
  ShieldAlert,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { RequirementItem } from "./RequirementItem";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import type { Requirement } from "@/types/compliance";

const ITEMS_PER_PAGE = 10;

export function ComplianceReview({
  requirements,
}: {
  requirements: Requirement[];
}) {
  const [expandedIds, setExpandedIds] = useState<Set<string>>(() => {
    // Auto-expand items with 'non' compliance status
    const nonCompliant = requirements
      .filter((req) => req.compliance_status === "non")
      .map((req) => req.requirement_id);
    return new Set(nonCompliant);
  });

  const [viewedIds, setViewedIds] = useState<Set<string>>(new Set());
  const [currentPage, setCurrentPage] = useState(1);

  const toggleExpanded = (id: string) => {
    const newExpanded = new Set(expandedIds);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedIds(newExpanded);
  };

  const toggleViewed = (id: string) => {
    const newViewed = new Set(viewedIds);
    if (newViewed.has(id)) {
      newViewed.delete(id);
    } else {
      newViewed.add(id);
    }
    setViewedIds(newViewed);
  };

  // Pagination calculations
  const totalPages = Math.ceil(requirements.length / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const endIndex = startIndex + ITEMS_PER_PAGE;
  const paginatedRequirements = requirements.slice(startIndex, endIndex);

  // Reset to page 1 when requirements change and current page is invalid
  useEffect(() => {
    const maxPage = Math.ceil(requirements.length / ITEMS_PER_PAGE);
    if (requirements.length > 0 && currentPage > maxPage) {
      setCurrentPage(1);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requirements.length]);

  const goToPage = (page: number) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const getPageNumbers = () => {
    const pages: (number | string)[] = [];
    const maxVisible = 5;

    if (totalPages <= maxVisible) {
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      if (currentPage <= 3) {
        for (let i = 1; i <= 4; i++) {
          pages.push(i);
        }
        pages.push("...");
        pages.push(totalPages);
      } else if (currentPage >= totalPages - 2) {
        pages.push(1);
        pages.push("...");
        for (let i = totalPages - 3; i <= totalPages; i++) {
          pages.push(i);
        }
      } else {
        pages.push(1);
        pages.push("...");
        for (let i = currentPage - 1; i <= currentPage + 1; i++) {
          pages.push(i);
        }
        pages.push("...");
        pages.push(totalPages);
      }
    }

    return pages;
  };

  // Group paginated requirements by status
  const groupedPaginatedRequirements = useMemo(() => {
    return {
      non: paginatedRequirements.filter((r) => r.compliance_status === "non"),
      partial: paginatedRequirements.filter(
        (r) => r.compliance_status === "partial"
      ),
      full: paginatedRequirements.filter((r) => r.compliance_status === "full"),
    };
  }, [paginatedRequirements]);

  const getStatusColor = (status: string) => {
    switch (status) {
      case "full":
        return "bg-emerald-500";
      case "partial":
        return "bg-amber-500";
      case "non":
        return "bg-red-500";
      default:
        return "bg-gray-500";
    }
  };

  const renderStatusGroup = (title: string, reqs: Requirement[]) => {
    if (reqs.length === 0) return null;

    return (
      <div key={title} className="mb-2">
        <div className="text-xs font-semibold text-slate-400 px-4 py-2 uppercase tracking-wider">
          {title} ({reqs.length})
        </div>
        {reqs.map((req) => {
          // Find the actual index in paginatedRequirements for unique key
          const absoluteIndex = paginatedRequirements.findIndex(
            (r) => r.requirement_id === req.requirement_id
          );
          const uniqueKey =
            absoluteIndex >= 0
              ? `${req.requirement_id}-${startIndex + absoluteIndex}`
              : `${req.requirement_id}-${Date.now()}`;

          return (
            <div key={uniqueKey} className="border-b border-slate-800">
              <div
                onClick={() => toggleExpanded(req.requirement_id)}
                className="flex items-center gap-3 px-4 py-3 hover:bg-slate-900 cursor-pointer transition-colors"
              >
                <ChevronDown
                  size={18}
                  className={`text-slate-500 shrink-0 transition-transform ${
                    expandedIds.has(req.requirement_id)
                      ? "rotate-0"
                      : "-rotate-90"
                  }`}
                />

                {/* Status indicator */}
                <div className="flex gap-1 shrink-0">
                  {[0, 1, 2].map((i) => (
                    <div
                      key={i}
                      className={`w-2 h-2 rounded-sm ${
                        i === 0
                          ? getStatusColor(req.compliance_status)
                          : "bg-slate-700"
                      }`}
                    />
                  ))}
                </div>

                {/* Shield icon */}
                <ShieldAlert size={16} className="text-slate-500 shrink-0" />

                {/* Requirement ID and description */}
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium text-slate-100">
                    {req.requirement_id}
                  </div>
                  <div className="text-xs text-slate-400 truncate">
                    {req.requirement_text}
                  </div>
                </div>

                {/* Viewed checkbox */}
                <div className="flex items-center gap-2 shrink-0">
                  <span className="text-xs text-slate-400">
                    {viewedIds.has(req.requirement_id) ? "Viewed" : ""}
                  </span>
                  <Checkbox
                    checked={viewedIds.has(req.requirement_id)}
                    onCheckedChange={() => toggleViewed(req.requirement_id)}
                    onClick={(e) => e.stopPropagation()}
                    className="border-slate-600 data-[state=checked]:bg-blue-600 data-[state=checked]:border-blue-600"
                  />
                </div>
              </div>

              {/* Expanded details */}
              {expandedIds.has(req.requirement_id) && (
                <RequirementItem
                  requirement={req}
                  isViewed={viewedIds.has(req.requirement_id)}
                  onToggleViewed={() => toggleViewed(req.requirement_id)}
                />
              )}
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <div className="w-full max-w-6xl mx-auto">
      {/* Header */}
      <div className="border-b border-slate-800 bg-slate-900 sticky top-0 z-10">
        <div className="px-4 py-4">
          <h1 className="text-lg font-semibold text-slate-100">
            Compliance Requirements Review
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            {requirements.length} requirements total
          </p>
        </div>
      </div>

      {/* Requirements list */}
      <div className="divide-y divide-slate-800">
        {renderStatusGroup("Non-Compliant", groupedPaginatedRequirements.non)}
        {renderStatusGroup("Partial", groupedPaginatedRequirements.partial)}
        {renderStatusGroup("Full", groupedPaginatedRequirements.full)}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-6 pt-4 border-t border-slate-800 bg-slate-900 px-4 py-4">
          <div className="text-sm text-slate-400">
            Showing {startIndex + 1} to{" "}
            {Math.min(endIndex, requirements.length)} of {requirements.length}{" "}
            requirements
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => goToPage(currentPage - 1)}
              disabled={currentPage === 1}
              className="h-8 w-8 p-0 bg-slate-800 border-slate-700 text-slate-300 hover:bg-slate-700"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>

            {getPageNumbers().map((page, index) => {
              if (page === "...") {
                return (
                  <span
                    key={`ellipsis-${index}`}
                    className="px-2 text-slate-500"
                  >
                    ...
                  </span>
                );
              }

              return (
                <Button
                  key={page}
                  variant={currentPage === page ? "default" : "outline"}
                  size="sm"
                  onClick={() => goToPage(page as number)}
                  className={`h-8 w-8 p-0 ${
                    currentPage === page
                      ? "bg-blue-600 text-white border-blue-600"
                      : "bg-slate-800 border-slate-700 text-slate-300 hover:bg-slate-700"
                  }`}
                >
                  {page}
                </Button>
              );
            })}

            <Button
              variant="outline"
              size="sm"
              onClick={() => goToPage(currentPage + 1)}
              disabled={currentPage === totalPages}
              className="h-8 w-8 p-0 bg-slate-800 border-slate-700 text-slate-300 hover:bg-slate-700"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
