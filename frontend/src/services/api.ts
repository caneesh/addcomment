import axios from 'axios';
import type { Deadline, College, Scholarship, RegisterData } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auth API
export const authAPI = {
  register: async (data: RegisterData) => {
    const response = await api.post('/auth/register', data);
    return response.data;
  },

  login: async (email: string, password: string) => {
    const response = await api.post('/auth/login', { email, password });
    return response.data;
  },

  getProfile: async () => {
    const response = await api.get('/auth/profile');
    return response.data;
  },

  updateProfile: async (data: Partial<RegisterData>) => {
    const response = await api.put('/auth/profile', data);
    return response.data;
  },
};

// Deadlines API
export const deadlinesAPI = {
  getAll: async (params?: {
    status?: string;
    deadline_type?: string;
    priority?: string;
    upcoming?: boolean;
  }) => {
    const response = await api.get('/deadlines', { params });
    return response.data;
  },

  getById: async (id: number) => {
    const response = await api.get(`/deadlines/${id}`);
    return response.data;
  },

  create: async (data: Partial<Deadline>) => {
    const response = await api.post('/deadlines', data);
    return response.data;
  },

  update: async (id: number, data: Partial<Deadline>) => {
    const response = await api.put(`/deadlines/${id}`, data);
    return response.data;
  },

  delete: async (id: number) => {
    const response = await api.delete(`/deadlines/${id}`);
    return response.data;
  },

  getUpcoming: async (days?: number) => {
    const response = await api.get('/deadlines/upcoming', {
      params: { days },
    });
    return response.data;
  },
};

// Colleges API
export const collegesAPI = {
  getAll: async (params?: { search?: string; platform?: string }) => {
    const response = await api.get('/colleges', { params });
    return response.data;
  },

  getById: async (id: number) => {
    const response = await api.get(`/colleges/${id}`);
    return response.data;
  },

  create: async (data: Partial<College>) => {
    const response = await api.post('/colleges', data);
    return response.data;
  },
};

// Scholarships API
export const scholarshipsAPI = {
  getAll: async (params?: { search?: string; min_amount?: number; max_amount?: number }) => {
    const response = await api.get('/scholarships', { params });
    return response.data;
  },

  getById: async (id: number) => {
    const response = await api.get(`/scholarships/${id}`);
    return response.data;
  },

  create: async (data: Partial<Scholarship>) => {
    const response = await api.post('/scholarships', data);
    return response.data;
  },
};

export default api;
