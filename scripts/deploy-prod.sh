#!/usr/bin/env bash
# 一键部署后端到生产轻量服务器
# 用法:
#   ./scripts/deploy-prod.sh                  # 打包 + 上传 JAR + 重启 + 健康检查
#   ./scripts/deploy-prod.sh --product-images # 额外同步 backend/uploads/products/（商品图）
#   ./scripts/deploy-prod.sh --check          # 仅远程健康检查
#   ./scripts/deploy-prod.sh --no-build       # 跳过 mvn，只上传已有 target/*.jar
#
# 注意: 不同步 uploads/ 根目录临时图（对账单/凭证 UUID 文件），避免覆盖生产数据。
#
# 可选环境变量（或在本机创建 scripts/deploy-prod.env 覆盖）:
#   DEPLOY_SSH=vwholesale
#   DEPLOY_REMOTE_DIR=/opt/vwholesale
#   DEPLOY_SERVICE=vwholesale

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND="$ROOT/backend"
ENV_FILE="$ROOT/scripts/deploy-prod.env"

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

SSH_HOST="${DEPLOY_SSH:-vwholesale}"
REMOTE_DIR="${DEPLOY_REMOTE_DIR:-/opt/vwholesale}"
SERVICE="${DEPLOY_SERVICE:-vwholesale}"

SYNC_PRODUCT_IMAGES=0
SKIP_BUILD=0
CHECK_ONLY=0

for arg in "$@"; do
  case "$arg" in
    --product-images) SYNC_PRODUCT_IMAGES=1 ;;
    --uploads)
      echo ">>> 提示: --uploads 已弃用，请改用 --product-images（仅同步商品图目录）" >&2
      SYNC_PRODUCT_IMAGES=1
      ;;
    --no-build) SKIP_BUILD=1 ;;
    --check) CHECK_ONLY=1 ;;
    -h|--help)
      sed -n '2,12p' "$0"
      exit 0
      ;;
    *)
      echo "未知参数: $arg（可用 --product-images --no-build --check）" >&2
      exit 1
      ;;
  esac
done

remote() {
  ssh "$SSH_HOST" "$@"
}

health_check() {
  echo ">>> 健康检查..."
  remote "curl -sf http://127.0.0.1:8080/api/health" && echo
}

if [[ "$CHECK_ONLY" -eq 1 ]]; then
  health_check
  exit 0
fi

echo ">>> 目标: $SSH_HOST:$REMOTE_DIR"

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  echo ">>> Maven 打包..."
  (cd "$BACKEND" && mvn -DskipTests clean package -q)
fi

JAR=( "$BACKEND"/target/vegetable-wholesale-backend-*.jar )
if [[ ! -f "${JAR[0]}" ]]; then
  echo "未找到 JAR: $BACKEND/target/vegetable-wholesale-backend-*.jar" >&2
  exit 1
fi

echo ">>> 上传 JAR (${JAR[0]##*/})..."
scp "${JAR[0]}" "$SSH_HOST:$REMOTE_DIR/app.jar"

if [[ "$SYNC_PRODUCT_IMAGES" -eq 1 ]]; then
  if [[ ! -d "$BACKEND/uploads/products" ]]; then
    echo "未找到商品图目录: $BACKEND/uploads/products" >&2
    exit 1
  fi
  echo ">>> 同步商品图 uploads/products/（不删除远端其它文件）..."
  remote "mkdir -p $REMOTE_DIR/uploads/products"
  rsync -az "$BACKEND/uploads/products/" "$SSH_HOST:$REMOTE_DIR/uploads/products/"
fi

echo ">>> 重启服务 $SERVICE..."
remote "sudo systemctl restart $SERVICE"

echo ">>> 等待启动..."
for i in {1..30}; do
  if remote "curl -sf http://127.0.0.1:8080/api/health" >/dev/null 2>&1; then
    health_check
    echo ">>> 部署完成"
    exit 0
  fi
  sleep 2
done

echo ">>> 启动超时，最近日志:" >&2
remote "sudo journalctl -u $SERVICE -n 30 --no-pager" >&2
exit 1
