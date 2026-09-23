<script setup>
// 화면 공통 푸터. 현재 연도만 브라우저 시간 기준으로 표시합니다.
const year = new Date().getFullYear();
</script>

<template>
  <footer class="shell site-footer">
    <b>JOJI COMMINITY</b>
    <span>이야기가 모이고, 대화가 이어지는 곳.</span>
    <span>© {{ year }} JOJI</span>
  </footer>
</template>
