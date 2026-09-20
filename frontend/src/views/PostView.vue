<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ThumbsUp,
  ArrowLeft,
  Link,
  PenLine,
  Trash2,
  MessageCircle,
} from "lucide-vue-next";
import { api, dateTime } from "../api";
import { state, notify, rememberNickname } from "../state";
import Modal from "../components/Modal.vue";
const route = useRoute(),
  router = useRouter(),
  post = ref(null),
  comments = ref([]),
  error = ref(""),
  busy = ref(false),
  liking = ref(false),
  more = ref(false);
const form = reactive({ nickname: state.nickname, password: "", content: "" }),
  deleting = ref(null),
  password = ref(""),
  deleteError = ref(""),
  deleteBusy = ref(false);
async function load() {
  try {
    post.value = await api(`/posts/${route.params.id}`);
    const rows = await api(`/posts/${route.params.id}/comments`);
    comments.value = rows;
    more.value = rows.length === 100;
  } catch (e) {
    error.value = e.message;
  }
}
onMounted(load);
async function loadMore() {
  try {
    const rows = await api(
      `/posts/${post.value.id}/comments?after=${comments.value.at(-1)?.id || 0}`,
    );
    comments.value.push(...rows);
    more.value = rows.length === 100;
  } catch (e) {
    notify(e.message);
  }
}
async function like() {
  liking.value = true;
  try {
    post.value = await api(`/posts/${post.value.id}/like`, { method: "POST" });
  } catch (e) {
    notify(e.message);
  } finally {
    liking.value = false;
  }
}
async function comment() {
  busy.value = true;
  try {
    const c = await api(`/posts/${post.value.id}/comments`, {
      method: "POST",
      body: form,
    });
    if (!more.value) comments.value.push(c);
    post.value.commentCount++;
    form.content = "";
    form.password = "";
    state.nickname = form.nickname;
    rememberNickname();
    notify("댓글을 등록했습니다.");
  } catch (e) {
    notify(e.message);
  } finally {
    busy.value = false;
  }
}
function openDelete(target) {
  deleting.value = target;
  password.value = "";
  deleteError.value = "";
}
async function remove() {
  deleteBusy.value = true;
  try {
    const path =
      deleting.value === "post"
        ? `/posts/${post.value.id}`
        : `/posts/${post.value.id}/comments/${deleting.value}`;
    await api(path, { method: "DELETE", body: { password: password.value } });
    if (deleting.value === "post") {
      state.galleries = await api("/galleries").catch(() => state.galleries);
      router.push(`/galleries/${post.value.galleryId}`);
    } else {
      comments.value = comments.value.filter((c) => c.id !== deleting.value);
      post.value.commentCount--;
    }
    deleting.value = null;
    notify("삭제했습니다.");
  } catch (e) {
    deleteError.value = e.message;
  } finally {
    deleteBusy.value = false;
  }
}
async function copy() {
  try {
    await navigator.clipboard.writeText(location.href);
    notify("게시글 주소를 복사했습니다.");
  } catch {
    notify("주소 표시줄의 URL을 복사해 주세요.");
  }
}
</script>
<template>
  <div v-if="error" class="empty">
    <h2>{{ error }}</h2>
    <RouterLink to="/">목록으로</RouterLink>
  </div>
  <div v-else-if="!post" class="empty">게시글을 불러오는 중입니다.</div>
  <template v-else
    ><div class="breadcrumb">
      커뮤니티 <span>/</span
      ><RouterLink :to="`/galleries/${post.galleryId}`"
        >{{ post.galleryName }} 갤러리</RouterLink
      >
    </div>
    <div class="post-top-actions">
      <RouterLink :to="`/galleries/${post.galleryId}`" class="text-button"
        ><ArrowLeft :size="16" />목록으로</RouterLink
      ><RouterLink to="/write" class="primary"
        ><PenLine :size="16" />글쓰기</RouterLink
      >
    </div>
    <article class="panel article">
      <header>
        <span class="category-badge">{{ post.category }}</span>
        <h1>{{ post.title }}</h1>
        <div class="post-meta">
          <b>{{ post.nickname }}</b
          ><span>#{{ post.authorTag }}</span
          ><span>{{ dateTime(post.createdAt) }}</span
          ><span>조회 {{ post.views }}</span
          ><span>댓글 {{ post.commentCount }}</span>
        </div>
      </header>
      <div class="post-content">{{ post.content }}</div>
      <div class="vote-area">
        <button
          :class="['vote-button', { liked: post.liked }]"
          :disabled="liking"
          @click="like"
        >
          <ThumbsUp :size="23" /><b>{{ post.likes }}</b
          ><span>{{ post.liked ? "추천 취소" : "이 글 추천하기" }}</span>
        </button>
      </div>
      <footer class="article-footer">
        <button class="text-button" @click="copy">
          <Link :size="15" />주소 복사
        </button>
        <div>
          <RouterLink :to="`/posts/${post.id}/edit`" class="text-button"
            >수정</RouterLink
          ><button class="text-button" @click="openDelete('post')">삭제</button>
        </div>
      </footer>
    </article>
    <section class="panel comments-panel">
      <h2>
        <MessageCircle :size="19" />댓글
        <span class="accent">{{ post.commentCount }}</span>
      </h2>
      <p v-if="!comments.length" class="muted empty-small">
        첫 번째 댓글을 남겨 보세요.
      </p>
      <div v-for="c in comments" :key="c.id" class="comment">
        <div class="comment-meta">
          <b>{{ c.nickname }}</b
          ><span>#{{ c.authorTag }}</span
          ><time>{{ dateTime(c.createdAt) }}</time
          ><button
            class="icon-button"
            aria-label="댓글 삭제"
            @click="openDelete(c.id)"
          >
            <Trash2 :size="14" />
          </button>
        </div>
        <p>{{ c.content }}</p>
      </div>
      <button v-if="more" class="button" @click="loadMore">댓글 더 보기</button>
      <form class="comment-form" @submit.prevent="comment">
        <div class="form-row">
          <label
            >닉네임<input
              v-model="form.nickname"
              required
              maxlength="20" /></label
          ><label
            >삭제 비밀번호<input
              v-model="form.password"
              type="password"
              required
              minlength="4"
              maxlength="60"
              autocomplete="off"
              placeholder="4자 이상"
          /></label>
        </div>
        <label class="sr-only" for="comment-content">댓글 내용</label
        ><textarea
          id="comment-content"
          v-model="form.content"
          required
          maxlength="1000"
          rows="3"
          placeholder="서로를 존중하는 댓글을 남겨 주세요."
        />
        <div class="form-actions">
          <span class="help">{{ form.content.length }} / 1,000</span
          ><button class="primary" :disabled="busy">
            {{ busy ? "등록 중…" : "댓글 등록" }}
          </button>
        </div>
      </form>
    </section></template
  ><Modal v-if="deleting" title="삭제 비밀번호 확인" @close="deleting = null"
    ><form @submit.prevent="remove">
      <p class="muted">
        삭제하면 되돌릴 수 없습니다. 작성 시 설정한 비밀번호를 입력해 주세요.
      </p>
      <label
        >비밀번호<input
          v-model="password"
          type="password"
          required
          maxlength="60"
          autocomplete="off"
      /></label>
      <p v-if="deleteError" class="error" role="alert">{{ deleteError }}</p>
      <div class="form-actions">
        <button type="button" @click="deleting = null">취소</button
        ><button class="danger" :disabled="deleteBusy">
          {{ deleteBusy ? "확인 중…" : "삭제하기" }}
        </button>
      </div>
    </form></Modal
  >
</template>
