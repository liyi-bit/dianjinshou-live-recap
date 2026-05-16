import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      redirect: '/users',
      children: [
        {
          path: 'users',
          name: 'users',
          component: () => import('../views/Users.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'users/:id',
          name: 'user-detail',
          component: () => import('../views/UserDetail.vue'),
          meta: { title: '用户详情' }
        },
        {
          path: 'recordings',
          name: 'recordings',
          component: () => import('../views/Recordings.vue'),
          meta: { title: '录制情况' }
        },
        {
          path: 'recordings/:id',
          name: 'recording-detail',
          component: () => import('../views/RecordingDetail.vue'),
          meta: { title: '录制详情' }
        },
        {
          path: 'tasks',
          name: 'tasks',
          component: () => import('../views/Tasks.vue'),
          meta: { title: '任务情况' }
        },
        {
          path: 'tasks/:type/:id',
          name: 'task-detail',
          component: () => import('../views/TaskDetail.vue'),
          meta: { title: '任务详情' }
        }
      ]
    }
  ]
});

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore();
  if (to.meta.requiresAuth && !auth.token) {
    next({ name: 'login' });
  } else if (to.name === 'login' && auth.token) {
    next({ path: '/users' });
  } else {
    next();
  }
});

export default router;
