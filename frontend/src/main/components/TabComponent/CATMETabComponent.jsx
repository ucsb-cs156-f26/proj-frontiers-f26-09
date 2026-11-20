import React from "react";
import { Button } from "react-bootstrap";
import { toast } from "react-toastify";

export default function CATMETabComponent({ courseId, testIdPrefix }) {
  const onSuccessDownloadTriggered = () => {
    toast("Download successfully initiated.");
  };

  // The download uses the same format as DonwloadsTabComponent with the toast called in this function.
  const downloadFormattedCATMECsv = () => {
    window.open(`/api/csv/catme?courseId=${courseId}`, "_blank");
    onSuccessDownloadTriggered();
  };

  return (
    <div data-testid={`${testIdPrefix}-catme-tab-component`}>
      <h4
        className="mt-4"
        data-testid={`${testIdPrefix}-downloads-teams-header`}
      >
        CATME Downloads
      </h4>
      <Button
        variant="primary"
        onClick={downloadFormattedCATMECsv}
        data-testid={`${testIdPrefix}-btn-catme-formatted-teams-csv`}
      >
        Download CSV Roster in CATME format
      </Button>
    </div>
  );
}
