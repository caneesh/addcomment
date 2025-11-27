import { Request, Response } from 'express';
import { query } from '../config/database';

export const getScholarships = async (req: Request, res: Response) => {
  try {
    const { search, min_amount, max_amount } = req.query;

    let queryText = 'SELECT * FROM scholarships';
    const params: any[] = [];
    const conditions: string[] = [];

    if (search) {
      params.push(`%${search}%`);
      conditions.push(`(name ILIKE $${params.length} OR organization ILIKE $${params.length})`);
    }

    if (min_amount) {
      params.push(min_amount);
      conditions.push(`amount_min >= $${params.length}`);
    }

    if (max_amount) {
      params.push(max_amount);
      conditions.push(`amount_max <= $${params.length}`);
    }

    if (conditions.length > 0) {
      queryText += ' WHERE ' + conditions.join(' AND ');
    }

    queryText += ' ORDER BY amount_max DESC NULLS LAST, name ASC';

    const result = await query(queryText, params);
    res.json({ scholarships: result.rows });
  } catch (error) {
    console.error('Get scholarships error:', error);
    res.status(500).json({ error: 'Failed to get scholarships' });
  }
};

export const getScholarshipById = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    const result = await query('SELECT * FROM scholarships WHERE id = $1', [id]);

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Scholarship not found' });
    }

    res.json({ scholarship: result.rows[0] });
  } catch (error) {
    console.error('Get scholarship error:', error);
    res.status(500).json({ error: 'Failed to get scholarship' });
  }
};

export const createScholarship = async (req: Request, res: Response) => {
  try {
    const {
      name,
      organization,
      amount_min,
      amount_max,
      website,
      description,
      eligibility_criteria,
      is_recurring,
    } = req.body;

    if (!name) {
      return res.status(400).json({ error: 'Scholarship name is required' });
    }

    const result = await query(
      `INSERT INTO scholarships
       (name, organization, amount_min, amount_max, website, description,
        eligibility_criteria, is_recurring)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [
        name,
        organization || null,
        amount_min || null,
        amount_max || null,
        website || null,
        description || null,
        eligibility_criteria || null,
        is_recurring || false,
      ]
    );

    res.status(201).json({
      message: 'Scholarship created successfully',
      scholarship: result.rows[0],
    });
  } catch (error) {
    console.error('Create scholarship error:', error);
    res.status(500).json({ error: 'Failed to create scholarship' });
  }
};
