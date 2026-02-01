import { v as vueExports, _ as _export_sfc, g as getDefaultExportFromCjs, c as client, $ as $i18n, l as lunaExports } from "./_plugin-vue_export-helper-BKs5ieuW.js";
const _hoisted_1$2 = { class: "card pad panel" };
const _hoisted_2$1 = { class: "content" };
const _sfc_main$2 = /* @__PURE__ */ vueExports.defineComponent({
  __name: "CardComponent",
  props: {
    card: Object
  },
  setup(__props) {
    return (_ctx, _cache) => {
      return vueExports.openBlock(), vueExports.createElementBlock("div", _hoisted_1$2, [
        vueExports.createElementVNode("div", _hoisted_2$1, vueExports.toDisplayString(__props.card.value), 1)
      ]);
    };
  }
});
const CardComponent = /* @__PURE__ */ _export_sfc(_sfc_main$2, [["__scopeId", "data-v-a9ebd6a6"]]);
var sortablejs;
var hasRequiredSortablejs;
function requireSortablejs() {
  if (hasRequiredSortablejs) return sortablejs;
  hasRequiredSortablejs = 1;
  sortablejs = Sortable;
  return sortablejs;
}
var sortablejsExports = requireSortablejs();
const Sortable$1 = /* @__PURE__ */ getDefaultExportFromCjs(sortablejsExports);
const _hoisted_1$1 = { class: "column" };
const _sfc_main$1 = /* @__PURE__ */ vueExports.defineComponent({
  __name: "TodoColumnComponent",
  props: {
    cards: Array,
    state: String
  },
  emits: {
    move: (cardId, state) => true
  },
  setup(__props, { emit: __emit }) {
    const props = __props;
    const emits = __emit;
    const container = vueExports.useTemplateRef("container");
    let sortable = null;
    const onAdd = (event) => {
      emits("move", event.item.dataset.id, props.state);
    };
    vueExports.onMounted(() => {
      sortable = new Sortable$1(container.value, {
        animation: 100,
        group: "column",
        onAdd,
        ghostClass: "ghost-card",
        forceFallback: true
      });
    });
    vueExports.onBeforeUnmount(() => {
      if (sortable) {
        sortable.destroy();
      }
    });
    return (_ctx, _cache) => {
      return vueExports.openBlock(), vueExports.createElementBlock("div", _hoisted_1$1, [
        vueExports.createElementVNode("h3", null, vueExports.toDisplayString(__props.state), 1),
        vueExports.createElementVNode("div", {
          class: "container panel pad",
          ref_key: "container",
          ref: container
        }, [
          (vueExports.openBlock(true), vueExports.createElementBlock(vueExports.Fragment, null, vueExports.renderList(__props.cards, (card) => {
            return vueExports.openBlock(), vueExports.createBlock(CardComponent, {
              class: "card-item",
              card,
              key: card.id,
              "data-id": card.id
            }, null, 8, ["card", "data-id"]);
          }), 128))
        ], 512)
      ]);
    };
  }
});
const TodoColumnComponent = /* @__PURE__ */ _export_sfc(_sfc_main$1, [["__scopeId", "data-v-57ba951b"]]);
class TodoService {
  async getAllCards() {
    return client.get("/todo");
  }
  async createCard(value) {
    return client.post("/todo", { value });
  }
  async updateCard(cardId, card) {
    return client.put(`/todo/${cardId}`, card);
  }
  async patchCard(cardId, card) {
    return client.patch(`/todo/${cardId}`, card);
  }
  async deleteCard(cardId) {
    return client.delete(`/todo/${cardId}`);
  }
}
const todoService = new TodoService();
const _hoisted_1 = { class: "todo-page pad" };
const _hoisted_2 = { class: "form" };
const _hoisted_3 = ["placeholder"];
const _hoisted_4 = ["title"];
const _sfc_main = /* @__PURE__ */ vueExports.defineComponent({
  __name: "TodoView",
  setup(__props) {
    const busy = vueExports.ref(false);
    const $notify = vueExports.inject("$notify");
    const cardValue = vueExports.ref(null);
    const cards = vueExports.ref([]);
    const errors = vueExports.ref({});
    const trash = vueExports.useTemplateRef("trash");
    let sortable = null;
    const cardsMap = vueExports.computed(() => {
      const out = {};
      for (const card of cards.value) {
        if (out[card.state]) {
          out[card.state].push(card);
        } else {
          out[card.state] = [card];
        }
      }
      return out;
    });
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
          errors.value = err.data.errors;
        }
      });
    };
    const loadCards = () => {
      busy.value = true;
      todoService.getAllCards().then((data) => {
        cards.value = data.data;
      }).finally(() => {
        busy.value = false;
      }).catch(() => {
        $notify.error($i18n.t("Failed load cards"));
      });
    };
    const moveCard = (cardId, state) => {
      busy.value = true;
      todoService.patchCard(cardId, { state }).then((data) => {
        const idx = cards.value.findIndex((c) => c.id == data.data.id);
        if (idx > -1) {
          cards.value.splice(idx, 1, data.data);
        } else {
          cards.value.push(data.data);
        }
      }).finally(() => {
        busy.value = false;
      });
    };
    const onTrash = (event) => {
      busy.value = true;
      todoService.deleteCard(event.item.dataset.id).then(() => {
        $notify.info($i18n.t("Card removed"));
      }).finally(() => {
        busy.value = false;
      });
    };
    vueExports.onMounted(() => {
      sortable = new Sortable$1(trash.value, {
        sort: false,
        group: "column",
        onAdd: onTrash
      });
      loadCards();
    });
    vueExports.onBeforeUnmount(() => {
      if (sortable) {
        sortable.destroy();
      }
    });
    return (_ctx, _cache) => {
      return vueExports.openBlock(), vueExports.createElementBlock("div", _hoisted_1, [
        vueExports.createVNode(TodoColumnComponent, {
          cards: cardsMap.value["open"],
          state: "open",
          onMove: moveCard
        }, null, 8, ["cards"]),
        vueExports.createVNode(TodoColumnComponent, {
          cards: cardsMap.value["done"],
          state: "done",
          onMove: moveCard
        }, null, 8, ["cards"]),
        vueExports.createElementVNode("div", _hoisted_2, [
          vueExports.createElementVNode("h3", null, vueExports.toDisplayString(vueExports.unref($i18n).t("New Card")), 1),
          vueExports.createElementVNode("form", {
            class: "form panel pad",
            onSubmit: vueExports.withModifiers(createCard, ["prevent"])
          }, [
            vueExports.withDirectives(vueExports.createElementVNode("textarea", {
              "onUpdate:modelValue": _cache[0] || (_cache[0] = ($event) => cardValue.value = $event),
              placeholder: vueExports.unref($i18n).t("Todo")
            }, null, 8, _hoisted_3), [
              [vueExports.vModelText, cardValue.value]
            ]),
            vueExports.createVNode(vueExports.unref(lunaExports.ButtonBusy), {
              busy: busy.value,
              disabled: busy.value || !cardValue.value,
              onClick: createCard
            }, {
              default: vueExports.withCtx(() => [
                vueExports.createTextVNode(vueExports.toDisplayString(vueExports.unref($i18n).t("Create")), 1)
              ]),
              _: 1
            }, 8, ["busy", "disabled"])
          ], 32),
          vueExports.createElementVNode("div", {
            class: "trash panel pad grey",
            title: vueExports.unref($i18n).t("Put it to remove")
          }, [
            _cache[1] || (_cache[1] = vueExports.createElementVNode("div", { class: "icon-bin" }, null, -1)),
            vueExports.createElementVNode("div", {
              class: "trash-container",
              ref_key: "trash",
              ref: trash
            }, null, 512)
          ], 8, _hoisted_4)
        ])
      ]);
    };
  }
});
const TodoView = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-c1d7fadf"]]);
export {
  TodoView as default
};
//# sourceMappingURL=TodoView.js.map
