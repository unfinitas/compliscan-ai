import { Sparkles } from "lucide-react";
import { Toaster } from "@/components/ui/Toaster";
import { FileUpload } from "@/components/upload/FileUpload";

export default function Home() {
  return (
    <>
      <div className="relative min-h-screen overflow-hidden bg-white">
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#00000008_1px,transparent_1px),linear-gradient(to_bottom,#00000008_1px,transparent_1px)] bg-size-[4rem_4rem]" />
        <div className="relative mx-auto max-w-7xl px-6 lg:px-8 pt-20 pb-16">
          <div className="mx-auto max-w-4xl text-center">
            <div className="inline-flex items-center gap-2 rounded-full border border-neutral-200 bg-neutral-50 px-4 py-1.5 text-sm font-medium text-neutral-900 mb-8">
              <Sparkles className="h-4 w-4" />
              AI-Powered Compliance Analysis
            </div>

            <h1 className="text-5xl md:text-6xl lg:text-7xl font-bold tracking-tight text-neutral-900 mb-6">
              Compliance Made
              <span className="block text-neutral-600 mt-2">Effortless</span>
            </h1>

            <p className="text-xl md:text-2xl text-neutral-600 max-w-3xl mx-auto mb-12">
              Upload your documents and get instant coverage insights, evidence
              mapping, and gap analysis.
            </p>

            <FileUpload />
          </div>
        </div>
      </div>
      <Toaster />
    </>
  );
}
