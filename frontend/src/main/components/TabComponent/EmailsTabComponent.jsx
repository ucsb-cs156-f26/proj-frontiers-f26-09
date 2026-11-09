import React, { useState } from "react";
import { Form } from "react-bootstrap";
import { useBackend } from "main/utils/useBackend";
import CourseEmailsCard from "main/components/Emails/CourseEmailsCard";
import { EMAIL_TYPES } from "main/utils/courseEmailsUtils";

export default function EmailsTabComponent({ courseId, testIdPrefix }) {
  const [type, setType] = useState(EMAIL_TYPES[0].value);
  const [team, setTeam] = useState("");

  const { data: teams } = useBackend(
    [`/api/teams/all?courseId=${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/teams/all?courseId=${courseId}` },
    [],
    true,
  );

  // The endpoint ignores team when type is STAFF
  const selectedTeam = type === "STAFF" ? "" : team;

  return (
    <div data-testid={`${testIdPrefix}-emails-tab-component`}>
      <Form.Group className="mb-3" controlId={`${testIdPrefix}-emails-type`}>
        <Form.Label>Type</Form.Label>
        <Form.Select
          value={type}
          onChange={(e) => setType(e.target.value)}
          data-testid={`${testIdPrefix}-emails-type`}
        >
          {EMAIL_TYPES.map((t) => (
            <option key={t.value} value={t.value}>
              {t.label}
            </option>
          ))}
        </Form.Select>
      </Form.Group>
      <Form.Group className="mb-3" controlId={`${testIdPrefix}-emails-team`}>
        <Form.Label>Team</Form.Label>
        <Form.Select
          value={selectedTeam}
          disabled={type === "STAFF"}
          onChange={(e) => setTeam(e.target.value)}
          data-testid={`${testIdPrefix}-emails-team`}
        >
          <option value="">All teams</option>
          {teams.map((t) => (
            <option key={t.id} value={t.name}>
              {t.name}
            </option>
          ))}
        </Form.Select>
      </Form.Group>
      <CourseEmailsCard
        courseId={courseId}
        type={type}
        team={selectedTeam}
        testIdPrefix={`${testIdPrefix}-EmailsTabCard`}
      />
    </div>
  );
}
