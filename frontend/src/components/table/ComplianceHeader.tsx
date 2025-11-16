"use client";

import { Button } from "@/components/ui/button";

interface ComplianceHeaderProps {
  statusCounts: {
    full: number;
    partial: number;
    non: number;
  };
  statusFilter: "all" | "full" | "partial" | "non";
  onStatusFilterChange: (status: "all" | "full" | "partial" | "non") => void;
}

export function ComplianceHeader({
  statusCounts,
  statusFilter,
  onStatusFilterChange,
}: ComplianceHeaderProps) {
  const totalRequirements =
    statusCounts.full + statusCounts.partial + statusCounts.non;

  return (
    <div className="sticky top-0 z-40 bg-white border-b border-slate-200">
      <div className="max-w-5xl mx-auto px-6 py-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">
              Compliance Requirements
            </h1>
            <p className="mt-2 text-sm text-slate-500">
              Review and track all {totalRequirements} requirements
            </p>
          </div>
        </div>

        {/* Filter buttons */}
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-slate-700 mr-2">
            Filter by status:
          </span>
          <Button
            variant={statusFilter === "all" ? "default" : "outline"}
            size="sm"
            onClick={() => onStatusFilterChange("all")}
            className={`${
              statusFilter === "all"
                ? "bg-slate-700 hover:bg-slate-800 text-white"
                : "border-slate-300 text-slate-700 hover:bg-slate-50"
            }`}
          >
            All ({totalRequirements})
          </Button>
          <Button
            variant={statusFilter === "full" ? "default" : "outline"}
            size="sm"
            onClick={() => onStatusFilterChange("full")}
            className={`${
              statusFilter === "full"
                ? "bg-emerald-600 hover:bg-emerald-700 text-white"
                : "border-emerald-200 text-emerald-700 hover:bg-emerald-50"
            }`}
          >
            Compliant ({statusCounts.full})
          </Button>
          <Button
            variant={statusFilter === "partial" ? "default" : "outline"}
            size="sm"
            onClick={() => onStatusFilterChange("partial")}
            className={`${
              statusFilter === "partial"
                ? "bg-amber-500 hover:bg-amber-600 text-white"
                : "border-amber-200 text-amber-700 hover:bg-amber-50"
            }`}
          >
            Partial ({statusCounts.partial})
          </Button>
          <Button
            variant={statusFilter === "non" ? "default" : "outline"}
            size="sm"
            onClick={() => onStatusFilterChange("non")}
            className={`${
              statusFilter === "non"
                ? "bg-rose-600 hover:bg-rose-700 text-white"
                : "border-rose-200 text-rose-700 hover:bg-rose-50"
            }`}
          >
            Non-compliant ({statusCounts.non})
          </Button>
        </div>
      </div>
    </div>
  );
}
