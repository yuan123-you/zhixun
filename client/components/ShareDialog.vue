<template>
  <!-- 分享弹框 -->
  <Teleport to="body">
    <Transition name="share-dialog">
      <div
        v-if="visible"
        class="fixed inset-0 z-[200] flex items-center justify-center"
        @click.self="close"
      >
        <!-- 背景遮罩 -->
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" />

        <!-- 弹框主体 -->
        <div class="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-[340px] max-w-[92vw] overflow-hidden">
          <!-- 标题栏 -->
          <div class="px-5 pt-5 pb-3 flex items-center justify-between">
            <h3 class="text-base font-semibold text-slate-900 dark:text-gray-100">
              {{ title }}
            </h3>
            <button
              class="w-8 h-8 flex items-center justify-center rounded-full text-slate-400 hover:text-slate-600 dark:text-gray-500 dark:hover:text-gray-300 hover:bg-slate-100 dark:hover:bg-gray-700 transition-colors"
              @click="close"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- 分享渠道 -->
          <div class="px-5 pb-5" @click.stop>
            <!-- 主分享渠道（QQ / 微信 / 微博 / 复制链接） -->
            <div class="grid grid-cols-4 gap-4 mb-4">
              <!-- QQ -->
              <button
                class="share-channel group"
                @click="onClickPlatform('qq')"
              >
                <div class="share-channel-icon bg-[#12B7F5]/10 text-[#12B7F5] group-hover:bg-[#12B7F5] group-hover:text-white">
                  <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 13.2c-.18.53-.5.98-.93 1.33.27.12.58.2.9.2.16 0 .28-.01.39-.04-.32.3-.74.5-1.2.56-.16.02-.3.03-.45.03-.36 0-.7-.08-1.01-.22-.32.08-.65.13-1 .13s-.68-.05-1-.13c-.31.14-.65.22-1.01.22-.15 0-.3-.01-.45-.03-.46-.06-.88-.26-1.2-.56.11.03.23.04.39.04.32 0 .63-.08.9-.2-.43-.35-.75-.8-.93-1.33.38.32.86.52 1.39.52.24 0 .47-.04.68-.12-.3-.28-.5-.66-.5-1.09 0-.36.13-.69.35-.95-.53-.19-.91-.69-.91-1.28 0-.3.1-.58.28-.81-.1-.33-.16-.69-.16-1.07 0-1.82 1.17-3.29 2.61-3.29s2.61 1.47 2.61 3.29c0 .38-.06.74-.16 1.07.18.23.28.51.28.81 0 .59-.38 1.09-.91 1.28.22.26.35.59.35.95 0 .43-.2.81-.5 1.09.21.08.44.12.68.12.53 0 1.01-.2 1.39-.52z"/>
                  </svg>
                </div>
                <span class="share-channel-label">QQ</span>
              </button>

              <!-- 微信 -->
              <button
                class="share-channel group"
                @click="onClickPlatform('wechat')"
              >
                <div class="share-channel-icon bg-[#07C160]/10 text-[#07C160] group-hover:bg-[#07C160] group-hover:text-white">
                  <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05a6.42 6.42 0 01-.246-1.79c0-3.558 3.39-6.441 7.573-6.441.258 0 .509.025.764.042C16.626 4.834 13.004 2.188 8.691 2.188zm-2.6 4.408c.56 0 1.015.46 1.015 1.025 0 .566-.455 1.025-1.014 1.025-.56 0-1.015-.46-1.015-1.025 0-.566.456-1.025 1.015-1.025zm5.144 0c.56 0 1.015.46 1.015 1.025 0 .566-.456 1.025-1.015 1.025-.56 0-1.014-.46-1.014-1.025 0-.566.455-1.025 1.014-1.025zM16.1 9.273c-3.68 0-6.667 2.488-6.667 5.558 0 3.07 2.987 5.558 6.667 5.558.715 0 1.404-.108 2.055-.293a.697.697 0 01.58.08l1.377.807a.262.262 0 00.135.043c.13 0 .235-.108.235-.24 0-.059-.024-.116-.04-.173l-.282-1.07a.477.477 0 01.173-.539C21.913 18.478 22.9 16.77 22.9 14.83c0-3.07-2.988-5.558-6.668-5.558h-.132zm-2.3 3.283c.454 0 .822.373.822.833 0 .46-.368.833-.822.833a.828.828 0 01-.822-.833c0-.46.368-.833.822-.833zm4.6 0c.454 0 .822.373.822.833 0 .46-.368.833-.822.833a.828.828 0 01-.822-.833c0-.46.368-.833.822-.833z"/>
                  </svg>
                </div>
                <span class="share-channel-label">微信</span>
              </button>

              <!-- 微博 -->
              <button
                class="share-channel group"
                @click="onClickPlatform('weibo')"
              >
                <div class="share-channel-icon bg-[#E6162D]/10 text-[#E6162D] group-hover:bg-[#E6162D] group-hover:text-white">
                  <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M10.09 16.82c-2.83.3-5.27-1-5.46-2.9-.19-1.9 1.95-3.69 4.78-3.99 2.83-.3 5.27 1 5.46 2.9.19 1.9-1.95 3.69-4.78 3.99zm7.71-5.1c-.23-.7-.92-1.08-1.54-.85-.62.23-.94.94-.71 1.59.23.65.92 1.03 1.54.8.62-.22.94-.89.71-1.54zM17.6 3.4C15.8 1.6 13.3.8 10.9 1.1c-.8.1-1.4.8-1.3 1.6.1.8.8 1.4 1.6 1.3 1.6-.2 3.3.3 4.5 1.5 1.2 1.2 1.7 2.9 1.5 4.5-.1.8.5 1.5 1.3 1.6.8.1 1.5-.5 1.6-1.3.3-2.4-.5-4.9-2.5-6.9z"/>
                  </svg>
                </div>
                <span class="share-channel-label">微博</span>
              </button>

              <!-- 复制链接 -->
              <button
                class="share-channel group"
                @click="onClickPlatform('link')"
              >
                <div
                  class="share-channel-icon transition-all duration-200"
                  :class="linkCopied
                    ? 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/40 dark:text-emerald-400'
                    : 'bg-slate-100 text-slate-600 dark:bg-gray-700 dark:text-gray-300 group-hover:bg-primary group-hover:text-white'"
                >
                  <svg v-if="!linkCopied" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                  </svg>
                  <svg v-else class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <span
                  class="share-channel-label"
                  :class="{ 'text-emerald-600 dark:text-emerald-400': linkCopied }"
                >
                  {{ linkCopied ? '已复制' : '复制链接' }}
                </span>
              </button>
            </div>
          </div>
        </div>

        <!-- 确认弹框（QQ/微信/微博） -->
        <Transition name="confirm-dialog">
          <div
            v-if="confirmVisible"
            class="absolute inset-0 flex items-center justify-center z-10"
            @click.self="cancelConfirm"
          >
            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-[300px] max-w-[88vw] p-5" @click.stop>
              <!-- 应用 icon -->
              <div class="flex justify-center mb-3">
                <div
                  class="w-14 h-14 rounded-2xl flex items-center justify-center shadow-md"
                  :class="confirmPlatformStyle.bg"
                >
                  <div :class="confirmPlatformStyle.iconColor" v-html="confirmPlatformSvg"></div>
                </div>
              </div>

              <!-- 提示文本 -->
              <h4 class="text-center text-base font-semibold text-slate-900 dark:text-gray-100 mb-1">
                {{ confirmPlatformName }}
              </h4>
              <p class="text-center text-sm text-slate-500 dark:text-gray-400 mb-5">
                确定要跳转至{{ confirmPlatformName }}分享吗？
              </p>

              <!-- 操作按钮 -->
              <div class="flex gap-3">
                <button
                  class="flex-1 h-11 rounded-xl text-sm font-medium border border-slate-200 dark:border-gray-600 text-slate-600 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-700 transition-colors"
                  @click="cancelConfirm"
                >
                  取消
                </button>
                <button
                  class="flex-1 h-11 rounded-xl text-sm font-medium text-white transition-colors"
                  :class="confirmPlatformStyle.btnClass"
                  @click="confirmJump"
                >
                  确认
                </button>
              </div>
            </div>
          </div>
        </Transition>

        <!-- 微信二维码降级弹框（桌面端） -->
        <Transition name="confirm-dialog">
          <div
            v-if="qrcodeVisible"
            class="absolute inset-0 flex items-center justify-center z-10"
            @click.self="closeQRCode"
          >
            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-[280px] max-w-[88vw] p-5" @click.stop>
              <!-- 微信 icon -->
              <div class="flex justify-center mb-3">
                <div class="w-14 h-14 rounded-2xl bg-[#07C160]/10 flex items-center justify-center">
                  <svg class="w-8 h-8 text-[#07C160]" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05a6.42 6.42 0 01-.246-1.79c0-3.558 3.39-6.441 7.573-6.441.258 0 .509.025.764.042C16.626 4.834 13.004 2.188 8.691 2.188zm-2.6 4.408c.56 0 1.015.46 1.015 1.025 0 .566-.455 1.025-1.014 1.025-.56 0-1.015-.46-1.015-1.025 0-.566.456-1.025 1.015-1.025zm5.144 0c.56 0 1.015.46 1.015 1.025 0 .566-.456 1.025-1.015 1.025-.56 0-1.014-.46-1.014-1.025 0-.566.455-1.025 1.014-1.025zM16.1 9.273c-3.68 0-6.667 2.488-6.667 5.558 0 3.07 2.987 5.558 6.667 5.558.715 0 1.404-.108 2.055-.293a.697.697 0 01.58.08l1.377.807a.262.262 0 00.135.043c.13 0 .235-.108.235-.24 0-.059-.024-.116-.04-.173l-.282-1.07a.477.477 0 01.173-.539C21.913 18.478 22.9 16.77 22.9 14.83c0-3.07-2.988-5.558-6.668-5.558h-.132zm-2.3 3.283c.454 0 .822.373.822.833 0 .46-.368.833-.822.833a.828.828 0 01-.822-.833c0-.46.368-.833.822-.833zm4.6 0c.454 0 .822.373.822.833 0 .46-.368.833-.822.833a.828.828 0 01-.822-.833c0-.46.368-.833.822-.833z"/>
                  </svg>
                </div>
              </div>

              <h4 class="text-center text-base font-semibold text-slate-900 dark:text-gray-100 mb-1">
                微信分享
              </h4>
              <p class="text-center text-sm text-slate-500 dark:text-gray-400 mb-4">
                {{ isMobileDevice ? '已尝试唤起微信应用' : '请使用微信扫描二维码分享' }}
              </p>

              <!-- 二维码区域 -->
              <div v-if="!isMobileDevice" class="flex justify-center mb-4">
                <canvas ref="qrCanvasRef" class="rounded-lg" />
              </div>

              <!-- 备用：复制链接 -->
              <button
                class="w-full h-11 rounded-xl text-sm font-medium bg-[#07C160] text-white hover:bg-[#06AD56] transition-colors flex items-center justify-center gap-2"
                @click="copyLinkWithShare"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                </svg>
                复制链接
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>

    <!-- 复制成功 Toast -->
    <Transition name="toast-fade">
      <div
        v-if="toastVisible"
        class="fixed top-6 left-1/2 -translate-x-1/2 z-[9999]"
      >
        <div class="flex items-center gap-2 px-4 py-2.5 bg-gray-900/90 dark:bg-gray-700/90 backdrop-blur-md text-white rounded-xl shadow-lg">
          <svg class="w-4 h-4 text-emerald-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          <span class="text-sm">链接已复制到剪贴板</span>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
/** 分享弹框组件：支持 QQ/微信/微博 跳转确认 + 复制链接 */
import {
  type SharePlatform,
  type ShareParams,
  copyToClipboard,
  getPlatformName,
  isMobile,
  tryOpenWechatApp,
  buildQQShareUrl,
  buildWeiboShareUrl,
} from '~/composables/useShare'

const props = withDefaults(defineProps<{
  visible: boolean
  /** 分享标题 */
  title?: string
  /** 分享参数 */
  params: ShareParams
}>(), {
  title: '分享到',
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'shared', platform: SharePlatform): void
}>()

// 状态
const confirmVisible = ref(false)
const qrcodeVisible = ref(false)
const pendingPlatform = ref<SharePlatform>('qq')
const linkCopied = ref(false)
const linkCopiedTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const toastVisible = ref(false)
const toastTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const qrCanvasRef = ref<HTMLCanvasElement | null>(null)
const isMobileDevice = ref(false)

// 平台确认弹框样式映射
const confirmPlatformStyle = computed(() => {
  switch (pendingPlatform.value) {
    case 'qq':
      return {
        bg: 'bg-[#12B7F5]',
        iconColor: 'text-white',
        btnClass: 'bg-[#12B7F5] hover:bg-[#0EA5E0]',
      }
    case 'wechat':
      return {
        bg: 'bg-[#07C160]',
        iconColor: 'text-white',
        btnClass: 'bg-[#07C160] hover:bg-[#06AD56]',
      }
    case 'weibo':
      return {
        bg: 'bg-[#E6162D]',
        iconColor: 'text-white',
        btnClass: 'bg-[#E6162D] hover:bg-[#CF1528]',
      }
    default:
      return {
        bg: 'bg-primary',
        iconColor: 'text-white',
        btnClass: 'bg-primary hover:bg-primary-600',
      }
  }
})

// 确认弹框的 SVG 图标
const confirmPlatformSvg = computed(() => {
  switch (pendingPlatform.value) {
    case 'qq':
      return `<svg class="w-8 h-8" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 13.2c-.18.53-.5.98-.93 1.33.27.12.58.2.9.2.16 0 .28-.01.39-.04-.32.3-.74.5-1.2.56-.16.02-.3.03-.45.03-.36 0-.7-.08-1.01-.22-.32.08-.65.13-1 .13s-.68-.05-1-.13c-.31.14-.65.22-1.01.22-.15 0-.3-.01-.45-.03-.46-.06-.88-.26-1.2-.56.11.03.23.04.39.04.32 0 .63-.08.9-.2-.43-.35-.75-.8-.93-1.33.38.32.86.52 1.39.52.24 0 .47-.04.68-.12-.3-.28-.5-.66-.5-1.09 0-.36.13-.69.35-.95-.53-.19-.91-.69-.91-1.28 0-.3.1-.58.28-.81-.1-.33-.16-.69-.16-1.07 0-1.82 1.17-3.29 2.61-3.29s2.61 1.47 2.61 3.29c0 .38-.06.74-.16 1.07.18.23.28.51.28.81 0 .59-.38 1.09-.91 1.28.22.26.35.59.35.95 0 .43-.2.81-.5 1.09.21.08.44.12.68.12.53 0 1.01-.2 1.39-.52z"/></svg>`
    case 'wechat':
      return `<svg class="w-8 h-8" fill="currentColor" viewBox="0 0 24 24"><path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05a6.42 6.42 0 01-.246-1.79c0-3.558 3.39-6.441 7.573-6.441.258 0 .509.025.764.042C16.626 4.834 13.004 2.188 8.691 2.188zm-2.6 4.408c.56 0 1.015.46 1.015 1.025 0 .566-.455 1.025-1.014 1.025-.56 0-1.015-.46-1.015-1.025 0-.566.456-1.025 1.015-1.025zm5.144 0c.56 0 1.015.46 1.015 1.025 0 .566-.456 1.025-1.015 1.025-.56 0-1.014-.46-1.014-1.025 0-.566.455-1.025 1.014-1.025zM16.1 9.273c-3.68 0-6.667 2.488-6.667 5.558 0 3.07 2.987 5.558 6.667 5.558.715 0 1.404-.108 2.055-.293a.697.697 0 01.58.08l1.377.807a.262.262 0 00.135.043c.13 0 .235-.108.235-.24 0-.059-.024-.116-.04-.173l-.282-1.07a.477.477 0 01.173-.539C21.913 18.478 22.9 16.77 22.9 14.83c0-3.07-2.988-5.558-6.668-5.558h-.132zm-2.3 3.283c.454 0 .822.373.822.833 0 .46-.368.833-.822.833a.828.828 0 01-.822-.833c0-.46.368-.833.822-.833zm4.6 0c.454 0 .822.373.822.833 0 .46-.368.833-.822.833a.828.828 0 01-.822-.833c0-.46.368-.833.822-.833z"/></svg>`
    case 'weibo':
      return `<svg class="w-8 h-8" fill="currentColor" viewBox="0 0 24 24"><path d="M10.09 16.82c-2.83.3-5.27-1-5.46-2.9-.19-1.9 1.95-3.69 4.78-3.99 2.83-.3 5.27 1 5.46 2.9.19 1.9-1.95 3.69-4.78 3.99zm7.71-5.1c-.23-.7-.92-1.08-1.54-.85-.62.23-.94.94-.71 1.59.23.65.92 1.03 1.54.8.62-.22.94-.89.71-1.54zM17.6 3.4C15.8 1.6 13.3.8 10.9 1.1c-.8.1-1.4.8-1.3 1.6.1.8.8 1.4 1.6 1.3 1.6-.2 3.3.3 4.5 1.5 1.2 1.2 1.7 2.9 1.5 4.5-.1.8.5 1.5 1.3 1.6.8.1 1.5-.5 1.6-1.3.3-2.4-.5-4.9-2.5-6.9z"/></svg>`
    default:
      return ''
  }
})

const confirmPlatformName = computed(() => {
  return getPlatformName(pendingPlatform.value)
})

// 点击分享渠道
const onClickPlatform = (platform: SharePlatform) => {
  if (platform === 'link') {
    // 链接分享：直接复制
    handleCopyLink()
  } else {
    // QQ/微信/微博：先弹出确认弹框
    pendingPlatform.value = platform
    confirmVisible.value = true
  }
}

// 取消确认
const cancelConfirm = () => {
  confirmVisible.value = false
}

// 确认跳转
const confirmJump = async () => {
  confirmVisible.value = false
  const platform = pendingPlatform.value

  if (platform === 'wechat') {
    // 微信特殊处理
    await handleWechatShare()
  } else {
    // QQ / 微博直接跳转
    handleQQOrWeiboShare(platform)
  }
}

// 处理 QQ / 微博跳转
const handleQQOrWeiboShare = (platform: 'qq' | 'weibo') => {
  try {
    const shareUrl = platform === 'qq'
      ? buildQQShareUrl(props.params)
      : buildWeiboShareUrl(props.params)

    const win = window.open(shareUrl, '_blank', 'noopener,noreferrer')
    if (!win) {
      window.location.href = shareUrl
    }
    emit('shared', platform)
    close()
  } catch {
    // 跳转失败
  }
}

// 处理微信分享
const handleWechatShare = async () => {
  isMobileDevice.value = isMobile()

  if (isMobileDevice.value) {
    // 移动端尝试唤起微信
    try {
      const result = tryOpenWechatApp(props.params.url)
      if (result.success) {
        // 尝试检测是否成功唤起
        const appFail = await detectAppOpenFailure()
        if (appFail) {
          // 唤起失败，显示二维码降级弹框
          qrcodeVisible.value = true
          showToast('未检测到微信应用，请尝试其它分享方式')
        } else {
          emit('shared', 'wechat')
          close()
        }
      } else {
        // 方案不可用，显示二维码
        qrcodeVisible.value = true
      }
    } catch {
      qrcodeVisible.value = true
    }
  } else {
    // 桌面端直接显示二维码
    qrcodeVisible.value = true
    nextTick(() => generateQRCode())
  }
}

// 检测应用唤起失败（页面未隐藏 = 失败）
const detectAppOpenFailure = (): Promise<boolean> => {
  return new Promise((resolve) => {
    let resolved = false
    const timer = setTimeout(() => {
      if (!resolved) {
        resolved = true
        document.removeEventListener('visibilitychange', handler)
        resolve(true) // true = 失败了
      }
    }, 1500)
    const handler = () => {
      if (!resolved && document.hidden) {
        resolved = true
        clearTimeout(timer)
        document.removeEventListener('visibilitychange', handler)
        resolve(false) // false = 成功了
      }
    }
    document.addEventListener('visibilitychange', handler)
  })
}

// 生成微信二维码
const generateQRCode = async () => {
  if (!qrCanvasRef.value) return
  try {
    const QRCode = await import('qrcode')
    await QRCode.toCanvas(qrCanvasRef.value, props.params.url, {
      width: 160,
      margin: 2,
      color: {
        dark: '#1f2937',
        light: '#ffffff',
      },
    })
  } catch {
    // 二维码生成失败，静默处理
  }
}

// 关闭二维码弹框
const closeQRCode = () => {
  qrcodeVisible.value = false
  close()
}

// 关闭弹框
const close = () => {
  confirmVisible.value = false
  qrcodeVisible.value = false
  emit('close')
}

// 复制链接
const handleCopyLink = async () => {
  const result = await copyToClipboard(props.params.url)
  if (result.success) {
    // 显示复制成功状态
    linkCopied.value = true
    showToast('链接已复制到剪贴板')
    emit('shared', 'link')
    // 延迟恢复图标状态
    if (linkCopiedTimer.value) clearTimeout(linkCopiedTimer.value)
    linkCopiedTimer.value = setTimeout(() => {
      linkCopied.value = false
    }, 2000)
  } else {
    showToast(result.message || '复制失败，请稍后重试')
  }
}

// 从二维码弹框中复制链接并分享
const copyLinkWithShare = async () => {
  await handleCopyLink()
  qrcodeVisible.value = false
  close()
}

// 显示 Toast
const showToast = (message: string) => {
  toastVisible.value = true
  if (toastTimer.value) clearTimeout(toastTimer.value)
  toastTimer.value = setTimeout(() => {
    toastVisible.value = false
  }, 2000)
}

// 键盘 ESC 关闭
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (confirmVisible.value) {
      cancelConfirm()
    } else if (qrcodeVisible.value) {
      closeQRCode()
    } else {
      close()
    }
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    document.addEventListener('keydown', handleKeydown)
    // 重置状态
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
/* 分享弹框过渡 */
.share-dialog-enter-active,
.share-dialog-leave-active {
  transition: opacity 0.25s ease;
}
.share-dialog-enter-active > :first-child,
.share-dialog-leave-active > :first-child {
  transition: opacity 0.25s ease;
}
.share-dialog-enter-active > :nth-child(2),
.share-dialog-leave-active > :nth-child(2) {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.25s ease;
}
.share-dialog-enter-from,
.share-dialog-leave-to {
  opacity: 0;
}
.share-dialog-enter-from > :nth-child(2),
.share-dialog-leave-to > :nth-child(2) {
  transform: translateY(20px) scale(0.95);
  opacity: 0;
}

/* 确认弹框过渡 */
.confirm-dialog-enter-active,
.confirm-dialog-leave-active {
  transition: opacity 0.2s ease;
}
.confirm-dialog-enter-active > *,
.confirm-dialog-leave-active > * {
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.2s ease;
}
.confirm-dialog-enter-from,
.confirm-dialog-leave-to {
  opacity: 0;
}
.confirm-dialog-enter-from > *,
.confirm-dialog-leave-to > * {
  transform: scale(0.9);
  opacity: 0;
}

/* Toast 过渡 */
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -10px);
}

/* 分享渠道按钮 */
.share-channel {
  @apply flex flex-col items-center gap-2 p-2 rounded-xl transition-all duration-200 cursor-pointer;
  -webkit-tap-highlight-color: transparent;
}
.share-channel:active {
  @apply scale-95;
}

/* 分享渠道图标 */
.share-channel-icon {
  @apply w-12 h-12 rounded-xl flex items-center justify-center transition-all duration-200;
}

/* 分享渠道文字 */
.share-channel-label {
  @apply text-xs text-slate-500 dark:text-gray-400 font-medium transition-colors duration-200;
}
</style>
