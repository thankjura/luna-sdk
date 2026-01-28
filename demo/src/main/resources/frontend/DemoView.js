import { _ as _export_sfc } from "./_plugin-vue_export-helper-1tPrXgE0.js";
const __variableDynamicImportRuntimeHelper = (glob$1, path$13, segs) => {
  const v = glob$1[path$13];
  if (v) return typeof v === "function" ? v() : Promise.resolve(v);
  return new Promise((_, reject) => {
    (typeof queueMicrotask === "function" ? queueMicrotask : setTimeout)(reject.bind(null, /* @__PURE__ */ new Error("Unknown variable dynamic import: " + path$13 + (path$13.split("/").length !== segs ? ". Note that variables only represent file names one level deep." : ""))));
  });
};
const I18N = window["__LUNA_COMPONENTS__"].I18N;
const loadI18N = async (locale) => {
  try {
    const { messages } = await __variableDynamicImportRuntimeHelper(/* @__PURE__ */ Object.assign({}), `./locales/messages_${locale}.ts`, 3);
    return messages;
  } catch (e) {
    return {};
  }
};
const $i18n = new I18N(["ru"], loadI18N);
const axios = window["axios"];
const baseURL = "/rest/plugin/ru.slie.luna.templates.demo-addon";
const client = axios.create({ baseURL });
class DemoService {
  async getHello() {
    return client.get("/demo");
  }
}
const demoService = new DemoService();
const _defineComponent = window["Vue"].defineComponent;
const _unref = window["Vue"].unref;
const _toDisplayString = window["Vue"].toDisplayString;
const _createTextVNode = window["Vue"].createTextVNode;
const _withCtx = window["Vue"].withCtx;
const _createVNode = window["Vue"].createVNode;
const _openBlock = window["Vue"].openBlock;
const _createElementBlock = window["Vue"].createElementBlock;
const _hoisted_1 = { class: "demo-page pad" };
const TogglePanel = window["__LUNA_COMPONENTS__"].TogglePanel;
const ButtonBusy = window["__LUNA_COMPONENTS__"].ButtonBusy;
const inject = window["Vue"].inject;
const ref = window["Vue"].ref;
const _sfc_main = /* @__PURE__ */ _defineComponent({
  __name: "DemoView",
  setup(__props) {
    const busy = ref(false);
    const $notify = inject("$notify");
    const showMessage = () => {
      busy.value = true;
      demoService.getHello().then((data) => {
        $notify.ok(data.data.message);
      }).finally(() => {
        busy.value = false;
      });
    };
    return (_ctx, _cache) => {
      return _openBlock(), _createElementBlock("div", _hoisted_1, [
        _createVNode(_unref(TogglePanel), {
          "store-key": "demo-panel",
          class: "panel",
          title: _unref($i18n).t("Toggle panel")
        }, {
          default: _withCtx(() => [
            _createTextVNode(_toDisplayString(_unref($i18n).t("Hello from component")), 1)
          ]),
          _: 1
        }, 8, ["title"]),
        _createVNode(_unref(ButtonBusy), {
          class: "demo-button",
          busy: busy.value,
          disabled: busy.value,
          onClick: showMessage
        }, {
          default: _withCtx(() => [
            _createTextVNode(_toDisplayString(_unref($i18n).t("Click me")), 1)
          ]),
          _: 1
        }, 8, ["busy", "disabled"])
      ]);
    };
  }
});
const DemoView = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-b908a0eb"]]);
export {
  DemoView as default
};
//# sourceMappingURL=DemoView.js.map
