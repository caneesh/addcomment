# DeadlineKeeper - College Deadline Tracker MVP

DeadlineKeeper is a smart deadline tracking application for college-bound students. Track application deadlines, scholarship due dates, essay submissions, and more - all in one place with intelligent notifications.

## Features

### MVP Core Features
- **User Authentication**: Secure registration and login system with JWT-based authentication
- **Deadline Management**: Create, update, delete, and organize deadlines
- **Smart Organization**: Deadlines grouped by Today, Tomorrow, This Week, and Later
- **Priority Levels**: Mark deadlines as Low, Medium, High, or Critical
- **Status Tracking**: Track progress with Pending, In Progress, Completed, and Missed statuses
- **Email/SMS Notifications**: Automated reminders at strategic intervals (7 days, 3 days, 1 day, 3 hours)
- **College & Scholarship Database**: Maintain databases of colleges and scholarships
- **Multiple User Roles**: Support for Students, Parents, and Counselors

## Tech Stack

### Backend
- Node.js + Express.js
- TypeScript
- PostgreSQL
- JWT for authentication
- Nodemailer for email notifications
- Twilio for SMS notifications
- Node-cron for scheduled notifications

### Frontend
- React 18
- TypeScript
- Vite
- React Router for navigation
- Axios for API calls
- Tailwind CSS for styling
- date-fns for date formatting

## Project Structure

```
deadlinekeeper/
├── backend/
│   ├── src/
│   │   ├── config/
│   │   │   └── database.ts
│   │   ├── controllers/
│   │   │   ├── authController.ts
│   │   │   ├── deadlineController.ts
│   │   │   ├── collegeController.ts
│   │   │   └── scholarshipController.ts
│   │   ├── middleware/
│   │   │   └── auth.ts
│   │   ├── routes/
│   │   │   ├── authRoutes.ts
│   │   │   ├── deadlineRoutes.ts
│   │   │   ├── collegeRoutes.ts
│   │   │   └── scholarshipRoutes.ts
│   │   ├── services/
│   │   │   └── notificationService.ts
│   │   ├── types/
│   │   │   └── index.ts
│   │   ├── utils/
│   │   │   └── jwt.ts
│   │   └── server.ts
│   ├── package.json
│   └── tsconfig.json
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── AddDeadlineModal.tsx
│   │   │   └── DeadlineCard.tsx
│   │   ├── contexts/
│   │   │   └── AuthContext.tsx
│   │   ├── pages/
│   │   │   ├── Login.tsx
│   │   │   ├── Register.tsx
│   │   │   └── Dashboard.tsx
│   │   ├── services/
│   │   │   └── api.ts
│   │   ├── types/
│   │   │   └── index.ts
│   │   ├── App.tsx
│   │   ├── main.tsx
│   │   └── index.css
│   ├── package.json
│   └── vite.config.ts
└── database-schema.sql
```

## Setup Instructions

### Prerequisites
- Node.js 18+ and npm
- PostgreSQL 14+
- (Optional) Twilio account for SMS notifications
- (Optional) Gmail account for email notifications

### 1. Database Setup

```bash
# Install PostgreSQL (if not already installed)
# macOS
brew install postgresql@14

# Ubuntu/Debian
sudo apt-get install postgresql-14

# Start PostgreSQL
sudo service postgresql start

# Create database
psql -U postgres
CREATE DATABASE deadlinekeeper;
\q

# Run the schema
psql -U postgres -d deadlinekeeper -f database-schema.sql
```

### 2. Backend Setup

```bash
# Navigate to backend directory
cd backend

# Install dependencies
npm install

# Create .env file
cp .env.example .env

# Edit .env with your configuration
nano .env
```

Configure your `.env` file:

```env
# Server
PORT=5000
NODE_ENV=development

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=deadlinekeeper
DB_USER=postgres
DB_PASSWORD=your_password

# JWT (use a strong random string)
JWT_SECRET=your_strong_secret_key_here
JWT_EXPIRES_IN=7d

# Email (Gmail example)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your_email@gmail.com
SMTP_PASSWORD=your_app_password

# Twilio (optional, for SMS)
TWILIO_ACCOUNT_SID=your_account_sid
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_PHONE_NUMBER=+1234567890

# Frontend URL (for CORS)
FRONTEND_URL=http://localhost:3000
```

**Gmail Setup for Email Notifications:**
1. Enable 2-factor authentication on your Google account
2. Generate an App Password: https://myaccount.google.com/apppasswords
3. Use the generated password in `SMTP_PASSWORD`

```bash
# Build TypeScript
npm run build

# Run in development mode
npm run dev

# Or run in production mode
npm start
```

Backend will run on http://localhost:5000

### 3. Frontend Setup

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Create .env file
cp .env.example .env

# Edit .env if needed (default should work)
nano .env
```

Configure your `.env` file:

```env
VITE_API_URL=http://localhost:5000/api
```

```bash
# Run development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

Frontend will run on http://localhost:3000

## API Documentation

### Authentication Endpoints

#### Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "student@example.com",
  "password": "securepassword",
  "first_name": "John",
  "last_name": "Doe",
  "phone_number": "+12345678901",
  "role": "student"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "student@example.com",
  "password": "securepassword"
}
```

#### Get Profile
```http
GET /api/auth/profile
Authorization: Bearer <token>
```

### Deadline Endpoints

#### Create Deadline
```http
POST /api/deadlines
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Submit NYU Application",
  "description": "Common App submission",
  "deadline_date": "2024-01-01T23:59:00",
  "deadline_type": "application",
  "priority": "high",
  "notes": "Need to review essay one more time"
}
```

#### Get All Deadlines
```http
GET /api/deadlines?status=pending&upcoming=true
Authorization: Bearer <token>
```

#### Update Deadline
```http
PUT /api/deadlines/:id
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "completed"
}
```

#### Delete Deadline
```http
DELETE /api/deadlines/:id
Authorization: Bearer <token>
```

## Database Schema

The application uses PostgreSQL with the following main tables:

- **users**: User accounts (students, parents, counselors)
- **deadlines**: All deadline entries
- **colleges**: College database
- **scholarships**: Scholarship opportunities
- **notifications**: Scheduled email/SMS reminders
- **user_relationships**: Parent/counselor access to student accounts
- **applications**: Application status tracking

See `database-schema.sql` for complete schema.

## Notification System

The application automatically creates notifications when deadlines are added:

- **7 days before**: "Start working on this soon"
- **3 days before**: "Deadline approaching"
- **1 day before**: "Due tomorrow!"
- **3 hours before**: "Due in 3 hours!"

Notifications are processed every 5 minutes via a cron job.

## Development

### Running Tests
```bash
# Backend tests
cd backend
npm test

# Frontend tests
cd frontend
npm test
```

### Building for Production
```bash
# Build backend
cd backend
npm run build

# Build frontend
cd frontend
npm run build
```

## Deployment

### Backend Deployment (Heroku example)
```bash
cd backend
heroku create deadlinekeeper-api
heroku addons:create heroku-postgresql:hobby-dev
heroku config:set JWT_SECRET=your_secret
heroku config:set FRONTEND_URL=https://your-frontend-url.com
git push heroku main
```

### Frontend Deployment (Vercel example)
```bash
cd frontend
vercel --prod
```

## Future Enhancements

- [ ] Calendar integration (Google Calendar, Apple Calendar)
- [ ] Common App API integration
- [ ] Essay deadline coordination
- [ ] Recommendation letter tracking
- [ ] Financial aid calculator
- [ ] School district admin dashboard
- [ ] Mobile app (React Native)
- [ ] Push notifications
- [ ] Document upload and storage
- [ ] AI-powered deadline suggestions

## License

MIT

## Support

For issues and questions, please open an issue on GitHub.

---

**Built with ❤️ for college-bound students**
