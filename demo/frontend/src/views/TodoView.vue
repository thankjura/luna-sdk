<script setup lang="ts">
import { Errors, NotifyComponentInterface, ButtonBusy } from 'luna';
import { computed, inject, onBeforeUnmount, onMounted, ref, useTemplateRef } from "vue";
import TodoColumnComponent from "@/components/TodoColumnComponent.vue";
import { $i18n } from "@/utils/i18n.ts";
import { todoService } from "@/service/todoService.ts";
import { Card, CardState } from "@/interfaces/card.ts";
import Sortable, { SortableEvent } from "sortablejs";

const busy = ref(false);
const $notify = inject<NotifyComponentInterface>('$notify');
const cardValue = ref<string>(null);
const cards = ref<Array<Card>>([]);
const errors = ref<Errors>({});
const trash = useTemplateRef<HTMLDivElement>('trash');
let sortable: Sortable = null;

const cardsMap = computed<Record<CardState, Array<Card>>>(() => {
  const out = {} as Record<CardState, Array<Card>>;
  for (const card of cards.value) {
    if (out[card.state]) {
      out[card.state].push(card);
    } else {
      out[card.state] = [card];
    }
  }
  return out;
})

const createCard = () => {
  if (!cardValue.value) {
    return;
  }
  busy.value = true;
  todoService.createCard(cardValue.value).then((data) => {
    $notify.ok($i18n.t("Card created"), data.data.value);
    cards.value.push(data.data);
    cardValue.value = null;
  }).finally(() => {
    busy.value = false;
  }).catch((err) => {
    if (err.data.errors) {
      // ValidateException
      errors.value = err.data.errors;
    }
  })
}

const loadCards = () => {
  busy.value = true;
  todoService.getAllCards().then((data) => {
    cards.value = data.data;
  }).finally(() => {
    busy.value = false;
  }).catch(() => {
    $notify.error($i18n.t("Failed load cards"));
  })
}

const moveCard = (cardId: string, state: CardState) => {
  busy.value = true;
  todoService.patchCard(cardId, {state}).then((data) => {
    const idx = cards.value.findIndex(c => c.id == data.data.id);
    if (idx > -1) {
      cards.value.splice(idx, 1, data.data);
    } else {
      cards.value.push(data.data);
    }
  }).finally(() => {
    busy.value = false;
  });
}

const onTrash = (event: SortableEvent) => {
  busy.value = true;
  todoService.deleteCard(event.item.dataset.id).then(() => {
    $notify.info($i18n.t("Card removed"));
  }).finally(() => {
    busy.value = false;
  })
}

onMounted(() => {
  sortable = new Sortable(trash.value, {
    sort: false,
    group: 'column',
    onAdd: onTrash
  })
  loadCards();
})

onBeforeUnmount(() => {
  if (sortable) {
    sortable.destroy();
  }
})


</script>

<template>
  <div class="todo-page pad">
    <TodoColumnComponent :cards="cardsMap['open']" state="open" @move="moveCard"></TodoColumnComponent>
    <TodoColumnComponent :cards="cardsMap['done']" state="done" @move="moveCard"></TodoColumnComponent>
    <div>
      <h3>{{ $i18n.t("New Card") }}</h3>
      <form class="form panel pad" @submit.prevent="createCard">
          <textarea v-model="cardValue" :placeholder="$i18n.t('Todo')"></textarea>
          <ButtonBusy :busy="busy" :disabled="busy || !cardValue" @click="createCard">{{ $i18n.t("Create") }}</ButtonBusy>
      </form>
      <div class="trash panel pad grey" :title="$i18n.t('Put it to remove')">
        <div class="icon-bin"></div>
        <div class="trash-container" ref="trash"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
  .todo-page {
    padding: 20px;
    display: flex;
    gap: 20px;
    align-items: stretch;
    justify-content: flex-start;

    .form {
      margin-top: 0;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .trash {
      width: 100%;
      height: 200px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      position: relative;
      margin-top: 20px;

      .trash-container {
        position: absolute;
        width: 100%;
        height: 100%;
        opacity: 0;
      }

      .icon-bin {
        font-size: 100px;
      }
    }
  }
</style>
