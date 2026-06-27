<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { applyEntryQuery } from '@common/utils/tenant'

onLaunch((options) => {
  applyEntryQuery(options?.query as Record<string, string | undefined>)
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
