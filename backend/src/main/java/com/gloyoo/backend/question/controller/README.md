# Question Controller

`QuestionController` exposes the REST API for managing questions that belong to surveys. It supports listing questions for a survey, retrieving a single question, creating, updating, deleting, and reading answer statistics for a question.

Source file:

- `backend/src/main/java/com/gloyoo/backend/question/controller/QuestionController.java`

## Base Route

All routes in this controller are mounted under:

```http
/questions
```

## Authentication

Most controller methods read the authenticated user through:

```java
@AuthenticationPrincipal AuthenticatedUser user
```

The authenticated user's `id()` is passed to the service layer for ownership checks. This means that operations such as listing questions by survey, creating questions, updating questions, deleting questions, and reading statistics are scoped to surveys owned by the authenticated user.

`GET /questions/{id}` does not currently receive an authenticated user in the controller method and delegates directly to `questionService.getQuestionById(id)`.

## Question Types

The supported question types are defined by `QuestionType`:

```java
Single_Choice
Multiple_Choice
Freetext
```

Use these exact enum values in request JSON.

## Request Body

Create and update endpoints use `QuestionRequestDto`.

```json
{
  "question": "1. Yes\n2. No",
  "type": "Single_Choice",
  "surveyId": "2d0df3b1-3f5a-4e85-b894-2a4ed8d36350"
}
```

### Fields

| Field | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| `question` | `string` | Yes | `@NotBlank` | The question text. For choice questions, this text is parsed for answer choices by the service layer. |
| `type` | `QuestionType` | Yes | `@NotNull` | The question type. Must be `Single_Choice`, `Multiple_Choice`, or `Freetext`. |
| `surveyId` | `UUID` | Yes | `@NotNull` | The survey that owns the question. The authenticated user must own this survey for create and update operations. |

## Response Body

Most successful question endpoints return `QuestionResponseDto`.

```json
{
  "id": "e1f65e80-88f8-4f57-9b1a-25616611c8e9",
  "question": "1. Yes\n2. No",
  "type": "Single_Choice",
  "surveyId": "2d0df3b1-3f5a-4e85-b894-2a4ed8d36350"
}
```

### Fields

| Field | Type | Description |
| --- | --- | --- |
| `id` | `UUID` | Unique identifier of the question. |
| `question` | `string` | Question text. |
| `type` | `QuestionType` | Question type. |
| `surveyId` | `UUID` | ID of the survey that owns the question. |

## Endpoints

### List Questions for a Survey

```http
GET /questions?surveyId={surveyId}
```

Returns all questions belonging to the given survey.

#### Query Parameters

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `surveyId` | `UUID` | Yes | ID of the survey whose questions should be returned. |

#### Authentication and Ownership

The authenticated user must own the survey. The controller passes the current user's ID and the `surveyId` to:

```java
questionService.getQuestionsBySurvey(surveyId, user.id())
```

#### Successful Response

Status: `200 OK`

```json
[
  {
    "id": "e1f65e80-88f8-4f57-9b1a-25616611c8e9",
    "question": "1. Yes\n2. No",
    "type": "Single_Choice",
    "surveyId": "2d0df3b1-3f5a-4e85-b894-2a4ed8d36350"
  }
]
```

#### Example

```bash
curl -X GET "http://localhost:8080/questions?surveyId=2d0df3b1-3f5a-4e85-b894-2a4ed8d36350" \
  -H "Authorization: Bearer <token>"
```

### Get Question by ID

```http
GET /questions/{id}
```

Returns one question by its ID.

#### Path Parameters

| Name | Type | Description |
| --- | --- | --- |
| `id` | `UUID` | ID of the question to retrieve. |

#### Successful Response

Status: `200 OK`

```json
{
  "id": "e1f65e80-88f8-4f57-9b1a-25616611c8e9",
  "question": "1. Yes\n2. No",
  "type": "Single_Choice",
  "surveyId": "2d0df3b1-3f5a-4e85-b894-2a4ed8d36350"
}
```

#### Example

```bash
curl -X GET "http://localhost:8080/questions/e1f65e80-88f8-4f57-9b1a-25616611c8e9" \
  -H "Authorization: Bearer <token>"
```

### Get Question Statistics

```http
GET /questions/{id}/statistics
```

Returns answer statistics for a question.

#### Path Parameters

| Name | Type | Description |
| --- | --- | --- |
| `id` | `UUID` | ID of the question whose statistics should be returned. |

#### Authentication and Ownership

The authenticated user must own the question's survey. The controller passes the question ID and current user's ID to:

```java
questionService.getStatistics(id, user.id())
```

#### Successful Response

Status: `200 OK`

```json
{
  "questionId": "e1f65e80-88f8-4f57-9b1a-25616611c8e9",
  "type": "Single_Choice",
  "totalAnswers": 10,
  "choices": [
    {
      "choice": 1,
      "label": "Yes",
      "count": 7,
      "respondentPercentage": 70.0
    },
    {
      "choice": 2,
      "label": "No",
      "count": 3,
      "respondentPercentage": 30.0
    }
  ]
}
```

#### Freetext Statistics

For `Freetext` questions, each submitted answer is returned as an entry in `choices`. The `label` contains the answer text, `count` is `1`, and `respondentPercentage` is `100.0` for each answer.

#### Example

```bash
curl -X GET "http://localhost:8080/questions/e1f65e80-88f8-4f57-9b1a-25616611c8e9/statistics" \
  -H "Authorization: Bearer <token>"
```

### Create Question

```http
POST /questions
```

Creates a new question in a survey owned by the authenticated user.

#### Request Body

```json
{
  "question": "1. Very satisfied\n2. Satisfied\n3. Neutral\n4. Unsatisfied",
  "type": "Single_Choice",
  "surveyId": "2d0df3b1-3f5a-4e85-b894-2a4ed8d36350"
}
```

#### Authentication and Ownership

The authenticated user must own the survey referenced by `surveyId`.

#### Successful Response

Status: `201 Created`

```json
{
  "id": "e1f65e80-88f8-4f57-9b1a-25616611c8e9",
  "question": "1. Very satisfied\n2. Satisfied\n3. Neutral\n4. Unsatisfied",
  "type": "Single_Choice",
  "surveyId": "2d0df3b1-3f5a-4e85-b894-2a4ed8d36350"
}
```

#### Example

```bash
curl -X POST "http://localhost:8080/questions" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "1. Very satisfied\n2. Satisfied\n3. Neutral\n4. Unsatisfied",
    "type": "Single_Choice",
    "surveyId": "2d0df3b1-3f5a-4e85-b894-2a4ed8d36350"
  }'
```

### Update Question

```http
PUT /questions/{id}
```

Updates an existing question.

#### Path Parameters

| Name | Type | Description |
| --- | --- | --- |
| `id` | `UUID` | ID of the question to update. |

#### Request Body

```json
{
  "question": "1. Excellent\n2. Good\n3. Average\n4. Poor",
  "type": "Single_Choice",
  "surveyId": "2d0df3b1-3f5a-4e85-b894-2a4ed8d36350"
}
```

#### Authentication and Ownership

The authenticated user must own the existing question's survey and the target survey referenced by `surveyId`.

#### Successful Response

Status: `200 OK`

```json
{
  "id": "e1f65e80-88f8-4f57-9b1a-25616611c8e9",
  "question": "1. Excellent\n2. Good\n3. Average\n4. Poor",
  "type": "Single_Choice",
  "surveyId": "2d0df3b1-3f5a-4e85-b894-2a4ed8d36350"
}
```

#### Example

```bash
curl -X PUT "http://localhost:8080/questions/e1f65e80-88f8-4f57-9b1a-25616611c8e9" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "1. Excellent\n2. Good\n3. Average\n4. Poor",
    "type": "Single_Choice",
    "surveyId": "2d0df3b1-3f5a-4e85-b894-2a4ed8d36350"
  }'
```

### Delete Question

```http
DELETE /questions/{id}
```

Deletes an existing question.

#### Path Parameters

| Name | Type | Description |
| --- | --- | --- |
| `id` | `UUID` | ID of the question to delete. |

#### Authentication and Ownership

The authenticated user must own the question's survey.

#### Successful Response

Status: `204 No Content`

The response body is empty.

#### Example

```bash
curl -X DELETE "http://localhost:8080/questions/e1f65e80-88f8-4f57-9b1a-25616611c8e9" \
  -H "Authorization: Bearer <token>"
```

## Error Handling

This controller handles `EntityNotFoundException` locally.

### Not Found

Status: `404 Not Found`

```json
{
  "message": "Question not found with id: e1f65e80-88f8-4f57-9b1a-25616611c8e9"
}
```

This error can occur when:

- A question ID does not exist.
- A survey ID does not exist.
- The authenticated user does not own the survey or question being accessed.

## Validation Errors

Request validation is enabled with `@Valid` on create and update request bodies.

Invalid request bodies can fail when:

- `question` is missing, empty, or blank.
- `type` is missing or does not match a valid `QuestionType` enum value.
- `surveyId` is missing or is not a valid UUID.

Validation errors are handled by Spring's default validation/error handling unless a global exception handler overrides them elsewhere in the application.

## Service Layer Responsibilities

The controller is intentionally thin. Business rules are delegated to `QuestionService`.

Important service responsibilities include:

- Verifying survey and question ownership.
- Parsing and validating choice formats for choice-based questions.
- Loading questions from the repository.
- Persisting created or updated questions.
- Deleting questions.
- Calculating statistics from stored answers.

## Implementation Summary

| Method | Route | Status | Service Call |
| --- | --- | --- | --- |
| `getAllQuestions` | `GET /questions?surveyId={surveyId}` | `200 OK` | `getQuestionsBySurvey(surveyId, user.id())` |
| `getQuestionById` | `GET /questions/{id}` | `200 OK` | `getQuestionById(id)` |
| `getQuestionStatistics` | `GET /questions/{id}/statistics` | `200 OK` | `getStatistics(id, user.id())` |
| `createQuestion` | `POST /questions` | `201 Created` | `createQuestion(requestDto, user.id())` |
| `updateQuestion` | `PUT /questions/{id}` | `200 OK` | `updateQuestion(id, requestDto, user.id())` |
| `deleteQuestion` | `DELETE /questions/{id}` | `204 No Content` | `deleteQuestion(id, user.id())` |
