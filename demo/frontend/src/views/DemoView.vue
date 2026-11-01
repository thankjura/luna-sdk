<script setup lang="ts">
import { TogglePanel, ButtonBusy, $notify } from 'luna';
import { $i18n } from "@/utils/i18n";
import { ref } from "vue";
import { demoService } from "@/service/demoService";

const busy = ref(false);

const showMessage = () => {
  busy.value = true;
  demoService.getHello().then((data) => {
    $notify.ok(data.data.message);
  }).finally(() => {
    busy.value = false;
  });
}

</script>

<template>
  <div class="demo-page pad">
    <TogglePanel store-key="demo-panel" class="panel" :title="$i18n.t('Toggle panel')">
      {{ $i18n.t("Hello from component") }}
    </TogglePanel>

    <ButtonBusy class="demo-button" :busy="busy" :disabled="busy" @click="showMessage">{{ $i18n.t("Click me") }}</ButtonBusy>
  </div>
</template>

<style scoped>
  .demo-page {
    padding: 20px;
  }

  .demo-button {
    margin-top: 20px;
  }
</style>
