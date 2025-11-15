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

export function FileUpload() {
  const { toast } = useToast();
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isUploaded, setIsUploaded] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [analyzeProgress, setAnalyzeProgress] = useState(0);

  const handleFileUpload = useCallback(
    (acceptedFiles: File[], fileRejections: FileRejection[]) => {
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
        setFile(acceptedFiles[0]);
        setIsUploaded(false);
        setIsUploading(true);
        setUploadProgress(0);

        // Simulate upload progress
        const interval = setInterval(() => {
          setUploadProgress((prev) => {
            if (prev >= 100) {
              clearInterval(interval);
              setIsUploading(false);
              setIsUploaded(true);
              toast({
                title: "File uploaded successfully!",
                description: `${acceptedFiles[0].name} has been uploaded.`,
              });
              return 100;
            }
            return prev + 10;
          });
        }, 200);
      }
    },
    [toast]
  );

  const handleAnalyze = useCallback(() => {
    if (!file || !isUploaded) return;

    setIsAnalyzing(true);
    setAnalyzeProgress(0);

  }, [file, isUploaded]);

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

