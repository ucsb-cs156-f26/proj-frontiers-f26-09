import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import CATMEAuditCSVUploadForm from "main/components/CATME/CATMEAuditCSVUploadForm";
import { expect, vi } from "vitest";

describe("CATMEAuditCSVUploadForm tests", () => {
  test("renders a labelled csv file input and a submit button", () => {
    render(<CATMEAuditCSVUploadForm submitAction={vi.fn()} />);

    const fileInput = screen.getByTestId("CATMEAuditCSVUploadForm-upload");
    expect(fileInput).toHaveAttribute("type", "file");
    expect(fileInput).toHaveAttribute("accept", ".csv");
    expect(fileInput).toHaveAttribute("id", "catme-audit-upload");
    expect(screen.getByLabelText("CATME Teammaker CSV")).toBe(fileInput);

    const submit = screen.getByTestId("CATMEAuditCSVUploadForm-submit");
    expect(submit).toHaveAttribute("type", "submit");
    expect(submit).toHaveTextContent("Audit");
    expect(
      screen.queryByText("A CATME Teammaker CSV file is required."),
    ).not.toBeInTheDocument();
  });

  test("uses the given testIdPrefix", () => {
    render(
      <CATMEAuditCSVUploadForm submitAction={vi.fn()} testIdPrefix="Other" />,
    );

    expect(screen.getByTestId("Other-upload")).toBeInTheDocument();
    expect(screen.getByTestId("Other-submit")).toBeInTheDocument();
  });

  test("shows a validation error, and does not submit, when no file is chosen", async () => {
    const submitAction = vi.fn();
    render(<CATMEAuditCSVUploadForm submitAction={submitAction} />);

    await userEvent.click(screen.getByTestId("CATMEAuditCSVUploadForm-submit"));

    await waitFor(() =>
      expect(
        screen.getByText("A CATME Teammaker CSV file is required."),
      ).toBeInTheDocument(),
    );
    expect(submitAction).not.toHaveBeenCalled();
  });

  test("calls submitAction with the chosen file", async () => {
    const submitAction = vi.fn();
    render(<CATMEAuditCSVUploadForm submitAction={submitAction} />);
    const file = new File(["a,b"], "catme.csv", { type: "text/csv" });

    await userEvent.upload(
      screen.getByTestId("CATMEAuditCSVUploadForm-upload"),
      file,
    );
    await userEvent.click(screen.getByTestId("CATMEAuditCSVUploadForm-submit"));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction.mock.calls[0][0].upload[0]).toBe(file);
    expect(
      screen.queryByText("A CATME Teammaker CSV file is required."),
    ).not.toBeInTheDocument();
  });
});
