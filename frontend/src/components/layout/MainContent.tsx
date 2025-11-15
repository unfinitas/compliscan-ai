"use client";

import { useSidebar } from "@/components/ui/sidebar";
import { motion } from "motion/react";

export function MainContent({ children }: { children: React.ReactNode }) {
  const { open, animate } = useSidebar();

  return (
    <motion.main
      className="min-h-screen bg-neutral-950 flex-1"
      animate={{
        marginLeft: animate ? (open ? "256px" : "64px") : "256px",
      }}
      transition={{
        duration: 0.3,
        ease: "easeInOut",
      }}
    >
      {children}
    </motion.main>
  );
}

