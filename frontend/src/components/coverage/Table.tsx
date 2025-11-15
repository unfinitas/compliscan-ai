"use client";
import { useState, useMemo } from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";
import { AlertCircle, Loader2, ChevronDown } from "lucide-react";
import { Clause } from "@/api/coverage/modal/response";
import { StatusType, matchType } from "@/api/coverage/modal/enum";

type FilterStatus = "ALL" | `${StatusType}`;
type FilterMatchType = "ALL" | `${matchType}`;

const mockData: Clause[] = [
  {
    clauseId: "145.A.30(a)",
    title: "Personnel requirements",
    status: StatusType.IN_PROGRESS,
    matchType: matchType.SINGLE,
    similarity: 0.81,
    excerpt:
      "The organisation ensures that all personnel involved in the operation are properly qualified...",
    explanation:
      "Key responsibilities need to be aligned with current regulations.",
    section: "2.1 Staff Management",
  },
  {
    clauseId: "145.A.31(b)",
    title: "Training and competence",
    status: StatusType.COMPLETED,
    matchType: matchType.AGGREGATE,
    similarity: 0.95,
    excerpt:
      "Staff training requirements must cover both initial and recurrent competency assessments...",
    explanation:
      "Fully aligned with MOE standards and industry best practices.",
    section: "2.2 Training Programs",
  },
  {
    clauseId: "145.A.32(c)",
    title: "Documentation control",
    status: StatusType.FAILED,
    matchType: matchType.DISTRIBUTED,
    similarity: 0.0,
    excerpt: null,
    explanation: "No matching requirements found in current documentation.",
    section: null,
  },
  {
    clauseId: "145.A.33(d)",
    title: "Audit procedures",
    status: StatusType.PENDING,
    matchType: matchType.SINGLE,
    similarity: 0.72,
    excerpt:
      "Internal audit requirements must be conducted at least annually with documented findings...",
    explanation:
      "Scope of audits needs expansion to cover all operational areas.",
    section: "3.1 Audit Requirements",
  },
  {
    clauseId: "145.A.34(e)",
    title: "Risk management",
    status: StatusType.COMPLETED,
    matchType: matchType.AGGREGATE,
    similarity: 0.88,
    excerpt:
      "Risk assessment procedures must identify, analyze, and mitigate operational risks...",
    explanation:
      "Comprehensive coverage provided with multiple control layers.",
    section: "3.2 Risk Assessment",
  },
];

function getStatusBadge(status: Clause["status"]) {
  const styles = {
    PENDING: "bg-blue-100 text-blue-800 hover:bg-blue-100",
    IN_PROGRESS: "bg-purple-100 text-purple-800 hover:bg-purple-100",
    COMPLETED: "bg-green-100 text-green-800 hover:bg-green-100",
    FAILED: "bg-red-100 text-red-800 hover:bg-red-100",
  };
  return (
    <Badge variant="secondary" className={styles[status]}>
      {status.replace("_", " ")}
    </Badge>
  );
}

function getMatchTypeBadge(matchType: Clause["matchType"]) {
  const styles = {
    SINGLE: "bg-gray-100 text-gray-800 hover:bg-gray-100",
    AGGREGATE: "bg-cyan-100 text-cyan-800 hover:bg-cyan-100",
    DISTRIBUTED: "bg-orange-100 text-orange-800 hover:bg-orange-100",
  };
  return (
    <Badge variant="secondary" className={styles[matchType]}>
      {matchType}
    </Badge>
  );
}

function truncateText(text: string | null, maxLength: number = 60): string {
  if (!text) return "N/A";
  return text.length > maxLength ? text.substring(0, maxLength) + "..." : text;
}

const ITEMS_PER_PAGE = 10;

export default function CoverageTable() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const [filterStatus, setFilterStatus] = useState<FilterStatus>("ALL");
  const [filterMatchType, setFilterMatchType] =
    useState<FilterMatchType>("ALL");
  const [expandedRows, setExpandedRows] = useState<Set<number>>(new Set());
  const [currentPage, setCurrentPage] = useState(1);
  const [filterKey, setFilterKey] = useState(
    `${filterStatus}-${filterMatchType}`
  );

  // Compute filter key and reset page when filters change
  const currentFilterKey = useMemo(
    () => `${filterStatus}-${filterMatchType}`,
    [filterStatus, filterMatchType]
  );

  // Reset page when filter key changes
  if (currentFilterKey !== filterKey) {
    setFilterKey(currentFilterKey);
    setCurrentPage(1);
  }

  const filteredData = useMemo(() => {
    let data = mockData;

    if (filterStatus !== "ALL") {
      data = data.filter((clause) => clause.status === filterStatus);
    }

    if (filterMatchType !== "ALL") {
      data = data.filter((clause) => clause.matchType === filterMatchType);
    }

    return data;
  }, [filterStatus, filterMatchType]);

  const paginatedData = useMemo(() => {
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    const endIndex = startIndex + ITEMS_PER_PAGE;
    return filteredData.slice(startIndex, endIndex);
  }, [filteredData, currentPage]);

  const totalPages = Math.ceil(filteredData.length / ITEMS_PER_PAGE);

  const toggleExpandRow = (idx: number) => {
    const newExpanded = new Set(expandedRows);
    if (newExpanded.has(idx)) {
      newExpanded.delete(idx);
    } else {
      newExpanded.add(idx);
    }
    setExpandedRows(newExpanded);
  };

  const handleLoadData = () => {
    setLoading(true);
    setTimeout(() => setLoading(false), 1000);
  };

  return (
    <div className="min-h-screen bg-linear-to-b from-gray-50 to-white p-8">
      <div className="mx-auto max-w-7xl space-y-6">
        {/* Header */}
        <div className="space-y-2">
          <h1 className="text-3xl font-bold text-gray-900">
            Coverage Dashboard
          </h1>
          <p className="text-gray-600">
            Review compliance coverage across clauses and MOE requirements
          </p>
        </div>

        {/* Error State */}
        {error && (
          <Card className="border-red-200 bg-red-50">
            <CardContent className="flex items-center gap-3 pt-6">
              <AlertCircle className="h-5 w-5 text-red-600" />
              <div>
                <p className="font-medium text-red-900">Error loading data</p>
                <p className="text-sm text-red-700">Please try again later</p>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Controls Card */}
        <Card>
          <CardHeader>
            <CardTitle>Filters</CardTitle>
            <CardDescription>Filter by status and match type</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
              {/* Status Dropdown */}
              <div className="flex-1">
                <p className="mb-2 text-sm font-medium text-gray-700">Status</p>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="outline"
                      className="w-full justify-between"
                    >
                      {filterStatus.replace("_", " ")}
                      <ChevronDown className="h-4 w-4 opacity-50" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="start">
                    {(
                      [
                        "ALL",
                        "PENDING",
                        "IN_PROGRESS",
                        "COMPLETED",
                        "FAILED",
                      ] as const
                    ).map((status) => (
                      <DropdownMenuItem
                        key={status}
                        onClick={() => setFilterStatus(status)}
                        className={filterStatus === status ? "bg-accent" : ""}
                      >
                        {status.replace("_", " ")}
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>

              {/* Match Type Dropdown */}
              <div className="flex-1">
                <p className="mb-2 text-sm font-medium text-gray-700">
                  Match Type
                </p>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="outline"
                      className="w-full justify-between"
                    >
                      {filterMatchType}
                      <ChevronDown className="h-4 w-4 opacity-50" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="start">
                    {(
                      ["ALL", "SINGLE", "AGGREGATE", "DISTRIBUTED"] as const
                    ).map((type) => (
                      <DropdownMenuItem
                        key={type}
                        onClick={() => setFilterMatchType(type)}
                        className={filterMatchType === type ? "bg-accent" : ""}
                      >
                        {type}
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>

              <Button
                onClick={handleLoadData}
                disabled={loading}
                variant="outline"
                className="border-gray-300"
              >
                {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {loading ? "Refreshing..." : "Refresh"}
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Table Card */}
        <Card>
          <CardHeader>
            <CardTitle>Coverage Details</CardTitle>
            <CardDescription>
              {filteredData.length} clauses found
            </CardDescription>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="flex items-center justify-center py-12">
                <div className="text-center">
                  <Loader2 className="mx-auto h-8 w-8 animate-spin text-gray-400" />
                  <p className="mt-2 text-gray-600">Loading data...</p>
                </div>
              </div>
            ) : filteredData.length === 0 ? (
              <div className="flex items-center justify-center py-12">
                <div className="text-center">
                  <AlertCircle className="mx-auto h-8 w-8 text-gray-400" />
                  <p className="mt-2 text-gray-600">
                    No clauses found with these filters
                  </p>
                </div>
              </div>
            ) : (
              <>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-20">Clause ID</TableHead>
                      <TableHead>Title</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Match Type</TableHead>
                      <TableHead>Similarity</TableHead>
                      <TableHead className="text-center w-10">
                        Details
                      </TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {paginatedData.map((clause, idx) => {
                      const globalIndex =
                        (currentPage - 1) * ITEMS_PER_PAGE + idx;
                      return [
                        <TableRow key={`row-${globalIndex}`}>
                          <TableCell className="font-medium">
                            {clause.clauseId}
                          </TableCell>
                          <TableCell>
                            <p className="font-medium">{clause.title}</p>
                          </TableCell>
                          <TableCell>{getStatusBadge(clause.status)}</TableCell>
                          <TableCell>
                            {getMatchTypeBadge(clause.matchType)}
                          </TableCell>
                          <TableCell className="font-medium">
                            {(clause.similarity * 100).toFixed(0)}%
                          </TableCell>
                          <TableCell className="text-center">
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => toggleExpandRow(globalIndex)}
                            >
                              <ChevronDown
                                className={`h-4 w-4 transition-transform ${
                                  expandedRows.has(globalIndex)
                                    ? "rotate-180"
                                    : ""
                                }`}
                              />
                            </Button>
                          </TableCell>
                        </TableRow>,
                        expandedRows.has(globalIndex) && (
                          <TableRow key={`expanded-${globalIndex}`}>
                            <TableCell colSpan={6} className="bg-muted/50">
                              <div className="space-y-3 py-2">
                                {clause.section && (
                                  <div>
                                    <p className="text-xs font-semibold text-muted-foreground uppercase">
                                      Section
                                    </p>
                                    <p className="text-sm">{clause.section}</p>
                                  </div>
                                )}
                                {clause.excerpt && (
                                  <div>
                                    <p className="text-xs font-semibold text-muted-foreground uppercase">
                                      Excerpt
                                    </p>
                                    <p className="text-sm">
                                      {truncateText(clause.excerpt)}
                                    </p>
                                  </div>
                                )}
                                {clause.explanation && (
                                  <div>
                                    <p className="text-xs font-semibold text-muted-foreground uppercase">
                                      Explanation
                                    </p>
                                    <p className="text-sm">
                                      {truncateText(clause.explanation)}
                                    </p>
                                  </div>
                                )}
                              </div>
                            </TableCell>
                          </TableRow>
                        ),
                      ].filter(Boolean);
                    })}
                  </TableBody>
                </Table>
                {totalPages > 1 && (
                  <div className="mt-4">
                    <Pagination>
                      <PaginationContent>
                        <PaginationItem>
                          <PaginationPrevious
                            href="#"
                            onClick={(e) => {
                              e.preventDefault();
                              if (currentPage > 1) {
                                setCurrentPage(currentPage - 1);
                              }
                            }}
                            className={
                              currentPage === 1
                                ? "pointer-events-none opacity-50"
                                : "cursor-pointer"
                            }
                          />
                        </PaginationItem>
                        {(() => {
                          const pages: (number | "ellipsis")[] = [];
                          const showEllipsis = totalPages > 7;

                          if (!showEllipsis) {
                            // Show all pages if 7 or fewer
                            for (let i = 1; i <= totalPages; i++) {
                              pages.push(i);
                            }
                          } else {
                            // Always show first page
                            pages.push(1);

                            if (currentPage <= 3) {
                              // Near the start
                              for (let i = 2; i <= 4; i++) {
                                pages.push(i);
                              }
                              pages.push("ellipsis");
                              pages.push(totalPages);
                            } else if (currentPage >= totalPages - 2) {
                              // Near the end
                              pages.push("ellipsis");
                              for (
                                let i = totalPages - 3;
                                i <= totalPages;
                                i++
                              ) {
                                pages.push(i);
                              }
                            } else {
                              // In the middle
                              pages.push("ellipsis");
                              for (
                                let i = currentPage - 1;
                                i <= currentPage + 1;
                                i++
                              ) {
                                pages.push(i);
                              }
                              pages.push("ellipsis");
                              pages.push(totalPages);
                            }
                          }

                          return pages.map((page, idx) => {
                            if (page === "ellipsis") {
                              return (
                                <PaginationItem key={`ellipsis-${idx}`}>
                                  <PaginationEllipsis />
                                </PaginationItem>
                              );
                            }
                            return (
                              <PaginationItem key={page}>
                                <PaginationLink
                                  href="#"
                                  onClick={(e) => {
                                    e.preventDefault();
                                    setCurrentPage(page);
                                  }}
                                  isActive={currentPage === page}
                                  className="cursor-pointer"
                                >
                                  {page}
                                </PaginationLink>
                              </PaginationItem>
                            );
                          });
                        })()}
                        <PaginationItem>
                          <PaginationNext
                            href="#"
                            onClick={(e) => {
                              e.preventDefault();
                              if (currentPage < totalPages) {
                                setCurrentPage(currentPage + 1);
                              }
                            }}
                            className={
                              currentPage === totalPages
                                ? "pointer-events-none opacity-50"
                                : "cursor-pointer"
                            }
                          />
                        </PaginationItem>
                      </PaginationContent>
                    </Pagination>
                  </div>
                )}
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
