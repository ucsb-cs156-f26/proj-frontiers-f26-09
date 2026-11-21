import { toast } from "react-toastify";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import React, { useState } from "react";
import {
  Button,
  Col,
  Form,
  ModalBody,
  ModalHeader,
  Row,
  OverlayTrigger,
  Tooltip,
} from "react-bootstrap";
import { BsInfoCircle } from "react-icons/bs";
import CourseStaffForm from "main/components/CourseStaff/CourseStaffForm";
import CourseStaffCSVUploadForm from "main/components/CourseStaff/CourseStaffCSVUploadForm";
import CourseStaffTable from "main/components/CourseStaff/CourseStaffTable";
import Modal from "react-bootstrap/Modal";
import CourseEmailsCard from "main/components/Emails/CourseEmailsCard";
import { courseEmailsQueryKey } from "main/utils/courseEmailsUtils";

export default function StaffTabComponent({
  courseId,
  testIdPrefix,
  currentUser,
  isInstructor = true,
}) {
  const [postModal, showPostModal] = useState(false);
  const [csvModal, setCsvModal] = useState(false);
  const { data: courseStaff } = useBackend(
    [`/api/coursestaff/course?courseId=${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/coursestaff/course?courseId=${courseId}` },
    [],
    true,
  );
  const [searchTerm, setSearchTerm] = useState("");

  const objectToAxiosParamsPost = (staff) => ({
    url: `/api/coursestaff/post`,
    method: "POST",
    params: {
      courseId: courseId,
      firstName: staff.firstName,
      lastName: staff.lastName,
      email: staff.email,
    },
  });

  const objectToAxiosParamsCSV = (formData) => {
    const file = new FormData();
    file.append("file", formData.upload[0]);
    return {
      url: `/api/coursestaff/upload/csv`,
      data: file,
      params: {
        courseId: courseId,
      },
      method: "POST",
    };
  };

  const onSuccessStaff = (modalFn) => {
    toast("Staff roster successfully updated.");
    // Clear the search filter to show the updated roster
    setSearchTerm("");
    modalFn(false);
  };

  const staffPostMutation = useBackendMutation(
    objectToAxiosParamsPost,
    { onSuccess: () => onSuccessStaff(showPostModal) },
    [
      `/api/coursestaff/course?courseId=${courseId}`,
      courseEmailsQueryKey(courseId),
    ],
  );

  const staffCsvMutation = useBackendMutation(
    objectToAxiosParamsCSV,
    {
      onSuccess: () => onSuccessStaff(setCsvModal),
      onError: (error) => {
        toast.error(
          `Error uploading CSV: ${JSON.stringify(error.response.data, null, 2)}`,
        );
        setCsvModal(false);
      },
    },
    [
      `/api/coursestaff/course?courseId=${courseId}`,
      courseEmailsQueryKey(courseId),
    ],
  );

  const handlePostSubmit = (staff) => {
    staffPostMutation.mutate(staff);
  };

  const handleCsvSubmit = (formData) => {
    staffCsvMutation.mutate(formData);
  };

  const openCsvHelp = () => {
    window.open("/help/csv#staff-information", "_blank");
  };

  // The download uses the same format as Upload Staff CSV
  const downloadCsv = () => {
    window.open(`/api/csv/staff?courseId=${courseId}`, "_blank");
  };

  return (
    <div data-testid={`${testIdPrefix}-StaffTabComponent`}>
      <Modal
        show={postModal}
        onHide={() => showPostModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-post-modal`}
      >
        <ModalHeader closeButton>Add Staff Member</ModalHeader>
        <ModalBody>
          <CourseStaffForm
            submitAction={handlePostSubmit}
            cancelDisabled={true}
          />
        </ModalBody>
      </Modal>
      <Modal
        show={csvModal}
        onHide={() => setCsvModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-csv-modal`}
      >
        <ModalHeader closeButton>Upload Staff CSV</ModalHeader>
        <ModalBody>
          <CourseStaffCSVUploadForm submitAction={handleCsvSubmit} />
        </ModalBody>
      </Modal>
      {isInstructor && (
        <Row sm={3} className="p-2">
          <Col>
            <div className="d-flex align-items-center position-relative">
              <Button
                onClick={() => setCsvModal(true)}
                data-testid={`${testIdPrefix}-csv-button`}
                className="w-100 pe-5"
              >
                Upload Staff CSV
              </Button>
              <CsvHelpIcon
                tooltipId="csv-help-tooltip"
                placement="right"
                onClick={openCsvHelp}
                testId={`${testIdPrefix}-csv-info-icon`}
              >
                Staff CSV Upload Format Help
              </CsvHelpIcon>
            </div>
          </Col>
          <Col>
            <Button
              onClick={() => showPostModal(true)}
              data-testid={`${testIdPrefix}-post-button`}
              className="w-100"
            >
              Add Staff Member
            </Button>
          </Col>
          <Col>
            <div className="d-flex align-items-center position-relative">
              <Button
                onClick={downloadCsv}
                data-testid={`${testIdPrefix}-download-csv-button`}
                className="w-100 pe-5"
              >
                Download Staff CSV
              </Button>
              {/* the button is at the right edge of the page, so the tooltip opens to the left */}
              <CsvHelpIcon
                tooltipId="csv-download-help-tooltip"
                placement="left"
                onClick={openCsvHelp}
                testId={`${testIdPrefix}-download-csv-info-icon`}
              >
                Staff CSV Download Format Help (same format as upload)
              </CsvHelpIcon>
            </div>
          </Col>
        </Row>
      )}
      <Row className="mb-1">
        <Form>
          <Form.Group as={Row} controlId="searchFilter">
            <Form.Label column sm={2}>
              Search Staff:
            </Form.Label>
            <Col sm={10}>
              <Form.Control
                type="text"
                placeholder="Search by name, email, or Github Login"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                data-testid={`${testIdPrefix}-search`}
              />
            </Col>
          </Form.Group>
        </Form>
      </Row>
      <Row>
        <CourseStaffTable
          staff={courseStaff.filter((staffMember) => {
            const searchTermLower = searchTerm.toLowerCase();
            const fullName = `${staffMember.firstName} ${staffMember.lastName}`;
            if (staffMember.email.toLowerCase().includes(searchTermLower)) {
              return true;
            } else if (
              staffMember.githubLogin?.toLowerCase().includes(searchTermLower)
            ) {
              return true;
            } else if (fullName.toLowerCase().includes(searchTermLower)) {
              return true;
            }
            return false;
          })}
          currentUser={currentUser}
          courseId={courseId}
          testIdPrefix={`${testIdPrefix}-CourseStaffTable`}
          isInstructor={isInstructor}
        />
      </Row>
      <CourseEmailsCard
        courseId={courseId}
        type="STAFF"
        testIdPrefix={`${testIdPrefix}-StaffEmailsCard`}
      />
    </div>
  );
}

/**
 * An info icon placed over the right end of a CSV button; hovering shows the
 * tooltip given as children on the given side of the icon, and clicking calls
 * onClick (which opens the CSV help page).
 */
function CsvHelpIcon({ tooltipId, placement, onClick, testId, children }) {
  return (
    <OverlayTrigger
      placement={placement}
      overlay={<Tooltip id={tooltipId}>{children}</Tooltip>}
    >
      <BsInfoCircle
        onClick={onClick}
        style={{
          position: "absolute",
          top: "50%",
          right: "0.75rem",
          transform: "translateY(-50%)",
          color: "#fff",
          cursor: "pointer",
          fontSize: "0.9rem",
          userSelect: "none",
        }}
        data-testid={testId}
      />
    </OverlayTrigger>
  );
}
