import { fireEvent, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { vi } from "vitest";
import CourseStaffTable from "main/components/CourseStaff/CourseStaffTable";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";

const mockMutate = vi.fn();

vi.mock("main/utils/useBackend", async (importOriginal) => ({
  ...(await importOriginal()),
  useBackendMutation: () => ({ mutate: mockMutate }),
}));

// A stand-in for the Delete window whose submit button is always on the page,
// so the test can submit even after the real window would have closed.
vi.mock("main/components/CourseStaff/CourseStaffDeleteModal", () => ({
  default: ({ onSubmitAction }) => (
    <button
      data-testid="FakeDeleteModal-submit"
      onClick={() => onSubmitAction({ removeFromOrg: "false" })}
    >
      Submit
    </button>
  ),
}));

vi.mock("react-toastify", async (importOriginal) => ({
  ...(await importOriginal()),
  toast: vi.fn(),
}));

describe("CourseStaffTable delete guard", () => {
  const staff = courseStaffFixtures.threeStaff;
  const queryClient = new QueryClient();

  const renderTable = (instructorEmail) => (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <CourseStaffTable
          staff={staff}
          currentUser={currentUserFixtures.adminUser}
          courseId={7}
          courseLoaded={true}
          instructorEmail={instructorEmail}
        />
      </MemoryRouter>
    </QueryClientProvider>
  );

  beforeEach(() => {
    mockMutate.mockClear();
  });

  test("does not delete a staff member who became the instructor after the Delete window was opened", () => {
    const { rerender } = render(renderTable(staff[0].email));

    // open the Delete window for the staff member in row 1
    fireEvent.click(
      screen.getByTestId("CourseStaffTable-cell-row-1-col-Delete-button"),
    );

    // they become the instructor, then the deletion is submitted anyway
    rerender(renderTable(staff[1].email));
    fireEvent.click(screen.getByTestId("FakeDeleteModal-submit"));

    expect(mockMutate).not.toHaveBeenCalled();

    // once they are no longer the instructor, the same submit goes through
    rerender(renderTable(staff[0].email));
    fireEvent.click(screen.getByTestId("FakeDeleteModal-submit"));

    expect(mockMutate).toHaveBeenCalledTimes(1);
    expect(mockMutate).toHaveBeenCalledWith({
      id: staff[1].id,
      removeFromOrg: "false",
    });
  });
});
