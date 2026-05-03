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
}
