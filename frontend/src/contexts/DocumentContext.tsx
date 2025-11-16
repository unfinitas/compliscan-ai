"use client";

import { createContext, useContext, useState, ReactNode } from "react";

interface DocumentContextType {
  uploadedDocumentId: string | null;
  setUploadedDocumentId: (id: string | null) => void;
  analysisId: string | null;
  setAnalysisId: (id: string | null) => void;
  isAnalyzing: boolean;
  setIsAnalyzing: (isAnalyzing: boolean) => void;
}

const DocumentContext = createContext<DocumentContextType | undefined>(undefined);

export function DocumentProvider({ children }: { children: ReactNode }) {
  const [uploadedDocumentId, setUploadedDocumentId] = useState<string | null>(null);
  const [analysisId, setAnalysisId] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState<boolean>(false);

  return (
    <DocumentContext.Provider value={{
      uploadedDocumentId,
      setUploadedDocumentId,
      analysisId,
      setAnalysisId,
      isAnalyzing,
      setIsAnalyzing
    }}>
      {children}
    </DocumentContext.Provider>
  );
}

export function useDocument() {
  const context = useContext(DocumentContext);
  if (!context) {
    throw new Error("useDocument must be used within DocumentProvider");
  }
  return context;
}

