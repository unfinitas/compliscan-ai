import { matchType, StatusType } from "@/api/coverage/modal/enum";

export type Clause = {
  clauseId: string;
  title: string;
  status: StatusType;
  matchType: matchType;
  similarity: number;
  excerpt: string | null;
  explanation: string | null;
  section: string | null;
};