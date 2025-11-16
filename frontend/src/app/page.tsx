"use client";

import { ComplianceRequirementsContainer } from "@/components/table/ComplianceRequirementsContainer";
import { useDocument } from "@/contexts/DocumentContext";

export default function Home() {
  const { analysisId } = useDocument();

  return (
    <div className="relative min-h-screen overflow-hidden bg-white">
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#00000008_1px,transparent_1px),linear-gradient(to_bottom,#00000008_1px,transparent_1px)] bg-size-[4rem_4rem]" />
      <div className="relative h-full">
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
