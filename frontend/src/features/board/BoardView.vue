<script setup>
// 게시판 목록 화면: 검색·카테고리 필터·정렬·페이지 이동을 담당합니다.
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  PenLine,
  RefreshCw,
  ArrowUpRight,
  MessageCircle,
  ChevronLeft,
  ChevronRight,
  Search,
  Flame,
} from "lucide-vue-next";
import { api } from "../../services/api";
import { shortDate } from "../../utils/date";
import { state } from "../../stores/appState";
import ChatPanel from "../chat/ChatPanel.vue";
const route = useRoute(),
  router = useRouter();
const result = ref({ items: [], total: 0 }),
  loading = ref(false),
  error = ref(""),
  category = ref(""),
  page = ref(1),
  search = ref(route.query.q || "");
const sort = computed(() => route.query.sort || "new"),
  gallery = computed(() =>
    state.galleries.find((g) => g.id === Number(route.params.id)),
  );
const pages = computed(() => Math.max(1, Math.ceil(result.value.total / 15)));
const pageNumbers = computed(() => {
  const start = Math.max(1, Math.min(page.value - 2, pages.value - 4));
  return Array.from({ length: Math.min(5, pages.value) }, (_, i) => start + i);
});
// 비동기 요청이 겹쳤을 때 늦게 도착한 과거 응답이 최신 화면을 덮지 못하게 순번을 사용합니다.
let sequence = 0;
// 현재 URL의 갤러리/검색/정렬 조건으로 게시글 목록을 다시 조회합니다.
async function load() {
  const current = ++sequence;
  loading.value = true;
  error.value = "";
  const params = new URLSearchParams({
    page: page.value,
    size: 15,
    sort: sort.value,
    category: category.value,
    q: route.query.q || "",
  });
  if (gallery.value) params.set("galleryId", gallery.value.id);
  try {
    const data = await api(`/posts?${params}`);
    if (current === sequence) result.value = data;
  } catch (e) {
    if (current === sequence) error.value = e.message;
  } finally {
    if (current === sequence) loading.value = false;
  }
}
// URL이 바뀌면 같은 컴포넌트를 재사용하면서 목록만 새 조건으로 갱신합니다.
watch(
  () => [route.params.id, route.query.q, route.query.sort],
  () => {
    page.value = 1;
    search.value = route.query.q || "";
    load();
  },
  { immediate: true },
);
function filter(value) {
  category.value = value;
  page.value = 1;
  load();
}
function goPage(value) {
  page.value = value;
  load();
}
function searchPosts() {
  router.push({
    path: route.path,
    query: { ...route.query, q: search.value.trim() || undefined },
  });
}
</script>
<template>
  <div class="breadcrumb">
    커뮤니티 <span>/</span> {{ gallery?.name || "전체 갤러리" }}
  </div>
  <div class="page-heading">
    <div>
      <span class="eyebrow">{{ gallery ? "GALLERY" : "THE COMMUNITY" }}</span>
      <h1>
        {{ gallery ? `${gallery.name} 갤러리` : "오늘의 이야기"
        }}<span class="heading-dot">.</span>
      </h1>
      <p>
        {{
          gallery?.description ||
          "관심사를 나누고, 자유롭게 이야기하는 우리들의 공간."
        }}
      </p>
    </div>
    <RouterLink
      :to="{ path: '/write', query: { gallery: gallery?.id } }"
      class="primary"
      ><PenLine :size="16" />글쓰기</RouterLink
    >
  </div>
  <div v-if="!gallery" class="feature-strip">
    <div class="feature-text">
      <span class="eyebrow">LIVE TOGETHER</span>
      <h2>같은 관심사로 모이는<br />실시간 익명 라운지</h2>
      <RouterLink to="/chat"
        >지금 대화에 참여하기 <ArrowUpRight :size="16"
      /></RouterLink>
    </div>
    <div class="feature-rooms">
      <RouterLink
        v-for="r in state.rooms.slice(0, 3)"
        :key="r.id"
        :to="`/chat/${r.id}`"
        ><span>#</span>{{ r.name }}<ArrowUpRight :size="15"
      /></RouterLink>
    </div>
  </div>
  <section class="panel board-panel">
    <div class="board-tabs">
      <button
        :class="{ active: sort === 'new' }"
        @click="router.push({ query: { ...route.query, sort: undefined } })"
      >
        전체글</button
      ><button
        :class="{ active: sort === 'hot' }"
        @click="router.push({ query: { ...route.query, sort: 'hot' } })"
      >
        <Flame :size="15" />인기글</button
      ><button
        :class="{ active: sort === 'best' }"
        @click="router.push({ query: { ...route.query, sort: 'best' } })"
      >
        개념글 <small>30+</small></button
      ><span>{{ result.total.toLocaleString() }}개의 이야기</span>
    </div>
    <div class="category-row">
      <button
        v-for="c in ['', '일반', '질문', '정보', '유머', '후기', '잡담']"
        :key="c"
        :class="{ selected: category === c }"
        @click="filter(c)"
      >
        {{ c || "전체" }}
      </button>
    </div>
    <div v-if="route.query.q" class="search-notice">
      “{{ route.query.q }}” 검색 결과
      <RouterLink :to="{ query: { ...route.query, q: undefined } }"
        >검색 해제</RouterLink
      >
    </div>
    <div v-if="error" class="empty">
      <p>{{ error }}</p>
      <button @click="load">다시 시도</button>
    </div>
    <div v-else-if="loading" class="empty" role="status">
      게시글을 불러오는 중입니다.
    </div>
    <template v-else
      ><div class="table-wrap">
        <table class="post-table">
          <thead>
            <tr>
              <th class="number-col">번호</th>
              <th class="category-col">말머리</th>
              <th class="title-col">제목</th>
              <th class="author-col">글쓴이</th>
              <th class="date-col">작성일</th>
              <th class="stat-col">조회</th>
              <th class="stat-col">추천</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in result.items" :key="p.id">
              <td class="number-col">{{ p.id }}</td>
              <td class="category-col">
                <span class="category-badge">{{ p.category }}</span>
              </td>
              <td class="title-col">
                <RouterLink :to="`/posts/${p.id}`"
                  ><small v-if="!gallery" class="gallery-prefix">{{
                    p.galleryName
                  }}</small
                  >{{ p.title }}
                  <b v-if="p.commentCount" class="comment-count"
                    >[{{ p.commentCount }}]</b
                  ></RouterLink
                >
              </td>
              <td class="author-col">{{ p.nickname }}</td>
              <td class="date-col">{{ shortDate(p.createdAt) }}</td>
              <td class="stat-col">{{ p.views }}</td>
              <td class="stat-col" :class="{ accent: p.likes > 0 }">
                {{ p.likes }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="!result.items.length" class="empty">
        <MessageCircle :size="28" />
        <h3>
          {{
            sort === "best"
              ? "아직 추천 30개를 받은 글이 없어요."
              : "아직 이야기가 없어요."
          }}
        </h3>
        <p>조건을 바꾸거나 첫 번째 이야기를 남겨 보세요.</p>
        <RouterLink to="/write" class="primary">첫 글 쓰기</RouterLink>
      </div></template
    >
    <div class="board-bottom">
      <button class="text-button" @click="load" aria-label="게시글 새로고침">
        <RefreshCw :size="15" />
      </button>
      <div class="pagination">
        <button
          :disabled="page <= 1"
          @click="goPage(page - 1)"
          aria-label="이전 페이지"
        >
          <ChevronLeft :size="16" /></button
        ><button
          v-for="n in pageNumbers"
          :key="n"
          :class="{ current: n === page }"
          @click="goPage(n)"
        >
          {{ n }}</button
        ><button
          :disabled="page >= pages"
          @click="goPage(page + 1)"
          aria-label="다음 페이지"
        >
          <ChevronRight :size="16" />
        </button>
      </div>
      <span>{{ page }} / {{ pages }}</span>
    </div>
    <form class="board-search" @submit.prevent="searchPosts">
      <span>제목 + 내용</span
      ><input
        v-model="search"
        maxlength="100"
        aria-label="갤러리 내 검색"
        placeholder="이야기를 검색해 보세요"
      /><button class="primary" aria-label="검색"><Search :size="17" /></button>
    </form>
  </section>
  <ChatPanel v-if="state.rooms[0]" :room="state.rooms[0]" compact />
</template>
