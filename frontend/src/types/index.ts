export interface User {
  id: number;
  email: string;
  first_name: string;
  last_name: string;
  phone_number?: string;
  role: 'student' | 'parent' | 'counselor';
  notification_preferences?: {
    email: boolean;
    sms: boolean;
  };
}

export interface Deadline {
  id: number;
  user_id: number;
  college_id?: number;
  scholarship_id?: number;
  title: string;
  description?: string;
  deadline_date: string;
  deadline_type: 'application' | 'essay' | 'transcript' | 'scholarship' | 'financial_aid' | 'other';
  priority: 'low' | 'medium' | 'high' | 'critical';
  status: 'pending' | 'in_progress' | 'completed' | 'missed';
  completion_date?: string;
  notes?: string;
  college_name?: string;
  scholarship_name?: string;
  created_at: string;
  updated_at: string;
}

export interface College {
  id: number;
  name: string;
  website?: string;
  application_platform?: string;
  location?: string;
  logo_url?: string;
}

export interface Scholarship {
  id: number;
  name: string;
  organization?: string;
  amount_min?: number;
  amount_max?: number;
  website?: string;
  description?: string;
  eligibility_criteria?: string;
  is_recurring: boolean;
}

export interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

export interface RegisterData {
  email: string;
  password: string;
  first_name: string;
  last_name: string;
  phone_number?: string;
  role?: 'student' | 'parent' | 'counselor';
}
