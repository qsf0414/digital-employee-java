<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/store/modules/user';
import { getCaptcha } from '@/api/auth-api';
import type { ICaptchaData } from '@/api/types';
import CaptchaInput from '@/components/CaptchaInput.vue';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const loginForm = ref({
  username: '',
  password: '',
  captchaKey: '',
  captchaCode: '',
});

const captcha = ref<ICaptchaData | null>(null);
const loading = ref(false);

async function refreshCaptcha(): Promise<void> {
  try {
    const res = await getCaptcha();
    captcha.value = res.data;
    loginForm.value.captchaKey = res.data.captchaKey;
    loginForm.value.captchaCode = '';
  } catch {
    // ignore
  }
}

async function handleLogin(): Promise<void> {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  if (!loginForm.value.captchaCode) {
    ElMessage.warning('请输入验证码');
    return;
  }

  loading.value = true;
  try {
    await userStore.login(loginForm.value);
    ElMessage.success('登录成功');
    const redirect = (route.query.redirect as string) || '/';
    router.push(redirect);
  } catch {
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  refreshCaptcha();
});
</script>

<template>
  <div class="login-view">
    <div class="login-view__card">
      <h2 class="login-view__title">数字员工管理系统</h2>
      <el-form
        ref="formRef"
        :model="loginForm"
        class="login-view__form"
        @keyup.enter="handleLogin"
      >
        <el-form-item>
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item v-if="captcha">
          <CaptchaInput
            v-model="loginForm.captchaCode"
            :captcha-image="captcha.captchaImage"
            @refresh="refreshCaptcha"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-view__submit"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-view {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  &__card {
    width: 420px;
    padding: 40px;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 8px 32px #0000001a;
  }

  &__title {
    text-align: center;
    margin-bottom: 32px;
    font-size: 22px;
    color: $color-text-primary;
  }

  &__form {
    width: 100%;
  }

  &__submit {
    width: 100%;
  }
}
</style>
