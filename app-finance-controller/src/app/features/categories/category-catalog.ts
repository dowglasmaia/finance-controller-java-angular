import {
  Baby,
  Briefcase,
  Car,
  Dumbbell,
  Film,
  Gamepad2,
  Gift,
  GraduationCap,
  Heart,
  House,
  LucideIconData,
  Music,
  PawPrint,
  PiggyBank,
  Plane,
  ShoppingCart,
  Shirt,
  Smartphone,
  Soup,
  Stethoscope,
  Tag,
  TrendingUp,
  Utensils,
  Wallet,
  Wrench,
} from 'lucide-angular';

export interface CategoryIconOption {
  /** Identificador gravado no servidor (nomes da semente: `directions_car`, `restaurant`, …). */
  readonly key: string;
  readonly label: string;
  readonly icon: LucideIconData;
}

/** Os 24 ícones oferecidos no diálogo; os 11 primeiros são os da semente. */
export const CATEGORY_ICONS: readonly CategoryIconOption[] = [
  { key: 'directions_car', label: 'Carro', icon: Car },
  { key: 'restaurant', label: 'Restaurante', icon: Utensils },
  { key: 'home', label: 'Casa', icon: House },
  { key: 'favorite', label: 'Saúde', icon: Heart },
  { key: 'sports_esports', label: 'Jogos', icon: Gamepad2 },
  { key: 'school', label: 'Educação', icon: GraduationCap },
  { key: 'checkroom', label: 'Roupas', icon: Shirt },
  { key: 'work', label: 'Trabalho', icon: Briefcase },
  { key: 'lunch_dining', label: 'Refeição', icon: Soup },
  { key: 'trending_up', label: 'Investimento', icon: TrendingUp },
  { key: 'card_giftcard', label: 'Presente', icon: Gift },
  { key: 'shopping_cart', label: 'Compras', icon: ShoppingCart },
  { key: 'flight', label: 'Viagem', icon: Plane },
  { key: 'pets', label: 'Animais', icon: PawPrint },
  { key: 'savings', label: 'Poupança', icon: PiggyBank },
  { key: 'smartphone', label: 'Celular', icon: Smartphone },
  { key: 'fitness_center', label: 'Academia', icon: Dumbbell },
  { key: 'local_hospital', label: 'Hospital', icon: Stethoscope },
  { key: 'movie', label: 'Cinema', icon: Film },
  { key: 'music_note', label: 'Música', icon: Music },
  { key: 'build', label: 'Manutenção', icon: Wrench },
  { key: 'child_care', label: 'Crianças', icon: Baby },
  { key: 'account_balance_wallet', label: 'Carteira', icon: Wallet },
  { key: 'sell', label: 'Etiqueta', icon: Tag },
];

export interface CategoryColorOption {
  readonly hex: string;
  readonly label: string;
}

/** As 12 cores oferecidas (inclui as 11 da semente). */
export const CATEGORY_COLORS: readonly CategoryColorOption[] = [
  { hex: '#FF9800', label: 'Laranja' },
  { hex: '#4CAF50', label: 'Verde' },
  { hex: '#2196F3', label: 'Azul' },
  { hex: '#F44336', label: 'Vermelho' },
  { hex: '#9C27B0', label: 'Roxo' },
  { hex: '#3F51B5', label: 'Índigo' },
  { hex: '#795548', label: 'Marrom' },
  { hex: '#009688', label: 'Verde-azulado' },
  { hex: '#8BC34A', label: 'Verde-claro' },
  { hex: '#FFC107', label: 'Âmbar' },
  { hex: '#00BCD4', label: 'Ciano' },
  { hex: '#E91E63', label: 'Rosa' },
];

const FALLBACK: LucideIconData = Tag;

/** Ícone para o identificador gravado; desconhecido cai em "Etiqueta". */
export function iconFor(key: string): LucideIconData {
  return CATEGORY_ICONS.find((option) => option.key === key)?.icon ?? FALLBACK;
}

export const TYPE_LABELS = { EXPENSE: 'Despesa', INCOME: 'Receita', BOTH: 'Ambos' } as const;
