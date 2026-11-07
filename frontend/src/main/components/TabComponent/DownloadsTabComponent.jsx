import React from "react";
import { Button } from "react-bootstrap";
import CourseDownloadsForm from "../Courses/CourseDownloadsForm";
import { toast } from "react-toastify";

export default function DownloadsTabComponent({ courseId, testIdPrefix }) {
  const onSuccessDownloadTriggered = () => {
    toast("Download successfully initiated.");
  };

  const handleSubmit = () => {
    console.log(`Frontend form submit action captured for course: ${courseId}`);
    onSuccessDownloadTriggered();
  };

  const downloadFormattedTeamsCsv = () => {
    window.open(`/api/csv/formattedteams?courseId=${courseId}`, "_blank");
  };

  return (
    <div data-testid={`${testIdPrefix}-downloadsTab`}>
      <CourseDownloadsForm
        downloadAction={handleSubmit}
        testIdPrefix={testIdPrefix}
      />
      <h4
        className="mt-4"
        data-testid={`${testIdPrefix}-downloads-teams-header`}
      >
        Teams
      </h4>
      <p data-testid={`${testIdPrefix}-downloads-teams-description`}>
        Every student&apos;s name and team, listed first by name and then by
        team, in side-by-side columns. Imported into Google Sheets, it only
        needs some formatting and splitting into two tabs to become a team
        sheet.
      </p>
      <Button
        variant="primary"
        onClick={downloadFormattedTeamsCsv}
        data-testid={`${testIdPrefix}-btn-download-formatted-teams-csv`}
      >
        Download Formatted Teams CSV
      </Button>
    </div>
  );
}
