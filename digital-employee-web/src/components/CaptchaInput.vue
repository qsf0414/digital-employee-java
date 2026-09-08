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
    <img
      class="captcha-input__image"
      :src="captchaImage"
      alt="验证码"
      title="点击刷新"
      @click="emit('refresh')"
    />
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

  &__image {
    width: 120px;
    height: 40px;
    border-radius: 4px;
    cursor: pointer;
    border: 1px solid $color-border;
    object-fit: cover;
  }
}
</style>
