import { client } from "@/utils/client.ts";
import { Card } from "@/interfaces/card.ts";

class TodoService {
  async getAllCards() {
    return client.get<Array<Card>>('/todo');
  }

  async createCard(value: string) {
    return client.post<Card>('/todo', {value});
  }

  async updateCard(cardId: number, card: Partial<Card>) {
    return client.put<Card>(`/todo/${cardId}`, card);
  }

  async patchCard(cardId: number, card: Partial<Card>) {
    return client.patch<Card>(`/todo/${cardId}`, card);
  }

  async deleteCard(cardId: number) {
    return client.delete(`/todo/${cardId}`);
  }
}

export const todoService = new TodoService();