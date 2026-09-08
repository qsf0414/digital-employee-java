<script setup lang="ts">
defineProps<{
  modelValue: string;
  captchaImage: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
  refresh: [];
}>();

function handleInput(value: string): void {
  emit('update:modelValue', value);
}
</script>

<template>
  <div class="captcha-input">
    <el-input
      :model-value="modelValue"
      placeholder="验证码"
      size="large"
      prefix-icon="Key"
      @update:model-value="handleInput"
    />
    <div
      class="captcha-input__text"
      title="点击刷新"
      @click="emit('refresh')"
    >
      {{ captchaImage }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.captcha-input {
  display: flex;
  width: 100%;
  gap: 12px;

  .el-input {
    flex: 1;
  }

  &__text {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 120px;
    height: 40px;
    padding: 0 12px;
    font-size: 16px;
    font-weight: bold;
    font-family: 'Courier New', monospace;
    color: #333;
    letter-spacing: 2px;
    background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
    user-select: none;
    background-image: repeating-linear-gradient(
      0deg,
      transparent,
      transparent 3px,
      rgba(0, 0, 0, 0.03) 3px,
      rgba(0, 0, 0, 0.03) 6px
    );
  }
}
</style>
