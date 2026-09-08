<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance } from 'element-plus';
import { useTable } from '@/composables/use-table';
import { checkPermi } from '@/utils/permission';
import { ACCESS_CODES } from '@/constants/access-codes';
import {
  getUserList,
  createUser,
  updateUser,
  deleteUser,
  resetUserPassword,
  getRoleList,
} from '@/api/system-api';
import type { ISysUser, ISysRole, UserCreateDTO, UserUpdateDTO } from '@/api/types';
import TablePagination from '@/components/TablePagination.vue';

const searchKeyword = ref('');
const dialogVisible = ref(false);
const dialogTitle = ref('新建用户');
const formRef = ref<FormInstance>();
const isEdit = ref(false);

const { loading, tableData, pagination, loadData } =
  useTable<ISysUser>({
    fetchApi: (params) => getUserList(params as { page: number; pageSize: number }),
  });

const roleOptions = ref<ISysRole[]>([]);

async function loadRoles() {
  try {
    const res = await getRoleList({ page: 1, pageSize: 100 });
    roleOptions.value = res.data.records;
  } catch {
    // ignore
  }
}

const formData = reactive<UserCreateDTO & { id: number }>({
  id: 0,
  username: '',
  password: '',
  nickname: '',
  phone: '',
  roleId: 0,
});

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
};

function resetForm() {
  formData.id = 0;
  formData.username = '';
  formData.password = '';
  formData.nickname = '';
  formData.phone = '';
  formData.roleId = 0;
  isEdit.value = false;
}

function handleCreate() {
  resetForm();
  dialogTitle.value = '新建用户';
  dialogVisible.value = true;
}

function handleEdit(row: ISysUser) {
  isEdit.value = true;
  dialogTitle.value = '编辑用户';
  formData.id = row.id;
  formData.username = row.username;
  formData.nickname = row.nickname;
  formData.phone = row.phone || '';
  formData.roleId = row.roleId;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    try {
      if (isEdit.value) {
        const data: UserUpdateDTO = {
          id: formData.id,
          nickname: formData.nickname,
          phone: formData.phone || undefined,
          roleId: formData.roleId,
          status: 1,
        };
        await updateUser(data);
        ElMessage.success('更新成功');
      } else {
        await createUser({
          username: formData.username,
          password: formData.password,
          nickname: formData.nickname,
          phone: formData.phone || undefined,
          roleId: formData.roleId,
        });
        ElMessage.success('创建成功');
      }
      dialogVisible.value = false;
      loadData();
    } catch {
      // error handled by interceptor
    }
  });
}

async function handleDelete(row: ISysUser) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.nickname}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await deleteUser(row.id);
    ElMessage.success('删除成功');
    loadData();
  } catch {
    // cancelled or error
  }
}

async function handleResetPassword(row: ISysUser) {
  try {
    await ElMessageBox.confirm(`确定重置用户「${row.nickname}」的密码吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await resetUserPassword(row.id);
    ElMessage.success('密码已重置');
  } catch {
    // cancelled or error
  }
}

function handleSearch() {
  pagination.page = 1;
  loadData();
}

function handleDialogClose() {
  formRef.value?.resetFields();
  resetForm();
}

loadRoles();
</script>

<template>
  <div class="user-manage">
    <el-card class="user-manage__card">
      <template #header>
        <div class="user-manage__header">
          <div class="user-manage__search">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索用户"
              clearable
              class="user-manage__search-input"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
          <el-button
            v-if="checkPermi(ACCESS_CODES.USER_MANAGE)"
            type="primary"
            @click="handleCreate"
          >
            新建用户
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="roleId" label="角色ID" width="80" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="checkPermi(ACCESS_CODES.USER_MANAGE)"
              type="primary"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="checkPermi(ACCESS_CODES.USER_MANAGE)"
              type="danger"
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <el-button type="warning" link @click="handleResetPassword(row)">
              重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <TablePagination
        v-model:page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        @change="loadData"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="formData.username"
            :disabled="isEdit"
            placeholder="请输入用户名"
          />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="formData.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="formData.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.user-manage {
  &__card {
    :deep(.el-card__header) {
      padding: 12px 20px;
    }
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__search {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__search-input {
    width: 240px;
  }
}
</style>
