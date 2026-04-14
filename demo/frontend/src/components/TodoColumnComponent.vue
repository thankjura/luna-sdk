<script setup lang="ts">

import { onBeforeUnmount, onMounted, PropType, useTemplateRef } from "vue";
import { Card } from "@/interfaces/card.ts";
import CardComponent from "@/components/CardComponent.vue";
import Sortable, { SortableEvent } from "sortablejs";

const props = defineProps({
  cards: Array as PropType<Array<Card>>,
  state: String
})

const emits = defineEmits({
  move: (_cardId: number, _state: string) => true
})

const container = useTemplateRef<HTMLDivElement>('container');
let sortable: Sortable = null;

const onAdd = (event: SortableEvent) => {
  emits('move', Number(event.item.dataset.id), props.state);
}

onMounted(() => {
  sortable = new Sortable(container.value, {
    animation: 100,
    group: "column",
    onAdd: onAdd,
    ghostClass: 'ghost-card',
    forceFallback: true,

  })
})

onBeforeUnmount(() => {
  if (sortable) {
    sortable.destroy();
  }
})

</script>

<template>
  <div class="column">
    <h3>{{ state }}</h3>
    <div class="container panel pad" ref="container">
      <CardComponent class="card-item" :card="card" v-for="card in cards" :key="card.id" :data-id="card.id"></CardComponent>
    </div>
  </div>
</template>

<style scoped>
  .column {
    width: 300px;
    margin-top: 0;

    .container {
      height: 100%;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
  }
</style>