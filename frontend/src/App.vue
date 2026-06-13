<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useUserStore } from './stores/user'

onLaunch(() => {
  useUserStore().hydrateFromStorage()
})
</script>

<style lang="scss">
@import 'uview-plus/index.scss';

page {
  background-color: #f5f6f7;
  font-size: 28rpx;
  color: #333;
}
</style>
