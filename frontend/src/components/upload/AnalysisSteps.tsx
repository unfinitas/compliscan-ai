"use client";

import { useEffect, useState } from "react";
import { Check } from "lucide-react";

const ANALYSIS_STEPS = [
  "Validating regulation availability…",
  "Loading MOE document and metadata…",
  "Extracting paragraphs from MOE (PDF → structured text)…",
  "Normalizing and cleaning paragraph content…",
  "Generating vector embeddings for MOE paragraphs…",
  "Loading active regulation and clauses…",
  "Embedding regulation clauses…",
  "Computing semantic similarity (MOE ↔ Regulation)…",
  "Identifying compliance status for each clause…",
  "Aggregating compliance metrics (coverage, gaps)…",
  "Persisting analysis result and compliance outcomes…",
  "Analysis completed successfully.",
];

const STEP_DURATION = 20000; // 20 seconds per step
const FINAL_STEP_DURATION = 2000; // 2 seconds for final step

interface AnalysisStepsProps {
  isActive: boolean;
  onComplete?: () => void;
  apiCompleted?: boolean;
}

export function AnalysisSteps({
  isActive,
  onComplete,
  apiCompleted = false,
}: AnalysisStepsProps) {
  const [currentStep, setCurrentStep] = useState(0);
  const [isAnimating, setIsAnimating] = useState(false);

  useEffect(() => {
    if (!isActive) {
      setCurrentStep(0);
      setIsAnimating(false);
      return;
    }

    setIsAnimating(true);
    let stepIndex = 0;
    let timeoutId: NodeJS.Timeout | null = null;
    let checkApiInterval: NodeJS.Timeout | null = null;

    const advanceStep = () => {
      if (stepIndex < ANALYSIS_STEPS.length - 1) {
        // For the "Persisting" step (index 10), wait for API if not completed
        if (stepIndex === 10 && !apiCompleted) {
          // Wait for API to complete before moving to final step
          checkApiInterval = setInterval(() => {
            if (apiCompleted) {
              if (checkApiInterval) clearInterval(checkApiInterval);
              stepIndex++;
              setCurrentStep(stepIndex);
              // Show final step for 2 seconds
              timeoutId = setTimeout(() => {
                setIsAnimating(false);
                onComplete?.();
              }, FINAL_STEP_DURATION);
            }
          }, 100); // Check every 100ms
          return;
        }

        // Regular step progression
        stepIndex++;
        setCurrentStep(stepIndex);

        // If this is the final step, show it for 2 seconds then complete
        if (stepIndex === ANALYSIS_STEPS.length - 1) {
          timeoutId = setTimeout(() => {
            setIsAnimating(false);
            onComplete?.();
          }, FINAL_STEP_DURATION);
        } else {
          // Continue to next step after STEP_DURATION
          timeoutId = setTimeout(advanceStep, STEP_DURATION);
        }
      } else {
        // Already at final step
        timeoutId = setTimeout(() => {
          setIsAnimating(false);
          onComplete?.();
        }, FINAL_STEP_DURATION);
      }
    };

    // Start first step immediately, then advance every STEP_DURATION
    timeoutId = setTimeout(advanceStep, STEP_DURATION);

    return () => {
      if (timeoutId) clearTimeout(timeoutId);
      if (checkApiInterval) clearInterval(checkApiInterval);
    };
  }, [isActive, apiCompleted, onComplete]);

  if (!isActive) return null;

  return (
    <div className="w-full space-y-4">
      <div className="space-y-3">
        {ANALYSIS_STEPS.map((step, index) => {
          const isCompleted = index < currentStep;
          const isCurrent = index === currentStep;
          const isPending = index > currentStep;

          return (
            <div
              key={index}
              className={`flex items-start gap-3 transition-all duration-500 ${
                isCurrent
                  ? "opacity-100"
                  : isCompleted
                  ? "opacity-60"
                  : "opacity-40"
              }`}
            >
              <div className="mt-0.5 shrink-0">
                {isCompleted ? (
                  <Check className="h-5 w-5 text-green-500" />
                ) : isCurrent ? (
                  <div className="h-5 w-5 rounded-full border-2 border-blue-500 border-t-transparent animate-spin" />
                ) : (
                  <div className="h-5 w-5 rounded-full border-2 border-gray-300" />
                )}
              </div>
              <div className="flex-1">
                <p
                  className={`text-sm transition-colors duration-300 ${
                    isCurrent
                      ? "text-foreground font-medium"
                      : isCompleted
                      ? "text-muted-foreground"
                      : "text-muted-foreground/50"
                  }`}
                >
                  {step}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
