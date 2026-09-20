"""표준 라이브러리만 사용해 .env를 만듭니다. 기존 설정 파일은 절대 덮어쓰지 않습니다."""
from pathlib import Path
import os
import secrets

root = Path(__file__).resolve().parents[1]
target = root / ".env"
if target.exists():
    raise SystemExit(".env가 이미 있습니다. 기존 설정을 그대로 사용합니다.")
text = (root / ".env.example").read_text(encoding="utf-8")
for placeholder in (
    "replace_with_a_long_random_database_password",
    "replace_with_a_different_long_root_password",
    "replace_with_a_demo_post_password",
):
    text = text.replace(placeholder, secrets.token_hex(24))
# x 모드로 동시에 실행되더라도 이미 있는 .env를 덮어쓰지 않습니다.
with target.open("x", encoding="utf-8") as file:
    file.write(text)
os.chmod(target, 0o600)
print(".env를 생성했습니다. 이제 docker compose up --build -d 를 실행하세요.")
print("학습용 예시 글의 수정·삭제 비밀번호는 .env의 DEMO_POST_PASSWORD입니다.")
