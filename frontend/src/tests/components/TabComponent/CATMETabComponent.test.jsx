import { fireEvent, render, screen } from "@testing-library/react";
import CATMETabComponent from "main/components/TabComponent/CATMETabComponent";
import { expect, vi } from "vitest";

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

describe("CATMETabComponent tests", () => {
  const testIdPrefix = "InstructorCourseShowPage";
  let openSpy;

  beforeEach(() => {
    mockToast.mockClear();
    openSpy = vi.spyOn(window, "open").mockImplementation(() => null);
  });

  afterEach(() => {
    openSpy.mockRestore();
  });

  test("renders the tab container, the header and the download button", () => {
    render(<CATMETabComponent courseId={1} testIdPrefix={testIdPrefix} />);

    expect(
      screen.getByTestId(`${testIdPrefix}-catme-tab-component`),
    ).toBeInTheDocument();

    const header = screen.getByTestId(`${testIdPrefix}-downloads-teams-header`);
    expect(header).toHaveTextContent("CATME Downloads");
    expect(header.tagName).toBe("H4");
    expect(header).toHaveClass("mt-4");

    const button = screen.getByTestId(
      `${testIdPrefix}-btn-catme-formatted-teams-csv`,
    );
    expect(button).toHaveTextContent("Download CSV Roster in CATME format");
    expect(button).toHaveClass("btn-primary");
  });

  test("uses the given testIdPrefix for all of its test ids", () => {
    render(<CATMETabComponent courseId={1} testIdPrefix="SomeOtherPrefix" />);

    expect(
      screen.getByTestId("SomeOtherPrefix-catme-tab-component"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("SomeOtherPrefix-downloads-teams-header"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("SomeOtherPrefix-btn-catme-formatted-teams-csv"),
    ).toBeInTheDocument();
  });

  test("does not open a download or show a toast until the button is clicked", () => {
    render(<CATMETabComponent courseId={1} testIdPrefix={testIdPrefix} />);

    expect(openSpy).not.toHaveBeenCalled();
    expect(mockToast).not.toHaveBeenCalled();
  });

  test("clicking the button opens the CATME csv for this course in a new tab", () => {
    render(<CATMETabComponent courseId={7} testIdPrefix={testIdPrefix} />);

    fireEvent.click(
      screen.getByTestId(`${testIdPrefix}-btn-catme-formatted-teams-csv`),
    );

    expect(openSpy).toHaveBeenCalledTimes(1);
    // The leading slash matters: without it, the browser resolves the URL
    // relative to /instructor/courses/<id> instead of the site root.
    expect(openSpy).toHaveBeenCalledWith("/api/csv/catme?courseId=7", "_blank");
  });

  test("clicking the button shows the download toast exactly once", () => {
    render(<CATMETabComponent courseId={7} testIdPrefix={testIdPrefix} />);

    fireEvent.click(
      screen.getByTestId(`${testIdPrefix}-btn-catme-formatted-teams-csv`),
    );

    expect(mockToast).toHaveBeenCalledTimes(1);
    expect(mockToast).toHaveBeenCalledWith("Download successfully initiated.");
  });

  test("each click opens a download and shows a toast", () => {
    render(<CATMETabComponent courseId={3} testIdPrefix={testIdPrefix} />);
    const button = screen.getByTestId(
      `${testIdPrefix}-btn-catme-formatted-teams-csv`,
    );

    fireEvent.click(button);
    fireEvent.click(button);

    expect(openSpy).toHaveBeenCalledTimes(2);
    expect(openSpy).toHaveBeenNthCalledWith(
      2,
      "/api/csv/catme?courseId=3",
      "_blank",
    );
    expect(mockToast).toHaveBeenCalledTimes(2);
  });
});
