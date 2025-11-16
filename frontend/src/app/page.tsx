"use client";

import { ComplianceRequirementsContainer } from "@/components/table/ComplianceRequirementsContainer";
import { useDocument } from "@/contexts/DocumentContext";
import { AnalysisSteps } from "@/components/upload/AnalysisSteps";
import { useEffect, useState } from "react";
import { getMoeIdFromStorage } from "@/utils/moeStore";
import { getAnalysisReport } from "@/api/analysis/analysisApi";

export default function Home() {
  const { analysisId, isAnalyzing, setIsAnalyzing } = useDocument();
  const [apiCompleted, setApiCompleted] = useState(false);

  // Check if analysis is complete by trying to fetch the report
  useEffect(() => {
    if (isAnalyzing && analysisId) {
      // Poll for analysis completion
      const checkAnalysisComplete = async () => {
        try {
          const moeId = getMoeIdFromStorage();

          if (moeId) {
            const report = await getAnalysisReport(analysisId, moeId);
            if (report && report.compliance && report.compliance.length > 0) {
              setApiCompleted(true);
              setIsAnalyzing(false);
            }
          }
        } catch {
          // Analysis not ready yet, continue waiting
        }
      };

      // Check every 10 seconds
      const interval = setInterval(checkAnalysisComplete, 10000);
      checkAnalysisComplete(); // Check immediately

      return () => clearInterval(interval);
    }
  }, [isAnalyzing, analysisId, setIsAnalyzing]);

  return (
    <div className="relative min-h-screen overflow-hidden bg-white">
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#00000008_1px,transparent_1px),linear-gradient(to_bottom,#00000008_1px,transparent_1px)] bg-size-[4rem_4rem]" />
      <div className="relative h-full">
        {/* Loading Overlay when analyzing */}
        {isAnalyzing && (
          <div className="fixed inset-0 bg-white/80 backdrop-blur-sm z-50 flex items-center justify-center">
            <div className="w-full max-w-2xl px-6">
              <div className="mb-6 text-center">
                <h2 className="text-2xl font-semibold text-foreground mb-2">
                  Analyzing Document
                </h2>
              </div>
              <AnalysisSteps
                isActive={isAnalyzing}
                apiCompleted={apiCompleted}
                onComplete={() => {
                  setIsAnalyzing(false);
                }}
              />
            </div>
          </div>
        )}

        {/* Main Content */}
        <div className="p-6 lg:p-8">
          {!analysisId ? (
            <div className="flex items-center justify-center min-h-[400px]">
              <div className="text-center">
                <p className="text-slate-600 text-lg mb-2">
                  Upload a document and start analysis to view compliance
                  requirements
                </p>
                <p className="text-slate-500 text-sm">
                  Use the sidebar to upload your document
                </p>
              </div>
            </div>
          ) : (
            <div className="w-full">
              <ComplianceRequirementsContainer analysisId={analysisId} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
