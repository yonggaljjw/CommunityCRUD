# components/layout

앱 전체에 공통으로 보이는 헤더·좌측 메뉴·우측 라운지·푸터를 분리했습니다. `App.vue`는 이 컴포넌트들을 조립만 합니다.

## 파일 역할

- `SiteHeader.vue`: 브랜드, 검색, 상단 메뉴, 모바일 메뉴 버튼
- `LeftSidebar.vue`: 갤러리 목록과 모바일 사이드 메뉴
- `RightSidebar.vue`: 오른쪽 보조 정보 영역
- `SiteFooter.vue`: 공통 하단 영역

## 기본 개념

### Layout Component

페이지의 실제 업무 기능보다 여러 화면이 공유하는 **배치와 골격**을 담당하는 컴포넌트입니다.

```text
SiteHeader
────────────────
LeftSidebar | RouterView | RightSidebar
────────────────
SiteFooter
```

이렇게 분리하면 게시판 화면과 채팅 화면이 바뀌더라도 공통 레이아웃을 반복 작성하지 않아도 됩니다.

### 부모-자식 이벤트

예를 들어 `SiteHeader`의 모바일 메뉴 버튼은 `App.vue`의 `mobileMenu` 상태를 직접 수정하지 않습니다.

```text
SiteHeader
  ↓ emit("toggle-menu")
App.vue
  ↓ 상태 변경
LeftSidebar
```

상태를 실제로 소유하는 부모가 변경을 담당하고 자식은 이벤트만 알리는 패턴입니다.

### RouterLink

일반 `<a>` 대신 Vue Router의 `<RouterLink>`를 사용하면 SPA 내부 페이지 이동 시 전체 HTML을 다시 불러오지 않고 필요한 화면만 교체할 수 있습니다.

## 분리 기준

페이지 고유 로직이 생긴다면 `layout`에 넣지 않습니다. 이 폴더는 "어느 페이지에서도 공통으로 보이는가?"를 기준으로 유지합니다.
