# UI Documentation - MinistryProject

Document generated from `src/main/resources/templates` and web controllers in `src/main/java/ro/church_office/teamleaf/web`.

## 1) Global UI Elements (available on most authenticated pages)

### Church Switcher (fragment)
- Route: POST `/church/select`
- Purpose: switch active church context.
- Fields:
  - `churchId` (select)
  - `redirect` (hidden)

### Header Birthday Notifications (fragment)
- Route: POST `/notifications/birthdays/action`
- Purpose: quick actions for birthday reminders.
- Fields:
  - `personId` (hidden)
  - `mode` (hidden: `snooze` or `done`)
  - `redirect` (hidden)

### Language Selector (fragment)
- Purpose: switch UI language (client-side i18n behavior in UI).

---

## 2) Authentication

### Login
- Route: GET `/login`, POST `/login`
- Template: `auth/login.html`
- Purpose: user authentication.
- Fields:
  - `username` (text)
  - `password` (password)

### Forgot Password
- Route: GET `/forgot-password`, POST `/forgot-password`
- Template: `auth/forgot-password.html`
- Purpose: password reset using security questions.
- Fields:
  - `username` (text)
  - `answerOne` (text)
  - `answerTwo` (text)
  - `newPassword` (password)
  - `confirmPassword` (password)

### Logged Out
- Route: GET `/logged-out`
- Template: `auth/logged-out.html`
- Purpose: logout confirmation page.

---

## 3) Dashboard

### Dashboard Home
- Route: GET `/dashboard`
- Template: `dashboard/index.html`
- Purpose: overview (pastoral alerts, inbox, recommendations, birthdays).
- Main quick actions (hidden/system fields only):
  - Mark follow-up status: POST `/follow-ups/{id}/status` with `status`, `redirect`
  - Birthday action: POST `/notifications/birthdays/action` with `personId`, `mode`, `redirect`
  - Dismiss/restore recommendation: POST `/dashboard/recommendations/dismiss|restore`
  - Inbox check-in: POST `/dashboard/inbox/check-in` with `followUpId`, `status`, `redirect`

---

## 4) Persons

### Persons List
- Route: GET `/persons`, POST `/persons/results`
- Template: `persons/list.html`
- Purpose: search/filter people and navigate to details/edit.
- Filters (query params):
  - `q` (text search)
  - plus paging/sorting params from controller/list UI.

### Person Form (Create/Edit)
- Routes:
  - GET `/persons/new`, POST `/persons`
  - GET `/persons/{id}/edit`, POST `/persons/{id}`
- Template: `persons/form.html`
- Purpose: create or edit a person profile.
- Fields:
  - `firstName` (text)
  - `lastName` (text)
  - `memberType` (select)
  - `birthDate` (date)
  - `phone` (text)
  - `churchRole` (text)
  - `address` (text)
  - `position` (text)
  - `spouseId` (select)
  - `childrenIds` (checkbox multi-select)

### Person Details
- Route: GET `/persons/{id}`
- Template: `persons/view.html`
- Purpose: profile details, linked follow-ups, private notes.
- Private Notes form:
  - Route: POST `/persons/{id}/private-notes`
  - Fields:
    - `text` (textarea)
    - `allowedRoles` (checkbox multi-select)
- Additional actions:
  - Delete note: POST `/persons/{id}/private-notes/{noteId}/delete`
  - Update follow-up status inline (hidden fields: `status`, `redirect`)

---

## 5) Families

### Family View
- Route: GET `/families/{key}`
- Template: `families/view.html`
- Purpose: family grouping and relationship visualization.
- No direct editable form fields on this page.

---

## 6) Groups

### Groups List
- Route: GET `/groups`, POST `/groups/results`
- Template: `groups/list.html`
- Purpose: list, filter, quick create/edit groups.
- Filters:
  - `q` (text)
  - `type` (select)
- Inline group form fields:
  - `id` (hidden for edit)
  - `name` (text)
  - `type` (select)
  - `leaderId` (select)
  - `description` (textarea)
  - `memberIds` (checkbox multi-select)

### Group Form (Dedicated Create/Edit)
- Routes:
  - GET `/groups/new`, POST `/groups`
  - GET `/groups/{id}/edit`, POST `/groups/{id}`
- Template: `groups/form.html`
- Fields:
  - `id` (hidden)
  - `name` (text)
  - `type` (select)
  - `leaderId` (select)
  - `memberIds` (checkbox multi-select)
  - `description` (textarea)

### Group View
- Route: GET `/groups/{id}`
- Template: `groups/view.html`
- Purpose: read-only group details and member view.

---

## 7) Attendance

### Attendance Index
- Route: GET `/attendance`
- Template: `attendance/index.html`
- Purpose: open attendance editor for a date/session/group.
- Fields:
  - `date` (date)
  - `session` (select)
  - `groupId` (select)

### Attendance Details Modal/Section
- Route: GET `/attendance/details`, POST `/attendance`
- Template: `attendance/details.html`
- Purpose: mark presence for selected group/session/date.
- Fields:
  - `date` (hidden)
  - `session` (hidden)
  - `groupId` (hidden)
  - `personIds` (hidden repeated)
  - `presentPersonIds` (checkbox repeated)
  - local search input for filtering list in modal

---

## 8) Events

### Events List
- Route: GET `/events`, POST `/events/results`
- Template: `events/list.html`
- Purpose: event listing, filtering, deletion, paging.
- Filters:
  - `q` (text)
  - `status` (select)
  - `eventType` (select)
  - `groupId` (select)
  - `sort` (hidden/select state)
  - `page` (hidden)
  - `size` (select)
  - `scrollOnly` (checkbox)

### Event Form (Create/Edit)
- Routes:
  - GET `/events/new`, POST `/events`
  - GET `/events/{id}/edit`, POST `/events/{id}`
- Template: `events/form.html`
- Purpose: event planning + optional task management.
- Event fields:
  - `eventName` (text)
  - `priority` (select)
  - `status` (select)
  - `eventType` (select)
  - `about` (textarea)
  - `openDate` (date)
  - `implementedByName` (text search/display)
  - `implementedById` (hidden)
  - `groupId` (select)
  - `recurrenceType` (select)
  - `recurrenceUntil` (date)
  - `frontReminderEnabled` (checkbox)
  - `frontReminderDaysBefore` (number)
- Task fields (per task + new task form):
  - `title` (text)
  - `status` (select)
  - `dueDate` (date)
  - `assignedToId` (select)
  - `orderIndex` (number)
  - `notes` (textarea)

### Move Event Between Churches
- Route: GET `/events/move`, POST `/events/move`
- Template: `events/move.html`
- Purpose: transfer event to another church context.
- Fields:
  - `eventId` (select)
  - `targetChurchId` (select)
  - `clearImplementedBy` (checkbox)

---

## 9) Calendar

### Calendar View
- Route: GET `/calendar`
- Template: `calendar/index.html`
- Purpose: calendar visualization of events/occurrences.
- No editable fields in main page form context.

---

## 10) Visits

### Visits List
- Route: GET `/visits`
- Template: `visits/list.html`
- Purpose: list pastoral visits.

### Visit Form (Create/Edit)
- Routes:
  - GET `/visits/new`, POST `/visits`
  - GET `/visits/{id}/edit`, POST `/visits/{id}`
- Template: `visits/form.html`
- Fields:
  - `personName` (text)
  - `phone` (text)
  - `address` (text)
  - `visitDate` (date)
  - `notes` (textarea)

---

## 11) Follow-ups (Pastoral)

### Follow-ups List
- Route: GET `/follow-ups`, POST `/follow-ups/results`
- Template: `follow-ups/list.html`
- Purpose: pipeline board/list for pastoral follow-up cases.
- Filters:
  - `q` (text)
  - `status` (select)
  - `groupId` (select)
  - `due` (checkbox)
  - `personId` (hidden optional)
- Inline actions:
  - Mark contacted: POST `/follow-ups/{id}/contacted`
  - Change status: POST `/follow-ups/{id}/status` with `status`, `redirect`
  - Change stage: POST `/follow-ups/stage` with `id`, `stage`, `redirect`

### Follow-up Form (Create/Edit)
- Routes:
  - GET `/follow-ups/new`, POST `/follow-ups`
  - GET `/follow-ups/{id}/edit`, POST `/follow-ups/{id}`
- Template: `follow-ups/form.html`
- Fields:
  - `redirect` (hidden)
  - `personId` (select)
  - `groupId` (select)
  - `status` (select)
  - `contactMethod` (text)
  - `lastContactDate` (date)
  - `nextContactDate` (date)
  - `notes` (textarea)

---

## 12) Finance

### Finance Page
- Route: GET `/finance`, POST `/finance`
- Template: `finance/index.html`
- Purpose: transaction register with add/delete.
- Fields:
  - `type` (select: income/expense)
  - `name` (text)
  - `description` (text)
  - `amount` (number, decimal)

---

## 13) Reports

### Reports Page
- Route: GET `/reports`, POST `/reports/results`, GET `/reports/export`
- Template: `reports/index.html`
- Purpose: reporting dashboard and export.
- Filters:
  - `from` (date)
  - `to` (date)

---

## 14) Preaching

### Preaching Assistant
- Route: GET `/preaching`
- Template: `preaching/index.html`
- Purpose: sermon generation helper UI + Kindle delivery form.
- Sermon form fields:
  - `passage` (text)
  - `style` (select)
  - `mainPoints` (number)
  - `scripturesPerPoint` (number)
  - `historicalIllustrationsPerPoint` (number)
  - `literatureIllustrationsPerPoint` (number)
  - `fathersIllustrationsPerPoint` (number)
  - `includeExegeticalNotes` (checkbox)
  - `extraRequirements` (textarea)
- Kindle form fields:
  - `kindleEmail` (email)
  - `subject` (text)

---

## 15) Church

### Church View
- Route: GET `/church`
- Template: `church/view.html`
- Purpose: display active church profile.

### Church Form
- Route: GET `/church/edit`, POST `/church`
- Template: `church/form.html`
- Purpose: edit church profile and avatar.
- Fields:
  - `id` (hidden)
  - `name` (text)
  - `address` (text)
  - `avatarUrl` (text)
  - `avatarFile` (file upload UI helper)
  - `pastorName` (text)
  - `pastorPhone` (text)
  - `secretaryName` (text)
  - `secretaryPhone` (text)
  - `treasurerName` (text)
  - `treasurerPhone` (text)
- Related upload endpoint:
  - POST `/church/avatar-upload` (multipart image upload)

---

## 16) Settings

Single template with tabs/sections: `settings/index.html`

### Appearance Settings
- Routes: GET `/settings/appearance`, POST `/settings/appearance`
- Fields:
  - `uiTheme` (radio)
  - `statusCustomization` (textarea)
  - `priorityCustomization` (textarea)
  - `nameCustomization` (textarea)

### Church Settings
- Routes: GET `/settings/church`, POST `/settings/church`
- Fields:
  - `defaultChurchId` (select)
  - `newChurchName` (text)
  - `churchAvatarEnabled` (checkbox)
  - `currentChurchId` (hidden)
  - `currentChurchName` (text)
  - `avatarUrl` (text)
  - `avatarFile` (file upload helper)
  - `address` (textarea)
  - `pastorName` (text)
  - `pastorPhone` (text)
  - `secretaryName` (text)
  - `secretaryPhone` (text)
  - `treasurerName` (text)
  - `treasurerPhone` (text)

### System Settings
- Routes:
  - GET `/settings/system`, POST `/settings/system`
  - POST `/settings/system/import`
  - GET `/settings/system/export`
  - POST `/settings/system/runtime-mode`
  - POST `/settings/system/shutdown`
- Fields:
  - `rowsPerPage` (select)
  - `eventTasksEnabled` (checkbox)
  - `privateMode` (checkbox)
  - `passwordRestrictionsEnabled` (checkbox)
  - `passwordMinSixEnabled` (checkbox)
  - import: `file` (zip upload)
  - runtime mode: `mode` (select), `cloneFromCurrent` (checkbox)

### Pastoral Settings
- Routes: GET `/settings/pastoral`, POST `/settings/pastoral`
- Numeric thresholds:
  - `pastoralAbsenceCount` (number)
  - `pastoralAnalysisDays` (number)
  - `pastoralBirthdayWindowDays` (number)
  - `pastoralSnoozeDays` (number)
  - `pastoralMaxRecommendations` (number)
- Feature toggles:
  - `pastoralEnableAbsence` (checkbox)
  - `pastoralEnableChildAbsence` (checkbox)
  - `pastoralEnableOverdueFollowUp` (checkbox)
  - `pastoralEnableWithoutGroup` (checkbox)
  - `pastoralEnableNewPerson` (checkbox)
  - `pastoralEnableBirthday` (checkbox)
- Recommendation templates:
  - `pastoralTemplateAbsence` (textarea)
  - `pastoralTemplateChildAbsence` (textarea)
  - `pastoralTemplateOverdueFollowUp` (textarea)
  - `pastoralTemplateWithoutGroup` (textarea)
  - `pastoralTemplateNewPerson` (textarea)
  - `pastoralTemplateBirthday` (textarea)

---

## 17) Account

### Account Security Page
- Route: GET `/account/password`
- Template: `account/password.html`
- Purpose: change password + security question answers.

#### Change Password form
- Route: POST `/account/password`
- Fields:
  - `currentPassword` (password)
  - `newPassword` (password)
  - `confirmPassword` (password)

#### Security Questions form
- Route: POST `/account/security-questions`
- Fields:
  - `currentPassword` (password)
  - `answerOne` (text)
  - `answerTwo` (text)

---

## 18) User Administration

### Users Admin
- Route: GET `/admin/users`
- Template: `users-admin/index.html`
- Purpose: create/edit/delete app users.

#### Create User form
- Route: POST `/admin/users`
- Fields:
  - `username` (text)
  - `password` (password)
  - `role` (select)
  - `answerOne` (text)
  - `answerTwo` (text)

#### Edit User form
- Route: POST `/admin/users/{id}`
- Fields:
  - `username` (text)
  - `password` (password, optional)
  - `role` (select)
  - `answerOne` (text)
  - `answerTwo` (text)

---

## Notes for Designer
- The app mixes full pages with partial refresh/inline interactions (`:: resultsSection`, modals, board actions).
- Major dense forms: `events/form`, `settings/index`, `persons/form`, `groups/form`.
- Several flows depend on context (active church and role permissions), especially private notes and admin sections.
