<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

function captureLaunchEntry(options?: { query?: Record<string, string> }) {
  const query = options?.query
  if (!query) return
  if (query.m) {
    const id = Number(query.m)
    if (!Number.isNaN(id) && id > 0) {
      uni.setStorageSync('entry_merchant_id', id)
    }
  }
  if (query.act) {
    uni.setStorageSync('entry_activation_token', String(query.act).trim())
  }
  if (query.code) {
    uni.setStorageSync('entry_invite_code', String(query.code).trim().toUpperCase())
  }
}

onLaunch((options) => {
  captureLaunchEntry(options as { query?: Record<string, string> })
})
</script>

<style lang="scss">
@import 'uview-plus/index.scss';
@import './styles/readable-ui.scss';

page {
  height: 100%;
  background-color: #f5f7f3;
  font-size: 30rpx;
  color: #17211b;
}
</style>
