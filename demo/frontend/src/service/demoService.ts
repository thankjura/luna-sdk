import { client } from "@/utils/client.ts";

class DemoService {
  async getHello() {
    return client.get<{message: string}>('/demo');
  }
}

export const demoService = new DemoService();