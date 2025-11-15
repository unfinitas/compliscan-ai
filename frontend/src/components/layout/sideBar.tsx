"use client";

import { useMemo } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Home,
  BarChart3,
  FileSearch,
  HelpCircle,
  PieChart,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { SidebarBody, useSidebar } from "@/components/ui/sidebar";
import { motion } from "motion/react";

const navigation = [
  { label: "Home", href: "/", icon: <Home className="h-5 w-5" /> },
  {
    label: "Coverage",
    href: "/coverage",
    icon: <BarChart3 className="h-5 w-5" />,
  },
  {
    label: "Questions",
    href: "/questions",
    icon: <HelpCircle className="h-5 w-5" />,
  },
  {
    label: "Summary",
    href: "/summary",
    icon: <PieChart className="h-5 w-5" />,
  },
  {
    label: "Diff Viewer",
    href: "/diff",
    icon: <FileSearch className="h-5 w-5" />,
  },
];

const CustomSidebarLink = ({
  link,
  className,
}: {
  link: (typeof navigation)[0];
  className?: string;
}) => {
  const { open, animate } = useSidebar();
  const pathname = usePathname();
  const isActive = pathname === link.href;
  const showIndicator = open || !animate;

  const isCollapsed = animate && !open;

  return (
    <Link
      href={link.href}
      className={cn(
        "flex items-center gap-2 group/sidebar py-2 rounded-lg px-2 transition-all",
        isCollapsed ? "justify-center" : "justify-start",
        isActive && showIndicator
          ? "bg-primary text-primary-foreground shadow-lg shadow-primary/30"
          : "text-muted-foreground hover:bg-muted/70 hover:text-foreground",
        className
      )}
    >
      <span
        className={cn(
          isActive && showIndicator
            ? "text-primary-foreground"
            : "text-muted-foreground"
        )}
      >
        {link.icon}
      </span>
      <motion.span
        animate={{
          display: animate ? (open ? "inline-block" : "none") : "inline-block",
          opacity: animate ? (open ? 1 : 0) : 1,
        }}
        className="text-sm group-hover/sidebar:translate-x-1 transition duration-150 whitespace-pre inline-block p-0! m-0!"
      >
        {link.label}
      </motion.span>
    </Link>
  );
};

export function AppSidebar() {
  const currentYear = useMemo(() => new Date().getFullYear(), []);

  return (
    <SidebarBody className="fixed left-0 top-0 z-40 h-screen border-r border-border bg-card/95 shadow-xl shadow-primary/10 backdrop-blur-xl">
      <div className="flex h-full flex-col">
        <nav className="flex-1 space-y-1 px-3 py-6">
          {navigation.map((item) => (
            <CustomSidebarLink key={item.label} link={item} />
          ))}
        </nav>
        <div className="border-t border-border pt-4 px-4 pb-4">
          <motion.div
            animate={{
              opacity: 1,
            }}
            className="text-xs text-muted-foreground"
          ></motion.div>
        </div>
      </div>
    </SidebarBody>
  );
}
