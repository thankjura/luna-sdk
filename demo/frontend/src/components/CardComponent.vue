<script setup lang="ts">

import { PropType } from "vue";
import { Card } from "@/interfaces/card.ts";

defineProps({
  card: Object as PropType<Card>
})

</script>

<template>
  <div class="card pad panel">
    <div class="content">{{ card.value }}</div>
  </div>
</template>

<style scoped>
  .card {
    box-shadow: 1px 1px 2px #ccc;
    border: 1px solid #ccc;
    user-select: none;
    cursor: pointer;
    margin-top: 0;

    &.ghost-card {
      border: 1px dashed #cccccc;
      box-shadow: none;

      .content {
        opacity: 0;
      }
    }
  }
</style>