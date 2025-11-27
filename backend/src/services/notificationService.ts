import nodemailer from 'nodemailer';
import twilio from 'twilio';
import { query } from '../config/database';

// Email configuration
const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: parseInt(process.env.SMTP_PORT || '587'),
  secure: false,
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASSWORD,
  },
});

// Twilio configuration
const twilioClient = process.env.TWILIO_ACCOUNT_SID && process.env.TWILIO_AUTH_TOKEN
  ? twilio(process.env.TWILIO_ACCOUNT_SID, process.env.TWILIO_AUTH_TOKEN)
  : null;

export const sendEmailNotification = async (
  to: string,
  subject: string,
  message: string
): Promise<boolean> => {
  try {
    await transporter.sendMail({
      from: process.env.SMTP_USER,
      to,
      subject,
      html: message,
    });
    console.log(`✅ Email sent to ${to}`);
    return true;
  } catch (error) {
    console.error('❌ Email error:', error);
    return false;
  }
};

export const sendSMSNotification = async (
  to: string,
  message: string
): Promise<boolean> => {
  try {
    if (!twilioClient) {
      console.error('❌ Twilio not configured');
      return false;
    }

    await twilioClient.messages.create({
      body: message,
      from: process.env.TWILIO_PHONE_NUMBER,
      to,
    });
    console.log(`✅ SMS sent to ${to}`);
    return true;
  } catch (error) {
    console.error('❌ SMS error:', error);
    return false;
  }
};

export const createDeadlineNotifications = async (deadlineId: number, userId: number) => {
  try {
    // Get deadline details
    const deadlineResult = await query(
      `SELECT d.*, u.email, u.phone_number, u.notification_preferences
       FROM deadlines d
       JOIN users u ON d.user_id = u.id
       WHERE d.id = $1 AND d.user_id = $2`,
      [deadlineId, userId]
    );

    if (deadlineResult.rows.length === 0) {
      return;
    }

    const deadline = deadlineResult.rows[0];
    const deadlineDate = new Date(deadline.deadline_date);

    // Create notifications at different intervals
    const intervals = [
      { days: 7, message: 'Start working on this soon' },
      { days: 3, message: 'Deadline approaching' },
      { days: 1, message: 'Due tomorrow!' },
      { hours: 3, message: 'Due in 3 hours!' },
    ];

    for (const interval of intervals) {
      let scheduledTime: Date;
      if ('days' in interval) {
        scheduledTime = new Date(deadlineDate.getTime() - interval.days * 24 * 60 * 60 * 1000);
      } else {
        scheduledTime = new Date(deadlineDate.getTime() - interval.hours * 60 * 60 * 1000);
      }

      // Only create notifications for future times
      if (scheduledTime > new Date()) {
        const notificationMessage = `📅 ${deadline.title}: ${interval.message}`;

        // Create email notification if enabled
        if (deadline.notification_preferences.email) {
          await query(
            `INSERT INTO notifications
             (deadline_id, user_id, notification_type, scheduled_time, message)
             VALUES ($1, $2, 'email', $3, $4)`,
            [deadlineId, userId, scheduledTime, notificationMessage]
          );
        }

        // Create SMS notification if enabled and phone number exists
        if (deadline.notification_preferences.sms && deadline.phone_number) {
          await query(
            `INSERT INTO notifications
             (deadline_id, user_id, notification_type, scheduled_time, message)
             VALUES ($1, $2, 'sms', $3, $4)`,
            [deadlineId, userId, scheduledTime, notificationMessage]
          );
        }
      }
    }

    console.log(`✅ Created notifications for deadline ${deadlineId}`);
  } catch (error) {
    console.error('❌ Create notifications error:', error);
  }
};

export const processPendingNotifications = async () => {
  try {
    const result = await query(
      `SELECT n.*, u.email, u.phone_number, d.title as deadline_title
       FROM notifications n
       JOIN users u ON n.user_id = u.id
       JOIN deadlines d ON n.deadline_id = d.id
       WHERE n.status = 'pending'
         AND n.scheduled_time <= CURRENT_TIMESTAMP
       ORDER BY n.scheduled_time ASC
       LIMIT 100`
    );

    for (const notification of result.rows) {
      let success = false;

      if (notification.notification_type === 'email') {
        success = await sendEmailNotification(
          notification.email,
          `Deadline Reminder: ${notification.deadline_title}`,
          notification.message
        );
      } else if (notification.notification_type === 'sms') {
        success = await sendSMSNotification(notification.phone_number, notification.message);
      }

      // Update notification status
      await query(
        `UPDATE notifications
         SET status = $1, sent_at = CURRENT_TIMESTAMP
         WHERE id = $2`,
        [success ? 'sent' : 'failed', notification.id]
      );
    }

    if (result.rows.length > 0) {
      console.log(`✅ Processed ${result.rows.length} notifications`);
    }
  } catch (error) {
    console.error('❌ Process notifications error:', error);
  }
};
