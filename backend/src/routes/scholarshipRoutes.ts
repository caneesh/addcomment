import { Router } from 'express';
import {
  getScholarships,
  getScholarshipById,
  createScholarship,
} from '../controllers/scholarshipController';
import { authMiddleware } from '../middleware/auth';

const router = Router();

router.get('/', getScholarships);
router.get('/:id', getScholarshipById);
router.post('/', authMiddleware, createScholarship);

export default router;
