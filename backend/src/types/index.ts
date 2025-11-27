import { Request } from 'express';

export interface User {
  id: number;
  email: string;
  password_hash: string;
  first_name: string;
  last_name: string;
  phone_number?: string;
  role: 'student' | 'parent' | 'counselor';
  notification_preferences: {
    email: boolean;
    sms: boolean;
  };
  created_at: Date;
  updated_at: Date;
  last_login?: Date;
}

export interface AuthRequest extends Request {
  user?: {
    id: number;
    email: string;
    role: string;
  };
}

export interface Deadline {
  id: number;
  user_id: number;
  college_id?: number;
  scholarship_id?: number;
  title: string;
  description?: string;
  deadline_date: Date;
  deadline_type: 'application' | 'essay' | 'transcript' | 'scholarship' | 'financial_aid' | 'other';
  priority: 'low' | 'medium' | 'high' | 'critical';
  status: 'pending' | 'in_progress' | 'completed' | 'missed';
  completion_date?: Date;
  notes?: string;
  created_at: Date;
  updated_at: Date;
}

export interface College {
  id: number;
  name: string;
  website?: string;
  application_platform?: string;
  location?: string;
  logo_url?: string;
  created_at: Date;
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
  created_at: Date;
}

export interface Notification {
  id: number;
  deadline_id: number;
  user_id: number;
  notification_type: 'email' | 'sms' | 'push';
  scheduled_time: Date;
  sent_at?: Date;
  status: 'pending' | 'sent' | 'failed';
  message?: string;
  created_at: Date;
}
