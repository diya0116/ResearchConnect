from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.text import WD_BREAK
from docx.enum.section import WD_ORIENTATION
from pathlib import Path

OUT = Path('ResearchConnect_Complete_Project_Report.docx').resolve()
doc = Document()
sec = doc.sections[0]
sec.top_margin = Inches(0.75); sec.bottom_margin = Inches(0.75)
sec.left_margin = Inches(0.85); sec.right_margin = Inches(0.85)

styles = doc.styles
styles['Normal'].font.name = 'Times New Roman'; styles['Normal']._element.rPr.rFonts.set(qn('w:eastAsia'), 'Times New Roman')
styles['Normal'].font.size = Pt(11)
styles['Normal'].paragraph_format.space_after = Pt(6)
styles['Normal'].paragraph_format.line_spacing = 1.15
for name, size in [('Title', 20), ('Heading 1', 15), ('Heading 2', 13), ('Heading 3', 11)]:
    st = styles[name]; st.font.name = 'Times New Roman'; st._element.rPr.rFonts.set(qn('w:eastAsia'), 'Times New Roman')
    st.font.size = Pt(size); st.font.bold = True; st.font.color.rgb = RGBColor(0,0,0)
    st.paragraph_format.space_before = Pt(12); st.paragraph_format.space_after = Pt(6)
styles['Title'].paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
code = styles.add_style('Code Block', WD_STYLE_TYPE.PARAGRAPH)
code.font.name='Consolas'; code._element.rPr.rFonts.set(qn('w:eastAsia'), 'Consolas'); code.font.size=Pt(8.5)
code.paragraph_format.left_indent=Inches(.25); code.paragraph_format.space_after=Pt(5)

def shade(cell, fill):
    tcPr=cell._tc.get_or_add_tcPr(); shd=OxmlElement('w:shd'); shd.set(qn('w:fill'),fill); tcPr.append(shd)
def set_cell(cell, text, bold=False):
    cell.text=''; p=cell.paragraphs[0]; r=p.add_run(str(text)); r.bold=bold; r.font.name='Times New Roman'; r.font.size=Pt(9); cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
def table(headers, rows, widths=None):
    t=doc.add_table(rows=1, cols=len(headers)); t.alignment=WD_TABLE_ALIGNMENT.CENTER; t.style='Table Grid'
    for i,h in enumerate(headers): set_cell(t.rows[0].cells[i],h,True); shade(t.rows[0].cells[i],'1F4E78');
    for c in t.rows[0].cells:
        for r in c.paragraphs[0].runs: r.font.color.rgb=RGBColor(255,255,255)
    for ri,row in enumerate(rows):
        cells=t.add_row().cells
        for i,v in enumerate(row):
            set_cell(cells[i],v)
            if ri%2==1: shade(cells[i],'EAF2F8')
    if widths:
        for row in t.rows:
            for i,w in enumerate(widths): row.cells[i].width=Inches(w)
    doc.add_paragraph()
    return t
def para(text='', boldlead=None):
    p=doc.add_paragraph()
    if boldlead and text.startswith(boldlead):
        p.add_run(boldlead).bold=True; p.add_run(text[len(boldlead):])
    else: p.add_run(text)
    return p
def bullets(items):
    for x in items: doc.add_paragraph(x, style='List Bullet')
def codeblock(text):
    for line in text.strip('\n').split('\n'): doc.add_paragraph(line, style='Code Block')
def pagebreak(): doc.add_page_break()
def heading(s, level=1): doc.add_heading(s,level=level)

# Cover
for _ in range(3): doc.add_paragraph()
p=doc.add_paragraph(); p.alignment=WD_ALIGN_PARAGRAPH.CENTER; r=p.add_run('JAVA MINI PROJECT REPORT'); r.bold=True; r.font.size=Pt(16)
doc.add_paragraph()
p=doc.add_paragraph(style='Title'); p.add_run('ResearchConnect')
p=doc.add_paragraph(); p.alignment=WD_ALIGN_PARAGRAPH.CENTER; r=p.add_run('Academic Research Collaboration Platform'); r.bold=True; r.font.size=Pt(14)
doc.add_paragraph(); doc.add_paragraph()
for label,value in [('Submitted in partial fulfillment of the requirements for', 'Java Mini Project'),('Prepared by', '____________________________'),('Roll Number', '____________________________'),('Department', '____________________________'),('Institution', 'Thapar Institute of Engineering and Technology'),('Academic Session', '2026-27')]:
    p=doc.add_paragraph(); p.alignment=WD_ALIGN_PARAGRAPH.CENTER; p.add_run(label+'\n').bold=True; p.add_run(value)
doc.add_paragraph(); p=doc.add_paragraph();p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.add_run('Project Guide: ____________________________').bold=True
pagebreak()

heading('Declaration',1)
para('This report presents the design and current implementation status of ResearchConnect, an academic research collaboration platform. The implemented baseline covers Phases 1 to 3. Phases 4 to 9 are documented as the proposed completion scope so that the complete target system, its architecture, data model, workflows and interfaces are clear. Planned features are not represented as completed source-code modules or test results.')
heading('Acknowledgement',1)
para('The authors acknowledge the guidance of the project mentor, faculty members and peers whose feedback helped shape the requirements for a secure academic collaboration platform.')
pagebreak()

heading('Abstract',1)
para('ResearchConnect is a web-based platform intended to help students and professors discover, publish and manage academic research opportunities within a verified institutional community. The platform uses a Spring Boot backend, Firebase Authentication for Google sign-in, Cloud Firestore for persistence, and Thymeleaf/JavaScript pages for the browser interface. A verified @thapar.edu account is required before protected operations can be performed.')
para('The current implementation provides authentication and profile onboarding, a searchable project feed, project creation and project ownership-based management. The complete proposed system further introduces collaboration applications, research team management, richer academic profiles, a researcher directory, in-app notifications, project milestones and tasks, and a consolidated testing/security/deployment phase. The design keeps the backend stateless, verifies Firebase ID tokens at the server boundary, uses service and repository layers for separation of concerns, and stores document-oriented records in Firestore. This report distinguishes the implemented baseline from proposed modules while describing their intended integration into one coherent system.')
heading('Keywords',2); para('Spring Boot, Java, Firebase Authentication, Cloud Firestore, REST API, research collaboration, CRUD, role-based authorization.')
pagebreak()

heading('Table of Contents',1)
toc=[('Abstract','1'),('Chapter 1 Introduction','2'),('1.1 Project Introduction','2'),('1.2 Problem Statement','2'),('1.3 Objectives','3'),('1.4 Scope and Phase Status','3'),('Chapter 2 System Design','4'),('2.1 Architecture','4'),('2.2 Use Case Diagram','5'),('2.3 Class Diagram','6'),('2.4 Activity Diagram','7'),('2.5 Database and ER Design','8'),('2.6 Module Description','10'),('Chapter 3 Implementation','14'),('3.1 Technologies and Tools','14'),('3.2 Project Structure','15'),('3.3 Existing Java Classes and Interfaces','16'),('3.4 Proposed Extension Design','18'),('3.5 Security, Exceptions, Collections and CRUD','21'),('3.6 Important Code Extracts','23'),('Chapter 4 Results and Screenshots','25'),('References','27')]
table(['Section','Page'],toc,[5.5,1])
pagebreak()

heading('Chapter 1 Introduction',1)
heading('1.1 Project Introduction',2)
para('Research opportunities are often communicated through informal messages, notice boards or isolated faculty contacts. This makes it difficult for students to find suitable work and difficult for project owners to identify interested collaborators. ResearchConnect centralizes this interaction in an authenticated institutional platform. Users create an academic profile, browse published projects, filter projects by area, department and publisher role, and manage the projects that they own.')
para('The target system is organized in nine incremental phases. Phases 1 to 3 constitute the current coded baseline. The remaining phases define the planned pathway from a project-discovery application to a controlled research-team workspace.')
heading('1.2 Problem Statement',2)
para('There is no single structured channel through which verified students and professors can publish research requirements, evaluate requests to join, form teams, expose suitable academic expertise and track work after a team is formed. Manual processes provide limited visibility, poor status tracking and weak auditability of who owns a project or who has permission to change it.')
heading('1.3 Objectives',2)
bullets(['Authenticate only verified institutional users and maintain Student and Professor profiles.', 'Enable publication, discovery, filtering, viewing, editing and deletion of research projects with ownership protection.', 'Provide a controlled join-request workflow with capacity and duplicate-request checks.', 'Maintain a visible research team and project lifecycle once applicants are accepted.', 'Support discovery through public academic profiles and a searchable researcher directory.', 'Notify affected users of application and project events.', 'Track milestones, tasks, assignees and completion progress.', 'Apply validation, error handling, authorization, testing and deployment practices suitable for a Spring Boot application.'])
heading('1.4 Scope and Phase Status',2)
table(['Phase','Scope','Status represented in this report'],[
['1','Authentication and user profiles','Implemented baseline'],['2','Project feed and creation','Implemented baseline'],['3','Project management','Implemented baseline'],['4','Collaboration and join requests','Proposed design'],['5','Research team management','Proposed design'],['6','Profiles and researcher directory','Proposed design'],['7','In-app notifications','Proposed design'],['8','Project lifecycle and tasks','Proposed design'],['9','Testing, security and deployment','Proposed completion work']], [0.5,3.0,2.5])
para('Out of scope: chatbots, AI recommendations, social-media functions, payment workflows and unrelated modules. The proposed scope remains focused on academic research collaboration.')
pagebreak()

heading('Chapter 2 System Design',1)
heading('2.1 System Architecture',2)
para('ResearchConnect follows a layered web architecture. The browser renders Thymeleaf templates and JavaScript calls REST endpoints with a Firebase ID token in the Authorization header. Spring Security invokes a Firebase authentication filter, which verifies the token and the verified institutional email. Controllers expose HTTP resources, services apply business rules, repositories isolate Firestore access, and Firestore stores document collections.')
codeblock('''Browser UI (Thymeleaf, CSS, JavaScript)
        | Firebase Google sign-in and Bearer ID token
        v
Spring Boot Web Layer: PageController | AuthController | ProjectController
        v
Spring Security + FirebaseAuthenticationFilter
        v
Service Layer: UserService | ProjectService | proposed domain services
        v
Repository Layer: FirestoreUserRepository | FirestoreProjectRepository | proposed repositories
        v
Firebase Admin SDK --------------------> Cloud Firestore
Firebase Authentication <--------------- Google identity provider''')
para('All planned services use the same boundary: an authenticated FirebaseUserPrincipal supplies the UID, and user-controlled request bodies must never determine ownership, membership permissions or notification recipients.')
heading('2.2 Use Case Diagram',2)
codeblock('''Actors: Student, Professor, Project Owner (Student or Professor), Firebase

Student/Professor --> Sign in with Google --> Complete Profile
Student/Professor --> Explore, search and filter projects --> View project details
Student/Professor --> Create project --> Edit/Delete own project
Student --> Submit join application --> Track application status
Project Owner --> View applications --> Accept/Reject application
Project Owner --> View/manage team --> Update project status
User --> Update public profile --> Search researcher directory
User --> View/mark notifications read
Project Owner --> Create/manage milestones and tasks --> Assign tasks
Firebase --> Verify identity token; Firestore --> Persist application data''')
heading('2.3 Class Diagram',2)
codeblock('''Implemented
UserProfile(uid, email, fullName, role, department, description, cohort, ...)
Project(projectId, title, category, requiredSkills: List<String>, ownerId, status, ...)
ProjectController --> ProjectService --> FirestoreProjectRepository
AuthController -----> UserService -----> FirestoreUserRepository
FirebaseAuthenticationFilter --> FirebaseUserPrincipal

Proposed
JoinApplication(applicationId, projectId, applicantId, status, motivation, ...)
ProjectMember(memberId, projectId, userId, role, joinedAt)
Notification(notificationId, recipientId, type, read, payload, ...)
Milestone(milestoneId, projectId, title, dueDate, status)
ProjectTask(taskId, projectId, assigneeId, status, dueDate)

ApplicationService implements ApplicationWorkflow
TeamService implements TeamManagement
NotificationService implements NotificationPublisher
ProjectLifecycleService coordinates Milestone and ProjectTask''')
heading('2.4 Activity Diagram Join Workflow Proposed Phase 4',2)
codeblock('''Start -> Authenticated student opens project -> Is project OPEN and capacity available?
  No -> Show unavailable message -> End
  Yes -> Has student already applied or joined?
      Yes -> Show existing status -> End
      No -> Enter motivation/skills -> Validate -> Create PENDING application
          -> Notify project owner -> Student sees Pending -> End

Owner opens application -> Verify owner UID -> Accept or reject?
  Reject -> mark REJECTED -> notify applicant -> End
  Accept -> Firestore transaction: recheck capacity, mark ACCEPTED, create member,
             decrement availability -> notify applicant -> End''')
heading('2.5 Database Design and ER Diagram',2)
para('Firestore is document-oriented, so relationships are stored through stable identifiers rather than database joins. Denormalized display fields such as ownerName may be retained to make reads efficient; the UID remains the authorization source of truth.')
codeblock('''users (uid PK) 1 ---- owns ---- * projects (projectId PK)
users (uid PK) 1 ---- submits --- * join_applications (applicationId PK) * --- for --- 1 projects
projects 1 ---- has ---- * project_members * ---- references ---- 1 users
users 1 ---- receives ---- * notifications
projects 1 ---- has ---- * milestones
projects 1 ---- has ---- * project_tasks * ---- assigned to ---- 0..1 users''')
table(['Collection','Key fields','Purpose'],[
['users','uid, email, role, department, description, cohort; proposed interests, skills, academicYear','Identity and public academic profile'],
['projects','projectId, ownerId, title, requiredSkills[], teamSize, availablePositions, status, createdAt','Published project and lifecycle summary'],
['join_applications (proposed)','applicationId, projectId, applicantId, status, motivation, submittedAt, decidedAt','One application per applicant/project'],
['project_members (proposed)','memberId, projectId, userId, memberRole, joinedAt','Accepted team membership'],
['notifications (proposed)','notificationId, recipientId, type, read, referenceId, createdAt','User event inbox'],
['milestones (proposed)','milestoneId, projectId, title, dueDate, status','Progress checkpoints'],
['project_tasks (proposed)','taskId, projectId, assigneeId, title, status, dueDate','Assigned work items']], [1.35,3.0,2.2])
para('Recommended Firestore constraints: use deterministic application IDs such as projectId_applicantId or a transaction-backed uniqueness check; use a transaction when accepting an application; query projects by ownerId and notifications by recipientId/read; and enforce security rules for every new collection, since Admin SDK server calls bypass client rules.')
pagebreak()

heading('2.6 Module Description',2)
for title, body in [
('Phase 1 Authentication and User Profiles Implemented','Firebase browser authentication performs Google sign-in. The browser sends a bearer token; FirebaseAuthenticationFilter verifies it with Firebase Admin SDK and permits only verified @thapar.edu accounts. AuthController exposes GET /api/auth/me and POST /api/auth/profile. UserService creates a UserProfile in users, with role, department, description and cohort. Roles are selected during onboarding, not inferred from email.'),
('Phase 2 Project Feed and Project Creation Implemented','ProjectController exposes GET /api/projects and POST /api/projects. ProjectService creates a UUID project, obtains the owner from the verified principal/profile, validates team size, positions and weekly commitment, then persists it through FirestoreProjectRepository. The feed supports text search over title, descriptions and requiredSkills, plus department, category and owner-role filters. Results are ordered newest first.'),
('Phase 3 Project Management Implemented','GET /api/projects/mine lists projects with ownerId equal to the authenticated UID. PUT and DELETE /api/projects/{projectId} call requireOwnedProject before any modification. Immutable identity fields, owner fields and createdAt are preserved during updates. GET /api/projects/{projectId}/member-count currently reads the project_members collection; it supports the displayed capacity count but does not implement application or team management screens.'),
('Phase 4 Research Collaboration and Join Requests Proposed','ApplicationController, ApplicationService and FirestoreApplicationRepository will accept a JoinApplicationRequest from a student. The service will validate project status, capacity, ownership and duplicate applications. Owners will retrieve project applications and decide them through authorization-protected endpoints. Acceptance will atomically create a ProjectMember and update availability; rejection preserves an auditable decision status.'),
('Phase 5 Research Team Management Proposed','TeamController and TeamService will show accepted members on project details, including name, department and member role. Only the project owner may remove a member. The system will adjust availability safely and permit status changes Open, In Progress and Completed. Membership relationship records will remain separate from project documents to support querying and avoid uncontrolled embedded-array growth.'),
('Phase 6 Enhanced Profiles and Researcher Directory Proposed','ProfileController will add controlled profile updates, and DirectoryController will provide filtered public-profile discovery. Proposed profile fields are researchInterests[], technicalSkills[], expertiseAreas[], academicYear and background. Sensitive identity/authentication data is excluded from public responses using a PublicProfileResponse DTO.'),
('Phase 7 In App Notifications Proposed','NotificationService will create recipient-specific notification documents after application submission, decisions, member removal, task assignment and significant project updates. NotificationController will list notifications, return unread count and mark a notification read. Notification creation remains a backend service responsibility so clients cannot forge notifications for other users.'),
('Phase 8 Project Lifecycle and Management Proposed','MilestoneService and TaskService will implement project progress. Owners create/update/delete milestones and tasks; tasks may be assigned only to accepted members. A status enum PENDING, IN_PROGRESS and COMPLETED supports progress aggregation. Project status and progress summaries will be returned in project-detail responses.'),
('Phase 9 Testing Security and Deployment Proposed','The completion phase will add validation and negative-path tests, service/controller tests, Firestore emulator or integration-test configuration, tightened Firestore rules, environment-based secrets, logging review, responsive UI review and a deployment guide. It is a planned quality phase, not a claim that the proposed modules have been tested.')]:
    heading(title,3); para(body)
pagebreak()

heading('Chapter 3 Implementation',1)
heading('3.1 Technologies and Tools',2)
table(['Technology','Use in ResearchConnect'],[
['Java 21','Programming language and record-based domain DTOs/entities'],['Spring Boot 3.5.7','Web application bootstrap and dependency injection'],['Spring Web','REST controllers and HTTP response handling'],['Spring Security','Stateless request authorization and security filter chain'],['Spring Validation','Jakarta validation of incoming request DTOs'],['Thymeleaf','Server-rendered page templates'],['Firebase Authentication','Google sign-in and ID token identity'],['Firebase Admin SDK 9.7.0','Backend token verification and Firestore access'],['Cloud Firestore','Document database for profiles and projects'],['Maven Wrapper','Build and dependency execution'],['JUnit / Spring Boot Test','Existing repository integration test scaffold']], [2.0,4.6])
heading('3.2 Existing Project Structure',2)
codeblock('''src/main/java/com/researchconnect/
  config/       FirebaseConfig, FirebaseProperties, SecurityConfig
  controller/   AuthController, PageController, ProjectController
  dto/          ProfileRequest, ProjectRequest
  entity/       UserProfile, Project
  enums/        UserRole, ProjectStatus
  exception/    ApiExceptionHandler
  repository/   FirestoreUserRepository, FirestoreProjectRepository
  security/     FirebaseAuthenticationFilter, FirebaseUserPrincipal
  service/      UserService, ProjectService
src/main/resources/
  templates/    login, onboarding, app, create-project, my-projects, project-details
  static/       JavaScript and site CSS
  application.yml
firestore.rules
src/test/java/com/researchconnect/FirestoreProjectRepositoryTest.java''')
heading('3.3 Java Classes, Interfaces and OOP',2)
para('The implemented domain model uses Java records, which provide immutable data carriers for Project, UserProfile, ProjectRequest and ProfileRequest. Encapsulation is achieved by keeping persistence operations in repository classes and business rules in services instead of controllers. The controllers depend on services, and services depend on repositories through constructor injection; this reduces coupling and supports later replacement or testing of repository implementations.')
table(['OOP concept','Application in current or proposed system'],[
['Encapsulation','Private service/repository dependencies; request DTOs separate external input from domain persistence; owner fields are server-derived.'],['Abstraction','Controller-service-repository layering; proposed interfaces ApplicationWorkflow, TeamManagement and NotificationPublisher hide service implementation details.'],['Inheritance','Spring classes such as OncePerRequestFilter provide framework inheritance; custom domain models favor composition because user roles differ by permissions, not object state.'],['Polymorphism','Future NotificationPublisher implementations can deliver in-app events or other approved channels without changing application workflow code.'],['Interfaces','Proposed service contracts enable mock implementations and clear module boundaries.'],['Records','Immutable Java records represent requests and persisted value objects with concise syntax.']], [1.3,5.3])
heading('3.4 Existing REST API',2)
table(['Method','Endpoint','Purpose','Status'],[
['GET','/api/auth/me','Return onboarding state/current profile','Implemented'],['POST','/api/auth/profile','Create first profile','Implemented'],['GET','/api/projects','Search/filter project feed','Implemented'],['GET','/api/projects/{projectId}','Read project details','Implemented'],['POST','/api/projects','Create project from authenticated profile','Implemented'],['GET','/api/projects/mine','List authenticated owner projects','Implemented'],['PUT','/api/projects/{projectId}','Update owner project','Implemented'],['DELETE','/api/projects/{projectId}','Delete owner project','Implemented'],['GET','/api/projects/{projectId}/member-count','Read count from project_members','Implemented endpoint']], [0.65,2.15,2.9,1.0])
heading('3.5 Proposed Classes, Interfaces and REST API',2)
codeblock('''public interface ApplicationWorkflow {
    JoinApplication submit(String applicantId, String projectId, JoinApplicationRequest request);
    JoinApplication decide(String ownerId, String applicationId, ApplicationDecision decision);
}
public interface NotificationPublisher {
    void publish(String recipientId, NotificationType type, String referenceId, String message);
}
public interface TeamManagement {
    List<ProjectMember> members(String projectId);
    void removeMember(String ownerId, String projectId, String memberId);
}''')
table(['Method','Proposed endpoint','Responsible module'],[
['POST','/api/projects/{id}/applications','Submit request'],['GET','/api/projects/{id}/applications','Owner view applications'],['PATCH','/api/applications/{id}/decision','Owner accepts/rejects'],['GET','/api/projects/{id}/members','View team'],['DELETE','/api/projects/{id}/members/{memberId}','Owner remove member'],['PATCH','/api/projects/{id}/status','Owner lifecycle status'],['PATCH','/api/profiles/me','Profile update'],['GET','/api/researchers','Researcher directory'],['GET/PATCH','/api/notifications and /{id}/read','Notification inbox'],['POST/PUT/DELETE','/api/projects/{id}/milestones/{milestoneId}','Milestones'],['POST/PUT/DELETE','/api/projects/{id}/tasks/{taskId}','Tasks']], [1.0,3.4,2.2])
heading('3.6 Exception Handling, Collections, Connectivity and CRUD',2)
para('Implemented validation annotations such as @NotBlank, @NotEmpty, @NotNull and @Min validate profile and project inputs. ProjectService additionally validates team size, available positions and weekly commitment. ApiExceptionHandler maps malformed input to HTTP 400, ownership/resource errors through ResponseStatusException, duplicate profile conflicts to HTTP 409, and unanticipated state/read/write failures to a controlled response. InterruptedException is handled by restoring the interrupt flag before wrapping the operation.')
para('The code uses List<String> for requiredSkills, ArrayList for retrieved projects, HashMap<String,Object> to map domain values to Firestore documents, Map for compact API responses, streams for filtering and normalization, Comparator for newest-first ordering, and List.of for immutable empty results. This demonstrates the Java Collection Framework in ordinary application logic.')
para('FirestoreProjectRepository and FirestoreUserRepository receive Firestore through constructor injection. They use the Firebase Admin SDK to read/write documents and explicitly map Firestore fields to Java records. CRUD is implemented for Project through save, find, update and delete. Planned modules follow the same repository pattern, with transactions for acceptance/capacity changes and appropriate indexes for directory/notification queries.')
heading('3.7 Important Code Extracts From Existing Source',2)
codeblock('''// Project ownership check in ProjectService
if (!ownerId.equals(project.ownerId())) {
    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
        "You do not own this project");
}

// Firebase filter institutional restriction
if (!Boolean.TRUE.equals(verified) || email == null
        || !THAPAR_EMAIL_PATTERN.matcher(email).matches()) {
    throw new IllegalArgumentException("Only verified thapar.edu accounts are allowed");
}''')
codeblock('''// Creation derives ownership from trusted authentication/profile data
UserProfile owner = userService.findByUid(token.getUid());
Project project = new Project(UUID.randomUUID().toString(), ..., owner.uid(),
    owner.fullName(), owner.role().name(), ProjectStatus.PUBLISHED, now, now);''')
pagebreak()

heading('Chapter 4 Results and Screenshots',1)
heading('4.1 Implemented Baseline Results',2)
table(['Feature','Observed implementation evidence','Result statement'],[
['Sign in and onboarding','login.html, onboarding.html, FirebaseAuthenticationFilter, AuthController','Implemented flow for verified institutional users and profile creation.'],['Project feed','app.html, projects.js, ProjectController.feed, ProjectService.findFiltered','Implemented retrieval, text search and declared filters.'],['Project creation','create-project.html, ProjectRequest, ProjectService.create','Implemented server-side profile/ownership derivation and validation.'],['My Projects and editing','my-projects.html, project-details.html, PUT/DELETE endpoints','Implemented owner-scoped listing and ownership checks for changes.']], [1.4,2.7,2.5])
heading('4.2 Existing Screen Set',2)
para('The repository contains the following implemented page templates. Insert captured screenshots from a configured local run in the corresponding slots before final college submission; screenshots are not fabricated in this report because the report is generated without live Firebase credentials.')
table(['Figure slot','Existing route/template','What the evaluator should see'],[
['Figure 4.1','/login - login.html','Google authentication entry page'],['Figure 4.2','/onboarding - onboarding.html','Role, department and profile form'],['Figure 4.3','/app - app.html','Project feed with search and filters'],['Figure 4.4','/create-project - create-project.html','Validated project creation/edit form'],['Figure 4.5','/projects/{projectId} - project-details.html','Project information and owner actions'],['Figure 4.6','/my-projects - my-projects.html','Projects owned by current authenticated user']], [1.1,2.8,2.7])
heading('4.3 Planned Phase Demonstration Criteria',2)
para('Phases 4 to 9 have no implementation screenshots because they are planned modules. When implemented, validation should demonstrate: a pending application becomes accepted/rejected; capacity cannot be exceeded; only an owner makes a decision/removes a member; public profiles are searchable without exposing sensitive fields; unread notification counts change correctly; and tasks can only be assigned to accepted members. These are acceptance criteria, not recorded test results.')
heading('4.4 Evaluation Summary',2)
para('The implemented baseline establishes the essential authenticated project publication and management path. The proposed modules extend this path without changing its central security rule: ownership and membership are determined by authenticated server-side identity and Firestore records, never by untrusted client input.')
pagebreak()

heading('References',1)
refs=[
'[1] ResearchConnect source repository: README.md, pom.xml, firestore.rules and source files under src/main/java/com/researchconnect. Accessed for this report.',
'[2] Spring Boot, "Spring Boot Reference Documentation," https://docs.spring.io/spring-boot/.',
'[3] Spring Security, "Servlet Architecture and Security Filter Chain," https://docs.spring.io/spring-security/.',
'[4] Firebase Documentation, "Authenticate with Firebase using Google Sign-In," https://firebase.google.com/docs/auth.',
'[5] Firebase Documentation, "Cloud Firestore," https://firebase.google.com/docs/firestore.',
'[6] Oracle, "Java Platform, Standard Edition Documentation," https://docs.oracle.com/en/java/.',
'[7] Apache Maven, "Maven in 5 Minutes," https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html.'
]
for x in refs: para(x)

# footer
for section in doc.sections:
    f=section.footer.paragraphs[0]; f.alignment=WD_ALIGN_PARAGRAPH.CENTER
    r=f.add_run('ResearchConnect - Java Mini Project Report'); r.font.name='Times New Roman'; r.font.size=Pt(9)

doc.core_properties.title='ResearchConnect Complete Project Report'
doc.core_properties.subject='Java Mini Project Report'
doc.core_properties.author='ResearchConnect Project Team'
doc.save(OUT)
print(OUT)
