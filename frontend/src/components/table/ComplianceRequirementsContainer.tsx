"use client";

import { useState, useEffect, useCallback } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ComplianceHeader } from "./ComplianceHeader";
import { RequirementItem } from "./RequirementItem";
import { getAnalysisOutcomes, getAnalysisReport } from "@/api/analysis/analysisApi";
import { transformComplianceData } from "@/utils/transformComplianceData";
import type { Requirement } from "@/types/compliance";

interface ComplianceRequirementsContainerProps {
  analysisId: string;
}

const ITEMS_PER_PAGE = 10;

export function ComplianceRequirementsContainer({
  analysisId,
}: ComplianceRequirementsContainerProps) {
  const [viewedItems, setViewedItems] = useState<Set<string>>(new Set());
  const [currentPage, setCurrentPage] = useState(0); // 0-indexed for API
  const [statusFilter, setStatusFilter] = useState<
    "all" | "full" | "partial" | "non"
  >("all");
  const [requirements, setRequirements] = useState<Requirement[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusCounts, setStatusCounts] = useState({
    full: 0,
    partial: 0,
    non: 0,
  });

  // Fetch status counts from full report
  const fetchStatusCounts = useCallback(async () => {
    try {
      const report = await getAnalysisReport(analysisId);
      const counts = {
        full: 0,
        partial: 0,
        non: 0,
      };

      report.compliance.forEach((req) => {
        if (req.compliance_status === "full") counts.full++;
        else if (req.compliance_status === "partial") counts.partial++;
        else if (req.compliance_status === "non") counts.non++;
      });

      setStatusCounts(counts);
    } catch (err) {
      console.error("Failed to fetch status counts:", err);
      // Don't fail the whole component if status counts fail
    }
  }, [analysisId]);

  // Fetch paginated requirements
  const fetchRequirements = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const complianceStatus = statusFilter === "all" ? undefined : statusFilter;
      const response = await getAnalysisOutcomes(
        analysisId,
        currentPage,
        ITEMS_PER_PAGE,
        complianceStatus
      );

      const transformed = transformComplianceData(response.content);
      setRequirements(transformed);
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : "Failed to load requirements";
      setError(errorMessage);
      console.error("Failed to fetch requirements:", err);
    } finally {
      setIsLoading(false);
    }
  }, [analysisId, currentPage, statusFilter]);

  // Initial load and when filter changes
  useEffect(() => {
    fetchStatusCounts();
  }, [fetchStatusCounts]);

  // Fetch requirements when page or filter changes
  useEffect(() => {
    fetchRequirements();
  }, [fetchRequirements]);

  const toggleViewed = (requirementId: string) => {
    const newViewed = new Set(viewedItems);
    if (newViewed.has(requirementId)) {
      newViewed.delete(requirementId);
    } else {
      newViewed.add(requirementId);
    }
    setViewedItems(newViewed);
  };

  const handleStatusFilterChange = (
    status: "all" | "full" | "partial" | "non"
  ) => {
    setStatusFilter(status);
    setCurrentPage(0); // Reset to first page when filter changes
  };

  const goToPage = (page: number) => {
    // Convert from 1-indexed to 0-indexed
    const pageIndex = page - 1;
    if (pageIndex >= 0 && pageIndex < totalPages) {
      setCurrentPage(pageIndex);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const getPageNumbers = () => {
    const pages: (number | string)[] = [];
    const maxVisible = 5;
    const displayTotalPages = totalPages;

    if (displayTotalPages <= maxVisible) {
      for (let i = 1; i <= displayTotalPages; i++) {
        pages.push(i);
      }
    } else {
      const displayCurrentPage = currentPage + 1; // Convert to 1-indexed for display
      if (displayCurrentPage <= 3) {
        for (let i = 1; i <= 4; i++) {
          pages.push(i);
        }
        pages.push("...");
        pages.push(displayTotalPages);
      } else if (displayCurrentPage >= displayTotalPages - 2) {
        pages.push(1);
        pages.push("...");
        for (let i = displayTotalPages - 3; i <= displayTotalPages; i++) {
          pages.push(i);
        }
      } else {
        pages.push(1);
        pages.push("...");
        for (let i = displayCurrentPage - 1; i <= displayCurrentPage + 1; i++) {
          pages.push(i);
        }
        pages.push("...");
        pages.push(displayTotalPages);
      }
    }

    return pages;
  };

  const startIndex = currentPage * ITEMS_PER_PAGE;
  const endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalElements);
  const displayCurrentPage = currentPage + 1; // Convert to 1-indexed for display

  if (error) {
    return (
      <div className="w-full">
        <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-8">
          <div className="text-center">
            <p className="text-red-600 mb-2">Error loading compliance data</p>
            <p className="text-slate-500 text-sm">{error}</p>
            <Button
              onClick={() => fetchRequirements()}
              className="mt-4"
              variant="outline"
            >
              Retry
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full">
      <ComplianceHeader
        statusCounts={statusCounts}
        statusFilter={statusFilter}
        onStatusFilterChange={handleStatusFilterChange}
      />
      <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <p className="text-slate-600">Loading requirements...</p>
          </div>
        ) : requirements.length === 0 ? (
          <div className="flex items-center justify-center py-12">
            <p className="text-slate-600">No requirements found</p>
          </div>
        ) : (
          <>
            <div className="space-y-0">
              {requirements.map((requirement, index) => (
                <RequirementItem
                  key={`${requirement.requirement_id}-${startIndex + index}`}
                  requirement={requirement}
                  isViewed={viewedItems.has(requirement.requirement_id)}
                  onToggleViewed={() => toggleViewed(requirement.requirement_id)}
                />
              ))}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-6 pt-4 border-t border-slate-200">
                <div className="text-sm text-slate-600">
                  Showing {startIndex + 1} to {endIndex} of {totalElements}{" "}
                  requirements
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => goToPage(displayCurrentPage - 1)}
                    disabled={currentPage === 0}
                    className="h-8 w-8 p-0 rounded bg-white border border-slate-300 shadow-sm hover:bg-slate-50"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>

                  {getPageNumbers().map((page, index) => {
                    if (page === "...") {
                      return (
                        <span
                          key={`ellipsis-${index}`}
                          className="px-2 text-slate-400"
                        >
                          ...
                        </span>
                      );
                    }

                    return (
                      <Button
                        key={page}
                        variant={displayCurrentPage === page ? "default" : "outline"}
                        size="sm"
                        onClick={() => goToPage(page as number)}
                        className={`h-8 w-8 p-0 rounded ${
                          displayCurrentPage === page
                            ? "bg-slate-900 text-white border-slate-900 shadow-md"
                            : "bg-white text-slate-900 border border-slate-300 shadow-sm hover:bg-slate-50"
                        }`}
                      >
                        {page}
                      </Button>
                    );
                  })}

                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => goToPage(displayCurrentPage + 1)}
                    disabled={currentPage >= totalPages - 1}
                    className="h-8 w-8 p-0 rounded bg-white border border-slate-300 shadow-sm hover:bg-slate-50"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
