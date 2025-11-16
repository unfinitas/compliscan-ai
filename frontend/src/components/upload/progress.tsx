"use client";

import { useEffect, useState } from "react";
import { Progress } from "@/components/ui/progress";

interface UploadProgressProps {
  duration: number;
  onComplete?: () => void;
}

export function UploadProgress({ duration, onComplete }: UploadProgressProps) {
  const [progress, setProgress] = useState(0);
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    const startTime = Date.now();
    const interval = setInterval(() => {
      const now = Date.now();
      const elapsedSeconds = Math.floor((now - startTime) / 1000);
      const newProgress = Math.min((elapsedSeconds / duration) * 100, 100);

      setElapsed(elapsedSeconds);
      setProgress(newProgress);

      if (elapsedSeconds >= duration) {
        clearInterval(interval);
        onComplete?.();
      }
    }, 100);

    return () => clearInterval(interval);
  }, [duration, onComplete]);

  const remainingSeconds = Math.max(0, duration - elapsed);
  const minutes = Math.floor(remainingSeconds / 60);
  const seconds = remainingSeconds % 60;

  return (
    <div className="w-full space-y-3">
      <div className="flex items-center justify-between text-sm">
        <span className="text-muted-foreground">Processing document...</span>
        <span className="text-muted-foreground font-medium">
          {minutes}:{seconds.toString().padStart(2, "0")} remaining
        </span>
      </div>
      <Progress value={progress} className="h-2" />
      <p className="text-xs text-muted-foreground text-center">
        Uploading and Embedding your document. This may take a few minutes.
      </p>
    </div>
  );
}
