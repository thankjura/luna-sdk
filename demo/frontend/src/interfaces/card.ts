export enum CardState {
  Open = 'open',
  Done = 'done',
}

export interface Card {
  id: number,
  value: string,
  state: CardState,
  created: string,
}