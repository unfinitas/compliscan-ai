"use client";

import React from "react";
import { useSidebar } from "@/components/ui/sidebar";
import { motion } from "motion/react";
import { UploadSidebar } from "@/components/upload/UploadSidebar";
import { DocumentProvider, useDocument } from "@/contexts/DocumentContext";

function LayoutContentInner({ children }: { children: React.ReactNode }) {
  const { setOpen } = useSidebar();
  const { setUploadedDocumentId } = useDocument();

  // Keep sidebar always open
  React.useEffect(() => {
    setOpen(true);
  }, [setOpen]);

  return (
    <div className="flex h-screen bg-background">
      {/* Sidebar */}
      <UploadSidebar onDocumentUploaded={setUploadedDocumentId} />

      {/* Main Content */}
      <motion.main
        className="flex-1 overflow-auto"
        initial={{ marginLeft: "320px" }}
        animate={{ marginLeft: "320px" }}
      >
        {children}
      </motion.main>
    </div>
  );
}

export function LayoutContent({ children }: { children: React.ReactNode }) {
  return (
    <DocumentProvider>
      <LayoutContentInner>{children}</LayoutContentInner>
    </DocumentProvider>
  );
}

