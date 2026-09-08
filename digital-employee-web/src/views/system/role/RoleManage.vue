<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance } from 'element-plus';
import { useTable } from '@/composables/use-table';
import { checkPermi } from '@/utils/permission';
import { ACCESS_CODES } from '@/constants/access-codes';
import {
  getRoleList,
  getRoleById,
  createRole,
  updateRole,
  deleteRole,
  getMenuTree,
} from '@/api/system-api';
import type { ISysRole, ISysMenuTreeNode, RoleCreateDTO, RoleUpdateDTO } from '@/api/types';
import TablePagination from '@/components/TablePagination.vue';

const dialogVisible = ref(false);
const dialogTitle = ref('新建角色');
const formRef = ref<FormInstance>();
const treeRef = ref<InstanceType<typeof import('element-plus')['ElTree']>>();
const isEdit = ref(false);

const { loading, tableData, pagination, loadData } =
  useTable<ISysRole>({
    fetchApi: (params) => getRoleList(params as { page: number; pageSize: number }),
  });

const menuTreeData = ref<ISysMenuTreeNode[]>([]);

const formData = reactive<RoleCreateDTO & { id: number }>({
  id: 0,
  roleKey: '',
  roleName: '',
  menuIds: [],
});

const formRules = {
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
};

function resetForm() {
  formData.id = 0;
  formData.roleKey = '';
  formData.roleName = '';
  formData.menuIds = [];
  isEdit.value = false;
}

async function loadMenuTree() {
  try {
    const res = await getMenuTree();
    menuTreeData.value = res.data;
  } catch {
    // ignore
  }
}

function handleCreate() {
  resetForm();
  dialogTitle.value = '新建角色';
  dialogVisible.value = true;
  nextTick(() => {
    treeRef.value?.setCheckedKeys([]);
  });
}

async function handleEdit(row: ISysRole) {
  isEdit.value = true;
  dialogTitle.value = '编辑角色';
  formData.id = row.id;
  formData.roleKey = row.roleKey;
  formData.roleName = row.roleName;

  try {
    const res = await getRoleById(row.id);
    formData.menuIds = (res.data as unknown as { menuIds?: number[] }).menuIds || [];
  } catch {
    formData.menuIds = [];
  }

  dialogVisible.value = true;
  nextTick(() => {
    treeRef.value?.setCheckedKeys(formData.menuIds);
  });
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    const checkedKeys = treeRef.value?.getCheckedKeys() as number[];
    const halfCheckedKeys = treeRef.value?.getHalfCheckedKeys() as number[];
    const allMenuIds = [...(checkedKeys || []), ...(halfCheckedKeys || [])];

    try {
      if (isEdit.value) {
        const data: RoleUpdateDTO = {
          id: formData.id,
          roleName: formData.roleName,
          status: 1,
          menuIds: allMenuIds,
        };
        await updateRole(data);
        ElMessage.success('更新成功');
      } else {
        await createRole({
          roleKey: formData.roleKey,
          roleName: formData.roleName,
          menuIds: allMenuIds,
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

async function handleDelete(row: ISysRole) {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await deleteRole(row.id);
    ElMessage.success('删除成功');
    loadData();
  } catch {
    // cancelled or error
  }
}

function handleDialogClose() {
  formRef.value?.resetFields();
  resetForm();
}

loadMenuTree();
</script>

<template>
  <div class="role-manage">
    <el-card class="role-manage__card">
      <template #header>
        <div class="role-manage__header">
          <span class="role-manage__title">角色管理</span>
          <el-button
            v-if="checkPermi(ACCESS_CODES.PERMISSION_MANAGE)"
            type="primary"
            @click="handleCreate"
          >
            新建角色
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleKey" label="角色标识" min-width="150" />
        <el-table-column prop="roleName" label="角色名称" min-width="150" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="checkPermi(ACCESS_CODES.PERMISSION_MANAGE)"
              type="primary"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="checkPermi(ACCESS_CODES.PERMISSION_MANAGE)"
              type="danger"
              link
              @click="handleDelete(row)"
            >
              删除
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
        <el-form-item label="角色标识" prop="roleKey">
          <el-input
            v-model="formData.roleKey"
            :disabled="isEdit"
            placeholder="请输入角色标识"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-tree
            ref="treeRef"
            :data="menuTreeData"
            :props="{ label: 'title', children: 'children' }"
            show-checkbox
            node-key="id"
            default-expand-all
            class="role-manage__tree"
          />
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
.role-manage {
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

  &__title {
    font-size: 16px;
    font-weight: 600;
  }

  &__tree {
    width: 100%;
    max-height: 320px;
    overflow-y: auto;
    border: 1px solid $color-border-light;
    border-radius: 4px;
    padding: 8px;
  }
}
</style>
