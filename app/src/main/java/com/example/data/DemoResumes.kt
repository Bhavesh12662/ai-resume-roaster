package com.example.data

object DemoResumes {
    val items = listOf(
        DemoResumeItem(
            title = "Fresher - Frontend Dev",
            description = "Objective statement copied from Google, standard simple projects, and no metrics.",
            content = """
                JOHN DOE
                Email: john.doe@email.com | Phone: 123-456-7890 | GitHub: github.com/johndoe
                
                OBJECTIVE:
                To secure a challenging position in a reputable organization to utilize my skills and contribute to the growth of the organization. Looking for an entry-level software developer opportunity.
                
                SKILLS:
                HTML, CSS, JavaScript, React (basic), MS Office, Teamwork, Communication, Problem Solving.
                
                PROJECTS:
                - Todo List App: Created a fully functional todo list using HTML, CSS and JS. Users can add and delete tasks.
                - Weather Widget: A widget that displays the weather in real time by calling an API. Built using React.
                - Personal Portfolio: A simple website to show my details and bio.
                
                EDUCATION:
                Bachelor of Technology in Computer Science
                XYZ College of Engineering (Graduated: 2025)
                CGPA: 7.2 / 10
                
                COURSES & EXTRA-CURRICULAR:
                - Web Development Course on Udemy
                - Member of College Technical Club
            """.trimIndent()
        ),
        DemoResumeItem(
            title = "Experienced Backend Dev",
            description = "Solid skill set, but uses passive 'Responsible for' bullets and has zero measurable impact metrics.",
            content = """
                ALICE SMITH
                Email: alice.smith@techmail.com | LinkedIn: linkedin.com/in/alicesmith | Portfolio: alicesmith.dev
                
                SUMMARY:
                Experienced Software Engineer with over 4 years of experience specializing in Java, Spring Boot, and database management. Proven track record of working in agile teams to deliver robust backend solutions.
                
                SKILLS:
                Java, Kotlin, Spring Boot, Microservices, PostgreSQL, MongoDB, Docker, Git, REST APIs, JUnit.
                
                EXPERIENCE:
                Senior Backend Developer | InnovateTech (2022 - Present)
                - Responsible for maintaining and writing backend APIs using Spring Boot.
                - Handled database optimizations and migrated several tables.
                - Participated in daily standups and sprint planning sessions.
                - Helped junior engineers learn Java and Kotlin development.
                - Fixed bugs and resolved critical customer issues in production.
                
                Software Engineer | DevCorp (2020 - 2022)
                - Developed backend endpoints for e-commerce clients.
                - Maintained existing codebase and resolved QA bug reports.
                - Worked with PostgreSQL databases and wrote custom SQL queries.
                
                EDUCATION:
                B.S. in Computer Science
                State University (2016 - 2020)
                
                PROJECTS:
                - E-commerce Gateway: Built a payment gateway integration using Spring Boot.
                - Notification Dispatcher: A service to send emails and SMS alerts automatically.
            """.trimIndent()
        ),
        DemoResumeItem(
            title = "Career Switcher - Marketer",
            description = "Strong achievements in marketing, but completely lacks technical keywords for a software role.",
            content = """
                ROBERT JOHNSON
                Email: robert.j@marketingpros.net | Phone: 555-019-2831 | GitHub: github.com/robj-learning
                
                PROFESSIONAL SUMMARY:
                High-achieving Marketing Manager with 5+ years of experience leading cross-functional teams, creating digital campaigns, and driving brand engagement. Successfully transition-ready and looking for a Full-Stack Developer position to merge my analytical skills with modern coding practices.
                
                CORE SKILLS:
                Brand Strategy, SEO, Digital Campaigns, Google Analytics, HTML/CSS (basic), Python (learning), Git (basic), Team Leadership, Project Management.
                
                EXPERIENCE:
                Digital Marketing Lead | GrowthSpark Agency (2021 - Present)
                - Directed a team of 6 marketers to design and execute digital campaigns.
                - Managed a ${'$'}120,000 annual advertising budget.
                - Improved organic website search traffic by implementing SEO best practices.
                - Compiled monthly reports for executive stakeholders.
                
                Marketing Specialist | BrandFlow Co. (2019 - 2021)
                - Created content for social media and managed customer newsletter lists.
                - Analyzed email campaign click-through rates.
                
                EDUCATION:
                Bachelor of Arts in Communications
                Media Institute of Chicago (2015 - 2019)
                
                CODING EDUCATION (Self-Taught):
                - Completed Harvard's CS50x (Introduction to Computer Science)
                - Full Stack Bootcamp Certificate of Completion (2025)
            """.trimIndent()
        )
    )
}

data class DemoResumeItem(
    val title: String,
    val description: String,
    val content: String
)
