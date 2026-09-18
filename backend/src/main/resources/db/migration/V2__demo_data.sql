-- Demo dataset for portfolio/recruiter review. Runs exactly once (Flyway
-- tracks it in flyway_schema_history), so redeploys/restarts never
-- duplicate it. All passwords below are the string "password123",
-- BCrypt-hashed with the same PasswordEncoder(strength 10) the app itself
-- uses - never a plaintext or ad-hoc secret.

-- demo@connecthub.dev is the clearly-identified account for recruiters to
-- log into. alice/bob/carol/dave exist only to give it friends, a pending
-- request, and a blocked user to look at.

INSERT INTO `users` (`user_id`, `email`, `username`, `hashed_password`, `date_of_birth`, `status`, `bio`, `created_at`, `updated_at`) VALUES
(UUID_TO_BIN('a0000000-0000-0000-0000-000000000001'), 'demo@connecthub.dev', 'demo',  '$2a$10$kEHhPoKCa77n/h6tMEMf9.BtatqOkXQPdwvuYhFFbhlcPNO93ZpCO', '1998-01-01', 'OFFLINE', 'Backend engineer | Spring Boot, MySQL, Docker | This is the ConnectHub demo account - log in and look around.', '2026-09-12 09:00:00', '2026-09-12 09:00:00'),
(UUID_TO_BIN('a0000000-0000-0000-0000-000000000002'), 'alice@connecthub.dev', 'alice', '$2a$10$kEHhPoKCa77n/h6tMEMf9.BtatqOkXQPdwvuYhFFbhlcPNO93ZpCO', '1997-04-11', 'OFFLINE', 'Frontend dev exploring backend concepts | Coffee enthusiast', '2026-09-12 09:05:00', '2026-09-12 09:05:00'),
(UUID_TO_BIN('a0000000-0000-0000-0000-000000000003'), 'bob@connecthub.dev',   'bob',   '$2a$10$kEHhPoKCa77n/h6tMEMf9.BtatqOkXQPdwvuYhFFbhlcPNO93ZpCO', '1996-08-22', 'OFFLINE', 'DevOps-curious backend engineer', '2026-09-12 09:10:00', '2026-09-12 09:10:00'),
(UUID_TO_BIN('a0000000-0000-0000-0000-000000000004'), 'carol@connecthub.dev', 'carol', '$2a$10$kEHhPoKCa77n/h6tMEMf9.BtatqOkXQPdwvuYhFFbhlcPNO93ZpCO', '1999-02-14', 'OFFLINE', 'Full-stack developer | Open to new opportunities', '2026-09-12 09:15:00', '2026-09-12 09:15:00'),
(UUID_TO_BIN('a0000000-0000-0000-0000-000000000005'), 'dave@connecthub.dev',  'dave',  '$2a$10$kEHhPoKCa77n/h6tMEMf9.BtatqOkXQPdwvuYhFFbhlcPNO93ZpCO', '1995-11-30', 'OFFLINE', 'Just here to demonstrate the blocking feature', '2026-09-12 09:20:00', '2026-09-12 09:20:00');

-- Friendships: demo has two accepted friends, one pending incoming
-- request, and has blocked one user. alice/bob/carol are also connected
-- to each other so the graph isn't purely radial around demo.
INSERT INTO `friendships` (`friendship_id`, `requester_id`, `receiver_id`, `status`, `created_at`, `updated_at`) VALUES
(UUID_TO_BIN('b0000000-0000-0000-0000-000000000001'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000001'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000002'), 'ACCEPTED', '2026-09-13 10:00:00', '2026-09-13 10:00:00'),
(UUID_TO_BIN('b0000000-0000-0000-0000-000000000002'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000001'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000003'), 'ACCEPTED', '2026-09-13 10:05:00', '2026-09-13 10:05:00'),
(UUID_TO_BIN('b0000000-0000-0000-0000-000000000003'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000002'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000003'), 'ACCEPTED', '2026-09-13 10:10:00', '2026-09-13 10:10:00'),
(UUID_TO_BIN('b0000000-0000-0000-0000-000000000004'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000002'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000004'), 'ACCEPTED', '2026-09-13 10:15:00', '2026-09-13 10:15:00'),
(UUID_TO_BIN('b0000000-0000-0000-0000-000000000005'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000004'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000001'), 'PENDING',  '2026-09-14 11:00:00', '2026-09-14 11:00:00'),
(UUID_TO_BIN('b0000000-0000-0000-0000-000000000006'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000001'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000005'), 'BLOCKED',  '2026-09-14 11:05:00', '2026-09-14 11:05:00');

-- Posts only - no stories (they expire 24h after creation by design, so a
-- one-time migration can't keep one "fresh" indefinitely) and no comments
-- (the feature doesn't exist in this application).
INSERT INTO `contents` (`content_id`, `author_id`, `content_text`, `content_type`, `timestamp`) VALUES
(UUID_TO_BIN('c0000000-0000-0000-0000-000000000001'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000001'), 'Welcome to my ConnectHub demo! This account shows off the core features - friends, posts, and friend requests. Feel free to click around.', 'POST', '2026-09-15 09:00:00'),
(UUID_TO_BIN('c0000000-0000-0000-0000-000000000002'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000001'), 'Under the hood: Spring Boot + MySQL in production, JWT auth, a Flyway-managed schema, and a Dockerized backend deployed on Railway.', 'POST', '2026-09-16 09:00:00'),
(UUID_TO_BIN('c0000000-0000-0000-0000-000000000003'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000002'), 'Really happy with how the friend request flow turned out - clean state transitions between pending, accepted, and blocked.', 'POST', '2026-09-15 12:00:00'),
(UUID_TO_BIN('c0000000-0000-0000-0000-000000000004'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000002'), 'Spent the afternoon reviewing pull requests. Small, focused PRs really do make code review so much easier.', 'POST', '2026-09-16 12:00:00'),
(UUID_TO_BIN('c0000000-0000-0000-0000-000000000005'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000003'), 'Docker multi-stage builds are still one of my favorite small-change-big-payoff patterns.', 'POST', '2026-09-15 15:00:00'),
(UUID_TO_BIN('c0000000-0000-0000-0000-000000000006'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000003'), 'Finally set up a proper CI pipeline for a side project. Feels good to have tests actually gating deploys.', 'POST', '2026-09-16 15:00:00'),
(UUID_TO_BIN('c0000000-0000-0000-0000-000000000007'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000004'), 'Three months into learning Spring Security and JWTs finally click. Stateless auth is elegant once it lands.', 'POST', '2026-09-15 18:00:00'),
(UUID_TO_BIN('c0000000-0000-0000-0000-000000000008'), UUID_TO_BIN('a0000000-0000-0000-0000-000000000004'), 'Looking for my next role - always happy to chat about backend architecture and clean API design.', 'POST', '2026-09-16 18:00:00');
