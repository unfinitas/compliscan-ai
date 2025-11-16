'use client';

import { useState } from 'react';
import { SidebarBody } from '@/components/ui/sidebar';
import { FileUpload } from '@/components/upload/FileUpload';

interface UploadSidebarProps {
  onDocumentUploaded: (documentId: string) => void;
}

export function UploadSidebar({
  onDocumentUploaded,
}: UploadSidebarProps) {
  const [fileName, setFileName] = useState<string | null>(null);

  return (
    <SidebarBody className="h-full">
      <div className="flex flex-col h-full">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-border">
          <h2 className="text-lg font-semibold text-foreground truncate">
            {fileName || "Upload Document"}
          </h2>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-auto p-6">
          <FileUpload
            onDocumentUploaded={onDocumentUploaded}
            onFileNameChange={setFileName}
          />
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-border">
          <p className="text-xs text-muted-foreground leading-relaxed">
            Regulation based on Easy Access Rules for Continuing Airworthiness (Regulation (EU) No 1321/2014)
          </p>
        </div>
      </div>
    </SidebarBody>
  );
}
