import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import CATMEAuditCard from "main/components/CATME/CATMEAuditCard";
import { expect } from "vitest";

const axiosMock = new AxiosMockAdapter(axios);

describe("CATMEAuditCard tests", () => {
  const p = "InstructorCourseShowPage";
  const file = new File(["csv"], "catme.csv", { type: "text/csv" });

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
  });

  const renderCard = (courseId = 7) =>
    render(
      <QueryClientProvider client={new QueryClient()}>
        <CATMEAuditCard courseId={courseId} testIdPrefix={p} />
      </QueryClientProvider>,
    );

  const uploadAndSubmit = async () => {
    await userEvent.upload(
      screen.getByTestId(`${p}-catme-audit-form-upload`),
      file,
    );
    await userEvent.click(screen.getByTestId(`${p}-catme-audit-form-submit`));
  };

  test("shows the heading, the explanation, the steps and the upload form", () => {
    renderCard();

    expect(screen.getByTestId(`${p}-catme-audit-card`)).toBeInTheDocument();
    expect(screen.getByTestId(`${p}-catme-audit-header`)).toHaveTextContent(
      "Audit CATME Teammaker CSV Download",
    );
    expect(
      screen.getByText(/this option will allow you to ensure that the roster/),
    ).toHaveTextContent(
      'When using CATME Teammaker, this option will allow you to ensure that the roster of students in CATME is up-to-date with the course roster in Frontiers. It treats the Frontiers as the "source of truth" and lets you know what students need to be added to CATME, removed from CATME, or have their information in CATME (name, section) updated.',
    );
    expect(screen.getByText("To use it:")).toBeInTheDocument();
    const steps = screen.getAllByRole("listitem");
    expect(steps).toHaveLength(3);
    expect(steps[0]).toHaveTextContent(
      "Navigate to the page /faculty/tm_display_results in CATME and click the Export to CSV button.",
    );
    expect(steps[1]).toHaveTextContent("Upload that file here.");
    expect(steps[2]).toHaveTextContent("Read the results.");
    expect(
      screen.getByTestId(`${p}-catme-audit-form-upload`),
    ).toBeInTheDocument();

    expect(
      screen.queryByTestId(`${p}-catme-audit-error`),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(`${p}-catme-audit-results-results`),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(`${p}-catme-audit-results-up-to-date`),
    ).not.toBeInTheDocument();
  });

  test("posts the file and this course's id, then shows the results", async () => {
    axiosMock.onPost("/api/catme/audit").reply(200, {
      studentsToUpdate: [
        {
          studentId: "3333333",
          name: "Lopez, Lupe",
          field: "Name",
          oldValue: "Lupe Lopez",
          newValue: "Lopez, Lupe",
        },
      ],
      studentsToDrop: [
        {
          studentId: "1111111",
          name: "Gaucho, Chris",
          email: "cgaucho@ucsb.edu",
          section: "0100",
        },
      ],
    });
    renderCard(7);

    await uploadAndSubmit();

    expect(
      await screen.findByTestId(`${p}-catme-audit-results-results`),
    ).toBeInTheDocument();
    expect(axiosMock.history.post).toHaveLength(1);
    expect(axiosMock.history.post[0].url).toBe("/api/catme/audit");
    expect(axiosMock.history.post[0].params).toEqual({ courseId: 7 });
    expect(axiosMock.history.post[0].data.get("file")).toBe(file);
    expect(
      screen.getByTestId(`${p}-catme-audit-results-drop-row-0-studentId`),
    ).toHaveTextContent("1111111");
    expect(
      screen.getByTestId(`${p}-catme-audit-results-update-row-0-newValue`),
    ).toHaveTextContent("Lopez, Lupe");
    expect(
      screen.queryByTestId(`${p}-catme-audit-error`),
    ).not.toBeInTheDocument();
  });

  test("shows that CATME is up to date when the audit finds nothing", async () => {
    axiosMock
      .onPost("/api/catme/audit")
      .reply(200, { studentsToUpdate: [], studentsToDrop: [] });
    renderCard();

    await uploadAndSubmit();

    expect(
      await screen.findByTestId(`${p}-catme-audit-results-up-to-date`),
    ).toBeInTheDocument();
  });

  test("shows the server's message when the file is not a CATME file", async () => {
    axiosMock.onPost("/api/catme/audit").reply(422, {
      message:
        "The uploaded file was not in a recognized CATME TeamMaker CSV format.",
    });
    renderCard();

    await uploadAndSubmit();

    const alert = await screen.findByTestId(`${p}-catme-audit-error`);
    expect(alert).toHaveTextContent(
      "The uploaded file was not in a recognized CATME TeamMaker CSV format.",
    );
    expect(alert).toHaveClass("alert-danger");
    expect(
      screen.queryByTestId(`${p}-catme-audit-results-results`),
    ).not.toBeInTheDocument();
  });

  test("shows a generic message when the error has no response", async () => {
    axiosMock.onPost("/api/catme/audit").networkError();
    renderCard();

    await uploadAndSubmit();

    expect(
      await screen.findByTestId(`${p}-catme-audit-error`),
    ).toHaveTextContent("The audit could not be completed. Please try again.");
  });

  test("shows a generic message when the error response has no message", async () => {
    axiosMock.onPost("/api/catme/audit").reply(500, {});
    renderCard();

    await uploadAndSubmit();

    expect(
      await screen.findByTestId(`${p}-catme-audit-error`),
    ).toHaveTextContent("The audit could not be completed. Please try again.");
  });

  test("shows a generic message when the error response has no body", async () => {
    axiosMock.onPost("/api/catme/audit").reply(500);
    renderCard();

    await uploadAndSubmit();

    expect(
      await screen.findByTestId(`${p}-catme-audit-error`),
    ).toHaveTextContent("The audit could not be completed. Please try again.");
  });

  test("a new upload clears the previous results and error", async () => {
    axiosMock
      .onPost("/api/catme/audit")
      .replyOnce(422, { message: "bad file" })
      .onPost("/api/catme/audit")
      .replyOnce(200, { studentsToUpdate: [], studentsToDrop: [] });
    renderCard();

    await uploadAndSubmit();
    expect(
      await screen.findByTestId(`${p}-catme-audit-error`),
    ).toHaveTextContent("bad file");

    await userEvent.click(screen.getByTestId(`${p}-catme-audit-form-submit`));

    await waitFor(() =>
      expect(
        screen.queryByTestId(`${p}-catme-audit-error`),
      ).not.toBeInTheDocument(),
    );
    expect(
      await screen.findByTestId(`${p}-catme-audit-results-up-to-date`),
    ).toBeInTheDocument();
  });

  test("results from an earlier audit are cleared when the next one fails", async () => {
    axiosMock
      .onPost("/api/catme/audit")
      .replyOnce(200, { studentsToUpdate: [], studentsToDrop: [] })
      .onPost("/api/catme/audit")
      .replyOnce(422, { message: "bad file" });
    renderCard();

    await uploadAndSubmit();
    await screen.findByTestId(`${p}-catme-audit-results-up-to-date`);

    await userEvent.click(screen.getByTestId(`${p}-catme-audit-form-submit`));

    expect(
      await screen.findByTestId(`${p}-catme-audit-error`),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId(`${p}-catme-audit-results-up-to-date`),
    ).not.toBeInTheDocument();
  });
});
