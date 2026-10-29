import { CATEGORY_COLORS, CATEGORY_ICONS, CategoryIconOption } from '../categories/category-catalog';

/** Os 10 ícones oferecidos para cofrinhos (subconjunto do catálogo de categorias, mesmos identificadores). */
const BOX_ICON_KEYS = [
  'savings',
  'flight',
  'directions_car',
  'home',
  'school',
  'card_giftcard',
  'smartphone',
  'favorite',
  'trending_up',
  'account_balance_wallet',
] as const;

export const MONEY_BOX_ICONS: readonly CategoryIconOption[] = BOX_ICON_KEYS.map(
  (key) => CATEGORY_ICONS.find((option) => option.key === key) as CategoryIconOption,
);

/** As 12 cores do catálogo de categorias. */
export const MONEY_BOX_COLORS = CATEGORY_COLORS;
