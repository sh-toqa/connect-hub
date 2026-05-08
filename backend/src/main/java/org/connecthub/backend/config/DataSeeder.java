package org.connecthub.backend.config;

import org.connecthub.backend.enums.ContentType;
import org.connecthub.backend.enums.FriendshipStatus;
import org.connecthub.backend.enums.UserStatus;
import org.connecthub.backend.model.Content;
import org.connecthub.backend.model.Friendship;
import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.ContentRepository;
import org.connecthub.backend.repository.FriendshipRepository;
import org.connecthub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository       userRepository;
    private final ContentRepository    contentRepository;
    private final FriendshipRepository friendshipRepository;
    private final PasswordEncoder      passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Database already has data — skipping seed");
            return;
        }

        log.info("Seeding development data...");

        String pw = passwordEncoder.encode("password123");

        // ── 8 Users ───────────────────────────────────────────────────────────
        User alice = save(User.builder()
                .email("alice@example.com").username("alice").hashedPassword(pw)
                .dateOfBirth(LocalDate.of(1998, 3, 15)).status(UserStatus.ONLINE)
                .bio("Hey! I love coding and coffee ☕ | Spring Boot enthusiast").build());

        User bob = save(User.builder()
                .email("bob@example.com").username("bob").hashedPassword(pw)
                .dateOfBirth(LocalDate.of(1995, 7, 22)).status(UserStatus.ONLINE)
                .bio("Software engineer by day, gamer by night 🎮 | Building cool stuff").build());

        User carol = save(User.builder()
                .email("carol@example.com").username("carol").hashedPassword(pw)
                .dateOfBirth(LocalDate.of(2000, 11, 5)).status(UserStatus.ONLINE)
                .bio("UI/UX designer & photographer 📸 | Making things beautiful").build());

        User dave = save(User.builder()
                .email("dave@example.com").username("dave").hashedPassword(pw)
                .dateOfBirth(LocalDate.of(1997, 1, 30)).status(UserStatus.OFFLINE)
                .bio("DevOps engineer ☁️ | Kubernetes, Docker, and too much coffee").build());

        User eve = save(User.builder()
                .email("eve@example.com").username("eve").hashedPassword(pw)
                .dateOfBirth(LocalDate.of(1999, 6, 18)).status(UserStatus.ONLINE)
                .bio("Data scientist 📊 | Python, ML, and basketball 🏀").build());

        User frank = save(User.builder()
                .email("frank@example.com").username("frank").hashedPassword(pw)
                .dateOfBirth(LocalDate.of(1993, 9, 3)).status(UserStatus.OFFLINE)
                .bio("Fullstack dev | React + Spring Boot | Open source contributor").build());

        User grace = save(User.builder()
                .email("grace@example.com").username("grace").hashedPassword(pw)
                .dateOfBirth(LocalDate.of(2001, 4, 25)).status(UserStatus.ONLINE)
                .bio("CS student 👩‍💻 | Learning something new every day").build());

        User henry = save(User.builder()
                .email("henry@example.com").username("henry").hashedPassword(pw)
                .dateOfBirth(LocalDate.of(1996, 12, 8)).status(UserStatus.OFFLINE)
                .bio("Mobile dev | Flutter & React Native | Coffee addict ☕").build());

        // ── Friendships ───────────────────────────────────────────────────────
        // alice's network — friends with bob, carol, eve, frank
        friend(alice, bob);
        friend(alice, carol);
        friend(alice, eve);
        friend(alice, frank);

        // bob's extra connections
        friend(bob, carol);
        friend(bob, dave);
        friend(bob, grace);

        // carol's extra connections
        friend(carol, eve);
        friend(carol, henry);

        // other connections
        friend(dave, eve);
        friend(frank, grace);
        friend(grace, henry);

        // Pending requests to alice
        pending(dave, alice);
        pending(henry, alice);
        pending(grace, alice);

        // ── Posts (spread over past 7 days) ───────────────────────────────────

        // Alice's posts
        post(alice, "Just deployed my first Spring Boot app to production! 🚀 Feeling so proud after 3 weeks of work. The journey was worth it.", daysAgo(6));
        post(alice, "Hot take: writing tests first actually makes you code faster. Fight me 😅 #TDD #CleanCode", daysAgo(5));
        post(alice, "Coffee count today: 4. Lines of code written: 847. Bugs fixed: 3. Bugs introduced: probably more than 3.", daysAgo(4));
        post(alice, "Just learned about Java records and honestly WHERE HAS THIS BEEN ALL MY LIFE? No more boilerplate!", daysAgo(3));
        post(alice, "Finished the friend management feature today 🎉 Pull request is up — who wants to review?", daysAgo(2));
        post(alice, "Sunday coding session hits different with lo-fi music and a good playlist ✨", daysAgo(1));
        post(alice, "Good morning! Starting the week strong with a new feature. Let's gooo 💪", hoursAgo(3));

        // Bob's posts
        post(bob, "Anyone else find that the best ideas come in the shower? Solved a gnarly concurrency bug while shampooing my hair 😂", daysAgo(6));
        post(bob, "Kubernetes just decided to ruin my Thursday. Three hours debugging a networking issue that turned out to be a typo. THREE HOURS.", daysAgo(5));
        post(bob, "Game night recap: we played Catan until 2am, someone flipped the board, friendships were tested 🎲😂", daysAgo(4));
        post(bob, "Just pushed a commit with the message 'please work' and it worked. This is software engineering.", daysAgo(3));
        post(bob, "PostgreSQL query optimization is genuinely satisfying. Went from 4.2s to 180ms with a single index. Chef's kiss 🤌", daysAgo(2));
        post(bob, "Weekend project: built a little CLI tool to track my coffee intake. Turns out I drink 6 cups a day. That's fine. That's totally fine.", daysAgo(1));
        post(bob, "New week, new bugs to fix. At least the coffee is good ☕", hoursAgo(5));

        // Carol's posts
        post(carol, "Design systems are the unsung heroes of frontend development. Two hours setting it up saves you twenty hours of inconsistency later.", daysAgo(6));
        post(carol, "Spent the afternoon in the park with my camera 📸 Golden hour never disappoints. Posted some shots in my story!", daysAgo(5));
        post(carol, "The best UI is one the user doesn't have to think about. The worst UI is one that makes them question their life choices.", daysAgo(4));
        post(carol, "Just redesigned our onboarding flow and user drop-off went down 34%. Data-driven design wins every time 📊", daysAgo(3));
        post(carol, "Figma tip: use components for EVERYTHING. Future you will send past you a thank-you note.", daysAgo(2));
        post(carol, "Weekend photowalk through the old city district. The architecture is stunning 🏛️", daysAgo(1));

        // Dave's posts
        post(dave, "Reminder that 'it works on my machine' is not a deployment strategy 😂 Please write your Dockerfiles properly.", daysAgo(5));
        post(dave, "Just automated our entire CI/CD pipeline. Went from 45 minute deploys to 8 minutes. DevOps is magic.", daysAgo(4));
        post(dave, "Terraform state corruption at 11pm on a Friday. This is the DevOps life. Send help (and pizza).", daysAgo(2));
        post(dave, "Monitoring and observability are not optional. Set up your dashboards before you need them, not after.", daysAgo(1));

        // Eve's posts
        post(eve, "Spent all day training a model that achieved 94% accuracy. Then realized I had a data leak. Back to zero. Machine learning is humbling.", daysAgo(5));
        post(eve, "Pandas tip: avoid loops at all costs. Vectorized operations are 50x faster and your code will be so much cleaner.", daysAgo(4));
        post(eve, "Basketball practice was intense today 🏀 Nothing clears your head like running drills for two hours.", daysAgo(3));
        post(eve, "Just presented my research findings to the team. The Q&A was spicy but productive. I love a good technical debate.", daysAgo(1));

        // Frank's posts
        post(frank, "Open source contribution tip: start with documentation. It's impactful, welcoming, and you learn the codebase fast.", daysAgo(4));
        post(frank, "React 19 features are wild. The new use() hook is going to change how we think about async in components.", daysAgo(3));
        post(frank, "Code review etiquette: review the code, not the coder. Be specific, be kind, be constructive.", daysAgo(2));

        // Grace's posts
        post(grace, "First time contributing to an open source project today! The maintainers were so welcoming 🎉 Small PR but it felt huge.", daysAgo(3));
        post(grace, "Algorithms class is killing me but I solved my first dynamic programming problem today and the feeling is UNMATCHED.", daysAgo(2));
        post(grace, "Study tip that actually works: teach the concept to someone else (or a rubber duck). Forces you to fill your own knowledge gaps.", daysAgo(1));

        // Henry's posts
        post(henry, "Flutter vs React Native in 2025: both are great, pick the one your team knows. There, I saved you 40 minutes of YouTube.", daysAgo(4));
        post(henry, "Mobile performance optimization is a completely different beast from web. Screen size, battery, network — all matter so much more.", daysAgo(2));

        // ── Stories (all recent — within 24h so they're active) ───────────────
        story(alice,  "Working from a café today ☕✨ The vibe is immaculate",       hoursAgo(2));
        story(alice,  "Just had the best avocado toast of my life 🥑",              hoursAgo(1));
        story(bob,    "Game night starting in 1 hour! Who's ready to lose at Catan 🎲", hoursAgo(4));
        story(bob,    "This bug has been haunting me for 3 days. Today is the day I defeat it 🐛⚔️", hoursAgo(1));
        story(carol,  "Golden hour photoshoot in the park 🌅 Content incoming!",    hoursAgo(5));
        story(carol,  "New design system component just dropped — dark mode support added ✨", hoursAgo(2));
        story(eve,    "Model training at 96% accuracy and counting... 🤞",          hoursAgo(3));
        story(grace,  "Library vibes 📚 Exam season grind",                         hoursAgo(6));
        story(frank,  "Just merged my 50th PR on this project 🎉",                  hoursAgo(4));

        log.info("Seeded successfully!");
        log.info("   8 users, 12 friendships, 3 pending requests");
        log.info("   35 posts spread over 7 days");
        log.info("   9 active stories");
        log.info("   All passwords: password123");
        log.info("   Login as alice@example.com to see a full populated feed");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User save(User user) {
        return userRepository.save(user);
    }

    private void friend(User a, User b) {
        friendshipRepository.save(Friendship.builder()
                .requester(a).receiver(b)
                .status(FriendshipStatus.ACCEPTED).build());
    }

    private void pending(User from, User to) {
        friendshipRepository.save(Friendship.builder()
                .requester(from).receiver(to)
                .status(FriendshipStatus.PENDING).build());
    }

    private void post(User author, String text, LocalDateTime timestamp) {
        Content c = Content.builder()
                .author(author)
                .contentText(text)
                .contentType(ContentType.POST)
                .build();
        Content saved = contentRepository.save(c);
        // Manually set timestamp since @CreationTimestamp sets it to now
        saved.setTimestamp(timestamp);
        contentRepository.save(saved);
    }

    private void story(User author, String text, LocalDateTime timestamp) {
        Content c = Content.builder()
                .author(author)
                .contentText(text)
                .contentType(ContentType.STORY)
                .build();
        Content saved = contentRepository.save(c);
        saved.setTimestamp(timestamp);
        contentRepository.save(saved);
    }

    private LocalDateTime daysAgo(int days) {
        return LocalDateTime.now().minusDays(days).minusHours(2);
    }

    private LocalDateTime hoursAgo(int hours) {
        return LocalDateTime.now().minusHours(hours);
    }
}