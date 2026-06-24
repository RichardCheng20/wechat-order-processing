#!/usr/bin/env bash
# 清理 uploads 根目录下的临时 UUID 图片（对账单/凭证测试图），保留 uploads/products/
# 用法:
#   ./scripts/clean-upload-temp.sh           # 清理本机 backend/uploads/
#   ./scripts/clean-upload-temp.sh --remote  # 清理生产服务器

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$ROOT/scripts/deploy-prod.env"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

SSH_HOST="${DEPLOY_SSH:-vwholesale}"
REMOTE_DIR="${DEPLOY_REMOTE_DIR:-/opt/vwholesale}"
LOCAL_UPLOADS="$ROOT/backend/uploads"

clean_dir() {
  local dir=$1
  local before after
  before=$(find "$dir" -maxdepth 1 -type f 2>/dev/null | wc -l | tr -d ' ')
  find "$dir" -maxdepth 1 -type f \( -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' -o -name '*.webp' -o -name '*.gif' \) -delete 2>/dev/null || true
  after=$(find "$dir" -maxdepth 1 -type f 2>/dev/null | wc -l | tr -d ' ')
  echo ">>> $dir: 删除 $((before - after)) 个临时文件，保留 products/ 子目录"
}

if [[ "${1:-}" == "--remote" ]]; then
  echo ">>> 清理远端 $SSH_HOST:$REMOTE_DIR/uploads/"
  ssh "$SSH_HOST" "before=\$(find $REMOTE_DIR/uploads -maxdepth 1 -type f | wc -l); find $REMOTE_DIR/uploads -maxdepth 1 -type f \\( -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' -o -name '*.webp' -o -name '*.gif' \\) -delete; after=\$(find $REMOTE_DIR/uploads -maxdepth 1 -type f | wc -l); echo \"删除 \$((before - after)) 个临时文件\"; ls -la $REMOTE_DIR/uploads/"
  exit 0
fi

if [[ ! -d "$LOCAL_UPLOADS" ]]; then
  echo "未找到: $LOCAL_UPLOADS" >&2
  exit 1
fi

echo ">>> 清理本机 $LOCAL_UPLOADS"
clean_dir "$LOCAL_UPLOADS"
