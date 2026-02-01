export enum CardState {
  Open = 'open',
  Done = 'done',
}

export interface Card {
  id: string,
  value: string,
  state: CardState,
  created: string,
}