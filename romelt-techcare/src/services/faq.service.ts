import { apiClient } from "@/lib/api-client";

import type { PublicFaq } from "@/types/customer-review.types";

export async function getFeaturedFaqs() {
  return apiClient.get<PublicFaq[]>("/public/faqs/featured");
}

export async function getAllFaqs() {
  return apiClient.get<PublicFaq[]>("/public/faqs");
}
