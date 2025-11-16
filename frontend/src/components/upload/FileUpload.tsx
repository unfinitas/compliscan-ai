"use client";

import { useState, useCallback } from "react";
import { Sparkles, X } from "lucide-react";
import { useToast } from "@/hooks/useToast";
import { Dropzone } from "@/components/ui/shadcn-io/dropzone";
import { Upload } from "lucide-react";
import { HoverBorderGradient } from "@/components/ui/hover-border-gradient";
import { Spinner } from "@/components/ui/shadcn-io/spinner";
import type { FileRejection } from "react-dropzone";
import { uploadDocument } from "@/api/upload/documentApi";
import { useMoeId } from "@/utils/moeStore";
import { startAnalysis } from "@/api/analysis/analysisApi";
import { useDocument } from "@/contexts/DocumentContext";
import { config } from "@/config/env";
import type { AnalysisResponse } from "@/types/compliance";

interface FileUploadProps {
  onDocumentUploaded?: (documentId: string) => void;
  onAnalysisComplete?: (analysisData: AnalysisResponse) => void;
}

export function FileUpload({
  onDocumentUploaded,
  onAnalysisComplete,
}: FileUploadProps = {}) {
  const { toast } = useToast();
  const { moeId, setMoeId, clearMoeId } = useMoeId();
  const { setAnalysisId } = useDocument();
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isUploaded, setIsUploaded] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [documentId, setDocumentId] = useState<string | null>(null);

  const handleFileUpload = useCallback(
    async (acceptedFiles: File[], fileRejections: FileRejection[]) => {
      if (fileRejections.length > 0) {
        const error = fileRejections[0]?.errors[0]?.message || "Invalid file";
        toast({
          title: "Upload failed",
          description: error,
          variant: "destructive",
        });
        return;
      }

      if (acceptedFiles.length > 0) {
        const selectedFile = acceptedFiles[0];
        setFile(selectedFile);
        setIsUploaded(false);
        setIsUploading(true);
        setDocumentId(null);

        try {
          const response = await uploadDocument(selectedFile, null);

          // Store the document ID (moeId) in Redux
          setMoeId(response.documentId);
          setDocumentId(response.documentId);

          // Notify parent component about the uploaded document
          onDocumentUploaded?.(response.documentId);

          setIsUploading(false);
          setIsUploaded(true);

          toast({
            title: "File uploaded successfully!",
            description: `${selectedFile.name} has been uploaded and processing has started.`,
          });
        } catch (error) {
          setIsUploading(false);
          setIsUploaded(false);
          setFile(null);

          const errorMessage =
            error instanceof Error ? error.message : "Failed to upload file";
          toast({
            title: "Upload failed",
            description: errorMessage,
            variant: "destructive",
          });
        }
      }
    },
    [toast, setMoeId, onDocumentUploaded]
  );

  const handleAnalyze = useCallback(async () => {
    if (!file || !isUploaded || !documentId) return;

    // Get moeId from Redux (should be set after document upload)
    const currentMoeId = moeId || documentId;

    if (!currentMoeId) {
      toast({
        title: "Configuration error",
        description: "Document ID is not available. Please upload a document first.",
        variant: "destructive",
      });
      return;
    }

    if (!config.defaultRegulationId) {
      toast({
        title: "Configuration error",
        description: "Regulation ID is not configured. Please set NEXT_PUBLIC_DEFAULT_REGULATION_ID in your .env.local file. Query your database: SELECT id FROM regulations WHERE active = true LIMIT 1;",
        variant: "destructive",
      });
      return;
    }

    setIsAnalyzing(true);

    try {
      // Start the analysis via API
      // moeId comes from Redux, regulationVersion is sent in body from config
      const response = await startAnalysis(currentMoeId);

      // Store the analysis ID in context
      setAnalysisId(response.analysisId);

      setIsAnalyzing(false);

      toast({
        title: "Analysis started",
        description: "Analysis has been initiated. Results will be available shortly.",
      });

      // Notify parent if callback is provided
      if (onAnalysisComplete) {
        // The actual analysis data will be fetched separately when viewing results
        onAnalysisComplete({
          moeId: currentMoeId,
          analysisId: response.analysisId,
          totalRequirements: 0,
          compliance: [],
          regulationVersion: config.regulationVersion,
        });
      }
    } catch (error) {
      setIsAnalyzing(false);
      const errorMessage =
        error instanceof Error ? error.message : "Failed to start analysis";
      toast({
        title: "Analysis failed",
        description: errorMessage,
        variant: "destructive",
      });
    }
  }, [file, isUploaded, documentId, moeId, toast, onAnalysisComplete, setAnalysisId]);

  return (
    <div className="w-full">
      {/* Upload Instructions */}
      <div className="mb-4 p-3 rounded-md bg-muted/50 border border-border">
        <p className="text-sm text-muted-foreground text-center">
          Only support for PDF file and only English
        </p>
      </div>

      {!file ? (
        <Dropzone
          onDrop={handleFileUpload}
          onError={(error) => {
            toast({
              title: "Upload error",
              description: error.message,
              variant: "destructive",
            });
          }}
          accept={{
            "application/pdf": [".pdf"],
          }}
          maxFiles={1}
          maxSize={50 * 1024 * 1024} // 50 MB
          className="w-full"
        >
          <div className="flex flex-col items-center justify-center gap-4 py-8">
            <div className="flex size-16 items-center justify-center rounded-md bg-muted text-muted-foreground">
              <Upload className="h-8 w-8" />
            </div>
            <div className="text-center">
              <p className="font-medium text-sm mb-1">
                Drag and drop your PDF file here
              </p>
              <p className="text-xs text-muted-foreground">
                or click to browse
              </p>
            </div>
          </div>
        </Dropzone>
      ) : (
        <div className="space-y-4">
          {isUploading ? (
            <div className="flex flex-col items-center justify-center p-8">
              <Spinner variant="circle" size={32} className="text-foreground" />
            </div>
          ) : isAnalyzing ? (
            <div className="flex flex-col items-center justify-center p-8">
              <Spinner variant="circle" size={32} className="text-foreground" />
            </div>
          ) : isUploaded ? (
            <div className="rounded-lg border border-border bg-card/50 p-12 min-h-[400px] flex flex-col justify-between">
              <div className="flex items-start justify-between mb-8">
                <div className="flex-1 min-w-0 pr-4">
                  <h3 className="text-xl font-semibold text-foreground mb-3 wrap-break-word">
                    {file.name}
                  </h3>
                  <p className="text-base text-muted-foreground">
                    File size: {(file.size / 1024 / 1024).toFixed(2)} MB
                  </p>
                  <p className="text-sm text-muted-foreground mt-2">
                    File type: {file.type || "Unknown"}
                  </p>
                </div>
                <button
                  onClick={() => {
                    setFile(null);
                    setIsUploaded(false);
                    setDocumentId(null);
                    clearMoeId();
                    setAnalysisId(null);
                  }}
                  className="text-muted-foreground hover:text-foreground transition-colors ml-4 shrink-0"
                >
                  <X className="h-6 w-6" />
                </button>
              </div>
              <div className="flex justify-center">
                <HoverBorderGradient
                  as="button"
                  onClick={handleAnalyze}
                  containerClassName="rounded-full"
                  className="bg-black text-white flex items-center space-x-2 px-8 py-4 text-lg"
                >
                  <Sparkles className="h-5 w-5" />
                  <span>Analyze Document</span>
                </HoverBorderGradient>
              </div>
            </div>
          ) : null}
        </div>
      )}
    </div>
  );
}
