/**
 * Environment configuration
 *
 * For local development, create a `.env.local` file in the frontend directory with:
 *
 * NEXT_PUBLIC_BACKEND_URL=http://localhost:8080
 * NEXT_PUBLIC_DEFAULT_REGULATION_ID=your-regulation-uuid-here
 * NEXT_PUBLIC_REGULATION_VERSION=2025-09-AMC-GM
 *
 * To find your regulation ID, query your database:
 * SELECT id FROM regulations WHERE active = true LIMIT 1;
 */

export const config = {
  backendUrl: process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080",
  defaultRegulationId: process.env.NEXT_PUBLIC_DEFAULT_REGULATION_ID || "",
  regulationVersion: process.env.NEXT_PUBLIC_REGULATION_VERSION || "2025-09-AMC-GM",
};
