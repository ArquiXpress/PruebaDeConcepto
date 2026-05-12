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
  stockAvailable: number;
  status?: ProductStatus;
  version?: number;
  createdAt?: string;
}