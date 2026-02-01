import { c as client, v as vueExports, l as lunaExports, $ as $i18n, _ as _export_sfc } from "./_plugin-vue_export-helper-BKs5ieuW.js";
class DemoService {
  async getHello() {
    return client.get("/demo");
  }
}
const demoService = new DemoService();
const _hoisted_1 = { class: "demo-page pad" };
const _sfc_main = /* @__PURE__ */ vueExports.defineComponent({
  __name: "DemoView",
  setup(__props) {
    const busy = vueExports.ref(false);
    const $notify = vueExports.inject("$notify");
    const showMessage = () => {
      busy.value = true;
      demoService.getHello().then((data) => {
        $notify.ok(data.data.message);
      }).finally(() => {
        busy.value = false;
      });
    };
    return (_ctx, _cache) => {
      return vueExports.openBlock(), vueExports.createElementBlock("div", _hoisted_1, [
        vueExports.createVNode(vueExports.unref(lunaExports.TogglePanel), {
          "store-key": "demo-panel",
          class: "panel",
          title: vueExports.unref($i18n).t("Toggle panel")
        }, {
          default: vueExports.withCtx(() => [
            vueExports.createTextVNode(vueExports.toDisplayString(vueExports.unref($i18n).t("Hello from component")), 1)
          ]),
          _: 1
        }, 8, ["title"]),
        vueExports.createVNode(vueExports.unref(lunaExports.ButtonBusy), {
          class: "demo-button",
          busy: busy.value,
          disabled: busy.value,
          onClick: showMessage
        }, {
          default: vueExports.withCtx(() => [
            vueExports.createTextVNode(vueExports.toDisplayString(vueExports.unref($i18n).t("Click me")), 1)
          ]),
          _: 1
        }, 8, ["busy", "disabled"])
      ]);
    };
  }
});
const DemoView = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-e395567b"]]);
export {
  DemoView as default
};
//# sourceMappingURL=DemoView.js.map
