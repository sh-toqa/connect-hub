CREATE TABLE `users` (
  `user_id` binary(16) NOT NULL,
  `date_of_birth` date NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `bio` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_photo_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `profile_photo_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hashed_password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('OFFLINE','ONLINE') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `friendships` (
  `friendship_id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `receiver_id` binary(16) NOT NULL,
  `requester_id` binary(16) NOT NULL,
  `status` enum('ACCEPTED','BLOCKED','PENDING') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`friendship_id`),
  KEY `idx_friendship_requester` (`requester_id`),
  KEY `idx_friendship_receiver` (`receiver_id`),
  KEY `idx_friendship_status` (`status`),
  CONSTRAINT `FKas6bp8so5n3pfcqtfxt72e1ii` FOREIGN KEY (`requester_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKpk7w2cj6m9n224ny2t7fhi47` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `contents` (
  `content_id` binary(16) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `author_id` binary(16) NOT NULL,
  `image_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content_text` text COLLATE utf8mb4_unicode_ci,
  `content_type` enum('POST','STORY') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`content_id`),
  KEY `idx_content_author` (`author_id`),
  KEY `idx_content_type_timestamp` (`content_type`,`timestamp`),
  CONSTRAINT `FKf03bx3y4bjiboyr4gbor0g5n` FOREIGN KEY (`author_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
