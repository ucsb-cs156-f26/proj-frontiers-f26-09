import React from "react";
import OurTable, { ButtonColumn } from "main/components/OurTable";
import { Button, Tooltip, OverlayTrigger } from "react-bootstrap";

import { useBackendMutation } from "main/utils/useBackend";
import { hasRole } from "main/utils/currentUser";
import Modal from "react-bootstrap/Modal";
import CourseStaffForm from "main/components/CourseStaff/CourseStaffForm";
import { toast } from "react-toastify";
import CourseStaffDeleteModal from "main/components/CourseStaff/CourseStaffDeleteModal";
import { courseEmailsQueryKey } from "main/utils/courseEmailsUtils";

const cannotDeleteInstructorMessage =
  "You cannot delete the instructor as a staff member.";

/**
 * instructorEmail is the email of the course's instructor, whose row shows
 * "Instructor" instead of a Delete button. courseLoaded must be true once the
 * course's details (and so instructorEmail) have loaded; until then the Delete
 * buttons are disabled, so the instructor can't be deleted before we know who
 * they are.
 */
export default function CourseStaffTable({
  staff,
  currentUser,
  courseId,
  testIdPrefix = "CourseStaffTable",
  isInstructor = true,
  instructorEmail,
  courseLoaded = false,
}) {
  const [showEditModal, setShowEditModal] = React.useState(false);
  const [editStaff, setEditStaff] = React.useState(null);
  const [showDeleteModal, setShowDeleteModal] = React.useState(false);
  const [deleteStaff, setDeleteStaff] = React.useState(null);

  function cellToAxiosParamsDelete(formData) {
    return {
      // Stryker disable next-line StringLiteral
      url: "/api/coursestaff/delete",
      method: "DELETE",
      params: {
        id: formData.id,
        courseId: courseId,
        removeFromOrg: formData.removeFromOrg,
      },
    };
  }

  const cellToAxiosParamsEdit = (formData) => ({
    url: `/api/coursestaff`,
    method: "PUT",
    // Stryker disable next-line ObjectLiteral
    params: {
      firstName: formData.firstName,
      lastName: formData.lastName,
      id: formData.id,
      courseId: courseId,
    },
  });

  const hideModal = () => {
    setShowEditModal(false);
  };

  const hideDeleteModal = () => {
    setShowDeleteModal(false);
  };

  const onEditSuccess = () => {
    toast("Staff member updated successfully.");
    hideModal();
  };

  const onDeleteSuccess = () => {
    toast("Staff member deleted successfully.");
    hideDeleteModal();
  };

  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess },
    [
      `/api/coursestaff/course?courseId=${courseId}`,
      courseEmailsQueryKey(courseId),
    ],
  );

  // Stryker disable next-line all
  const deleteCallback = async (cell) => {
    setShowDeleteModal(true);
    setDeleteStaff(cell.row.original);
  };

  // If the staff member whose Delete window is open becomes the course's
  // instructor (for example, the course's details change while the window is
  // open), close the window instead of letting the deletion go ahead.
  React.useEffect(() => {
    if (showDeleteModal && deleteStaff.email === instructorEmail) {
      setShowDeleteModal(false);
      toast(cannotDeleteInstructorMessage);
    }
  }, [showDeleteModal, deleteStaff, instructorEmail]);

  const submitDeleteForm = (data) => {
    // Check again right before deleting, in case the staff member became the
    // instructor after the Delete window was opened.
    if (deleteStaff.email === instructorEmail) {
      return;
    }
    deleteMutation.mutate({
      id: deleteStaff.id,
      ...data,
    });
  };

  const editMutation = useBackendMutation(
    cellToAxiosParamsEdit,
    { onSuccess: onEditSuccess },
    [
      `/api/coursestaff/course?courseId=${courseId}`,
      courseEmailsQueryKey(courseId),
    ],
  );

  const editCallback = (cell) => {
    setEditStaff(cell.row.original);
    setShowEditModal(true);
  };

  const submitEditForm = (data) => {
    editMutation.mutate(data);
  };

  const columns = [
    {
      header: "id",
      accessorKey: "id",
      id: "id",
    },
    {
      header: "First Name",
      accessorKey: "firstName",
    },
    {
      header: "Last Name",
      accessorKey: "lastName",
    },
    {
      header: "Email",
      accessorKey: "email",
    },
    {
      header: "GitHub Login",
      accessorKey: "githubLogin",
    },
  ];

  const renderTooltip = (orgStatus) =>
    function TooltipWrapper(props) {
      let set_message;

      switch (orgStatus) {
        case "PENDING":
          set_message =
            "Staff member cannot join the course until it has been completely set up.";
          break;
        case "JOINCOURSE":
          set_message =
            "Staff member has been prompted to join, but hasn't yet clicked the 'Join Course' button to generate an invite to the organization.";
          break;
        case "INVITED":
          set_message =
            "Staff member has generated an invite, but has not yet accepted or declined the invitation.";
          break;
        case "OWNER":
          set_message =
            "Staff member is an owner of the GitHub organization associated with this course.";
          break;
        case "MEMBER":
          set_message =
            "Staff member is a member of the GitHub organization associated with this course.";
          break;
        default:
          set_message = "Tooltip for illegal status that will never occur";
          break;
      }
      return (
        <Tooltip id={`${orgStatus.toLowerCase()}-tooltip`} {...props}>
          {set_message}
        </Tooltip>
      );
    };

  columns.push({
    header: "Status",
    accessorKey: "orgStatus",
    cell: ({ cell }) => {
      const status = cell.row.original.orgStatus;
      if (status === "PENDING") {
        return (
          <OverlayTrigger placement="right" overlay={renderTooltip("PENDING")}>
            <span className="text-warning">Pending</span>
          </OverlayTrigger>
        );
      } else if (status === "JOINCOURSE") {
        return (
          <OverlayTrigger
            placement="right"
            overlay={renderTooltip("JOINCOURSE")}
          >
            <span className="text-primary">Join Course</span>
          </OverlayTrigger>
        );
      } else if (status === "INVITED") {
        return (
          <OverlayTrigger placement="right" overlay={renderTooltip("INVITED")}>
            <span className="text-primary">Invited</span>
          </OverlayTrigger>
        );
      } else if (status === "OWNER") {
        return (
          <OverlayTrigger placement="right" overlay={renderTooltip("OWNER")}>
            <span className="text-info">Owner</span>
          </OverlayTrigger>
        );
      } else if (status === "MEMBER") {
        return (
          <OverlayTrigger placement="right" overlay={renderTooltip("MEMBER")}>
            <span className="text-primary">Member</span>
          </OverlayTrigger>
        );
      }
      return (
        <OverlayTrigger
          placement="right"
          overlay={renderTooltip(cell.row.original.orgStatus)}
        >
          <span>{status}</span>
        </OverlayTrigger>
      );
    },
  });

  if (isInstructor && hasRole(currentUser, "ROLE_INSTRUCTOR")) {
    columns.push(ButtonColumn("Edit", "primary", editCallback, testIdPrefix));
    columns.push({
      id: "Delete",
      header: "Delete",
      cell: ({ cell }) => {
        // The instructor of the course can't be removed from the staff list,
        // so their row shows "Instructor" instead of a Delete button.
        if (cell.row.original.email === instructorEmail) {
          return (
            <span
              data-testid={`${testIdPrefix}-cell-row-${cell.row.index}-col-Delete-instructor`}
            >
              Instructor
            </span>
          );
        }
        return (
          <Button
            variant="danger"
            onClick={() => deleteCallback(cell)}
            disabled={!courseLoaded}
            data-testid={`${testIdPrefix}-cell-row-${cell.row.index}-col-Delete-button`}
          >
            Delete
          </Button>
        );
      },
    });
  }

  return (
    <>
      <Modal show={showEditModal} onHide={hideModal}>
        <Modal.Header closeButton>
          <Modal.Title>Edit Staff Member</Modal.Title>
        </Modal.Header>
        <Modal.Body
          className={"pb-3"}
          data-testid={`${testIdPrefix}-modal-body`}
        >
          <CourseStaffForm
            initialContents={editStaff}
            submitAction={submitEditForm}
            buttonLabel={"Update"}
            cancelDisabled={true}
          />
        </Modal.Body>
      </Modal>
      <CourseStaffDeleteModal
        showModal={showDeleteModal}
        toggleShowModal={setShowDeleteModal}
        onSubmitAction={submitDeleteForm}
      />
      <OurTable data={staff} columns={columns} testid={testIdPrefix} />
      <div
        style={{ display: "none" }}
        data-testid={`${testIdPrefix}-courseId`}
        data-course-id={`${courseId}`}
      />
    </>
  );
}
