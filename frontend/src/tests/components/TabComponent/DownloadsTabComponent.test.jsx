import axios from "axios";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import DownloadsTabComponent from "main/components/TabComponent/DownloadsTabComponent";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, vi } from "vitest";
import coursesFixtures from "fixtures/coursesFixtures";
import * as useBackendModule from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);
const mockToast = vi.fn();

const useBackendMutationSpy = vi.spyOn(useBackendModule, "useBackendMutation");

vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

describe("DownloadsTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.resetHistory();
    mockToast.mockClear();
  });

  afterEach(() => {
    useBackendMutationSpy.mockClear();
  });

  test("Downloads tab component and form elements render correctly", async () => {
    axiosMock.onGet("/api/courses/downloadStudentsCSV").reply(200);
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <DownloadsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          testIdPrefix="InstructorCourseShowPage"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("InstructorCourseShowPage-downloadsTab");

    expect(screen.getByText("Course Downloads")).toBeInTheDocument();
    expect(
      screen.getByTestId("InstructorCourseShowPage-downloads-header"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("InstructorCourseShowPage-btn-download-students-csv"),
    ).toBeInTheDocument();
  });

  test("Fires submit download handler cleanly on button click", async () => {
    const consoleSpy = vi.spyOn(console, "log").mockImplementation(() => {});
    axiosMock.onGet("/api/courses/downloadStudentsCSV").reply(200);
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <DownloadsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          testIdPrefix="InstructorCourseShowPage"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId(
      "InstructorCourseShowPage-btn-download-students-csv",
    );

    const submitButton = screen.getByTestId(
      "InstructorCourseShowPage-btn-download-students-csv",
    );
    fireEvent.click(submitButton);

    await waitFor(() => expect(mockToast).toHaveBeenCalled());
    expect(mockToast).toBeCalledWith("Download successfully initiated.");

    expect(consoleSpy).toHaveBeenCalledWith(
      "Frontend form submit action captured for course: 1",
    );
    consoleSpy.mockRestore();
  });

  test("Formatted teams CSV section renders correctly", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <DownloadsTabComponent
          courseId={7}
          testIdPrefix="InstructorCourseShowPage"
        />
      </QueryClientProvider>,
    );

    const header = await screen.findByTestId(
      "InstructorCourseShowPage-downloads-teams-header",
    );
    expect(header).toHaveTextContent("Teams");
    expect(header).toHaveClass("mt-4");
    expect(
      screen.getByTestId(
        "InstructorCourseShowPage-downloads-teams-description",
      ),
    ).toHaveTextContent(
      "Every student's name and team, listed first by name and then by team, in side-by-side columns. Imported into Google Sheets, it only needs some formatting and splitting into two tabs to become a team sheet.",
    );
    const button = screen.getByTestId(
      "InstructorCourseShowPage-btn-download-formatted-teams-csv",
    );
    expect(button).toHaveTextContent("Download Formatted Teams CSV");
    expect(button).toHaveClass("btn-primary");
  });

  test("Formatted teams CSV button opens the download for this course", async () => {
    const openSpy = vi.spyOn(window, "open").mockImplementation(() => null);
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <DownloadsTabComponent
          courseId={7}
          testIdPrefix="InstructorCourseShowPage"
        />
      </QueryClientProvider>,
    );

    fireEvent.click(
      await screen.findByTestId(
        "InstructorCourseShowPage-btn-download-formatted-teams-csv",
      ),
    );

    expect(openSpy).toHaveBeenCalledTimes(1);
    expect(openSpy).toHaveBeenCalledWith(
      "/api/csv/formattedteams?courseId=7",
      "_blank",
    );
    expect(mockToast).not.toHaveBeenCalled();
    openSpy.mockRestore();
  });
});
