<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { X } from "lucide-vue-next";
defineProps({ title: String });
const emit = defineEmits(["close"]);
const panel = ref(null);
let previous;
// 키보드 사용자가 모달 바깥으로 빠져나가지 않도록 포커스를 안에 유지합니다.
function keydown(e) {
  if (e.key === "Escape") emit("close");
  if (e.key !== "Tab") return;
  const items = [
    ...panel.value.querySelectorAll("button,input,select,textarea,a[href]"),
  ].filter((el) => !el.disabled && el.offsetParent !== null);
  if (!items.length) return;
  const first = items[0],
    last = items[items.length - 1];
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault();
    last.focus();
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault();
    first.focus();
  }
}
onMounted(() => {
  previous = document.activeElement;
  panel.value.querySelector("input,button")?.focus();
  document.addEventListener("keydown", keydown);
});
onBeforeUnmount(() => {
  document.removeEventListener("keydown", keydown);
  previous?.focus();
});
</script>
<template>
  <Teleport to="body"
    ><div class="modal-backdrop" @mousedown.self="emit('close')">
      <section
        ref="panel"
        class="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <header>
          <h2 id="modal-title">{{ title }}</h2>
          <button class="icon-button" aria-label="닫기" @click="emit('close')">
            <X :size="20" />
          </button>
        </header>
        <slot />
      </section></div
  ></Teleport>
</template>
