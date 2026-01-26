import { defineComponent, openBlock, createElementBlock, createElementVNode } from "vue";
const _hoisted_1 = { class: "demo-page pad" };
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "DemoView",
  setup(__props) {
    console.log("component");
    return (_ctx, _cache) => {
      return openBlock(), createElementBlock("div", _hoisted_1, [..._cache[0] || (_cache[0] = [
        createElementVNode("h3", null, "Demo page", -1)
      ])]);
    };
  }
});
export {
  _sfc_main as default
};
//# sourceMappingURL=DemoView.js.map
