import { Request, Response } from 'express';
import { query } from '../config/database';

export const getColleges = async (req: Request, res: Response) => {
  try {
    const { search, platform } = req.query;

    let queryText = 'SELECT * FROM colleges';
    const params: any[] = [];
    const conditions: string[] = [];

    if (search) {
      params.push(`%${search}%`);
      conditions.push(`name ILIKE $${params.length}`);
    }

    if (platform) {
      params.push(platform);
      conditions.push(`application_platform = $${params.length}`);
    }

    if (conditions.length > 0) {
      queryText += ' WHERE ' + conditions.join(' AND ');
    }

    queryText += ' ORDER BY name ASC';

    const result = await query(queryText, params);
    res.json({ colleges: result.rows });
  } catch (error) {
    console.error('Get colleges error:', error);
    res.status(500).json({ error: 'Failed to get colleges' });
  }
};

export const getCollegeById = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    const result = await query('SELECT * FROM colleges WHERE id = $1', [id]);

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'College not found' });
    }

    res.json({ college: result.rows[0] });
  } catch (error) {
    console.error('Get college error:', error);
    res.status(500).json({ error: 'Failed to get college' });
  }
};

export const createCollege = async (req: Request, res: Response) => {
  try {
    const { name, website, application_platform, location, logo_url } = req.body;

    if (!name) {
      return res.status(400).json({ error: 'College name is required' });
    }

    const result = await query(
      `INSERT INTO colleges (name, website, application_platform, location, logo_url)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [name, website || null, application_platform || null, location || null, logo_url || null]
    );

    res.status(201).json({
      message: 'College created successfully',
      college: result.rows[0],
    });
  } catch (error) {
    console.error('Create college error:', error);
    res.status(500).json({ error: 'Failed to create college' });
  }
};
