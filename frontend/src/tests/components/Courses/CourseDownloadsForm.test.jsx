import { createEvent, fireEvent, render, screen } from "@testing-library/react";
import { vi } from "vitest";

import CourseDownloadsForm from "main/components/Courses/CourseDownloadsForm";

describe("CourseDownloadsForm tests", () => {
  test("renders the header and the download button", () => {
    render(
      <CourseDownloadsForm downloadAction={vi.fn()} testIdPrefix="Prefix" />,
    );

    expect(screen.getByTestId("Prefix-downloads-header")).toHaveTextContent(
      /^Course Downloads$/,
    );
    expect(
      screen.getByTestId("Prefix-btn-download-students-csv"),
    ).toHaveTextContent(/^Download Students CSV$/);
  });

  test("submitting the form calls downloadAction and prevents the default form submission", () => {
    const downloadAction = vi.fn();
    render(
      <CourseDownloadsForm
        downloadAction={downloadAction}
        testIdPrefix="Prefix"
      />,
    );

    const form = screen.getByTestId("Prefix-downloads-header").closest("form");
    const submitEvent = createEvent.submit(form);
    fireEvent(form, submitEvent);

    expect(submitEvent.defaultPrevented).toBe(true);
    expect(downloadAction).toHaveBeenCalledTimes(1);
  });
});
