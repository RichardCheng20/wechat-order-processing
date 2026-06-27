#!/usr/bin/env bash
# 本地 API 冒烟测试（需后端 8080 已启动）
# 可选: SMOKE_DP_PASSWORD=123456（数据平台 6 位密码，未设置时默认 123456）
set -euo pipefail
BASE="${API_BASE:-http://127.0.0.1:8080}"
SMOKE_DP_PASSWORD="${SMOKE_DP_PASSWORD:-123456}"
DP_READY=0

pass=0
fail=0
warn=0

ok() { echo "  ✓ $1"; pass=$((pass + 1)); }
bad() { echo "  ✗ $1"; fail=$((fail + 1)); }
warn_msg() { echo "  ! $1"; warn=$((warn + 1)); }

login() {
  local openid=$1 role=$2 name=$3
  curl -s -X POST "$BASE/api/auth/dev-login" \
    -H 'Content-Type: application/json' \
    -d "{\"openid\":\"$openid\",\"nickname\":\"$name\",\"role\":\"$role\"}"
}

auth_get() {
  local token=$1 path=$2
  curl -s -H "Authorization: Bearer $token" "$BASE$path"
}

auth_get_dp() {
  local token=$1 path=$2
  curl -s -H "Authorization: Bearer $token" \
    -H "X-Data-Platform-Password: $SMOKE_DP_PASSWORD" \
    "$BASE$path"
}

auth_post() {
  local token=$1 path=$2 body=$3
  curl -s -X POST -H "Authorization: Bearer $token" \
    -H 'Content-Type: application/json' \
    -d "$body" "$BASE$path"
}

auth_put() {
  local token=$1 path=$2 body=$3
  curl -s -X PUT -H "Authorization: Bearer $token" \
    -H 'Content-Type: application/json' \
    -d "$body" "$BASE$path"
}

code_of() {
  python3 -c "import json,sys; print(json.load(sys.stdin).get('code','?'))" 2>/dev/null || echo "?"
}

data_field() {
  python3 -c "import json,sys; d=json.load(sys.stdin).get('data'); print('ok' if d is not None else 'null')" 2>/dev/null
}

ensure_data_platform_password() {
  local token=$1
  local status_resp enabled verify_resp set_resp

  status_resp=$(auth_get "$token" "/api/boss/data-platform/password/status")
  enabled=$(echo "$status_resp" | python3 -c "import json,sys; d=json.load(sys.stdin).get('data') or {}; print('1' if d.get('passwordEnabled') else '0')" 2>/dev/null || echo "0")

  if [ "$enabled" = "1" ]; then
    verify_resp=$(auth_post "$token" "/api/boss/data-platform/password/verify" "{\"password\":\"$SMOKE_DP_PASSWORD\"}")
    if [ "$(echo "$verify_resp" | code_of)" = "0" ]; then
      ok "数据平台密码验证"
      DP_READY=1
      return 0
    fi
    bad "数据平台密码不匹配（请设置 SMOKE_DP_PASSWORD 环境变量）"
    return 1
  fi

  set_resp=$(auth_put "$token" "/api/boss/data-platform/password" "{\"password\":\"$SMOKE_DP_PASSWORD\"}")
  if [ "$(echo "$set_resp" | code_of)" = "0" ]; then
    ok "数据平台密码已初始化（冒烟用）"
    DP_READY=1
    return 0
  fi
  bad "数据平台密码初始化失败"
  return 1
}

test_data_platform_get() {
  local token=$1 path=$2
  local resp c

  if [ "$DP_READY" -ne 1 ]; then
    warn_msg "跳过 $path（数据平台密码未就绪）"
    return
  fi
  resp=$(auth_get_dp "$token" "$path")
  c=$(echo "$resp" | code_of)
  if [ "$c" = "0" ]; then ok "老板 GET $path"; else bad "老板 GET $path (code=$c)"; fi
}

echo "=== API 冒烟测试 $BASE ==="

HEALTH=$(curl -s "$BASE/api/health")
echo "$HEALTH" | grep -q '"status":"UP"' && ok "health" || bad "health"

OWNER_JSON=$(login dev-owner-001 OWNER_ADMIN 老板)
OWNER_TOKEN=$(echo "$OWNER_JSON" | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null || true)
if [ -n "$OWNER_TOKEN" ]; then ok "老板登录"; else bad "老板登录"; fi

MGR_JSON=$(login dev-stall-manager-001 STALL_MANAGER 档口经理)
MGR_TOKEN=$(echo "$MGR_JSON" | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null || true)
if [ -n "$MGR_TOKEN" ]; then ok "档口经理登录"; else bad "档口经理登录"; fi

CUST_JSON=$(login dev-customer-001 CUSTOMER 客户A)
CUST_TOKEN=$(echo "$CUST_JSON" | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null || true)
if [ -n "$CUST_TOKEN" ]; then ok "客户登录"; else bad "客户登录"; fi

if [ -z "$OWNER_TOKEN" ]; then
  echo "无法继续：老板 token 缺失"
  exit 1
fi

ensure_data_platform_password "$OWNER_TOKEN" || true

# 老板端核心接口（无需数据平台密码）
for path in \
  "/api/boss/orders" \
  "/api/boss/customers" \
  "/api/boss/products" \
  "/api/boss/product-categories" \
  "/api/boss/personnel" \
  "/api/boss/procurement/tasks"; do
  resp=$(auth_get "$OWNER_TOKEN" "$path")
  c=$(echo "$resp" | code_of)
  if [ "$c" = "0" ]; then ok "老板 GET $path"; else bad "老板 GET $path (code=$c)"; fi
done

# 经营数据（需 X-Data-Platform-Password）
test_data_platform_get "$OWNER_TOKEN" "/api/boss/dashboard"
test_data_platform_get "$OWNER_TOKEN" "/api/boss/stats/revenue?dateFrom=2026-06-01&dateTo=2026-06-30"

# 档口经理：业务 OK，经营数据应 403
if [ -n "$MGR_TOKEN" ]; then
  resp=$(auth_get "$MGR_TOKEN" "/api/boss/orders")
  c=$(echo "$resp" | code_of)
  [ "$c" = "0" ] && ok "经理 GET /api/boss/orders" || bad "经理 GET orders (code=$c)"

  resp=$(auth_get "$MGR_TOKEN" "/api/boss/dashboard")
  c=$(echo "$resp" | code_of)
  if [ "$c" != "0" ]; then ok "经理 dashboard 被拦截 (code=$c)"; else bad "经理 dashboard 应拒绝但返回 0"; fi

  resp=$(auth_get "$MGR_TOKEN" "/api/boss/stats/revenue?dateFrom=2026-06-01&dateTo=2026-06-30")
  c=$(echo "$resp" | code_of)
  if [ "$c" != "0" ]; then ok "经理 stats 被拦截 (code=$c)"; else bad "经理 stats 应拒绝但返回 0"; fi
fi

# 客户订单
if [ -n "$CUST_TOKEN" ]; then
  resp=$(auth_get "$CUST_TOKEN" "/api/customer/orders?dateFrom=2026-01-01&dateTo=2026-12-31&tabFilter=ALL")
  c=$(echo "$resp" | code_of)
  [ "$c" = "0" ] && ok "客户 GET orders" || bad "客户 GET orders (code=$c)"
fi

# 客户详情统计：取第一个客户
CUST_LIST=$(auth_get "$OWNER_TOKEN" "/api/boss/customers")
FIRST_ID=$(echo "$CUST_LIST" | python3 -c "
import json,sys
d=json.load(sys.stdin).get('data') or []
print(d[0]['id'] if d else '')
" 2>/dev/null || true)
if [ -n "$FIRST_ID" ]; then
  detail=$(auth_get "$OWNER_TOKEN" "/api/boss/customers/$FIRST_ID")
  sales=$(echo "$detail" | python3 -c "import json,sys; d=json.load(sys.stdin).get('data') or {}; print(d.get('totalSalesAmount', 'null'))" 2>/dev/null)
  debt=$(echo "$detail" | python3 -c "import json,sys; d=json.load(sys.stdin).get('data') or {}; print(d.get('outstandingAmount', 'null'))" 2>/dev/null)
  ok "客户详情 id=$FIRST_ID 销售=$sales 欠款=$debt"

  orders=$(auth_get "$OWNER_TOKEN" "/api/boss/orders?customerId=$FIRST_ID&receivableOnly=true&dateType=DELIVERY&dateFrom=2026-01-01&dateTo=2026-12-31")
  cnt=$(echo "$orders" | python3 -c "import json,sys; print(len(json.load(sys.stdin).get('data') or []))" 2>/dev/null)
  has_dd=$(echo "$orders" | python3 -c "
import json,sys
rows=json.load(sys.stdin).get('data') or []
print('yes' if rows and rows[0].get('deliveryDate') else 'no')
" 2>/dev/null)
  ok "对账订单数=$cnt deliveryDate=$has_dd"
  if [ "$cnt" != "0" ] && [ "$has_dd" = "no" ]; then bad "列表缺 deliveryDate"; fi
else
  warn_msg "无客户数据，跳过详情校验"
fi

# 报价单
if [ -n "$FIRST_ID" ]; then
  q=$(auth_get "$OWNER_TOKEN" "/api/boss/customer-quotes/$FIRST_ID")
  c=$(echo "$q" | code_of)
  lines=$(echo "$q" | python3 -c "import json,sys; d=json.load(sys.stdin).get('data') or {}; print(len(d.get('lines') or []))" 2>/dev/null)
  [ "$c" = "0" ] && ok "报价单详情 lines=$lines" || bad "报价单详情 (code=$c)"
fi

echo ""
echo "=== 结果: 通过 $pass, 失败 $fail, 警告 $warn ==="
[ "$fail" -eq 0 ]
