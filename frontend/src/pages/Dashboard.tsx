import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { deadlinesAPI } from '../services/api';
import type { Deadline } from '../types';
import DeadlineCard from '../components/DeadlineCard';
import AddDeadlineModal from '../components/AddDeadlineModal';
import { format, isToday, isTomorrow, isPast } from 'date-fns';

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const [deadlines, setDeadlines] = useState<Deadline[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [filter, setFilter] = useState<'all' | 'upcoming' | 'completed'>('upcoming');

  useEffect(() => {
    fetchDeadlines();
  }, [filter]);

  const fetchDeadlines = async () => {
    try {
      const params: any = {};
      if (filter === 'upcoming') {
        params.upcoming = true;
      } else if (filter === 'completed') {
        params.status = 'completed';
      }

      const data = await deadlinesAPI.getAll(params);
      setDeadlines(data.deadlines);
    } catch (error) {
      console.error('Failed to fetch deadlines:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddDeadline = () => {
    setShowAddModal(false);
    fetchDeadlines();
  };

  const handleUpdateDeadline = async (id: number, updates: Partial<Deadline>) => {
    try {
      await deadlinesAPI.update(id, updates);
      fetchDeadlines();
    } catch (error) {
      console.error('Failed to update deadline:', error);
    }
  };

  const handleDeleteDeadline = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this deadline?')) {
      try {
        await deadlinesAPI.delete(id);
        fetchDeadlines();
      } catch (error) {
        console.error('Failed to delete deadline:', error);
      }
    }
  };

  const getDeadlinesByDate = () => {
    const today: Deadline[] = [];
    const tomorrow: Deadline[] = [];
    const thisWeek: Deadline[] = [];
    const later: Deadline[] = [];
    const past: Deadline[] = [];

    deadlines.forEach((deadline) => {
      const date = new Date(deadline.deadline_date);
      if (isPast(date) && deadline.status !== 'completed') {
        past.push(deadline);
      } else if (isToday(date)) {
        today.push(deadline);
      } else if (isTomorrow(date)) {
        tomorrow.push(deadline);
      } else if (date <= new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)) {
        thisWeek.push(deadline);
      } else {
        later.push(deadline);
      }
    });

    return { today, tomorrow, thisWeek, later, past };
  };

  const groupedDeadlines = getDeadlinesByDate();

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">DeadlineKeeper</h1>
              <p className="text-sm text-gray-600">
                Welcome back, {user?.first_name}!
              </p>
            </div>
            <button
              onClick={logout}
              className="px-4 py-2 text-sm text-gray-700 hover:text-gray-900"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Action Bar */}
        <div className="mb-6 flex justify-between items-center">
          <div className="flex space-x-2">
            <button
              onClick={() => setFilter('upcoming')}
              className={`px-4 py-2 rounded-md ${
                filter === 'upcoming'
                  ? 'bg-indigo-600 text-white'
                  : 'bg-white text-gray-700 hover:bg-gray-50'
              }`}
            >
              Upcoming
            </button>
            <button
              onClick={() => setFilter('all')}
              className={`px-4 py-2 rounded-md ${
                filter === 'all'
                  ? 'bg-indigo-600 text-white'
                  : 'bg-white text-gray-700 hover:bg-gray-50'
              }`}
            >
              All
            </button>
            <button
              onClick={() => setFilter('completed')}
              className={`px-4 py-2 rounded-md ${
                filter === 'completed'
                  ? 'bg-indigo-600 text-white'
                  : 'bg-white text-gray-700 hover:bg-gray-50'
              }`}
            >
              Completed
            </button>
          </div>
          <button
            onClick={() => setShowAddModal(true)}
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
          >
            + Add Deadline
          </button>
        </div>

        {/* Deadlines List */}
        {loading ? (
          <div className="text-center py-12">
            <p className="text-gray-600">Loading deadlines...</p>
          </div>
        ) : deadlines.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-600">No deadlines found. Add your first one!</p>
          </div>
        ) : (
          <div className="space-y-8">
            {groupedDeadlines.past.length > 0 && (
              <div>
                <h2 className="text-lg font-semibold text-red-600 mb-4">Overdue</h2>
                <div className="space-y-3">
                  {groupedDeadlines.past.map((deadline) => (
                    <DeadlineCard
                      key={deadline.id}
                      deadline={deadline}
                      onUpdate={handleUpdateDeadline}
                      onDelete={handleDeleteDeadline}
                    />
                  ))}
                </div>
              </div>
            )}

            {groupedDeadlines.today.length > 0 && (
              <div>
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Today</h2>
                <div className="space-y-3">
                  {groupedDeadlines.today.map((deadline) => (
                    <DeadlineCard
                      key={deadline.id}
                      deadline={deadline}
                      onUpdate={handleUpdateDeadline}
                      onDelete={handleDeleteDeadline}
                    />
                  ))}
                </div>
              </div>
            )}

            {groupedDeadlines.tomorrow.length > 0 && (
              <div>
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Tomorrow</h2>
                <div className="space-y-3">
                  {groupedDeadlines.tomorrow.map((deadline) => (
                    <DeadlineCard
                      key={deadline.id}
                      deadline={deadline}
                      onUpdate={handleUpdateDeadline}
                      onDelete={handleDeleteDeadline}
                    />
                  ))}
                </div>
              </div>
            )}

            {groupedDeadlines.thisWeek.length > 0 && (
              <div>
                <h2 className="text-lg font-semibold text-gray-900 mb-4">This Week</h2>
                <div className="space-y-3">
                  {groupedDeadlines.thisWeek.map((deadline) => (
                    <DeadlineCard
                      key={deadline.id}
                      deadline={deadline}
                      onUpdate={handleUpdateDeadline}
                      onDelete={handleDeleteDeadline}
                    />
                  ))}
                </div>
              </div>
            )}

            {groupedDeadlines.later.length > 0 && (
              <div>
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Later</h2>
                <div className="space-y-3">
                  {groupedDeadlines.later.map((deadline) => (
                    <DeadlineCard
                      key={deadline.id}
                      deadline={deadline}
                      onUpdate={handleUpdateDeadline}
                      onDelete={handleDeleteDeadline}
                    />
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {/* Add Deadline Modal */}
      {showAddModal && (
        <AddDeadlineModal
          onClose={() => setShowAddModal(false)}
          onSuccess={handleAddDeadline}
        />
      )}
    </div>
  );
};

export default Dashboard;
