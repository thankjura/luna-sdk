export const CardState = {
  Open: 'open',
  Done: 'done',
}

export type CardState = typeof CardState[keyof typeof CardState];

export interface Card {
  id: number,
  value: string,
  state: CardState,
  created: string,
}