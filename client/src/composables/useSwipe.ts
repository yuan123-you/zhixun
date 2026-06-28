/**
 * 滑动手势检测组合式函数
 * 支持触摸事件和指针事件，可配置阈值和回调
 */
export interface SwipeOptions {
  /** 滑动触发阈值（px），默认 50 */
  threshold?: number
  /** 左滑回调 */
  onSwipeLeft?: () => void
  /** 右滑回调 */
  onSwipeRight?: () => void
  /** 上滑回调 */
  onSwipeUp?: () => void
  /** 下滑回调 */
  onSwipeDown?: () => void
  /** 是否阻止默认滚动行为（水平滑动时），默认 true */
  preventDefaultOnHorizontal?: boolean
}

export interface SwipeState {
  /** 滑动方向 */
  direction: Ref<'left' | 'right' | 'up' | 'down' | null>
  /** 当前滑动距离 */
  distance: Ref<number>
  /** 是否正在滑动 */
  isSwiping: Ref<boolean>
}

export const useSwipe = (
  target: Ref<HTMLElement | null>,
  options: SwipeOptions = {},
): SwipeState & {
  /** 绑定到元素的事件处理（如需手动绑定） */
} => {
  const {
    threshold = 50,
    onSwipeLeft,
    onSwipeRight,
    onSwipeUp,
    onSwipeDown,
    preventDefaultOnHorizontal = true,
  } = options

  const direction = ref<'left' | 'right' | 'up' | 'down' | null>(null)
  const distance = ref(0)
  const isSwiping = ref(false)

  let startX = 0
  let startY = 0
  let axis: 'horizontal' | 'vertical' | null = null

  const onTouchStart = (e: TouchEvent) => {
    const touch = e.touches[0]
    startX = touch.clientX
    startY = touch.clientY
    axis = null
    isSwiping.value = true
    direction.value = null
    distance.value = 0
  }

  const onTouchMove = (e: TouchEvent) => {
    if (!isSwiping.value) return

    const touch = e.touches[0]
    const diffX = touch.clientX - startX
    const diffY = touch.clientY - startY

    // 首次移动时判断轴向
    if (axis === null) {
      if (Math.abs(diffX) > 5 || Math.abs(diffY) > 5) {
        axis = Math.abs(diffX) > Math.abs(diffY) ? 'horizontal' : 'vertical'
      }
      return
    }

    // 非水平滑动不处理
    if (axis === 'horizontal' && preventDefaultOnHorizontal && e.cancelable) {
      e.preventDefault()
    }

    if (axis === 'horizontal') {
      distance.value = Math.abs(diffX)
      direction.value = diffX < 0 ? 'left' : 'right'
    } else if (axis === 'vertical') {
      distance.value = Math.abs(diffY)
      direction.value = diffY < 0 ? 'up' : 'down'
    }
  }

  const onTouchEnd = () => {
    if (!isSwiping.value) return

    if (distance.value >= threshold) {
      switch (direction.value) {
        case 'left':
          onSwipeLeft?.()
          break
        case 'right':
          onSwipeRight?.()
          break
        case 'up':
          onSwipeUp?.()
          break
        case 'down':
          onSwipeDown?.()
          break
      }
    }

    isSwiping.value = false
    direction.value = null
    distance.value = 0
    axis = null
  }

  // 指针事件支持（用于桌面端触摸屏）
  const onPointerDown = (e: PointerEvent) => {
    if (e.pointerType !== 'touch') return
    startX = e.clientX
    startY = e.clientY
    axis = null
    isSwiping.value = true
    direction.value = null
    distance.value = 0

    const el = target.value
    if (el) {
      el.setPointerCapture(e.pointerId)
    }
  }

  const onPointerMove = (e: PointerEvent) => {
    if (!isSwiping.value || e.pointerType !== 'touch') return

    const diffX = e.clientX - startX
    const diffY = e.clientY - startY

    if (axis === null) {
      if (Math.abs(diffX) > 5 || Math.abs(diffY) > 5) {
        axis = Math.abs(diffX) > Math.abs(diffY) ? 'horizontal' : 'vertical'
      }
      return
    }

    if (axis === 'horizontal' && preventDefaultOnHorizontal && e.cancelable) {
      e.preventDefault()
    }

    if (axis === 'horizontal') {
      distance.value = Math.abs(diffX)
      direction.value = diffX < 0 ? 'left' : 'right'
    } else if (axis === 'vertical') {
      distance.value = Math.abs(diffY)
      direction.value = diffY < 0 ? 'up' : 'down'
    }
  }

  const onPointerUp = (e: PointerEvent) => {
    if (e.pointerType !== 'touch') return

    const el = target.value
    if (el) {
      el.releasePointerCapture(e.pointerId)
    }

    onTouchEnd()
  }

  onMounted(() => {
    const el = target.value
    if (!el) return

    // 优先使用触摸事件
    el.addEventListener('touchstart', onTouchStart, { passive: true })
    el.addEventListener('touchmove', onTouchMove, { passive: false })
    el.addEventListener('touchend', onTouchEnd, { passive: true })

    // 同时支持指针事件（桌面触摸屏）
    el.addEventListener('pointerdown', onPointerDown)
    el.addEventListener('pointermove', onPointerMove)
    el.addEventListener('pointerup', onPointerUp)
  })

  onUnmounted(() => {
    const el = target.value
    if (!el) return

    el.removeEventListener('touchstart', onTouchStart)
    el.removeEventListener('touchmove', onTouchMove)
    el.removeEventListener('touchend', onTouchEnd)

    el.removeEventListener('pointerdown', onPointerDown)
    el.removeEventListener('pointermove', onPointerMove)
    el.removeEventListener('pointerup', onPointerUp)
  })

  return {
    direction,
    distance,
    isSwiping,
  }
}
