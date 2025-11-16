"use client";

import { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { getAnalysisIdFromStorage, setAnalysisIdInStorage } from "@/utils/moeStore";

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
  // Initialize from localStorage on mount
  const [uploadedDocumentId, setUploadedDocumentId] = useState<string | null>(null);
  const [analysisId, setAnalysisId] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState<boolean>(false);
  const [isHydrated, setIsHydrated] = useState(false);

  // Restore from localStorage only on client after mount (prevents hydration mismatch)
  useEffect(() => {
    if (typeof window !== "undefined") {
      const storedAnalysisId = getAnalysisIdFromStorage();
      if (storedAnalysisId) {
        setAnalysisId(storedAnalysisId);
      }
      setIsHydrated(true);
    }
  }, []);

  // Sync analysisId to localStorage whenever it changes (only after hydration)
  useEffect(() => {
    if (isHydrated) {
      setAnalysisIdInStorage(analysisId);
    }
  }, [analysisId, isHydrated]);

  // Wrapper to set analysisId and sync to localStorage
  const setAnalysisIdWithStorage = (id: string | null) => {
    setAnalysisId(id);
    setAnalysisIdInStorage(id);
  };

  return (
    <DocumentContext.Provider value={{
      uploadedDocumentId,
      setUploadedDocumentId,
      analysisId,
      setAnalysisId: setAnalysisIdWithStorage,
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

