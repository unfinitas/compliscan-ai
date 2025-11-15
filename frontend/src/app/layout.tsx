import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import StoreProvider from "@/lib/redux/StoreProvider";
import { AppSidebar } from "@/components/layout/sideBar";
import { Sidebar } from "@/components/ui/sidebar";
import { MainContent } from "@/components/layout/MainContent";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "CompliScan-AI",
  description: "MOE Compliance Analysis Tool",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body
        suppressHydrationWarning
        className={`${geistSans.variable} ${geistMono.variable} antialiased bg-neutral-950 text-white`}
      >
        <StoreProvider>
          <Sidebar>
            <div className="min-h-screen bg-neutral-950 flex">
              <AppSidebar />
              <MainContent>{children}</MainContent>
            </div>
          </Sidebar>
        </StoreProvider>
      </body>
    </html>
  );
}
