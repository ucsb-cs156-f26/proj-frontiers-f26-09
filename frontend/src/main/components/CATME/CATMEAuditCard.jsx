import React, { useState } from "react";
import { Alert, Card } from "react-bootstrap";
import { useBackendMutation } from "main/utils/useBackend";
import CATMEAuditCSVUploadForm from "main/components/CATME/CATMEAuditCSVUploadForm";
import CATMEAuditResults from "main/components/CATME/CATMEAuditResults";

export default function CATMEAuditCard({ courseId, testIdPrefix }) {
  const [results, setResults] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  const objectToAxiosParams = (formData) => {
    const file = new FormData();
    file.append("file", formData.upload[0]);
    return {
      url: "/api/catme/audit",
      method: "POST",
      data: file,
      params: { courseId },
    };
  };

  const auditMutation = useBackendMutation(objectToAxiosParams, {
    onMutate: () => {
      setResults(null);
      setErrorMessage(null);
    },
    onSuccess: (data) => {
      setResults(data);
    },
    onError: (error) => {
      setErrorMessage(
        error.response?.data?.message ??
          "The audit could not be completed. Please try again.",
      );
    },
  });

  return (
    <Card className="mt-4" data-testid={`${testIdPrefix}-catme-audit-card`}>
      <Card.Header as="h5" data-testid={`${testIdPrefix}-catme-audit-header`}>
        Audit CATME Teammaker CSV Download
      </Card.Header>
      <Card.Body>
        <p>
          When using CATME Teammaker, this option will allow you to ensure that
          the roster of students in CATME is up-to-date with the course roster
          in Frontiers. It treats the Frontiers as the &quot;source of
          truth&quot; and lets you know what students need to be added to CATME,
          removed from CATME, or have their information in CATME (name, section)
          updated.
        </p>
        <p className="mb-1">To use it:</p>
        <ol>
          <li>
            Navigate to the page <code>/faculty/tm_display_results</code> in
            CATME and click the Export to CSV button.
          </li>
          <li>Upload that file here.</li>
          <li>Read the results.</li>
        </ol>
        <CATMEAuditCSVUploadForm
          submitAction={(formData) => auditMutation.mutate(formData)}
          testIdPrefix={`${testIdPrefix}-catme-audit-form`}
        />
        {errorMessage !== null && (
          <Alert
            variant="danger"
            className="mt-3"
            data-testid={`${testIdPrefix}-catme-audit-error`}
          >
            {errorMessage}
          </Alert>
        )}
        {results !== null && (
          <CATMEAuditResults
            results={results}
            testIdPrefix={`${testIdPrefix}-catme-audit-results`}
          />
        )}
      </Card.Body>
    </Card>
  );
}
