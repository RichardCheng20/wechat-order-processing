#!/usr/bin/env bash
# 老板端 API 冒烟测试
# 用法: BASE_URL=http://localhost:8081 ./scripts/smoke-test-boss-apis.sh

set -uo pipefail
BASE_URL="${BASE_URL:-http://localhost:8081}"
PASS=0
FAIL=0
RESULTS=()

log() { echo "[$(date +%H:%M:%S)] $*"; }

record_pass() {
  PASS=$((PASS + 1))
  RESULTS+=("PASS | $1")
  log "✓ $1"
}

record_fail() {
  FAIL=$((FAIL + 1))
  RESULTS+=("FAIL | $1 | $2")
  log "✗ $1 — $2"
}

json_get() {
  echo "$1" | python3 -c "import sys,json; d=json.load(sys.stdin); print($2)" 2>/dev/null
}

api() {
  local method="$1" path="$2" data="${3:-}"
  if [[ -n "$data" ]]; then
    curl -s -w "\n%{http_code}" -X "$method" "${BASE_URL}${path}" \
      -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$data"
  else
    curl -s -w "\n%{http_code}" -X "$method" "${BASE_URL}${path}" \
      -H "Authorization: Bearer $TOKEN"
  fi
}

split_response() {
  HTTP_CODE=$(echo "$1" | tail -1)
  BODY=$(echo "$1" | sed '$d')
}

log "=== 0. 健康检查 ==="
LOGIN_RAW=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/auth/dev-login" \
  -H "Content-Type: application/json" \
  -d '{"openid":"dev-owner-001","nickname":"老板","role":"OWNER_ADMIN"}')
split_response "$LOGIN_RAW"
[[ "$HTTP_CODE" == "200" ]] && record_pass "dev-login" || record_fail "dev-login" "code=$HTTP_CODE"
TOKEN=$(json_get "$BODY" "d['data']['token']")
[[ -n "$TOKEN" ]] || { log "无 token，退出"; exit 1; }

TODAY=$(date +%Y-%m-%d)
FROM7=$(python3 -c "from datetime import date,timedelta; print((date.today()-timedelta(days=6)).isoformat())")
UNIQ="t$(date +%s)"

log "=== 1. 客户编号 ==="
split_response "$(api GET /api/boss/customers)"
[[ "$HTTP_CODE" == "200" ]] && record_pass "客户列表" || record_fail "客户列表" "code=$HTTP_CODE"
CNO=$(json_get "$BODY" "d['data'][0]['customerNo'] if d.get('data') else ''")
[[ ${#CNO} -eq 11 ]] && record_pass "历史客户编号格式" || record_fail "历史客户编号格式" "got=$CNO"

split_response "$(api POST "/api/boss/customers" "$(printf '{"name":"客户%s"}' "$UNIQ")")"
[[ "$HTTP_CODE" == "200" ]] && record_pass "新建客户" || record_fail "新建客户" "code=$HTTP_CODE"
NEW_CNO=$(json_get "$BODY" "d['data']['customerNo']")
NEW_CID=$(json_get "$BODY" "d['data']['id']")
[[ ${#NEW_CNO} -eq 11 ]] && record_pass "新客户编号" || record_fail "新客户编号" "got=$NEW_CNO"

split_response "$(api GET "/api/boss/customers?keyword=${NEW_CNO}")"
FOUND=$(json_get "$BODY" "any(c.get('customerNo')=='${NEW_CNO}' for c in d.get('data',[]))")
[[ "$HTTP_CODE" == "200" && "$FOUND" == "True" ]] && record_pass "按编号搜客户" || record_fail "按编号搜客户" "code=$HTTP_CODE found=$FOUND"

log "=== 2. 供应商管理 ==="
split_response "$(api POST "/api/boss/suppliers" "$(printf '{"name":"供应商%s","phone":"13800001111"}' "$UNIQ")")"
[[ "$HTTP_CODE" == "200" ]] && record_pass "新建供应商" || record_fail "新建供应商" "code=$HTTP_CODE"
NEW_SNO=$(json_get "$BODY" "d['data']['supplierNo']")
NEW_SID=$(json_get "$BODY" "d['data']['id']")
[[ ${#NEW_SNO} -eq 11 ]] && record_pass "新供应商编号" || record_fail "新供应商编号" "got=$NEW_SNO"

split_response "$(api GET /api/boss/suppliers/${NEW_SID})"
[[ "$HTTP_CODE" == "200" ]] && record_pass "供应商详情" || record_fail "供应商详情" "code=$HTTP_CODE"

split_response "$(api PUT "/api/boss/suppliers/${NEW_SID}" "$(printf '{"name":"供应商%s改"}' "$UNIQ")")"
[[ "$HTTP_CODE" == "200" ]] && record_pass "更新供应商" || record_fail "更新供应商" "code=$HTTP_CODE"

split_response "$(api GET "/api/boss/suppliers?keyword=${NEW_SNO}")"
FOUND=$(json_get "$BODY" "any(s.get('supplierNo')=='${NEW_SNO}' for s in d.get('data',[]))")
[[ "$HTTP_CODE" == "200" && "$FOUND" == "True" ]] && record_pass "按编号搜供应商" || record_fail "按编号搜供应商" "code=$HTTP_CODE found=$FOUND"

log "=== 3. 当日第二个供应商（编号序号）==="
split_response "$(api POST "/api/boss/suppliers" "$(printf '{"name":"供应商%sb"}' "$UNIQ")")"
SNO2=$(json_get "$BODY" "d['data']['supplierNo']")
SID2=$(json_get "$BODY" "d['data']['id']")
SEQ1=${NEW_SNO: -3}; SEQ2=${SNO2: -3}
[[ "$SEQ2" -gt "$SEQ1" ]] && record_pass "供应商编号递增" || record_fail "供应商编号递增" "$NEW_SNO -> $SNO2"

log "=== 4. 采购记账 & 删除保护 ==="
split_response "$(api POST "/api/boss/purchase-payments" "$(printf '{"supplierId":%s,"amount":88,"paidAt":"%sT20:30:00"}' "$NEW_SID" "$TODAY")")"
[[ "$HTTP_CODE" == "200" ]] && record_pass "采购记账" || record_fail "采购记账" "code=$HTTP_CODE"

split_response "$(api DELETE /api/boss/suppliers/${NEW_SID})"
DEL_CODE=$(json_get "$BODY" "d.get('code', -1)")
[[ "$DEL_CODE" != "0" ]] && record_pass "有付款不可删供应商" || record_fail "有付款不可删供应商" "api code=$DEL_CODE"

split_response "$(api DELETE /api/boss/suppliers/${SID2})"
[[ "$HTTP_CODE" == "200" ]] && record_pass "无付款可删供应商" || record_fail "无付款可删供应商" "code=$HTTP_CODE"

log "=== 5. 报表 ==="
split_response "$(api GET "/api/boss/stats/supplier-report?dateFrom=${FROM7}&dateTo=${TODAY}")"
[[ "$HTTP_CODE" == "200" ]] && record_pass "供应商报表接口" || record_fail "供应商报表接口" "code=$HTTP_CODE body=${BODY:0:80}"
PAID=$(json_get "$BODY" "d['data']['summary']['paidAmount']")
[[ -n "$PAID" ]] && record_pass "供应商报表汇总" || record_fail "供应商报表汇总" "empty"

split_response "$(api GET "/api/boss/stats/inventory-report?dateFrom=${FROM7}&dateTo=${TODAY}&dateType=DELIVERY")"
[[ "$HTTP_CODE" == "200" ]] && record_pass "库存报表接口" || record_fail "库存报表接口" "code=$HTTP_CODE body=${BODY:0:80}"
STOCK=$(json_get "$BODY" "d['data']['summary'].get('productCount', 0)")
[[ -n "$STOCK" ]] && record_pass "库存报表汇总" || record_fail "库存报表汇总" "empty"

split_response "$(api GET "/api/boss/stats/customer-report?dateFrom=${FROM7}&dateTo=${TODAY}")"
[[ "$HTTP_CODE" == "200" ]] && record_pass "客户报表" || record_fail "客户报表" "code=$HTTP_CODE"

split_response "$(api GET /api/boss/dashboard)"
[[ "$HTTP_CODE" == "200" ]] && record_pass "经营仪表盘" || record_fail "经营仪表盘" "code=$HTTP_CODE"

log "=== 6. 采购任务 ==="
split_response "$(api GET "/api/boss/procurement/tasks?receiveDate=${TODAY}")"
[[ "$HTTP_CODE" == "200" ]] && record_pass "采购任务" || record_fail "采购任务" "code=$HTTP_CODE"

log "=== 7. 清理 ==="
split_response "$(api DELETE /api/boss/customers/${NEW_CID})"
[[ "$HTTP_CODE" == "200" ]] && record_pass "删除测试客户" || record_fail "删除测试客户" "code=$HTTP_CODE"

log ""
log "========== 结果: PASS=$PASS FAIL=$FAIL =========="
printf '%s\n' "${RESULTS[@]}"
[[ "$FAIL" -eq 0 ]]
