import { Router } from 'express';
import { getColleges, getCollegeById, createCollege } from '../controllers/collegeController';
import { authMiddleware } from '../middleware/auth';

const router = Router();

router.get('/', getColleges);
router.get('/:id', getCollegeById);
router.post('/', authMiddleware, createCollege);

export default router;
