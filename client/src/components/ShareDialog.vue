<template>
  <!-- 分享弹框（Element Plus 版） -->
  <Teleport to="body">
    <Transition name="share-dialog">
      <div v-if="visible" class="share-dialog-overlay" @click.self="close">
        <div class="share-dialog-bg" />
        <div class="share-dialog-body">
          <!-- 标题栏 -->
          <div class="share-dialog-header">
            <h3>{{ title }}</h3>
            <el-button :icon="Close" text circle @click="close" />
          </div>

          <!-- 分享渠道 -->
          <div class="share-channels">
            <button class="share-channel-btn" @click="onClickPlatform('qq')">
              <div class="channel-icon qq-bg">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12.003 2c-2.265 0-4.103 1.783-4.103 3.997 0 .912.318 1.747.846 2.412-1.597.965-2.978 2.147-4.058 3.472-.813.998-1.42 2.085-1.706 3.127-.155.564-.07 1.088.274 1.488.342.398.88.607 1.496.607.21 0 .424-.03.64-.092.622-.178 1.2-.386 1.76-.608.47-.187.933-.37 1.41-.53l.068-.023c-.025.533-.045 1.08-.06 1.638-.032 1.22.03 2.484.3 3.533.3 1.166.85 1.99 1.67 2.413.83.425 1.96.66 3.334.66h.04c1.37 0 2.5-.235 3.334-.66.82-.424 1.374-1.247 1.672-2.413.27-1.05.333-2.313.3-3.533-.015-.558-.035-1.105-.06-1.638l.068.023c.477.16.94.343 1.41.53.56.222 1.138.43 1.76.608.216.062.43.092.64.092.617 0 1.154-.21 1.496-.607.345-.4.43-.924.274-1.488-.286-1.042-.893-2.13-1.706-3.127-1.08-1.325-2.46-2.507-4.058-3.472.528-.665.846-1.5.846-2.412C16.106 3.783 14.268 2 12.003 2z"/>
                </svg>
              </div>
              <span>QQ</span>
            </button>
            <button class="share-channel-btn" @click="onClickPlatform('wechat')">
              <div class="channel-icon wechat-bg">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178A1.17 1.17 0 014.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178 1.17 1.17 0 01-1.162-1.178c0-.651.52-1.18 1.162-1.18zm3.024 4.22c-2.538 0-4.595 2.094-4.595 4.677 0 2.582 2.057 4.677 4.595 4.677.462 0 .92-.07 1.358-.203a.494.494 0 01.408.055l1.09.638a.186.186 0 00.095.03c.091 0 .166-.074.166-.168 0-.04-.015-.081-.027-.122l-.223-.846a.338.338 0 01.122-.38c1.048-.77 1.666-1.92 1.666-3.681 0-2.583-2.057-4.677-4.595-4.677zm-1.5 2.692c.367 0 .665.302.665.674a.67.67 0 01-.665.674.67.67 0 01-.665-.674c0-.372.298-.674.665-.674zm3.006 0c.367 0 .665.302.665.674a.67.67 0 01-.665.674.67.67 0 01-.665-.674c0-.372.298-.674.665-.674z"/>
                </svg>
              </div>
              <span>微信</span>
            </button>
            <button class="share-channel-btn" @click="onClickPlatform('weibo')">
              <div class="channel-icon weibo-bg">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M10.098 20.323c-3.977.391-7.414-1.406-7.672-4.02-.259-2.609 2.759-5.047 6.74-5.441 3.979-.394 7.413 1.404 7.671 4.018.259 2.6-2.759 5.049-6.739 5.443zm-3.522-4.955c-.543.574-.694 1.682-.367 2.3.334.635 1.116.927 2.094.751.484-.087.957-.285 1.275-.514.12-.085.184-.192.16-.278-.068-.247-.46-.363-1.002-.332-.387.022-.81-.088-1.044-.314-.285-.276-.098-.662.465-.893.26-.108.542-.153.817-.153h.003c.436 0 .72.135.781.283.036.087.009.156-.044.212-.225.254-.535.449-.535.449s.386-.06.876-.06h.071c.616 0 1.234.155 1.67.414.43.254.65.57.596.808-.035.163-.178.297-.435.374.219.066.392.19.48.37.156.318.03.798-.35 1.202-.617.652-1.72 1.148-3.035 1.34-1.306.189-2.5.018-3.068-.465-.57-.484-.44-1.222.329-1.898zm9.453-5.415c-.355.053-.665-.248-.693-.673-.026-.427.23-.807.585-.86.353-.054.664.248.69.673.027.427-.229.806-.582.86zm.368-1.563c-.534.14-1.1-.303-1.261-.99-.162-.688.14-1.355.674-1.494.534-.14 1.1.302 1.263.988.16.688-.143 1.355-.676 1.496zm3.233-.18c-.142.102-.33.04-.432-.177-.505-1.05-1.416-2.025-2.811-2.52-1.394-.495-2.748-.447-3.636.066-.219.125-.478.025-.58-.224-.1-.248.002-.541.222-.666.974-.562 2.512-.66 4.114-.093 2.201.78 3.817 2.601 3.995 3.704.044.271-.182.507-.43.489-.168-.012-.284-.127-.31-.28-.102-.642-.604-1.367-1.286-1.97-.681-.604-1.5-1.04-2.196-1.242-.17-.048-.87-.22-1.1-.303-.174-.062-.328-.174-.468-.317-.003-.002-.005-.006-.006-.008.002-.002.003-.004.005-.006.034-.007.066-.014.09-.02.262-.09.5-.164.796-.214.002-.001.003-.001.005-.001.005 0 .01.001.014.004.061.01.11.056.123.102.002.006.004.011.004.017.003.018.003.036.002.052-.002.094-.054.165-.13.196-1.01.425-1.696 1.043-2.147 1.788-.226.377-.367.79-.448 1.165-.029.133-.006.238.07.291z"/>
                </svg>
              </div>
              <span>微博</span>
            </button>
            <button class="share-channel-btn" @click="onClickPlatform('link')">
              <div class="channel-icon" :class="linkCopied ? 'link-copied' : 'link-default'">
                <svg v-if="!linkCopied" width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                </svg>
                <svg v-else width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <span :class="{ 'copied-text': linkCopied }">{{ linkCopied ? '已复制' : '复制链接' }}</span>
            </button>
          </div>
        </div>

        <!-- 确认弹框（QQ/微信/微博） -->
        <Transition name="confirm-dialog">
          <div v-if="confirmVisible" class="confirm-overlay" @click.self="cancelConfirm">
            <div class="confirm-body" @click.stop>
              <div class="confirm-icon" :class="confirmPlatformStyle.bgClass">
                <i :class="confirmPlatformStyle.iconClass">{{ confirmPlatformName }}</i>
              </div>
              <h4>{{ confirmPlatformName }}</h4>
              <p>确定要跳转至{{ confirmPlatformName }}分享吗？</p>
              <div class="confirm-actions">
                <el-button @click="cancelConfirm">取消</el-button>
                <el-button type="primary" @click="confirmJump">确认</el-button>
              </div>
            </div>
          </div>
        </Transition>

        <!-- 微信二维码降级弹框 -->
        <Transition name="confirm-dialog">
          <div v-if="qrcodeVisible" class="confirm-overlay" @click.self="closeQRCode">
            <div class="confirm-body" @click.stop>
              <div class="confirm-icon wechat-icon">
                <span>微信</span>
              </div>
              <h4>微信分享</h4>
              <p>{{ isMobileDevice ? '已尝试唤起微信应用' : '请使用微信扫描二维码分享' }}</p>
              <div v-if="!isMobileDevice" class="qr-wrap">
                <canvas ref="qrCanvasRef" />
              </div>
              <el-button type="success" @click="copyLinkWithShare">
                <el-icon><Link /></el-icon>
                复制链接
              </el-button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>

    <!-- Toast -->
    <Transition name="toast-fade">
      <div v-if="toastVisible" class="share-toast">
        <el-alert :title="'链接已复制到剪贴板'" type="success" :closable="false" show-icon />
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
/** 分享弹框组件 —— Element Plus 重构版 */
import { Close, Link } from '@element-plus/icons-vue'
import {
  type SharePlatform,
  type ShareParams,
  copyToClipboard,
  getPlatformName,
  isMobile,
  tryOpenWechatApp,
  buildQQShareUrl,
  buildWeiboShareUrl,
} from '@/composables/useShare'

const props = withDefaults(defineProps<{
  visible: boolean
  title?: string
  params: ShareParams
}>(), {
  title: '分享到',
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'shared', platform: SharePlatform): void
}>()

const confirmVisible = ref(false)
const qrcodeVisible = ref(false)
const pendingPlatform = ref<SharePlatform>('qq')
const linkCopied = ref(false)
const linkCopiedTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const toastVisible = ref(false)
const toastTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const qrCanvasRef = ref<HTMLCanvasElement | null>(null)
const isMobileDevice = ref(false)

const confirmPlatformStyle = computed(() => {
  switch (pendingPlatform.value) {
    case 'qq': return { bgClass: 'qq-confirm-bg', iconClass: 'qq-confirm-icon' }
    case 'wechat': return { bgClass: 'wechat-confirm-bg', iconClass: 'wechat-confirm-icon' }
    case 'weibo': return { bgClass: 'weibo-confirm-bg', iconClass: 'weibo-confirm-icon' }
    default: return { bgClass: '', iconClass: '' }
  }
})

const confirmPlatformName = computed(() => getPlatformName(pendingPlatform.value))

const onClickPlatform = (platform: SharePlatform) => {
  if (platform === 'link') { handleCopyLink() }
  else { pendingPlatform.value = platform; confirmVisible.value = true }
}

const cancelConfirm = () => { confirmVisible.value = false }

const confirmJump = async () => {
  confirmVisible.value = false
  const platform = pendingPlatform.value
  if (platform === 'wechat') { await handleWechatShare() }
  else { handleQQOrWeiboShare(platform as 'qq' | 'weibo' | 'link') }
}

const handleQQOrWeiboShare = (platform: 'qq' | 'weibo' | 'link') => {
  try {
    const shareUrl = platform === 'qq' ? buildQQShareUrl(props.params) : buildWeiboShareUrl(props.params)
    const win = window.open(shareUrl, '_blank', 'noopener,noreferrer')
    if (!win) window.location.href = shareUrl
    emit('shared', platform)
    close()
  } catch { /* 跳转失败 */ }
}

const handleWechatShare = async () => {
  isMobileDevice.value = isMobile()
  if (isMobileDevice.value) {
    try {
      const result = tryOpenWechatApp(props.params.url)
      if (result.success) {
        const appFail = await detectAppOpenFailure()
        if (appFail) {
          qrcodeVisible.value = true
          showToast('未检测到微信应用，请尝试其它分享方式')
        } else {
          emit('shared', 'wechat')
          close()
        }
      } else {
        qrcodeVisible.value = true
      }
    } catch { qrcodeVisible.value = true }
  } else {
    qrcodeVisible.value = true
    nextTick(() => generateQRCode())
  }
}

const detectAppOpenFailure = (): Promise<boolean> => {
  return new Promise((resolve) => {
    let resolved = false
    const timer = setTimeout(() => {
      if (!resolved) { resolved = true; document.removeEventListener('visibilitychange', handler); resolve(true) }
    }, 1500)
    const handler = () => {
      if (!resolved && document.hidden) { resolved = true; clearTimeout(timer); document.removeEventListener('visibilitychange', handler); resolve(false) }
    }
    document.addEventListener('visibilitychange', handler)
  })
}

const generateQRCode = async () => {
  if (!qrCanvasRef.value) return
  try {
    const QRCode = await import('qrcode')
    await QRCode.toCanvas(qrCanvasRef.value, props.params.url, {
      width: 160, margin: 2,
      color: { dark: '#1f2937', light: '#ffffff' },
    })
  } catch { /* 生成失败 */ }
}

const closeQRCode = () => { qrcodeVisible.value = false; close() }

const close = () => {
  confirmVisible.value = false
  qrcodeVisible.value = false
  emit('close')
}

const handleCopyLink = async () => {
  const result = await copyToClipboard(props.params.url)
  if (result.success) {
    linkCopied.value = true
    showToast('链接已复制到剪贴板')
    emit('shared', 'link')
    if (linkCopiedTimer.value) clearTimeout(linkCopiedTimer.value)
    linkCopiedTimer.value = setTimeout(() => { linkCopied.value = false }, 2000)
  } else {
    showToast(result.message || '复制失败，请稍后重试')
  }
}

const copyLinkWithShare = async () => {
  await handleCopyLink()
  qrcodeVisible.value = false
  close()
}

const showToast = (message: string) => {
  toastVisible.value = true
  if (toastTimer.value) clearTimeout(toastTimer.value)
  toastTimer.value = setTimeout(() => { toastVisible.value = false }, 2000)
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (confirmVisible.value) cancelConfirm()
    else if (qrcodeVisible.value) closeQRCode()
    else close()
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    document.addEventListener('keydown', handleKeydown)
    confirmVisible.value = false
    qrcodeVisible.value = false
    linkCopied.value = false
  } else {
    document.removeEventListener('keydown', handleKeydown)
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (linkCopiedTimer.value) clearTimeout(linkCopiedTimer.value)
  if (toastTimer.value) clearTimeout(toastTimer.value)
})
</script>

<style scoped>
/* 分享弹框 */
.share-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}
.share-dialog-bg {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
}
.share-dialog-body {
  position: relative;
  background: var(--el-bg-color);
  border-radius: 16px;
  box-shadow: var(--el-box-shadow-dark);
  width: 340px;
  max-width: 92vw;
  overflow: hidden;
}

.share-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 20px 12px;
}
.share-dialog-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.share-channels {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  padding: 0 20px 20px;
}

.share-channel-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: none;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}
.share-channel-btn:active { transform: scale(0.95); }

.channel-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
}
.channel-svg {
  font-style: normal;
  font-size: 20px;
}
.qq-bg { background: rgba(18,183,245,0.1); color: #12B7F5; }
.share-channel-btn:hover .qq-bg { background: #12B7F5; color: #fff; }
.wechat-bg { background: rgba(7,193,96,0.1); color: #07C160; }
.share-channel-btn:hover .wechat-bg { background: #07C160; color: #fff; }
.weibo-bg { background: rgba(230,22,45,0.1); color: #E6162D; }
.share-channel-btn:hover .weibo-bg { background: #E6162D; color: #fff; }
.link-default { background: var(--el-fill-color); color: var(--el-text-color-secondary); }
.share-channel-btn:hover .link-default { background: var(--el-color-primary); color: #fff; }
.link-copied { background: var(--el-color-success-light-9); color: var(--el-color-success); }
.copied-text { color: var(--el-color-success); }

/* 确认弹框 */
.confirm-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}
.confirm-body {
  background: var(--el-bg-color);
  border-radius: 16px;
  box-shadow: var(--el-box-shadow-dark);
  width: 300px;
  max-width: 88vw;
  padding: 20px;
  text-align: center;
}
.confirm-body h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}
.confirm-body p {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 20px;
}
.confirm-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #fff;
}
.qq-confirm-bg { background: #12B7F5; }
.wechat-confirm-bg { background: #07C160; }
.weibo-confirm-bg { background: #E6162D; }
.wechat-icon { background: rgba(7,193,96,0.1); color: #07C160; }
.confirm-actions {
  display: flex;
  gap: 12px;
}
.confirm-actions .el-button { flex: 1; height: 44px; border-radius: 12px; }

.qr-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
.qr-wrap canvas { border-radius: 8px; }

/* 动画 */
.share-dialog-enter-active,
.share-dialog-leave-active { transition: opacity 0.25s ease; }
.share-dialog-enter-from,
.share-dialog-leave-to { opacity: 0; }
.share-dialog-enter-active > :nth-child(2),
.share-dialog-leave-active > :nth-child(2) {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.25s ease;
}
.share-dialog-enter-from > :nth-child(2),
.share-dialog-leave-to > :nth-child(2) {
  transform: translateY(20px) scale(0.95);
  opacity: 0;
}

.confirm-dialog-enter-active,
.confirm-dialog-leave-active { transition: opacity 0.2s ease; }
.confirm-dialog-enter-from,
.confirm-dialog-leave-to { opacity: 0; }
.confirm-dialog-enter-active > *,
.confirm-dialog-leave-active > * {
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.2s ease;
}
.confirm-dialog-enter-from > *,
.confirm-dialog-leave-to > * {
  transform: scale(0.9);
  opacity: 0;
}

/* Toast */
.share-toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
}
.toast-fade-enter-active,
.toast-fade-leave-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.toast-fade-enter-from,
.toast-fade-leave-to { opacity: 0; transform: translate(-50%, -10px); }
</style>
