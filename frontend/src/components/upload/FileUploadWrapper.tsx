'use client';

import { useState, useCallback } from 'react';
import { Sparkles } from 'lucide-react';
import { useToast } from '@/hooks/useToast';
import { Progress } from '@/components/ui/progress';
import {
  Dropzone,
  DropzoneEmptyState,
} from '@/components/ui/shadcn-io/dropzone';
import { HoverBorderGradient } from '@/components/ui/hover-border-gradient';
import { Spinner } from '@/components/ui/shadcn-io/spinner';
import type { FileRejection } from 'react-dropzone';
import { uploadDocument } from '@/api/upload/documentApi';
import { useMoeId } from '@/utils/moeStore';

interface FileUploadProps {
  onDocumentUploaded?: (documentId: string) => void;
}

export function FileUpload({ onDocumentUploaded }: FileUploadProps) {
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
        const error = fileRejections[0]?.errors[0]?.message || 'Invalid file';
        toast({
          title: 'Upload failed',
          description: error,
          variant: 'destructive',
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

          setMoeId(response.documentId);
          setDocumentId(response.documentId);
          onDocumentUploaded?.(response.documentId);

          setIsUploading(false);
          setIsUploaded(true);
          setUploadProgress(100);

          toast({
            title: 'File uploaded successfully!',
            description: `${selectedFile.name} has been uploaded and processing has started.`,
          });
        } catch (error) {
          setIsUploading(false);
          setIsUploaded(false);
          setUploadProgress(0);
          setFile(null);

          const errorMessage =
            error instanceof Error ? error.message : 'Failed to upload file';
          toast({
            title: 'Upload failed',
            description: errorMessage,
            variant: 'destructive',
          });
        }
      }
    },
    [toast, setMoeId, onDocumentUploaded]
  );

  const handleAnalyze = useCallback(async () => {
    if (!file || !isUploaded || !documentId) return;

    setIsAnalyzing(true);
    setAnalyzeProgress(0);

    const interval = setInterval(() => {
      setAnalyzeProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          setIsAnalyzing(false);
          toast({
            title: 'Analysis complete',
            description: 'Document analysis has been completed successfully!',
          });
          return 100;
        }
        return prev + 2;
      });
    }, 100);

    setTimeout(() => {
      clearInterval(interval);
    }, 5000);
  }, [file, isUploaded, documentId, toast]);

  return (
    <div className="w-full">
      {!file ? (
        <div className="rounded-lg border border-border bg-muted/50 p-4">
          <Dropzone
            onDrop={handleFileUpload}
            onError={(error) => {
              toast({
                title: 'Upload error',
                description: error.message,
                variant: 'destructive',
              });
            }}
            accept={{
              'application/pdf': ['.pdf'],
              'application/vnd.openxmlformats-officedocument.wordprocessingml.document':
                ['.docx'],
            }}
            maxFiles={1}
            className="border-dashed"
          >
            <DropzoneEmptyState />
          </Dropzone>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="rounded-lg border border-border bg-card p-4">
            <div className="flex items-start justify-between mb-4">
              <div>
                <h3 className="font-semibold text-foreground mb-1">
                  {file.name}
                </h3>
                <p className="text-sm text-muted-foreground">
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
                  className="text-muted-foreground hover:text-foreground transition-colors text-sm"
                >
                  Remove
                </button>
              )}
            </div>

            {isUploading ? (
              <div className="space-y-3">
                <Progress value={uploadProgress} className="w-full" />
                <p className="text-xs text-muted-foreground text-center">
                  Uploading file... {uploadProgress}%
                </p>
              </div>
            ) : isAnalyzing ? (
              <div className="space-y-3">
                <div className="flex flex-col items-center justify-center space-y-2">
                  <Spinner variant="circle" size={24} className="text-muted-foreground" />
                  <p className="text-xs text-muted-foreground text-center">
                    Analyzing document... {analyzeProgress}%
                  </p>
                </div>
                <Progress value={analyzeProgress} className="w-full" />
              </div>
            ) : isUploaded ? (
              <div className="flex justify-center">
                <HoverBorderGradient
                  as="button"
                  onClick={handleAnalyze}
                  containerClassName="rounded-full"
                  className="bg-primary text-primary-foreground flex items-center space-x-2 px-4 py-2 text-sm"
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
