"use client";

import { ComplianceRequirementsContainer } from "@/components/table/ComplianceRequirementsContainer";
import { useDocument } from "@/contexts/DocumentContext";
import { Spinner } from "@/components/ui/shadcn-io/spinner";

export default function Home() {
  const { analysisId, isAnalyzing } = useDocument();

  return (
    <div className="relative min-h-screen overflow-hidden bg-white">
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#00000008_1px,transparent_1px),linear-gradient(to_bottom,#00000008_1px,transparent_1px)] bg-size-[4rem_4rem]" />
      <div className="relative h-full">
        {/* Loading Overlay when analyzing */}
        {isAnalyzing && (
          <div className="fixed inset-0 bg-white/80 backdrop-blur-sm z-50 flex items-center justify-center">
            <div className="flex flex-col items-center justify-center space-y-4">
              <Spinner variant="circle" size={48} className="text-foreground" />
              <div className="text-center">
                <p className="text-xl font-semibold text-foreground mb-2">
                  Analyzing Document
                </p>
                <p className="text-sm text-muted-foreground">
                  Processing compliance requirements. This may take a few minutes...
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Main Content */}
        <div className="p-6 lg:p-8">
          {!analysisId ? (
            <div className="flex items-center justify-center min-h-[400px]">
              <div className="text-center">
                <p className="text-slate-600 text-lg mb-2">
                  Upload a document and start analysis to view compliance requirements
                </p>
                <p className="text-slate-500 text-sm">
                  Use the sidebar to upload your document
                </p>
              </div>
            </div>
          ) : (
            <ComplianceRequirementsContainer analysisId={analysisId} />
          )}
        </div>
      </div>
    </div>
  );
}
