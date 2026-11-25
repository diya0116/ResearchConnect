# ResearchConnect

ResearchConnect is a Spring Boot academic research collaboration platform for verified
`@thapar.edu` accounts.

## Phase 1 setup

### Requirements

- Java 21
- A Firebase project with Google Authentication enabled
- A Cloud Firestore database
- A Firebase Admin SDK service-account JSON file

### Firebase configuration

Configure these environment variables in your local shell or deployment environment:

- `FIREBASE_SERVICE_ACCOUNT_PATH`: path to the Firebase Admin SDK service-account JSON
  file. Keep that file outside the repository and provide it as a secret file in
  production. If omitted, the application uses Google Application Default Credentials.
- `FIREBASE_WEB_API_KEY`
- `FIREBASE_WEB_AUTH_DOMAIN`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_WEB_APP_ID`

The web configuration variables are used by Firebase's browser SDK. The service
account path is used only by the Java backend and is never exposed to the browser.

### Run

Use the Maven Wrapper when it is available:

```powershell
.\mvnw.cmd spring-boot:run
```

Alternatively, run with a local Maven installation:

```powershell
mvn spring-boot:run
```

Open `http://localhost:8080`. The server uses the `PORT` environment variable when
provided and defaults to port `8080`.

## Render deployment

Render can build and run the included `Dockerfile`. Configure the Firebase environment
variables above in the Render service settings, and provide the Admin SDK JSON as a
Render secret file. Set `FIREBASE_SERVICE_ACCOUNT_PATH` to the mounted secret file's
path. Render supplies `PORT` automatically; the application defaults to `8080` when
running without it.

## Phase 1 behavior

1. Google Sign-In is handled by Firebase Authentication in the browser.
2. The browser sends the Firebase ID token to Spring Boot as a bearer token.
3. Firebase Admin SDK verifies the token on the backend.
4. The backend requires a verified email ending exactly in `@thapar.edu`.
5. A first-time user completes a role and profile form.
6. Profiles are stored in the Firestore `users` collection using the Firebase UID as
   the document ID.

Roles are selected during onboarding and are never inferred from an email address.

## Phase 2 behavior

The project feed retrieves documents from the configured Firestore `projects`
collection and shows the newest projects first. The feed supports title/keyword
search, department, category and publisher-role filters.

Students and professors with completed profiles can create projects. The owner is set
from the authenticated Firebase principal and the user's Firestore profile:

- Owner UID
- Owner name
- Owner role

Frontend-submitted owner fields are ignored.

Project pages:

```text
/app
/create-project
/projects/{projectId}
```

Protected project endpoints:

```text
GET  /api/projects
GET  /api/projects/{projectId}
POST /api/projects
```

## Firestore Security Rules

The repository includes `firestore.rules`. It permits verified `@thapar.edu`
Firebase users to read projects and allows project owners to create, update or
delete their own project documents.

Deploy it with:

```powershell
firebase deploy --only firestore:rules
```

The Java Firebase Admin SDK bypasses client Firestore Rules, while the rules still
protect direct browser/mobile access.

## Project feed troubleshooting

Project documents are written and read from the Firestore `projects` collection in
the configured Firebase project. Ensure its environment variables are set when
troubleshooting project feed access.

The repository reads a raw Firestore `requiredSkills` list and converts it without
generic Java deserialization. Feed queries use a collection read and sort projects
by `createdAt` in Java, avoiding a Firestore composite-index requirement.

## Phase 3 behavior

The My Projects page is available at `/my-projects`. It retrieves only projects whose
Firestore `ownerId` matches the verified Firebase UID. Owners can edit through
`/projects/{projectId}/edit` or delete from the project details page.

Management endpoints:

```text
GET    /api/projects/mine
PUT    /api/projects/{projectId}
DELETE /api/projects/{projectId}
GET    /api/projects/{projectId}/member-count
```

The backend ignores any owner fields from edit requests, preserves `ownerId`,
`ownerName`, `ownerRole`, `projectId`, and `createdAt`, and updates only editable
fields plus `updatedAt`. Non-owners receive HTTP 403.

## Phase 4: User dashboard

The authenticated dashboard is available at `/dashboard`. It brings together the
current user's profile, owned projects, join applications, accepted memberships and
saved projects. Project and user IDs are derived from the verified Firebase token;
the browser cannot select another user's dashboard data or make owner decisions.

Dashboard and collaboration API:

```text
GET    /api/dashboard
POST   /api/projects/{projectId}/applications
GET    /api/projects/{projectId}/applications/mine
PATCH  /api/applications/{applicationId}/decision
GET    /api/projects/{projectId}/saved
PUT    /api/projects/{projectId}/saved
DELETE /api/projects/{projectId}/saved
```

Applications are stored in `project_applications`. Decisions are rechecked
transactionally against the current project owner and available capacity; accepting
a request creates an accepted `project_members` record and decreases available
positions atomically. Saved projects are stored per user in `saved_projects`.

## Phase 5: Basic collaboration and notifications

Authenticated students and professors with completed profiles can comment on project
pages. Comment authors may delete their own comments, and the project owner may
moderate comments on that project. Comment identity and role are read from the
authenticated user's Firestore profile.

Join-request submissions notify the project owner; accepted or rejected decisions
notify the applicant. Notifications are stored in the `notifications` collection and
are visible only to their recipient. The dashboard lists notifications and allows
the recipient to mark them as read. Project comments are stored in
`project_comments`.

Additional API endpoints:

```text
GET    /api/projects/{projectId}/comments
POST   /api/projects/{projectId}/comments
DELETE /api/projects/{projectId}/comments/{commentId}
GET    /api/notifications
PATCH  /api/notifications/{notificationId}/read
```

Saved projects continue to use the Phase 4 `saved_projects` collection and existing
save, remove, and dashboard flows.
