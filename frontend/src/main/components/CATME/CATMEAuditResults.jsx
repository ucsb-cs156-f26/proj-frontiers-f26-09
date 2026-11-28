import React from "react";
import { Alert, Table } from "react-bootstrap";

export default function CATMEAuditResults({
  results,
  testIdPrefix = "CATMEAuditResults",
}) {
  const studentsToUpdate = results.studentsToUpdate;
  const studentsToDrop = results.studentsToDrop;

  if (studentsToUpdate.length === 0 && studentsToDrop.length === 0) {
    return (
      <Alert variant="success" data-testid={`${testIdPrefix}-up-to-date`}>
        CATME is up to date with the Frontiers roster. No changes are needed.
      </Alert>
    );
  }

  return (
    <div data-testid={`${testIdPrefix}-results`}>
      <h6 className="mt-3" data-testid={`${testIdPrefix}-drop-header`}>
        Remove from CATME ({studentsToDrop.length})
      </h6>
      <p className="text-muted mb-1">
        These students are in the CATME file but are not currently enrolled in
        Frontiers.
      </p>
      {studentsToDrop.length === 0 ? (
        <p data-testid={`${testIdPrefix}-drop-none`}>No students to remove.</p>
      ) : (
        <Table striped bordered size="sm" data-testid={`${testIdPrefix}-drop`}>
          <thead>
            <tr>
              <th>Student ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Section</th>
            </tr>
          </thead>
          <tbody>
            {studentsToDrop.map((student, index) => (
              <tr key={index}>
                <td data-testid={`${testIdPrefix}-drop-row-${index}-studentId`}>
                  {student.studentId}
                </td>
                <td data-testid={`${testIdPrefix}-drop-row-${index}-name`}>
                  {student.name}
                </td>
                <td data-testid={`${testIdPrefix}-drop-row-${index}-email`}>
                  {student.email}
                </td>
                <td data-testid={`${testIdPrefix}-drop-row-${index}-section`}>
                  {student.section}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}

      <h6 className="mt-3" data-testid={`${testIdPrefix}-update-header`}>
        Update in CATME ({studentsToUpdate.length})
      </h6>
      <p className="text-muted mb-1">
        These students are in CATME, but their name or section does not match
        Frontiers.
      </p>
      {studentsToUpdate.length === 0 ? (
        <p data-testid={`${testIdPrefix}-update-none`}>
          No students to update.
        </p>
      ) : (
        <Table
          striped
          bordered
          size="sm"
          data-testid={`${testIdPrefix}-update`}
        >
          <thead>
            <tr>
              <th>Student ID</th>
              <th>Name</th>
              <th>Field</th>
              <th>Currently in CATME</th>
              <th>Should be</th>
            </tr>
          </thead>
          <tbody>
            {studentsToUpdate.map((update, index) => (
              <tr key={index}>
                <td
                  data-testid={`${testIdPrefix}-update-row-${index}-studentId`}
                >
                  {update.studentId}
                </td>
                <td data-testid={`${testIdPrefix}-update-row-${index}-name`}>
                  {update.name}
                </td>
                <td data-testid={`${testIdPrefix}-update-row-${index}-field`}>
                  {update.field}
                </td>
                <td
                  data-testid={`${testIdPrefix}-update-row-${index}-oldValue`}
                >
                  {update.oldValue}
                </td>
                <td
                  data-testid={`${testIdPrefix}-update-row-${index}-newValue`}
                >
                  {update.newValue}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}
