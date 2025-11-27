import React from 'react';
import { format } from 'date-fns';
import type { Deadline } from '../types';

interface DeadlineCardProps {
  deadline: Deadline;
  onUpdate: (id: number, updates: Partial<Deadline>) => void;
  onDelete: (id: number) => void;
}

const DeadlineCard: React.FC<DeadlineCardProps> = ({ deadline, onUpdate, onDelete }) => {
  const priorityColors = {
    low: 'bg-gray-100 text-gray-800',
    medium: 'bg-blue-100 text-blue-800',
    high: 'bg-orange-100 text-orange-800',
    critical: 'bg-red-100 text-red-800',
  };

  const statusColors = {
    pending: 'bg-yellow-100 text-yellow-800',
    in_progress: 'bg-blue-100 text-blue-800',
    completed: 'bg-green-100 text-green-800',
    missed: 'bg-red-100 text-red-800',
  };

  const typeLabels = {
    application: 'Application',
    essay: 'Essay',
    transcript: 'Transcript',
    scholarship: 'Scholarship',
    financial_aid: 'Financial Aid',
    other: 'Other',
  };

  const handleStatusChange = (newStatus: Deadline['status']) => {
    onUpdate(deadline.id, { status: newStatus });
  };

  return (
    <div className="bg-white rounded-lg shadow p-4 hover:shadow-md transition">
      <div className="flex justify-between items-start">
        <div className="flex-1">
          <div className="flex items-center space-x-2 mb-2">
            <h3 className="text-lg font-semibold text-gray-900">{deadline.title}</h3>
            <span
              className={`px-2 py-1 rounded-full text-xs font-medium ${
                priorityColors[deadline.priority]
              }`}
            >
              {deadline.priority}
            </span>
            <span
              className={`px-2 py-1 rounded-full text-xs font-medium ${
                statusColors[deadline.status]
              }`}
            >
              {deadline.status.replace('_', ' ')}
            </span>
          </div>

          <div className="text-sm text-gray-600 space-y-1">
            <p>
              📅 {format(new Date(deadline.deadline_date), 'PPP p')}
            </p>
            <p>📋 {typeLabels[deadline.deadline_type]}</p>
            {deadline.college_name && <p>🎓 {deadline.college_name}</p>}
            {deadline.scholarship_name && <p>💰 {deadline.scholarship_name}</p>}
            {deadline.description && (
              <p className="text-gray-500">{deadline.description}</p>
            )}
          </div>
        </div>

        <div className="flex flex-col space-y-2 ml-4">
          <select
            value={deadline.status}
            onChange={(e) => handleStatusChange(e.target.value as Deadline['status'])}
            className="text-sm border border-gray-300 rounded px-2 py-1"
          >
            <option value="pending">Pending</option>
            <option value="in_progress">In Progress</option>
            <option value="completed">Completed</option>
            <option value="missed">Missed</option>
          </select>
          <button
            onClick={() => onDelete(deadline.id)}
            className="text-sm text-red-600 hover:text-red-800"
          >
            Delete
          </button>
        </div>
      </div>

      {deadline.notes && (
        <div className="mt-3 pt-3 border-t border-gray-200">
          <p className="text-sm text-gray-600">
            <strong>Notes:</strong> {deadline.notes}
          </p>
        </div>
      )}
    </div>
  );
};

export default DeadlineCard;
