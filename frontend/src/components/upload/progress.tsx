"use client";

import { Progress } from "@/components/ui/progress";

interface UploadProgressProps {
  progress: number; // Progress percentage (0-100)
}

export function UploadProgress({ progress }: UploadProgressProps) {
  return (
    <div className="w-full space-y-3">
      <div className="flex items-center justify-between text-sm">
        <span className="text-muted-foreground">Embedding document...</span>
        <span className="text-muted-foreground font-medium">{progress}%</span>
      </div>
      <Progress value={progress} className="h-2" />
      <p className="text-xs text-muted-foreground text-center">
        Uploading and Embedding your document. This may take a few minutes.
      </p>
    </div>
  );
}
