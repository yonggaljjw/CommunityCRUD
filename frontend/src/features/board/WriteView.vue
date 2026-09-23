<script setup>
// 게시글 작성/수정 화면: 같은 폼을 route에 따라 두 용도로 재사용합니다.
import { reactive, ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowLeft, PenLine } from "lucide-vue-next";
import { api } from "../../services/api";
import { state, notify, rememberNickname } from "../../stores/appState";
const route = useRoute(),
  router = useRouter(),
  editing = !!route.params.id,
  busy = ref(false),
  error = ref(""),
  loaded = ref(!editing);
const form = reactive({
  galleryId: Number(route.query.gallery) || state.galleries[0]?.id,
  category: "일반",
  title: "",
  content: "",
  nickname: state.nickname,
  password: "",
});
// 수정 화면이면 기존 게시글을 불러와 같은 form 객체에 채워 넣습니다.
onMounted(async () => {
  if (editing)
    try {
      const p = await api(`/posts/${route.params.id}`);
      Object.assign(form, {
        galleryId: p.galleryId,
        category: p.category,
        title: p.title,
        content: p.content,
        nickname: p.nickname,
      });
      loaded.value = true;
    } catch (e) {
      error.value = e.message;
    }
});
// 작성은 POST, 수정은 PUT을 사용하지만 폼과 성공 후 이동 흐름은 함께 재사용합니다.
async function submit() {
  busy.value = true;
  error.value = "";
  try {
    const post = await api(editing ? `/posts/${route.params.id}` : "/posts", {
      method: editing ? "PUT" : "POST",
      body: form,
    });
    state.nickname = form.nickname;
    rememberNickname();
    state.galleries = await api("/galleries").catch(() => state.galleries);
    notify(editing ? "게시글을 수정했습니다." : "이야기를 등록했습니다.");
    router.push(`/posts/${post.id}`);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="breadcrumb">
    커뮤니티 <span>/</span> {{ editing ? "글 수정" : "글쓰기" }}
  </div>
  <div class="page-heading">
    <div>
      <span class="eyebrow">YOUR STORY</span>
      <h1>{{ editing ? "이야기 수정하기" : "어떤 이야기를 나눌까요?" }}</h1>
      <p>익명으로, 편하게. 서로를 존중하는 이야기를 남겨 주세요.</p>
    </div>
  </div>
  <section class="panel editor-panel">
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <form v-if="loaded" @submit.prevent="submit">
      <div class="form-row">
        <label
          >갤러리<select v-model="form.galleryId">
            <option v-for="g in state.galleries" :value="g.id" :key="g.id">
              {{ g.name }}
            </option>
          </select></label
        ><label
          >말머리<select v-model="form.category">
            <option
              v-for="c in ['일반', '질문', '정보', '유머', '후기', '잡담']"
              :key="c"
            >
              {{ c }}
            </option>
          </select></label
        >
      </div>
      <label
        >제목<input
          v-model="form.title"
          maxlength="150"
          required
          placeholder="제목을 입력해 주세요" /></label
      ><label
        >내용<textarea
          v-model="form.content"
          rows="15"
          maxlength="20000"
          required
          placeholder="함께 나누고 싶은 이야기를 적어 주세요."
        />
      </label>
      <div class="editor-counter">
        {{ form.content.length.toLocaleString() }} / 20,000
      </div>
      <div class="form-row">
        <label
          >닉네임<input
            v-model="form.nickname"
            maxlength="20"
            required /></label
        ><label
          >{{ editing ? "작성할 때 사용한 비밀번호" : "수정·삭제 비밀번호"
          }}<input
            v-model="form.password"
            type="password"
            minlength="4"
            maxlength="60"
            required
            autocomplete="off"
            placeholder="4자 이상"
        /></label>
      </div>
      <p class="help">
        비밀번호는 복구할 수 없습니다. 글 수정·삭제에 필요하니 기억해 주세요.
      </p>
      <div class="form-actions">
        <RouterLink
          :to="editing ? `/posts/${route.params.id}` : '/'"
          class="button"
          ><ArrowLeft :size="16" /> 취소</RouterLink
        ><button class="primary" :disabled="busy">
          <PenLine :size="16" />{{
            busy ? "저장 중…" : editing ? "수정 완료" : "등록하기"
          }}
        </button>
      </div>
    </form>
  </section>
</template>
