<template>
  <!-- 省市区多级联动选择器 -->
  <div class="region-selector" ref="rootRef">
    <div class="flex flex-wrap items-center gap-1">
      <!-- 省份选择 -->
      <div class="relative" ref="provinceRef">
        <button
          class="flex items-center gap-0.5 px-2 py-1 text-xs border border-slate-300 rounded hover:border-primary transition-colors bg-white min-w-[70px]"
          :class="provinceLabel ? 'text-slate-700' : 'text-slate-400'"
          @click="toggleDropdown('province')"
        >
          <span class="truncate max-w-[90px]">{{ provinceLabel || '请选择省份' }}</span>
          <svg class="w-3 h-3 text-slate-400 shrink-0" :class="{ 'rotate-180': openDropdown === 'province' }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
        <!-- 省份下拉 -->
        <div
          v-if="openDropdown === 'province'"
          class="absolute left-0 top-full mt-1 bg-white border border-slate-200 rounded-lg shadow-lg z-20 max-h-44 overflow-y-auto min-w-[140px]"
        >
          <button
            v-for="p in provinces"
            :key="p.value"
            class="w-full text-left px-2 py-1 text-xs hover:bg-primary-50 transition-colors"
            :class="selectedProvince === p.value ? 'bg-primary-50 text-primary font-medium' : 'text-slate-700'"
            @click="selectProvince(p)"
          >
            {{ p.label }}
          </button>
        </div>
      </div>

      <!-- 城市选择 -->
      <div class="relative" v-if="currentCities.length > 0" ref="cityRef">
        <button
          class="flex items-center gap-0.5 px-2 py-1 text-xs border border-slate-300 rounded hover:border-primary transition-colors bg-white min-w-[70px]"
          :class="cityLabel ? 'text-slate-700' : 'text-slate-400'"
          @click="toggleDropdown('city')"
        >
          <span class="truncate max-w-[90px]">{{ cityLabel || '请选择城市' }}</span>
          <svg class="w-3 h-3 text-slate-400 shrink-0" :class="{ 'rotate-180': openDropdown === 'city' }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
        <!-- 城市下拉 -->
        <div
          v-if="openDropdown === 'city'"
          class="absolute left-0 top-full mt-1 bg-white border border-slate-200 rounded-lg shadow-lg z-20 max-h-44 overflow-y-auto min-w-[140px]"
        >
          <button
            v-for="c in currentCities"
            :key="c.value"
            class="w-full text-left px-2 py-1 text-xs hover:bg-primary-50 transition-colors"
            :class="selectedCity === c.value ? 'bg-primary-50 text-primary font-medium' : 'text-slate-700'"
            @click="selectCity(c)"
          >
            {{ c.label }}
          </button>
        </div>
      </div>

      <!-- 区/县选择 -->
      <div class="relative" v-if="currentDistricts.length > 0" ref="districtRef">
        <button
          class="flex items-center gap-0.5 px-2 py-1 text-xs border border-slate-300 rounded hover:border-primary transition-colors bg-white min-w-[70px]"
          :class="districtLabel ? 'text-slate-700' : 'text-slate-400'"
          @click="toggleDropdown('district')"
        >
          <span class="truncate max-w-[90px]">{{ districtLabel || '请选择区县' }}</span>
          <svg class="w-3 h-3 text-slate-400 shrink-0" :class="{ 'rotate-180': openDropdown === 'district' }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
        <!-- 区县下拉 -->
        <div
          v-if="openDropdown === 'district'"
          class="absolute left-0 top-full mt-1 bg-white border border-slate-200 rounded-lg shadow-lg z-20 max-h-44 overflow-y-auto min-w-[140px]"
        >
          <button
            v-for="d in currentDistricts"
            :key="d.value"
            class="w-full text-left px-2 py-1 text-xs hover:bg-primary-50 transition-colors"
            :class="selectedDistrict === d.value ? 'bg-primary-50 text-primary font-medium' : 'text-slate-700'"
            @click="selectDistrict(d)"
          >
            {{ d.label }}
          </button>
        </div>
      </div>

      <!-- 自动定位按钮 -->
      <button
        v-if="showAutoLocate"
        class="flex items-center gap-0.5 px-2 py-1 text-xs text-primary border border-primary rounded hover:bg-primary-50 transition-colors shrink-0"
        :disabled="locating"
        title="自动获取位置"
        @click="$emit('autoLocate')"
      >
        <svg class="w-3 h-3" :class="{ 'animate-spin': locating }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
        <span>{{ locating ? '获取中...' : '自动定位' }}</span>
      </button>
    </div>

    <!-- 选中的位置预览 -->
    <div v-if="displayText" class="mt-1.5 flex items-center gap-1.5">
      <span class="text-xs text-slate-600 bg-slate-50 px-2 py-0.5 rounded">
        <svg class="w-3 h-3 inline-block mr-0.5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
        {{ displayText }}
      </span>
      <button v-if="displayText" class="text-[10px] text-slate-400 hover:text-red-400 transition-colors" @click="clearAll">× 清除</button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 省市区三级联动选择器
 * 支持手动级联选择 + 自动定位
 */
import { provinces, findProvince, findCity, findDistrict } from '~/utils/regions'
import type { RegionNode } from '~/utils/regions'

const props = withDefaults(defineProps<{
  /** 自动定位按钮是否居中加载 */
  locating?: boolean
  /** 是否显示自动定位按钮 */
  showAutoLocate?: boolean
}>(), {
  locating: false,
  showAutoLocate: true,
})

const emit = defineEmits<{
  /** 选择变化时触发，返回格式化的地址字符串，如 "湖北省·武汉市·洪山区" */
  change: [location: string]
  /** 清空选择 */
  clear: []
  /** 请求自动定位 */
  autoLocate: []
  /** 更新定位后的省/市/区 */
  updateRegion: [data: { province: string; city: string; district: string }]
}>()

// 选择状态
const selectedProvince = ref('')
const selectedCity = ref('')
const selectedDistrict = ref('')
const openDropdown = ref<'province' | 'city' | 'district' | null>(null)

const rootRef = ref<HTMLElement | null>(null)

// 计算当前可选城市列表
const currentCities = computed(() => {
  if (!selectedProvince.value) return []
  const province = provinces.find(p => p.value === selectedProvince.value)
  return province?.children || []
})

// 计算当前可选区县列表
const currentDistricts = computed(() => {
  if (!selectedCity.value) return []
  const province = provinces.find(p => p.value === selectedProvince.value)
  const city = province?.children?.find(c => c.value === selectedCity.value)
  return city?.children || []
})

// 显示标签
const provinceLabel = computed(() => {
  return provinces.find(p => p.value === selectedProvince.value)?.label || ''
})

const cityLabel = computed(() => {
  const province = provinces.find(p => p.value === selectedProvince.value)
  return province?.children?.find(c => c.value === selectedCity.value)?.label || ''
})

const districtLabel = computed(() => {
  const province = provinces.find(p => p.value === selectedProvince.value)
  const city = province?.children?.find(c => c.value === selectedCity.value)
  return city?.children?.find(d => d.value === selectedDistrict.value)?.label || ''
})

// 显示的完整地址文本
const displayText = computed(() => {
  const parts: string[] = []
  if (provinceLabel.value) parts.push(provinceLabel.value)
  if (cityLabel.value && cityLabel.value !== provinceLabel.value) parts.push(cityLabel.value)
  if (districtLabel.value) parts.push(districtLabel.value)
  return parts.join('·')
})

// 切换下拉
function toggleDropdown(type: 'province' | 'city' | 'district') {
  if (openDropdown.value === type) {
    openDropdown.value = null
  } else {
    openDropdown.value = type
  }
}

// 选择省份
function selectProvince(province: RegionNode) {
  selectedProvince.value = province.value
  selectedCity.value = ''
  selectedDistrict.value = ''
  openDropdown.value = province.children && province.children.length > 0 ? 'city' : null
  emitChange()
}

// 选择城市
function selectCity(city: RegionNode) {
  selectedCity.value = city.value
  selectedDistrict.value = ''
  openDropdown.value = city.children && city.children.length > 0 ? 'district' : null
  emitChange()
}

// 选择区县
function selectDistrict(district: RegionNode) {
  selectedDistrict.value = district.value
  openDropdown.value = null
  emitChange()
}

// 触发变更事件
function emitChange() {
  emit('change', displayText.value)
}

// 清除所有选择
function clearAll() {
  selectedProvince.value = ''
  selectedCity.value = ''
  selectedDistrict.value = ''
  openDropdown.value = null
  emit('change', '')
  emit('clear')
}

/**
 * 通过省/市/区名称自动设置选中状态
 * @param data 包含 province, city, district 字段的对象
 */
function setRegion(data: { province: string; city: string; district?: string }) {
  // 匹配省份
  const provNode = findProvince(data.province)
  if (provNode) {
    selectedProvince.value = provNode.value
    // 匹配城市
    const cityNode = findCity(provNode, data.city)
    if (cityNode) {
      selectedCity.value = cityNode.value
      // 匹配区县
      if (data.district) {
        const distNode = findDistrict(cityNode, data.district)
        if (distNode) {
          selectedDistrict.value = distNode.value
        }
      }
    }
  }
  emitChange()
}

// 暴露方法给父组件
defineExpose({
  setRegion,
  clearAll,
  getDisplayText: () => displayText.value,
})

// 点击外部关闭下拉
function handleClickOutside(e: MouseEvent) {
  if (!rootRef.value) return
  if (!rootRef.value.contains(e.target as Node)) {
    openDropdown.value = null
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.region-selector {
  position: relative;
}
</style>
