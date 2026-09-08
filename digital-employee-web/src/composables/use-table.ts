import { ref, reactive, onMounted, type Ref } from 'vue';

interface UseTableOptions<T> {
  fetchApi: (params: Record<string, unknown>) => Promise<{ data: { records: T[]; total: number } }>;
}

export function useTable<T>(options: UseTableOptions<T>) {
  const loading = ref(false);
  const tableData = ref<T[]>([]) as Ref<T[]>;
  const pagination = reactive({ page: 1, pageSize: 10, total: 0 });

  async function loadData() {
    loading.value = true;
    try {
      const res = await options.fetchApi({ page: pagination.page, pageSize: pagination.pageSize });
      tableData.value = res.data.records;
      pagination.total = res.data.total;
    } finally {
      loading.value = false;
    }
  }

  function handlePageChange(page: number) {
    pagination.page = page;
    loadData();
  }

  function handleSizeChange(size: number) {
    pagination.pageSize = size;
    pagination.page = 1;
    loadData();
  }

  onMounted(loadData);

  return { loading, tableData, pagination, loadData, handlePageChange, handleSizeChange };
}
