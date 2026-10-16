-- Optional: create ONE new administrator in an EXISTING V2 schema.
-- Pass ADMIN_LOGIN_ID, ADMIN_EMAIL and ADMIN_PASSWORD_HASH as psql environment variables.
-- Generate a BCrypt hash first; never use the development plaintext seed passwords.
\set ON_ERROR_STOP on
\getenv admin_login_id ADMIN_LOGIN_ID
\getenv admin_email ADMIN_EMAIL
\getenv admin_password_hash ADMIN_PASSWORD_HASH

BEGIN;
CREATE TEMP TABLE bootstrap_input ON COMMIT DROP AS
SELECT :'admin_login_id'::text AS login_id,
       :'admin_email'::text AS email,
       :'admin_password_hash'::text AS password_hash;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM bootstrap_input
        WHERE length(login_id) BETWEEN 1 AND 255
          AND length(email) BETWEEN 3 AND 255
          AND password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'
    ) THEN
        RAISE EXCEPTION 'Valid login, email and BCrypt hash are required';
    END IF;
END $$;

INSERT INTO web.roles (role_name, description, created_by, updated_by)
VALUES ('ADMIN', 'StreamCell administrator', 'BOOTSTRAP', 'BOOTSTRAP')
ON CONFLICT (role_name) DO NOTHING;

-- An existing login/email aborts this transaction; it never resets an account.
INSERT INTO web.users (login_id, email, password, name, status, created_by, updated_by)
SELECT login_id, email, password_hash, 'Administrator', 'ACTIVE', 'BOOTSTRAP', 'BOOTSTRAP'
FROM bootstrap_input;

INSERT INTO web.user_roles (user_id, role_id, created_by, updated_by)
SELECT u.user_id, r.role_id, 'BOOTSTRAP', 'BOOTSTRAP'
FROM web.users u JOIN web.roles r ON r.role_name = 'ADMIN'
JOIN bootstrap_input i ON i.login_id = u.login_id;
COMMIT;
