# ConnectHub — Social Networking Platform


A full-stack social networking web application built with **Spring Boot** and **React**.
Originally implemented as a desktop application in **pure Java with Swing** and a
file-based JSON database, then re-architected to a modern web stack — replacing Swing
with React, file storage with MySQL, and adding a REST API layer with JWT authentication.

## Related Repositories

- Web version (current): https://github.com/sh-toqa/connect-hub
- Desktop Java Swing version: https://github.com/sh-toqa/Lab9-ConnectHub.git

---

## Features

- **User Authentication** — JWT-based register, login, logout with BCrypt password hashing
- **Profile Management** — Edit bio, upload profile photo and cover photo, change password
- **Posts & Stories** — Create text/image posts (permanent) and stories (auto-expire after 24h)
- **Newsfeed** — Three-column layout showing friend posts, active stories, and suggestions
- **Friend Management** — Send/accept/decline friend requests, remove friends, block users
- **Online/Offline Status** — Real-time friend status visible across all pages
- **Content Viewer** — Click any post or story to open a full-screen modal
- **User Profiles** — Click any username to visit their public profile

---

## Tech Stack

### Backend
| Technology | Purpose |
|---|---|
| Java 17 + Spring Boot 3 | REST API framework |
| Spring Security + JWT | Authentication & authorization |
| Spring Data JPA + Hibernate | ORM and database access |
| MySQL | Primary database |
| H2 (in-memory) | Test database |
| BCrypt | Password hashing |
| Maven | Build tool |

### Frontend
| Technology | Purpose |
|---|---|
| React 19 | UI framework |
| React Router v6 | Client-side routing |
| Axios | HTTP client |
| Vite | Build tool & dev server |
| Vitest + Testing Library | Frontend testing |

### Testing
| Tool | Purpose |
|---|---|
| JUnit 5 | Unit & integration tests |
| Mockito | Mocking dependencies |
| MockMvc | HTTP layer testing |
| Vitest | Frontend unit tests |
| axios-mock-adapter | API mocking |

---

## Architecture

The project follows a strict **layered architecture**:

```
React SPA (port 5173)
      ↕  HTTP/JSON
Spring Boot REST API (port 8080)
      ↕
MySQL Database
```

### Backend layers
```
Controller  →  Service  →  Repository  →  Database
```

Each layer has a single responsibility. Controllers handle HTTP, services contain business logic, repositories handle data access. Entities never leave the service layer — only DTOs are returned to controllers.

### Design patterns used
- **Builder** — All entities use Lombok `@Builder`
- **Facade** — Service classes hide multi-repository coordination
- **DTO** — `UserDto`, `ContentDto`, `FriendshipDto` prevent entity exposure
- **Mapper** — `UserMapper` centralises all entity → DTO conversions
- **Strategy** — `StorageService` interface abstracts file storage (swap to S3 without changing any other class)
- **Singleton** — All Spring beans (`@Service`, `@Component`) are singletons
- **Custom Hook** — `useProfile`, `useContent`, `useFriends` encapsulate React state + API calls
- **Context** — `AuthContext` provides global auth state without prop drilling
- **Guard** — `PrivateRoutes` / `PublicRoutes` protect routes based on auth state

---

## Project Structure

```
connecthub/
├── backend/
│   ├── src/main/java/org/connecthub/backend/
│   │   ├── controller/        AuthController, ProfileController,
│   │   │                      ContentController, FriendshipController
│   │   ├── service/           AuthService, ProfileService,
│   │   │                      ContentService, FriendshipService,
│   │   │                      CustomUserDetailsService,
│   │   │                      StorageService (interface), LocalStorageService
│   │   ├── repository/        UserRepository, ContentRepository, FriendshipRepository
│   │   ├── model/             User, Content, Friendship
│   │   ├── enums/             ContentType, FriendshipStatus, OnlineStatus
│   │   ├── dto/
│   │   │   ├── request/       RegisterRequest, LoginRequest, UpdateProfileRequest ...
│   │   │   └── response/      UserDto, ContentDto, FriendshipDto, LoginResponse ...
│   │   ├── mapper/            UserMapper
│   │   ├── security/          SecurityConfig, JwtUtil, JwtAuthFilter
│   │   ├── exception/         GlobalExceptionHandler, ResourceNotFoundException ...
│   │   └── config/            WebMvcConfig, DataSeeder
│   └── src/test/java/
│       ├── Unit/
│       │   ├── service/       ProfileServiceTest, ContentServiceTest,
│       │   │                  FriendshipServiceTest, LocalStorageServiceTest,
│       │   │                  UserServiceTest
│       │   └── controller/    ProfileControllerTest,
│       ├── Integration/       AuthControllerIntegrationTest, ContentControllerIntegrationTest,
│       │                      FriendshipControllerIntegrationTest, ProfileControllerIntegrationTest
│       └── System/            SystemTest
│
└── frontend/
    └── src/
        ├── api/               authApi.js, profileApi.js, contentApi.js, friendApi.js ...
        ├── context/           AuthContext.jsx
        ├── hooks/             useProfile.js, useContent.js, useFriends.js
        ├── pages/             LoginPage, SignupPage, ProfilePage, NewsfeedPage,
        │                      FriendsPage, UserProfilePage, NotFoundPage
        ├── styles/            global.css
        └── components/
            ├── common/        Navbar, AuthLayout, ProtectedRoute, GuestRoute
            ├── profile/       ProfileAvatar, CoverPhoto, PostCard,
            │                  FriendsList, EditProfileModal
            ├── content/       CreatePostForm, StoryStrip, ContentModal,
            │                  NewsfeedLeftSidebar, NewsfeedRightSidebar
            └── friends/       FriendCard, FriendRequestCard, SuggestionCard
      
```

---

## API Endpoints

### Auth — `/auth`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/register` | Public | Create new account |
| POST | `/login` | Public | Login, returns JWT |
| POST | `/logout` | JWT | Logout, sets status OFFLINE |

### Profile — `/profile`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/` | JWT | Get own profile |
| GET | `/{userId}` | JWT | Get any user's profile |
| PATCH | `/` | JWT | Update bio |
| POST | `/photo` | JWT | Upload profile photo |
| POST | `/cover` | JWT | Upload cover photo |
| PATCH | `/password` | JWT | Change password |
| GET | `/posts` | JWT | Get own posts (paginated) |
| GET | `/friends` | JWT | Get friends list |
| GET | `/stories` | JWT | Get own stories |
| GET | `/{userId}/posts` | JWT | Get another user's posts |
| GET | `/{userId}/stories` | JWT | Get another user's active stories |

### Content — `/content`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/posts` | JWT | Create a post |
| POST | `/stories` | JWT | Create a story |
| DELETE | `/{contentId}` | JWT | Delete own content |
| GET | `/feed/posts` | JWT | Friend posts (paginated) |
| GET | `/feed/stories` | JWT | Active friend stories |

### Friends — `/friends`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/request/{receiverId}` | JWT | Send friend request |
| POST | `/{friendshipId}/accept` | JWT | Accept request |
| DELETE | `/{friendshipId}/decline` | JWT | Decline request |
| DELETE | `/{friendshipId}` | JWT | Remove friend |
| POST | `/block/{targetId}` | JWT | Block user |
| DELETE | `/block/{targetId}` | JWT | Unblock user |
| GET | `/requests` | JWT | Pending received requests |
| GET | `/` | JWT | Accepted friends list |
| GET | `/suggestions` | JWT | Friend suggestions |
| GET | `/status/{otherUserId}` | JWT | Relationship status |

---

## Getting Started

### Prerequisites
- Java 17+
- Node.js 22+
- MySQL 9+
- Maven 3.9+

### Backend setup

```bash
# 1. Clone the repo
git clone https://github.com/sh-toqa/connect-hub.git
cd connecthub/backend

# 2. Create the database
mysql -u root -p -e "CREATE DATABASE connecthub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. Configure application.properties
# Edit src/main/resources/application.properties:
# spring.datasource.url=jdbc:mysql://localhost:3306/connecthub?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
# spring.datasource.username=${SQL_USERNAME}
# spring.datasource.password=${SQL_PASSWORD}
# app.jwt.secret=MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=

# 4. Run
mvn spring-boot:run

# API runs at http://localhost:8080
```

### Frontend setup

```bash
cd connecthub/frontend

npm install
npm run dev

# App runs at http://localhost:5173
```

### Development data (optional)

Add `spring.profiles.active=dev` to `application.properties` and set `spring.jpa.hibernate.ddl-auto=create-drop` to seed the database with 8 users, 35 posts, 9 stories, and 12 friendships on every restart.

Default credentials for all seeded users: `password123`

---

## Running Tests

```bash
# Backend — all tests
cd backend
mvn test

# Backend — specific test class
mvn test -Dtest=AuthControllerTest
mvn test -Dtest=SystemTest

# Frontend
cd frontend
npm test
```

### Test coverage

| Layer | Type | Tests |
|---|---|---|
| Service | Unit (Mockito) | ProfileService, ContentService, FriendshipService, LocalStorageService |
| Controller | Integration (MockMvc + H2) | Auth, Profile, Content, Friendship |
| API | System (full journey) | 8 end-to-end user workflows |
| Frontend API | Unit (Vitest + axios-mock) | authApi, profileApi, contentApi |
| Frontend UI | Unit (Testing Library) | LoginForm, SignupForm, ProfilePage |

---

## SDLC Followed

This project was developed following the full Software Development Lifecycle:

1. **Requirements** — SRS document with 40+ functional requirements and 27 non-functional requirements across performance, security, usability, reliability, and scalability categories
2. **Design** — UML diagrams (Use Case, Class, Sequence, Activity, State Machine), layered architecture design, API contract definition
3. **Implementation** — Feature branches, pull request reviews, incremental delivery across 4 feature modules
4. **Testing** — Unit tests, integration tests, and system tests covering the complete backend; frontend unit tests with Vitest
5. **Maintenance** — Documented future improvements and scalability paths

---

## Future Improvements

- **Real-time messaging** — WebSocket/STOMP chat between friends
- **Push notifications** — Friend requests, new posts from friends
- **Cloud storage** — Swap `LocalStorageService` for `S3StorageService` (interface already in place)
- **OAuth login** — Google/GitHub sign-in
- **Mobile app** — React Native client consuming the same REST API
- **Containerization** — Docker Compose setup for one-command local deployment
- **Admin dashboard** — Content moderation, user management

---
