import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import App from './App.vue';
import router from './router';
import { setupRouterGuard } from './router/guard';
import pinia from './store';
import { setupPermissionDirective } from './directives/permission';
import './styles/index.scss';

const app = createApp(App);

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(pinia);
app.use(router);
app.use(ElementPlus, { size: 'default' });
setupPermissionDirective(app);
setupRouterGuard();

app.mount('#app');
