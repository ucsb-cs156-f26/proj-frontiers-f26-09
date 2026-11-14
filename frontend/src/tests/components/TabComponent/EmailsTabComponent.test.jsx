import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { beforeEach, describe, expect, test, vi } from "vitest";

import EmailsTabComponent from "main/components/TabComponent/EmailsTabComponent";

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

const axiosMock = new AxiosMockAdapter(axios);
const testId = "InstructorCourseShowPage";
const emailsUrl = "/api/courses/emails";
const teamsUrl = "/api/teams/all?courseId=7";
const emailsGetHistory = () =>
  axiosMock.history.get.filter((request) => request.url === emailsUrl);
const lastEmailsParams = () => emailsGetHistory().at(-1).params;

const teams = [
  { id: 11, name: "team-alpha" },
  { id: 12, name: "team-beta" },
];

const renderTab = (queryClient = new QueryClient()) => {
  return render(
    <QueryClientProvider client={queryClient}>
      <EmailsTabComponent courseId={7} testIdPrefix={testId} />
    </QueryClientProvider>,
  );
};

const optionsOf = (select) =>
  Array.from(select.querySelectorAll("option")).map((o) => [
    o.value,
    o.textContent,
  ]);

describe("EmailsTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockToast.mockClear();
    axiosMock.onGet(teamsUrl).reply(200, teams);
    axiosMock
      .onGet(emailsUrl)
      .reply((config) => [
        200,
        `${config.params.type}-${config.params.team ?? "none"}@ucsb.edu`,
      ]);
  });

  test("renders the type and team dropdowns and the emails card", async () => {
    renderTab();

    expect(
      screen.getByTestId(`${testId}-emails-tab-component`),
    ).toBeInTheDocument();

    const type = screen.getByTestId(`${testId}-emails-type`);
    expect(screen.getByLabelText("Type")).toBe(type);
    expect(type).toHaveValue("STUDENTS");
    expect(optionsOf(type)).toEqual([
      ["STUDENTS", "Students"],
      ["STAFF", "Staff"],
      ["ALL", "All"],
    ]);

    const team = screen.getByTestId(`${testId}-emails-team`);
    expect(screen.getByLabelText("Team")).toBe(team);
    expect(team).toHaveValue("");
    expect(team).not.toBeDisabled();
    expect(type.closest(".mb-3")).not.toBeNull();
    expect(team.closest(".mb-3")).not.toBeNull();

    expect(
      screen.getByTestId(`${testId}-EmailsTabCard-card`),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByTestId(`${testId}-EmailsTabCard-emails`)).toHaveValue(
        "STUDENTS-none@ucsb.edu",
      ),
    );
  });

  test("asks for the student emails of the whole course by default", async () => {
    renderTab();

    await waitFor(() => expect(emailsGetHistory().length).toBe(1));
    expect(lastEmailsParams()).toEqual({
      courseId: 7,
      type: "STUDENTS",
      format: "ONE_PER_LINE",
    });
  });

  test("lists All teams followed by the teams of the course", async () => {
    renderTab();

    const team = screen.getByTestId(`${testId}-emails-team`);
    expect(optionsOf(team)).toEqual([["", "All teams"]]);

    await waitFor(() =>
      expect(optionsOf(team)).toEqual([
        ["", "All teams"],
        ["team-alpha", "team-alpha"],
        ["team-beta", "team-beta"],
      ]),
    );
    expect(
      axiosMock.history.get.filter((request) => request.url === teamsUrl)
        .length,
    ).toBe(1);
  });

  test("caches the teams under the teams of the course", async () => {
    const queryClient = new QueryClient();
    renderTab(queryClient);

    await waitFor(() =>
      expect(queryClient.getQueryData([teamsUrl])).toEqual(teams),
    );
  });

  test("does not toast and keeps only All teams when the teams cannot be loaded", async () => {
    axiosMock.reset();
    axiosMock.onGet(teamsUrl).reply(500);
    axiosMock.onGet(emailsUrl).reply(200, "a@ucsb.edu");

    renderTab();

    await waitFor(() =>
      expect(
        axiosMock.history.get.filter((request) => request.url === teamsUrl)
          .length,
      ).toBe(1),
    );
    expect(mockToast).not.toHaveBeenCalled();
    expect(optionsOf(screen.getByTestId(`${testId}-emails-team`))).toEqual([
      ["", "All teams"],
    ]);
  });

  test("choosing a type refetches the emails of that type", async () => {
    renderTab();
    const type = screen.getByTestId(`${testId}-emails-type`);
    const emails = screen.getByTestId(`${testId}-EmailsTabCard-emails`);
    await waitFor(() => expect(emails).toHaveValue("STUDENTS-none@ucsb.edu"));

    fireEvent.change(type, { target: { value: "ALL" } });

    await waitFor(() => expect(emails).toHaveValue("ALL-none@ucsb.edu"));
    expect(type).toHaveValue("ALL");
    expect(lastEmailsParams()).toEqual({
      courseId: 7,
      type: "ALL",
      format: "ONE_PER_LINE",
    });
    expect(screen.getByTestId(`${testId}-emails-team`)).not.toBeDisabled();
  });

  test("choosing a team refetches the emails of that team", async () => {
    renderTab();
    const team = screen.getByTestId(`${testId}-emails-team`);
    const emails = screen.getByTestId(`${testId}-EmailsTabCard-emails`);
    await waitFor(() => expect(optionsOf(team).length).toBe(3));

    fireEvent.change(team, { target: { value: "team-beta" } });

    await waitFor(() =>
      expect(emails).toHaveValue("STUDENTS-team-beta@ucsb.edu"),
    );
    expect(team).toHaveValue("team-beta");
    expect(lastEmailsParams()).toEqual({
      courseId: 7,
      type: "STUDENTS",
      format: "ONE_PER_LINE",
      team: "team-beta",
    });
  });

  test("the team is disabled and ignored for staff, and comes back for other types", async () => {
    renderTab();
    const type = screen.getByTestId(`${testId}-emails-type`);
    const team = screen.getByTestId(`${testId}-emails-team`);
    const emails = screen.getByTestId(`${testId}-EmailsTabCard-emails`);
    await waitFor(() => expect(optionsOf(team).length).toBe(3));
    fireEvent.change(team, { target: { value: "team-alpha" } });
    await waitFor(() =>
      expect(emails).toHaveValue("STUDENTS-team-alpha@ucsb.edu"),
    );

    fireEvent.change(type, { target: { value: "STAFF" } });

    await waitFor(() => expect(emails).toHaveValue("STAFF-none@ucsb.edu"));
    expect(team).toBeDisabled();
    expect(team).toHaveValue("");
    expect(lastEmailsParams()).toEqual({
      courseId: 7,
      type: "STAFF",
      format: "ONE_PER_LINE",
    });

    fireEvent.change(type, { target: { value: "ALL" } });

    await waitFor(() => expect(emails).toHaveValue("ALL-team-alpha@ucsb.edu"));
    expect(team).not.toBeDisabled();
    expect(team).toHaveValue("team-alpha");
    expect(lastEmailsParams()).toEqual({
      courseId: 7,
      type: "ALL",
      format: "ONE_PER_LINE",
      team: "team-alpha",
    });
  });
});
