<template>
  <!-- 省市区多级联动选择器 -->
  <div class="region-selector" ref="rootRef">
    <div class="region-row">
      <div class="region-col" ref="provinceRef">
        <el-button class="region-trigger" :class="{ placeholder: !provinceLabel }" size="small" @click="toggleDropdown('province')">
          <span>{{ provinceLabel || '请选择省份' }}</span>
          <el-icon :class="{ rotated: openDropdown === 'province' }"><ArrowDown /></el-icon>
        </el-button>
        <div v-if="openDropdown === 'province'" class="region-dropdown">
          <button v-for="p in provinces" :key="p.value" class="region-option"
            :class="{ active: selectedProvince === p.value }" @click="selectProvince(p)">
            {{ p.label }}
          </button>
        </div>
      </div>

      <div class="region-col" v-if="currentCities.length > 0" ref="cityRef">
        <el-button class="region-trigger" :class="{ placeholder: !cityLabel }" size="small" @click="toggleDropdown('city')">
          <span>{{ cityLabel || '请选择城市' }}</span>
          <el-icon :class="{ rotated: openDropdown === 'city' }"><ArrowDown /></el-icon>
        </el-button>
        <div v-if="openDropdown === 'city'" class="region-dropdown">
          <button v-for="c in currentCities" :key="c.value" class="region-option"
            :class="{ active: selectedCity === c.value }" @click="selectCity(c)">
            {{ c.label }}
          </button>
        </div>
      </div>

      <div class="region-col" v-if="currentDistricts.length > 0" ref="districtRef">
        <el-button class="region-trigger" :class="{ placeholder: !districtLabel }" size="small" @click="toggleDropdown('district')">
          <span>{{ districtLabel || '请选择区县' }}</span>
          <el-icon :class="{ rotated: openDropdown === 'district' }"><ArrowDown /></el-icon>
        </el-button>
        <div v-if="openDropdown === 'district'" class="region-dropdown">
          <button v-for="d in currentDistricts" :key="d.value" class="region-option"
            :class="{ active: selectedDistrict === d.value }" @click="selectDistrict(d)">
            {{ d.label }}
          </button>
        </div>
      </div>

      <el-button v-if="showAutoLocate" size="small" :loading="locating" @click="$emit('autoLocate')">
        <el-icon><Location /></el-icon>
        <span>{{ locating ? locatingText : '自动定位' }}</span>
      </el-button>
    </div>

    <div v-if="displayText" class="region-preview">
      <el-tag closable size="small" @close="clearAll">
        <el-icon><Location /></el-icon>
        {{ displayText }}
      </el-tag>
    </div>

    <div v-if="locateError" class="region-error" role="alert">
      <el-icon :size="12"><WarningFilled /></el-icon>
      <span>{{ locateError }}</span>
      <button v-if="errorRetryable" type="button" class="region-error-retry" @click="$emit('autoLocate')">重试</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, Location, WarningFilled } from '@element-plus/icons-vue'
import { provinces, findProvince, findCity, findDistrict } from '@/utils/regions'
import type { RegionNode } from '@/utils/regions'

const props = withDefaults(defineProps<{
  locating?: boolean
  showAutoLocate?: boolean
  /** 自定义定位中文案，默认"获取中..." */
  locatingText?: string
  /** 父组件传入的定位错误信息（用于在组件底部展示） */
  locateError?: string
  /** 定位失败时是否可重试 */
  errorRetryable?: boolean
}>(), {
  locating: false,
  showAutoLocate: true,
  locatingText: '获取中...',
  locateError: '',
  errorRetryable: true,
})

const emit = defineEmits<{
  change: [location: string]; clear: []; autoLocate: []; updateRegion: [data: { province: string; city: string; district: string }]
}>()

const selectedProvince = ref(''); const selectedCity = ref(''); const selectedDistrict = ref('')
const openDropdown = ref<'province' | 'city' | 'district' | null>(null)
const rootRef = ref<HTMLElement | null>(null)

const currentCities = computed(() => {
  if (!selectedProvince.value) return []
  return provinces.find(p => p.value === selectedProvince.value)?.children || []
})
const currentDistricts = computed(() => {
  if (!selectedCity.value) return []
  const province = provinces.find(p => p.value === selectedProvince.value)
  return province?.children?.find(c => c.value === selectedCity.value)?.children || []
})

const provinceLabel = computed(() => provinces.find(p => p.value === selectedProvince.value)?.label || '')
const cityLabel = computed(() => currentCities.value.find(c => c.value === selectedCity.value)?.label || '')
const districtLabel = computed(() => currentDistricts.value.find(d => d.value === selectedDistrict.value)?.label || '')

const displayText = computed(() => {
  const parts: string[] = []
  if (provinceLabel.value) parts.push(provinceLabel.value)
  if (cityLabel.value && cityLabel.value !== provinceLabel.value) parts.push(cityLabel.value)
  if (districtLabel.value) parts.push(districtLabel.value)
  return parts.join('·')
})

function toggleDropdown(type: 'province' | 'city' | 'district') {
  openDropdown.value = openDropdown.value === type ? null : type
}

function selectProvince(p: RegionNode) {
  selectedProvince.value = p.value; selectedCity.value = ''; selectedDistrict.value = ''
  openDropdown.value = p.children?.length ? 'city' : null; emitChange()
}
function selectCity(c: RegionNode) {
  selectedCity.value = c.value; selectedDistrict.value = ''
  openDropdown.value = c.children?.length ? 'district' : null; emitChange()
}
function selectDistrict(d: RegionNode) {
  selectedDistrict.value = d.value; openDropdown.value = null; emitChange()
}
function emitChange() { emit('change', displayText.value) }

function clearAll() {
  selectedProvince.value = ''; selectedCity.value = ''; selectedDistrict.value = ''
  openDropdown.value = null; emit('change', ''); emit('clear')
}

function setRegion(data: { province: string; city: string; district?: string }) {
  const provNode = findProvince(data.province)
  if (provNode) {
    selectedProvince.value = provNode.value
    const cityNode = findCity(provNode, data.city)
    if (cityNode) {
      selectedCity.value = cityNode.value
      if (data.district) {
        const distNode = findDistrict(cityNode, data.district)
        if (distNode) selectedDistrict.value = distNode.value
      }
    }
  }
  emitChange()
}

defineExpose({ setRegion, clearAll, getDisplayText: () => displayText.value })

function handleClickOutside(e: MouseEvent) {
  if (rootRef.value && !rootRef.value.contains(e.target as Node)) openDropdown.value = null
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>

<style scoped>
.region-selector { position: relative; }
.region-row { display: flex; flex-wrap: wrap; align-items: center; gap: 4px; }
.region-col { position: relative; }
.region-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  min-width: 70px;
}
.region-trigger.placeholder { color: var(--el-text-color-placeholder); }
.region-trigger span { max-width: 90px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.region-trigger .rotated { transform: rotate(180deg); }

.region-dropdown {
  position: absolute;
  left: 0;
  top: 100%;
  margin-top: 4px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  box-shadow: var(--el-box-shadow);
  z-index: 20;
  max-height: 176px;
  overflow-y: auto;
  min-width: 140px;
}
.region-option {
  display: block;
  width: 100%;
  padding: 4px 8px;
  font-size: 12px;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--el-text-color-regular);
  transition: background-color 0.15s;
}
.region-option:hover { background: var(--el-fill-color-lighter); }
.region-option.active { background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-weight: 500; }

.region-preview {
  margin-top: 6px;
  padding: 6px 10px;
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  color: #303133;
  font-size: 13px;
  line-height: 1.5;
}
.region-preview .el-tag {
  background-color: #ffffff;
  color: #303133;
  border-color: #dcdfe6;
}

.region-error {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-color-danger, #f56c6c);
  line-height: 1.4;
}
.region-error-retry {
  margin-left: auto;
  background: none;
  border: none;
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 12px;
  padding: 0 2px;
}
.region-error-retry:hover { text-decoration: underline; }
</style>
