# Splunky UI Specification

> Note: this document still describes the broader target product shape. The current shipped MVP is narrower: login, summary, raw logs preview, and executed SPL only. Timeline/graph/sequence/downstream-call/assistant UI remains deferred.

## 1. Product Summary

**Splunky** is an AI-assisted investigation workspace for API testing environments. Users can paste a Splunk search URL, an API error response, a correlation ID, or a natural-language question. Splunky queries or imports Splunk logs in the backend, analyzes the returned logs with an LLM, and renders a structured investigation result.

For the first frontend-only implementation, all backend calls should be mocked. The UI should feel like a real investigation tool, not only a chat application.

Core product idea:

> Chat is the control interface. Structured investigation views are the main product.

The user should be able to:

1. Start an investigation from free text.
2. View a diagnosis summary.
3. Inspect timeline, service graph, sequence view, downstream calls, raw logs, and executed SPL queries.
4. Ask follow-up questions.
5. Let follow-up questions either answer from current evidence or trigger a mocked re-query and refresh the active result.

---

## 2. MVP Scope

### 2.1 In Scope for Pure Frontend MVP

The frontend MVP should implement:

1. Initial investigation input page.
2. Result workspace page.
3. Summary view.
4. Timeline view.
5. Service graph view.
6. Sequence / swimlane view.
7. Downstream calls table.
8. Raw logs view with basic filtering.
9. SPL inspector view.
10. Right-side AI follow-up panel.
11. Mock follow-up handling:
    - answer from current result;
    - rerun query and replace current result;
    - start new investigation.
12. Lightweight run history.
13. Detail drawer for timeline events, graph nodes/edges, downstream calls, raw logs, and SPL queries.
14. Login screen with mocked session handling for Splunk credentials.

### 2.2 Out of Scope for Pure Frontend MVP

Do not implement real backend integration yet:

1. No real Splunk connection.
2. No real LLM call.
3. No real authentication integration beyond frontend mock session behavior.
4. No persistent database.
5. No real Confluence / knowledge base integration.
6. No ServiceNow write-back.
7. No export / report generation.
8. No saved case management.

Mock these parts cleanly so the backend can be added later.

---

## 3. Suggested Frontend Stack

Use the following stack unless there is a strong reason to simplify:

- React
- TypeScript
- Vite
- Ant Design
- Tailwind CSS
- React Flow for service graph
- Zustand or React Context for local state
- Mock data under `src/mocks`

Recommended Ant Design components:

- `Layout`
- `Card`
- `Tabs`
- `Tag`
- `Badge`
- `Timeline`
- `Table`
- `Drawer`
- `Input.TextArea`
- `Button`
- `Select`
- `Segmented`
- `Collapse`
- `Alert`
- `Spin`
- `Tooltip`
- `Dropdown`
- `Empty`

---

## 4. Information Architecture

```text
Splunky
│
├── Investigation Input
│   ├── Paste Splunk URL
│   ├── Paste error response
│   ├── Enter correlation ID
│   ├── Ask natural-language question
│   └── Select time range and timezone
│
├── Investigation Workspace
│   ├── Result Context Bar
│   ├── Summary
│   ├── Timeline
│   ├── Service Graph
│   ├── Sequence View
│   ├── Downstream Calls
│   ├── Raw Logs
│   └── SPL Inspector
│
├── AI Follow-up Panel
│   ├── Conversation
│   ├── Suggested follow-up chips
│   ├── Re-query status card
│   └── Lightweight run history
│
└── Detail Drawer
    ├── Timeline event detail
    ├── Service node detail
    ├── Graph edge detail
    ├── Downstream call detail
    ├── Raw log detail
    └── SPL query detail
```

---

## 5. Main Layout

### 5.1 Initial State

The initial page should be clean and centered.

```text
┌─────────────────────────────────────────────────────────────┐
│ Splunky                                                     │
│ AI-assisted API log investigation                           │
├─────────────────────────────────────────────────────────────┤
│ Paste Splunk URL, correlation ID, error response, or question │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │                                                         │ │
│ │                                                         │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ Time Range: [Last 30 min] [Last 1 hour] [Custom]            │
│ Custom: [Start datetime] [End datetime]   TZ: [HKT +08:00]  │
│                                                             │
│ [Investigate]                                               │
│                                                             │
│ Examples:                                                   │
│ - Find logs for correlation ID abc-123                      │
│ - Why did this payment propose fail?                        │
│ - Check 5xx spike for payment-sapi in last 30 minutes       │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Result State

After investigation starts, switch to a workspace layout.

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ Splunky                                                                    │
│ User Guide                                      New Search | User avatar    │
├──────────────────────────────────────────────────────────┬─────────────────┤
│ Current Investigation                                    │ AI Assistant    │
│ Original input: correlation id abc-123                   │                 │
│ Correlation ID: abc-123 | Last 1 hour | Updated: 10:15   │                 │
│ Run #3: Expanded to last 1 hour                          │                 │
├──────────────────────────────────────────────────────────┤                 │
│ Tabs: Summary | Timeline | Graph | Sequence | Calls | Logs | SPL          │
│                                                          │ Conversation    │
│ Main tab content                                         │ Suggested chips │
│                                                          │ Follow-up input │
└──────────────────────────────────────────────────────────┴─────────────────┘
```

### 5.3 Layout Ratio

Recommended desktop layout:

- Main investigation area: 70% width.
- AI follow-up panel: 30% width.
- Minimum width for AI panel: 360px.
- On smaller screens, AI panel can collapse into a drawer.

---

## 6. Header

The header should include:

- Product name: `Splunky`
- `User Guide` entry on the left side. Do not link it until content exists.
- `New Search` button only in the workspace page. It opens a new browser tab instead of replacing the current investigation.
- User avatar on the far right with a dropdown containing `Logout`.

Example:

```text
Splunky | User Guide                                           New Search | Avatar
```

---

## 7. Investigation Input

### 7.1 Input Box

Use a large text area with placeholder:

```text
Paste a Splunk URL, error response, correlation ID, or ask what you want to investigate...
```

Do not show a separate API input. A single investigation can involve multiple APIs, gateways, mesh proxies, or downstream systems, so API names should be inferred from logs and analysis results.

### 7.2 Input Type Detection

Do not make input type a single-select control. The frontend should auto-detect multiple input signals from the same text:

```text
Splunk URL                           -> SPLUNK_URL
contains "correlation" or UUID-like text  -> CORRELATION_ID
starts with "{" or "["                  -> ERROR_RESPONSE
contains "api" or "sapi"                -> API_NAME_OR_FIELD
otherwise                                -> NATURAL_LANGUAGE
```

### 7.3 Context Controls

Add these controls below the input:

- Time range preset selector
- Custom start datetime and end datetime when the user selects `Custom`
- Timezone select on the right side, defaulting to `HKT +08:00`

Initial options:

```text
Time Range: Last 15 min, Last 30 min, Last 1 hour, Last 4 hours, Custom
Timezone: HKT +08:00, UTC +00:00, SGT +08:00, JST +09:00, BST +01:00, EST -05:00
```

If the input is a Splunk URL, the UI can expose `From Splunk URL` as a time source while still allowing the user to override it with a preset or custom range.

---

## 8. Investigation Result Context Bar

Once a result is available, show a context bar above the tabs.

### 8.1 Fields

Display:

- Investigation ID
- Original user input, shown in full with wrapping
- Time range
- Correlation ID, if detected
- Last run time

Example:

```text
Investigation inv-001 | Updated: 10:15:22 | Correlation ID: abc-123
Original input: why did payment propose fail for correlation id abc-123
```

### 8.2 Run History Dropdown

Add a small dropdown:

```text
Run #3: Expanded to last 1 hour ▼
```

Dropdown content:

```text
Run #1 Initial search by correlation ID
Run #2 Find similar downstream timeout
Run #3 Expanded to last 1 hour
```

For the MVP, selecting an old run may either:

- show a lightweight summary only; or
- restore mock result if easy.

Do not build full historical result management yet.

---

## 9. Tabs

The main result area should use tabs:

```text
Summary | Timeline | Service Graph | Sequence | Downstream Calls | Raw Logs | SPL
```

Each tab should use the same active result object.

---

## 10. Summary View

### 10.1 Purpose

The summary view answers:

> What happened, where did it fail, what evidence supports the conclusion, and what should the user do next?

### 10.2 Layout

```text
┌──────────────────┬──────────────────┬──────────────────┬─────────────────┐
│ Final Status      │ Failure Point    │ Confidence       │ Logs Found      │
│ Failed            │ HUB Timeout      │ High             │ 86              │
└──────────────────┴──────────────────┴──────────────────┴─────────────────┘

Diagnosis Summary
The request reached payment-sapi successfully. Validation passed, but the downstream
call to hub-payment-propose-api timed out after 30 seconds. SAPI then returned HTTP 500.

Evidence
1. payment-sapi received the inbound request at 10:01:03.120
2. payment-sapi validation passed at 10:01:03.450
3. payment-sapi called hub-payment-propose-api at 10:01:05.000
4. hub-payment-propose-api timed out at 10:01:35.005
5. payment-sapi returned HTTP 500 at 10:01:35.100

Recommended Actions
1. Check HUB service health around 10:01 - 10:05
2. Search similar HUB timeout errors in the last 30 minutes
3. Escalate to HUB support if there is a spike
```

### 10.3 Visual Elements

Use cards for:

- Final status
- Failure point
- Confidence
- Total logs found
- Related APIs / traffic hops
- Failed downstream

Use tags:

- `FAILED`
- `TIMEOUT`
- `HIGH CONFIDENCE`
- `DOWNSTREAM ISSUE`

### 10.4 Evidence Interaction

Each evidence item should be clickable and open the detail drawer with:

- linked timeline event
- raw log references
- related service
- timestamp

---

## 11. Timeline View

### 11.1 Purpose

The timeline view answers:

> In what order did events happen for this investigation?

### 11.2 Layout

Use either Ant Design `Timeline` or a table-like timeline.

Recommended columns:

```text
Time | Service | Event Type | Status | Duration | Message | Action
```

Example:

```text
10:01:03.120 | payment-sapi | inbound_request      | OK      | -      | Received request
10:01:03.450 | payment-sapi | validation           | OK      | 330ms  | Validation passed
10:01:04.010 | payment-sapi | downstream_request   | OK      | -      | Calling payee-service
10:01:04.240 | payee-service| downstream_response  | OK      | 230ms  | Returned 200
10:01:05.000 | payment-sapi | downstream_request   | PENDING | -      | Calling hub-payment-propose-api
10:01:35.005 | payment-sapi | downstream_response  | FAILED  | 30s    | HUB read timeout
10:01:35.100 | payment-sapi | outbound_response    | FAILED  | -      | Returned 500 to caller
```

### 11.3 Event Status

Use status tags:

- `OK`
- `FAILED`
- `TIMEOUT`
- `WARNING`
- `INFERRED`

### 11.4 Row Click

Clicking a row opens the detail drawer.

Drawer should show:

- full event detail
- related raw logs
- related service
- related downstream call, if any

---

## 12. Service Graph View

### 12.1 Purpose

The service graph view answers:

> Which services were involved, and where did the failure happen?

### 12.2 Graph Example

```text
Gateway
  │ 200
  ▼
payment-sapi
  ├── payee-service              200 / 230ms
  ├── limit-service              200 / 180ms
  └── hub-payment-propose-api    Timeout / 30000ms
```

### 12.3 Node Content

Each node should display:

- service name
- platform, if available
- status
- log count
- error count

Example node:

```text
payment-sapi
SHP_AWS
Logs: 46 | Errors: 1
```

### 12.4 Edge Content

Each edge should display:

- caller
- callee
- operation or endpoint
- status
- latency
- evidence type: `Confirmed` or `Inferred`

Example:

```text
payment-sapi -> hub-payment-propose-api
POST /payments/propose
Timeout / 30000ms / Confirmed
```

### 12.5 Graph Interactions

- Click node: open service detail drawer.
- Click edge: open call detail drawer.
- Failed node or edge should be visually emphasized.
- Use different line styles for confirmed vs inferred calls.

Do not rely only on color. Always show textual status labels.

---

## 13. Sequence / Swimlane View

### 13.1 Purpose

The sequence view answers:

> How did one request move across services over time?

### 13.2 Example

```text
Gateway        Payment SAPI        Payee API        HUB API
   │                │                  │              │
   │── request ────>│                  │              │
   │                │── get payee ────>│              │
   │                │<── 200 ──────────│              │
   │                │── propose ─────────────────────>│
   │                │                                  │
   │                │<──────── timeout after 30s ──────│
   │<── 500 ────────│                                  │
```

### 13.3 Implementation Suggestion

For the pure frontend MVP, implement sequence view using simple CSS/HTML/SVG. It does not need a full diagram engine.

Possible layout:

- Participants as vertical columns.
- Messages as horizontal arrows.
- Failed messages use a `FAILED` tag.
- Clicking a message opens the detail drawer.

### 13.4 Message Fields

Each message should have:

- from
- to
- label
- timestamp
- status
- duration
- related log IDs

---

## 14. Downstream Calls View

### 14.1 Purpose

The downstream calls view answers:

> Which downstream calls were made, which one failed, and how long did they take?

### 14.2 Table Columns

```text
# | Caller | Downstream | Operation | Status | Latency | Error | Evidence
```

Example:

```text
1 | payment-sapi | payee-service           | GET /payees/{id}      | 200     | 230ms   | -            | 3 logs
2 | payment-sapi | limit-service           | POST /limits/check    | 200     | 180ms   | -            | 2 logs
3 | payment-sapi | hub-payment-propose-api | POST /payments/prop   | Timeout | 30000ms | Read timeout | 5 logs
```

### 14.3 Filters

Add quick filters:

```text
[All] [Failed Only] [Timeout Only] [Group by Downstream]
```

### 14.4 Row Click

Click row to open downstream call detail drawer.

---

## 15. Raw Logs View

### 15.1 Purpose

The raw logs view gives developers confidence and allows manual inspection.

### 15.2 Filters

Support basic filters:

- Service
- Log level
- Event type
- Status
- Keyword
- Failed only toggle

### 15.3 Log List Columns

```text
Time | Level | Service | Event Type | Message | Action
```

### 15.4 Raw Log Detail

Clicking a log opens the detail drawer.

Drawer should show:

1. Summary fields.
2. Pretty JSON view.
3. Raw message.
4. Related timeline event.
5. `Open in Splunk` mock link.

### 15.5 Sensitive Data

For this testing-environment MVP, no advanced data masking is required in frontend. However, keep the raw log rendering component isolated so masking can be added later.

---

## 16. SPL Inspector View

### 16.1 Purpose

The SPL inspector answers:

> What did Splunky query, when, and how many logs were returned?

### 16.2 Query Card Fields

Each query card should show:

- query ID
- template name
- reason
- SPL
- time range
- result count
- execution duration
- status
- mock `Open in Splunk` link

Example:

```text
Query 1: Find logs by correlation ID
Reason: User provided correlation ID abc-123
Result count: 86
Execution duration: 1.2s
Status: Success

index=payment_sit correlationId="abc-123" earliest="2026-04-30T10:00:00" latest="2026-04-30T10:10:00"
```

### 16.3 MVP Rule

The SPL inspector is read-only in the frontend MVP. Do not implement free-form SPL editing yet.

---

## 17. AI Follow-up Panel

### 17.1 Purpose

The AI panel lets the user ask follow-up questions during the same investigation.

Follow-up can result in one of three behaviors:

1. Answer from current result.
2. Rerun query and replace active result.
3. Start a new investigation.

### 17.2 Panel Layout

```text
┌──────────────────────────────┐
│ AI Assistant                  │
├──────────────────────────────┤
│ Conversation messages         │
│                              │
│ Suggested Follow-ups          │
│ [Find similar errors]         │
│ [Expand to 1 hour]            │
│ [Failed downstream only]      │
│ [Generate incident summary]   │
│                              │
│ Ask follow-up...              │
└──────────────────────────────┘
```

### 17.3 Suggested Follow-up Chips

After a result is rendered, show suggested chips:

```text
[Find similar errors]
[Expand to last 1 hour]
[Show failed downstream only]
[Explain failure point]
[Generate incident summary]
[Show raw evidence]
```

### 17.4 Follow-up Classification

For the pure frontend MVP, classify follow-ups using simple rule-based mock logic.

#### ANSWER_FROM_CURRENT_RESULT

Examples:

```text
Why did it fail?
Is this a frontend issue?
Explain the failure point.
What evidence supports this?
```

Behavior:

- Do not refresh main result.
- Add AI answer to the conversation.

#### REFINE_ANALYSIS

Examples:

```text
Generate an incident summary.
Write a message to HUB team.
Summarize for QA.
Only show evidence.
```

Behavior:

- Do not refresh main result.
- Add formatted AI answer to the conversation.

#### RERUN_QUERY

Examples:

```text
Expand to last 1 hour.
Find similar timeout errors.
Search failed downstream only.
Check whether this API has a 5xx spike today.
```

Behavior:

- Show a re-query status card in the AI panel.
- Create a new mock run.
- Replace active result in the main workspace.
- Add lightweight run history entry.
- Add final AI message explaining what changed.

#### NEW_INVESTIGATION

Examples:

```text
Check correlation ID def-456.
Now investigate payee-sapi.
Search another error response.
```

Behavior:

- Start a new mock investigation.
- Clear or archive current active result.
- Keep global app state simple.

### 17.5 Rerun Status Card

When follow-up triggers rerun, show:

```text
Re-running investigation
Changes:
- Time range: Last 1 hour
- Added filter: downstream = hub-payment-propose-api
- Query type: similar timeout search

[View SPL]
```

After mock completion:

```text
I expanded the time range to the last 1 hour and found 12 similar HUB timeout errors. The current result has been updated.
```

### 17.6 Result Replacement Rule

MVP rule:

> There is only one active result. If a follow-up triggers re-query, replace the active result. Keep only lightweight run history and conversation messages.

Do not build complex multi-result comparison in MVP.

---

## 18. Detail Drawer

### 18.1 Purpose

The drawer gives details without navigating away from the workspace.

### 18.2 Drawer Types

The drawer should support:

1. `TIMELINE_EVENT`
2. `SERVICE_NODE`
3. `SERVICE_EDGE`
4. `DOWNSTREAM_CALL`
5. `RAW_LOG`
6. `SPL_QUERY`

### 18.3 Common Drawer Header

Each drawer should show:

- title
- status tag
- timestamp, if relevant
- service name, if relevant

### 18.4 Common Drawer Body

Use sections:

```text
Overview
Evidence
Related Logs
Raw Data
Actions
```

Actions can include:

- `Open in Splunk` mock link
- `Copy ID`
- `Copy JSON`

---

## 19. Loading, Empty, and Error States

### 19.1 Loading State

When the user clicks `Investigate`, show progress:

```text
Understanding input...
Selecting query templates...
Querying Splunk logs...
Building investigation timeline...
Analyzing evidence...
```

For frontend MVP, simulate this with a short staged loading UI.

### 19.2 Empty State

If no logs found:

```text
No matching logs found for the selected input and time range.
Try expanding the time range or checking the correlation ID.
```

Suggested actions:

```text
[Expand to last 1 hour]
[Review inferred inputs]
[Start new search]
```

### 19.3 Error State

If mock query fails:

```text
Investigation failed
Splunky could not complete the mock query. Please retry or start a new investigation.
```

---

## 20. TypeScript Data Models

Use these as frontend contract models. They can later align with backend DTOs.

```ts
export type InvestigationInputType =
  | 'SPLUNK_URL'
  | 'CORRELATION_ID'
  | 'ERROR_RESPONSE'
  | 'API_NAME_OR_FIELD'
  | 'NATURAL_LANGUAGE';

export type InvestigationStatus = 'SUCCESS' | 'FAILED' | 'PARTIAL' | 'NO_RESULT';

export type Confidence = 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN';

export type EventStatus = 'OK' | 'FAILED' | 'TIMEOUT' | 'WARNING' | 'INFERRED';

export interface Investigation {
  id: string;
  activeRunId: string;
  createdAt: string;
  updatedAt: string;
  input: InvestigationInput;
  runs: InvestigationRunSummary[];
  activeResult: InvestigationResult;
  conversation: ChatMessage[];
}

export interface InvestigationInput {
  rawText: string;
  detectedTypes: InvestigationInputType[];
  timeRange: TimeRange;
  apiName?: string;
  correlationId?: string;
}

export interface TimeRange {
  label: string;
  from: string;
  to: string;
  timezone: TimezoneOption;
}

export interface TimezoneOption {
  label: string;
  offset: string;
}

export interface InvestigationRunSummary {
  runId: string;
  runNumber: number;
  title: string;
  userQuestion?: string;
  action: FollowUpActionType;
  summary: string;
  createdAt: string;
}

export interface InvestigationResult {
  investigationId: string;
  runId: string;
  runNumber: number;
  context: InvestigationContext;
  summary: DiagnosisSummary;
  timeline: TimelineEvent[];
  serviceGraph: ServiceGraph;
  sequence: SequenceViewModel;
  downstreamCalls: DownstreamCall[];
  rawLogs: RawLogEntry[];
  queries: SplQueryRecord[];
  suggestedFollowUps: SuggestedFollowUp[];
}

export interface InvestigationContext {
  timeRange: TimeRange;
  correlationId?: string;
  apiName?: string;
  lastRunAt: string;
}

export interface DiagnosisSummary {
  status: InvestigationStatus;
  finalHttpStatus?: number;
  failurePoint?: string;
  rootCauseHypothesis?: string;
  confidence: Confidence;
  logsFound: number;
  affectedServices: string[];
  evidence: EvidenceItem[];
  recommendedActions: string[];
}

export interface EvidenceItem {
  id: string;
  title: string;
  description: string;
  timestamp?: string;
  service?: string;
  relatedLogIds: string[];
  relatedTimelineEventIds: string[];
}

export interface TimelineEvent {
  id: string;
  timestamp: string;
  service: string;
  eventType: string;
  status: EventStatus;
  durationMs?: number;
  message: string;
  relatedLogIds: string[];
  downstreamCallId?: string;
}

export interface ServiceGraph {
  nodes: ServiceNode[];
  edges: ServiceEdge[];
}

export interface ServiceNode {
  id: string;
  serviceName: string;
  platform?: string;
  status: EventStatus;
  logCount: number;
  errorCount: number;
}

export interface ServiceEdge {
  id: string;
  source: string;
  target: string;
  label: string;
  operation?: string;
  status: EventStatus;
  latencyMs?: number;
  evidenceType: 'CONFIRMED' | 'INFERRED';
  relatedLogIds: string[];
  downstreamCallId?: string;
}

export interface SequenceViewModel {
  participants: SequenceParticipant[];
  messages: SequenceMessage[];
}

export interface SequenceParticipant {
  id: string;
  label: string;
  serviceName: string;
}

export interface SequenceMessage {
  id: string;
  from: string;
  to: string;
  label: string;
  timestamp: string;
  status: EventStatus;
  durationMs?: number;
  relatedLogIds: string[];
  downstreamCallId?: string;
}

export interface DownstreamCall {
  id: string;
  caller: string;
  downstream: string;
  operation: string;
  endpoint?: string;
  status: EventStatus;
  latencyMs?: number;
  errorMessage?: string;
  relatedLogIds: string[];
}

export interface RawLogEntry {
  id: string;
  timestamp: string;
  level: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
  service: string;
  eventType?: string;
  message: string;
  fields: Record<string, unknown>;
  splunkUrl?: string;
}

export interface SplQueryRecord {
  id: string;
  templateName: string;
  reason: string;
  spl: string;
  timeRange: TimeRange;
  resultCount: number;
  executionDurationMs: number;
  status: 'SUCCESS' | 'FAILED';
  splunkUrl?: string;
}

export type FollowUpActionType =
  | 'ANSWER_FROM_CURRENT_RESULT'
  | 'REFINE_ANALYSIS'
  | 'RERUN_QUERY'
  | 'NEW_INVESTIGATION';

export interface ChatMessage {
  id: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  createdAt: string;
  actionType?: FollowUpActionType;
  relatedRunId?: string;
}

export interface SuggestedFollowUp {
  id: string;
  label: string;
  prompt: string;
  actionType: FollowUpActionType;
}
```

---

## 21. Mock Data Requirements

Create mock files:

```text
src/mocks/investigationMock.ts
src/mocks/logsMock.ts
src/mocks/followUpMock.ts
```

### 21.1 Default Mock Scenario

The main mock scenario should be:

```text
User input:
correlation id abc-123

Diagnosis:
payment-sapi received the request, validation passed, then downstream hub-payment-propose-api timed out after 30 seconds. payment-sapi returned HTTP 500.

Services:
- gateway
- payment-sapi
- payee-service
- limit-service
- hub-payment-propose-api

Failed downstream:
hub-payment-propose-api
```

### 21.2 Rerun Mock Scenario

For follow-up `Expand to last 1 hour`, generate a new run:

```text
Run #2 or #3
Time range: Last 1 hour
Additional result: 12 similar timeout errors found for hub-payment-propose-api
Confidence: High
Recommendation: Check HUB availability and escalate if needed
```

### 21.3 No Result Mock Scenario

If user input contains `no-result`, return a no-result state.

### 21.4 Error Mock Scenario

If user input contains `mock-error`, return an error state.

---

## 22. Component Breakdown

Recommended component structure:

```text
src/
  app/
    App.tsx
    routes.tsx
  components/
    layout/
      AppHeader.tsx
      WorkspaceLayout.tsx
    investigation/
      InvestigationInput.tsx
      ResultContextBar.tsx
      RunHistoryDropdown.tsx
      SummaryView.tsx
      TimelineView.tsx
      ServiceGraphView.tsx
      SequenceView.tsx
      DownstreamCallsView.tsx
      RawLogsView.tsx
      SplInspectorView.tsx
      DetailDrawer.tsx
    assistant/
      AiFollowUpPanel.tsx
      ChatMessageList.tsx
      SuggestedFollowUps.tsx
      RequeryStatusCard.tsx
    common/
      StatusTag.tsx
      EvidenceList.tsx
      JsonViewer.tsx
  mocks/
    investigationMock.ts
    logsMock.ts
    followUpMock.ts
  services/
    mockInvestigationService.ts
    mockFollowUpService.ts
  store/
    investigationStore.ts
  types/
    investigation.ts
  utils/
    inputDetection.ts
    formatters.ts
```

---

## 23. Frontend Behavior Rules

### 23.1 Starting Investigation

When user clicks `Investigate`:

1. Detect input type.
2. Show staged loading.
3. Load mock investigation result.
4. Render workspace.
5. Add initial assistant message:

```text
I found logs for correlation ID abc-123 and built an investigation timeline. The likely failure point is hub-payment-propose-api timeout.
```

### 23.2 Follow-up Without Rerun

When user asks:

```text
Is this a frontend issue?
```

Mock response:

```text
Based on the current evidence, this does not look like a frontend issue. The request reached payment-sapi successfully, and validation passed before the HUB timeout occurred.
```

Main result should not change.

### 23.3 Follow-up With Rerun

When user asks:

```text
Expand to last 1 hour.
```

Behavior:

1. Add user message.
2. Show re-query status card.
3. Simulate loading.
4. Replace active result with rerun mock result.
5. Add run history entry.
6. Add assistant message:

```text
I expanded the time range to the last 1 hour and found 12 similar timeout errors for hub-payment-propose-api. The current result has been updated.
```

### 23.4 New Investigation

When user asks:

```text
Check correlation ID def-456.
```

Behavior:

1. Start new mock investigation.
2. Reset active result.
3. Keep UI simple; no need to build multi-case navigation yet.

---

## 24. Visual Design Direction

Use a clean, technical, operations-focused style.

### 24.1 Tone

- Calm
- Evidence-driven
- Developer-friendly
- Not too flashy

### 24.2 Visual Priority

Important information should stand out:

1. Failure point
2. Confidence
3. Timeline failure event
4. Failed service graph edge
5. Raw evidence links

### 24.3 Accessibility

Do not rely only on color. Always show status text labels.

Use clear status tags:

```text
OK | FAILED | TIMEOUT | WARNING | INFERRED | CONFIRMED
```

---

## 25. Acceptance Criteria

The frontend MVP is acceptable when:

1. User can start an investigation from initial input.
2. Workspace renders a structured investigation result.
3. Summary view clearly shows failure point and evidence.
4. Timeline view shows ordered events.
5. Service graph shows multi-service call relationship.
6. Sequence view shows request movement across services.
7. Downstream calls table identifies failed downstream.
8. Raw logs are viewable and filterable.
9. SPL inspector shows executed mock queries.
10. AI follow-up panel supports simple conversation.
11. Some follow-ups answer from current result without refreshing.
12. Some follow-ups trigger rerun and replace active result.
13. Lightweight run history is visible.
14. Detail drawer works for at least timeline events, raw logs, downstream calls, and graph edges.
15. The app runs fully with mock data and no backend.

---

## 26. Recommended First Codex Task

Ask Codex to implement the frontend in this order:

1. Create Vite + React + TypeScript project.
2. Add Ant Design and Tailwind.
3. Create core types under `src/types/investigation.ts`.
4. Create mock investigation data.
5. Build initial investigation input page.
6. Build workspace layout with tabs and AI panel.
7. Build Summary, Timeline, Raw Logs, and SPL views first.
8. Add Service Graph using React Flow.
9. Add Sequence view.
10. Add follow-up mock handling and run history.
11. Add detail drawer interactions.
12. Polish layout and loading/empty/error states.

---

## 27. Codex Implementation Prompt

Use this prompt when asking Codex to generate the pure frontend:

```text
Build a pure frontend MVP for a product named Splunky.

Splunky is an AI-assisted API log investigation workspace. It allows users to paste a Splunk URL, correlation ID, API error response, or natural-language question. The frontend should use mock data only. No backend integration is required.

Use React + TypeScript + Vite + Ant Design + Tailwind CSS. Use React Flow for the service graph if available.

Implement:
- Initial investigation input page
- Result workspace with tabs: Summary, Timeline, Service Graph, Sequence, Downstream Calls, Raw Logs, SPL
- Right-side AI follow-up panel
- Follow-up behavior where some questions answer from current result, while others trigger a mocked re-query and replace the active result
- Lightweight run history
- Detail drawer for timeline events, raw logs, downstream calls, graph nodes/edges, and SPL queries

Use the UI spec and data models from this document. Keep the code modular and backend-ready. Put mock data in src/mocks and types in src/types.
```
