-- A user may hold tickets of an event only once at a time: enforced by the database, not just the application.
-- (The application expires an overdue HELD reservation before it creates a new one, so this never blocks a valid request.)
DROP INDEX reservation_held_by_user_and_event;
CREATE UNIQUE INDEX reservation_held_by_user_and_event ON reservation (user_id, event_id) WHERE status = 'HELD';
