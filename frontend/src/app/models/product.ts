export type ProductStatus = 'ACTIVE' | 'INACTIVE';

export interface Product {
  id: string;
  sellerId: string;
  title: string;
  description: string;
  category: string;
  imageUrl: string;
  imageUrls?: string[];
  price: number;
  discountPercent?: number | null;
  salePrice?: number | null;
  offerEndsAt?: string | null;
  stockAvailable: number;
  status?: ProductStatus;
  moderationReason?: string | null;
  moderationAt?: string | null;
  appealNote?: string | null;
  appealRequestedAt?: string | null;
  appealResolutionNote?: string | null;
  appealResolvedAt?: string | null;
  version?: number;
  createdAt?: string;
}
