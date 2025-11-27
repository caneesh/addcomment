import { Response } from 'express';
import { query } from '../config/database';
import { AuthRequest } from '../types';

export const createDeadline = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    const {
      college_id,
      scholarship_id,
      title,
      description,
      deadline_date,
      deadline_type,
      priority,
      notes,
    } = req.body;

    if (!title || !deadline_date || !deadline_type) {
      return res.status(400).json({ error: 'Missing required fields' });
    }

    const result = await query(
      `INSERT INTO deadlines
       (user_id, college_id, scholarship_id, title, description, deadline_date,
        deadline_type, priority, notes)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       RETURNING *`,
      [
        req.user.id,
        college_id || null,
        scholarship_id || null,
        title,
        description || null,
        deadline_date,
        deadline_type,
        priority || 'medium',
        notes || null,
      ]
    );

    res.status(201).json({
      message: 'Deadline created successfully',
      deadline: result.rows[0],
    });
  } catch (error) {
    console.error('Create deadline error:', error);
    res.status(500).json({ error: 'Failed to create deadline' });
  }
};

export const getDeadlines = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    const { status, deadline_type, priority, upcoming } = req.query;

    let queryText = `
      SELECT d.*, c.name as college_name, s.name as scholarship_name
      FROM deadlines d
      LEFT JOIN colleges c ON d.college_id = c.id
      LEFT JOIN scholarships s ON d.scholarship_id = s.id
      WHERE d.user_id = $1
    `;
    const params: any[] = [req.user.id];
    let paramCount = 1;

    if (status) {
      paramCount++;
      queryText += ` AND d.status = $${paramCount}`;
      params.push(status);
    }

    if (deadline_type) {
      paramCount++;
      queryText += ` AND d.deadline_type = $${paramCount}`;
      params.push(deadline_type);
    }

    if (priority) {
      paramCount++;
      queryText += ` AND d.priority = $${paramCount}`;
      params.push(priority);
    }

    if (upcoming === 'true') {
      queryText += ` AND d.deadline_date >= CURRENT_TIMESTAMP AND d.status != 'completed'`;
    }

    queryText += ` ORDER BY d.deadline_date ASC`;

    const result = await query(queryText, params);

    res.json({ deadlines: result.rows });
  } catch (error) {
    console.error('Get deadlines error:', error);
    res.status(500).json({ error: 'Failed to get deadlines' });
  }
};

export const getDeadlineById = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    const { id } = req.params;

    const result = await query(
      `SELECT d.*, c.name as college_name, s.name as scholarship_name
       FROM deadlines d
       LEFT JOIN colleges c ON d.college_id = c.id
       LEFT JOIN scholarships s ON d.scholarship_id = s.id
       WHERE d.id = $1 AND d.user_id = $2`,
      [id, req.user.id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Deadline not found' });
    }

    res.json({ deadline: result.rows[0] });
  } catch (error) {
    console.error('Get deadline error:', error);
    res.status(500).json({ error: 'Failed to get deadline' });
  }
};

export const updateDeadline = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    const { id } = req.params;
    const {
      title,
      description,
      deadline_date,
      deadline_type,
      priority,
      status,
      notes,
    } = req.body;

    // Check ownership
    const checkResult = await query(
      'SELECT id FROM deadlines WHERE id = $1 AND user_id = $2',
      [id, req.user.id]
    );

    if (checkResult.rows.length === 0) {
      return res.status(404).json({ error: 'Deadline not found' });
    }

    const result = await query(
      `UPDATE deadlines
       SET title = COALESCE($1, title),
           description = COALESCE($2, description),
           deadline_date = COALESCE($3, deadline_date),
           deadline_type = COALESCE($4, deadline_type),
           priority = COALESCE($5, priority),
           status = COALESCE($6, status),
           notes = COALESCE($7, notes),
           completion_date = CASE WHEN $6 = 'completed' THEN CURRENT_TIMESTAMP ELSE completion_date END
       WHERE id = $8
       RETURNING *`,
      [title, description, deadline_date, deadline_type, priority, status, notes, id]
    );

    res.json({
      message: 'Deadline updated successfully',
      deadline: result.rows[0],
    });
  } catch (error) {
    console.error('Update deadline error:', error);
    res.status(500).json({ error: 'Failed to update deadline' });
  }
};

export const deleteDeadline = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    const { id } = req.params;

    const result = await query(
      'DELETE FROM deadlines WHERE id = $1 AND user_id = $2 RETURNING id',
      [id, req.user.id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Deadline not found' });
    }

    res.json({ message: 'Deadline deleted successfully' });
  } catch (error) {
    console.error('Delete deadline error:', error);
    res.status(500).json({ error: 'Failed to delete deadline' });
  }
};

export const getUpcomingDeadlines = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    const { days } = req.query;
    const daysAhead = parseInt(days as string) || 7;

    const result = await query(
      `SELECT d.*, c.name as college_name, s.name as scholarship_name
       FROM deadlines d
       LEFT JOIN colleges c ON d.college_id = c.id
       LEFT JOIN scholarships s ON d.scholarship_id = s.id
       WHERE d.user_id = $1
         AND d.deadline_date >= CURRENT_TIMESTAMP
         AND d.deadline_date <= CURRENT_TIMESTAMP + INTERVAL '${daysAhead} days'
         AND d.status != 'completed'
       ORDER BY d.deadline_date ASC`,
      [req.user.id]
    );

    res.json({ deadlines: result.rows });
  } catch (error) {
    console.error('Get upcoming deadlines error:', error);
    res.status(500).json({ error: 'Failed to get upcoming deadlines' });
  }
};
