import { Router } from 'express';
import {
  createDeadline,
  getDeadlines,
  getDeadlineById,
  updateDeadline,
  deleteDeadline,
  getUpcomingDeadlines,
} from '../controllers/deadlineController';
import { authMiddleware } from '../middleware/auth';

const router = Router();

// All deadline routes require authentication
router.use(authMiddleware);

router.post('/', createDeadline);
router.get('/', getDeadlines);
router.get('/upcoming', getUpcomingDeadlines);
router.get('/:id', getDeadlineById);
router.put('/:id', updateDeadline);
router.delete('/:id', deleteDeadline);

export default router;
