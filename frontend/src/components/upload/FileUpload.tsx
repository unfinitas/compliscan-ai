"use client";

import { useState, useCallback } from "react";
import { Sparkles, X } from "lucide-react";
import { useToast } from "@/hooks/useToast";
import { Dropzone } from "@/components/ui/shadcn-io/dropzone";
import { Upload } from "lucide-react";
import { HoverBorderGradient } from "@/components/ui/hover-border-gradient";
import { Spinner } from "@/components/ui/shadcn-io/spinner";
import type { FileRejection } from "react-dropzone";
import { uploadDocument, pollDocumentStatus } from "@/api/upload/documentApi";
import { useMoeId } from "@/utils/moeStore";
import { startAnalysis } from "@/api/analysis/analysisApi";
import { useDocument } from "@/contexts/DocumentContext";
import { UploadProgress } from "./progress";
import type { AnalysisResponse } from "@/types/compliance";

/**
 * Format file name to show first 4 words with .pdf extension
 */
function formatFileName(fileName: string): string {
  if (!fileName) return "";

  // Remove .pdf extension temporarily
  const nameWithoutExt = fileName.replace(/\.pdf$/i, "");

  // Split into words
  const words = nameWithoutExt.split(/[\s_-]+/).filter(Boolean);

  // Take first 4 words
  const firstFourWords = words.slice(0, 4).join(" ");

  // Add .pdf extension back
  return firstFourWords ? `${firstFourWords}.pdf` : fileName;
}

interface FileUploadProps {
  onDocumentUploaded?: (documentId: string) => void;
  onAnalysisComplete?: (analysisData: AnalysisResponse) => void;
  onFileNameChange?: (fileName: string | null) => void;
}

export function FileUpload({
  onDocumentUploaded,
  onAnalysisComplete,
  onFileNameChange,
}: FileUploadProps = {}) {
  const { toast } = useToast();
  const { moeId, setMoeId, clearMoeId } = useMoeId();
  const { setAnalysisId } = useDocument();
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isUploaded, setIsUploaded] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [documentId, setDocumentId] = useState<string | null>(null);
  const [showProgress, setShowProgress] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

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
        setShowProgress(true);
        onFileNameChange?.(formatFileName(selectedFile.name));

        try {
          // Upload the document
          const response = await uploadDocument(selectedFile, null);

          // Store the document ID (moeId) in Redux
          setMoeId(response.documentId);
          setDocumentId(response.documentId);

          // Notify parent component about the uploaded document
          onDocumentUploaded?.(response.documentId);

          // Start polling for document status every 5 seconds
          setUploadProgress(0);
          await pollDocumentStatus(
            response.documentId,
            response.documentId,
            (progress) => {
              setUploadProgress(progress);
            },
            5000 // Poll every 5 seconds
          );

          // Processing complete
          setIsUploading(false);
          setIsUploaded(true);
          setShowProgress(false);
          setUploadProgress(100);

          toast({
            title: "File uploaded successfully!",
            description: `${selectedFile.name} has been uploaded and processing is complete.`,
          });
        } catch (error) {
          setIsUploading(false);
          setIsUploaded(false);
          setFile(null);
          setShowProgress(false);
          onFileNameChange?.(null);

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
    [toast, setMoeId, onDocumentUploaded, onFileNameChange]
  );

  const handleAnalyze = useCallback(async () => {
    // Prevent multiple clicks
    if (isAnalyzing) {
      return;
    }

    if (!file || !isUploaded || !documentId) {
      return;
    }

    // Get moeId from Redux (should be set after document upload)
    const currentMoeId = moeId || documentId;

    if (!currentMoeId) {
      toast({
        title: "Configuration error",
        description:
          "Document ID is not available. Please upload a document first.",
        variant: "destructive",
      });
      return;
    }

    // Set analyzing state immediately
    setIsAnalyzing(true);

    try {
      // Start the analysis via API immediately when button is clicked
      const response = await startAnalysis(currentMoeId);

      // Store the analysis ID in context immediately
      setAnalysisId(response.analysisId);

      toast({
        title: "Analysis started",
        description:
          "Analysis is now running. Results will be available shortly.",
      });

      // Notify parent if callback is provided
      if (onAnalysisComplete) {
        // The actual analysis data will be fetched separately when viewing results
        onAnalysisComplete({
          moeId: currentMoeId,
          analysisId: response.analysisId,
          totalRequirements: 0,
          compliance: [],
          regulationVersion: "",
        });
      }

      // Keep analyzing state true - will be updated when progress API is implemented
      // For now, we keep it running to show the user that analysis is in progress
      // setIsAnalyzing(false); // Will be controlled by progress API later
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
  }, [
    file,
    isUploaded,
    documentId,
    moeId,
    isAnalyzing,
    toast,
    onAnalysisComplete,
    setAnalysisId,
  ]);

  return (
    <div className="w-full">
      {/* Upload Instructions */}
      <div className="mb-4 p-3 rounded-md bg-muted/50 border border-border">
        <p className="text-sm text-muted-foreground text-center">
          For now only support for PDF file and only English
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
          {isUploading && showProgress ? (
            <div className="rounded-lg border border-border bg-card/50 p-8">
              <div className="mb-4 text-center">
                <h3 className="text-lg font-semibold text-foreground mb-2">
                  {file.name ? formatFileName(file.name) : "Uploading..."}
                </h3>
              </div>
              <UploadProgress progress={uploadProgress} />
            </div>
          ) : isUploading ? (
            <div className="flex flex-col items-center justify-center p-8">
              <Spinner variant="circle" size={32} className="text-foreground" />
            </div>
          ) : isAnalyzing ? (
            <div className="rounded-lg border border-border bg-card/50 p-8">
              <div className="flex flex-col items-center justify-center space-y-4">
                <Spinner
                  variant="circle"
                  size={32}
                  className="text-foreground"
                />
                <div className="text-center">
                  <p className="text-lg font-semibold text-foreground mb-2">
                    Analyzing Document
                  </p>
                  <p className="text-sm text-muted-foreground">
                    Processing compliance requirements. This may take a few
                    minutes...
                  </p>
                </div>
              </div>
            </div>
          ) : isUploaded ? (
            <div className="rounded-lg border border-border bg-card/50 p-12 min-h-[400px] flex flex-col justify-between">
              <div className="flex items-start justify-between mb-8">
                <div className="flex-1 min-w-0 pr-4">
                  <h3 className="text-xl font-semibold text-foreground mb-3 wrap-break-word">
                    {formatFileName(file.name)}
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
                    onFileNameChange?.(null);
                  }}
                  className="text-muted-foreground hover:text-foreground transition-colors ml-4 shrink-0 cursor-pointer hover:scale-110 active:scale-95"
                >
                  <X className="h-6 w-6" />
                </button>
              </div>
              <div className="flex justify-center">
                <HoverBorderGradient
                  as="button"
                  onClick={(e) => {
                    if (isAnalyzing) {
                      e.preventDefault();
                      e.stopPropagation();
                      return;
                    }
                    e.preventDefault();
                    e.stopPropagation();
                    handleAnalyze();
                  }}
                  containerClassName="rounded-full"
                  className={`bg-black text-white flex items-center space-x-2 px-8 py-4 text-lg ${
                    isAnalyzing
                      ? "opacity-50 cursor-not-allowed pointer-events-none"
                      : "cursor-pointer hover:opacity-90 active:opacity-80"
                  }`}
                >
                  {isAnalyzing ? (
                    <>
                      <Spinner
                        variant="circle"
                        size={20}
                        className="text-white"
                      />
                      <span>Analyzing...</span>
                    </>
                  ) : (
                    <>
                      <Sparkles className="h-5 w-5" />
                      <span>Analyze Document</span>
                    </>
                  )}
                </HoverBorderGradient>
              </div>
            </div>
          ) : null}
        </div>
      )}
    </div>
  );
}
