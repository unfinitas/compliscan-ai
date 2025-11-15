"use client";

import { useState, useCallback } from "react";
import { Sparkles } from "lucide-react";
import { useToast } from "@/hooks/useToast";
import { Progress } from "@/components/ui/progress";
import {
  Dropzone,
  DropzoneEmptyState,
} from "@/components/ui/shadcn-io/dropzone";
import { HoverBorderGradient } from "@/components/ui/hover-border-gradient";
import type { FileRejection } from "react-dropzone";
import { uploadDocument, getDocumentStatus } from "@/api/upload/documentApi";
import { useMoeId } from "@/utils/moeStore";
import { ProcessingStatus } from "@/api/model/ProcessingStatus";

export function FileUpload() {
  const { toast } = useToast();
  const { setMoeId, clearMoeId } = useMoeId();
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isUploaded, setIsUploaded] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [analyzeProgress, setAnalyzeProgress] = useState(0);
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
        setUploadProgress(0);
        setDocumentId(null);

        try {
          setUploadProgress(50);

          const response = await uploadDocument(selectedFile, null);

          if (
            response.status === "success" ||
            response.status === "SUCCESS" ||
            response.data
          ) {
            // Store the document ID (moeId) in Redux
            setMoeId(response.data.documentId);
            setDocumentId(response.data.documentId);

            setIsUploading(false);
            setIsUploaded(true);
            setUploadProgress(100);

            toast({
              title: "File uploaded successfully!",
              description: `${selectedFile.name} has been uploaded and processing has started.`,
            });
          } else {
            setIsUploading(false);
            setIsUploaded(false);
            setUploadProgress(0);
            setFile(null);

            toast({
              title: "Upload failed",
              description: response.message || "Failed to upload file",
              variant: "destructive",
            });
          }
        } catch (error) {
          setIsUploading(false);
          setIsUploaded(false);
          setUploadProgress(0);
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
    [toast, setMoeId]
  );

  const handleAnalyze = useCallback(async () => {
    if (!file || !isUploaded || !documentId) return;

    setIsAnalyzing(true);
    setAnalyzeProgress(0);

    const pollStatus = async () => {
      try {
        const response = await getDocumentStatus(documentId, null);

        if (
          response.status === "success" ||
          response.status === "SUCCESS" ||
          response.data
        ) {
          const status = response.data;

          if (status.status === ProcessingStatus.COMPLETED) {
            setAnalyzeProgress(100);
            setIsAnalyzing(false);
            toast({
              title: "Analysis complete!",
              description: `Document processed successfully with ${status.paragraphCount} paragraphs.`,
            });
          } else if (status.status === ProcessingStatus.FAILED) {
            setIsAnalyzing(false);
            toast({
              title: "Analysis failed",
              description: status.errorMessage || "Failed to process document",
              variant: "destructive",
            });
          } else if (status.status === ProcessingStatus.PROCESSING) {
            if (status.paragraphCount > 0) {
              setAnalyzeProgress(Math.min(90, status.paragraphCount * 2));
            } else {
              setAnalyzeProgress(10);
            }
            setTimeout(pollStatus, 2000);
          }
        } else {
          setIsAnalyzing(false);
          toast({
            title: "Status check failed",
            description: response.message || "Failed to check status",
            variant: "destructive",
          });
        }
      } catch (error) {
        setIsAnalyzing(false);
        const errorMessage =
          error instanceof Error ? error.message : "Failed to check status";
        toast({
          title: "Status check failed",
          description: errorMessage,
          variant: "destructive",
        });
      }
    };

    pollStatus();
  }, [file, isUploaded, documentId, toast]);

  return (
    <div className="mb-16">
      {!file ? (
        <div className="mx-auto max-w-2xl rounded-2xl border border-neutral-200 bg-neutral-50/50 p-8">
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
              "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                [".docx"],
            }}
            maxFiles={1}
            className="border-dashed"
          >
            <DropzoneEmptyState />
          </Dropzone>
        </div>
      ) : (
        <div className="mx-auto max-w-2xl space-y-6">
          <div className="rounded-2xl border border-neutral-200 bg-white p-8">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-lg font-semibold text-neutral-900 mb-1">
                  {file.name}
                </h3>
                <p className="text-sm text-neutral-600">
                  {(file.size / 1024 / 1024).toFixed(2)} MB
                </p>
              </div>
              {!isUploading && !isAnalyzing && (
                <button
                  onClick={() => {
                    setFile(null);
                    setIsUploaded(false);
                    setUploadProgress(0);
                    setAnalyzeProgress(0);
                    setDocumentId(null);
                    clearMoeId();
                  }}
                  className="text-neutral-600 hover:text-neutral-900 transition-colors"
                >
                  Remove
                </button>
              )}
            </div>
            {isUploading ? (
              <div className="space-y-4">
                <Progress value={uploadProgress} className="w-full" />
                <p className="text-sm text-neutral-600 text-center">
                  Uploading file... {uploadProgress}%
                </p>
              </div>
            ) : isAnalyzing ? (
              <div className="space-y-4">
                <Progress value={analyzeProgress} className="w-full" />
                <p className="text-sm text-neutral-600 text-center">
                  Analyzing document... {analyzeProgress}%
                </p>
              </div>
            ) : isUploaded ? (
              <div className="flex justify-center">
                <HoverBorderGradient
                  as="button"
                  onClick={handleAnalyze}
                  containerClassName="rounded-full"
                  className="bg-black text-white flex items-center space-x-2 px-6 py-3"
                >
                  <Sparkles className="h-4 w-4" />
                  <span>Analyze Document</span>
                </HoverBorderGradient>
              </div>
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
}
