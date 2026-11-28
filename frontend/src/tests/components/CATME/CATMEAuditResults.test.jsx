import { render, screen, within } from "@testing-library/react";
import CATMEAuditResults from "main/components/CATME/CATMEAuditResults";
import { expect } from "vitest";

const drop = [
  {
    studentId: "1111111",
    name: "Gaucho, Chris",
    email: "cgaucho@ucsb.edu",
    section: "0100",
  },
  {
    studentId: "2222222",
    name: "Del Playa, Dolphin",
    email: "ddelplaya@ucsb.edu",
    section: "0200",
  },
];

const update = [
  {
    studentId: "3333333",
    name: "Lopez, Lupe",
    field: "Name",
    oldValue: "Lupe Lopez",
    newValue: "Lopez, Lupe",
  },
  {
    studentId: "3333333",
    name: "Lopez, Lupe",
    field: "Section",
    oldValue: "0100",
    newValue: "0200",
  },
];

describe("CATMEAuditResults tests", () => {
  const p = "CATMEAuditResults";

  test("says CATME is up to date when there is nothing to change", () => {
    render(
      <CATMEAuditResults
        results={{ studentsToUpdate: [], studentsToDrop: [] }}
      />,
    );

    expect(screen.getByTestId(`${p}-up-to-date`)).toHaveTextContent(
      "CATME is up to date with the Frontiers roster. No changes are needed.",
    );
    expect(screen.getByTestId(`${p}-up-to-date`)).toHaveClass("alert-success");
    expect(screen.queryByTestId(`${p}-results`)).not.toBeInTheDocument();
  });

  test("shows both tables, with counts and every value", () => {
    render(
      <CATMEAuditResults
        results={{ studentsToUpdate: update, studentsToDrop: drop }}
      />,
    );

    expect(screen.getByTestId(`${p}-results`)).toBeInTheDocument();
    expect(screen.queryByTestId(`${p}-up-to-date`)).not.toBeInTheDocument();

    expect(screen.getByTestId(`${p}-drop-header`)).toHaveTextContent(
      "Remove from CATME (2)",
    );
    expect(screen.getByTestId(`${p}-update-header`)).toHaveTextContent(
      "Update in CATME (2)",
    );
    expect(
      screen.getByText(
        "These students are in the CATME file but are not currently enrolled in Frontiers.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "These students are in CATME, but their name or section does not match Frontiers.",
      ),
    ).toBeInTheDocument();

    expect(screen.getByTestId(`${p}-drop`)).toBeInTheDocument();
    expect(screen.getByTestId(`${p}-update`)).toBeInTheDocument();
    expect(screen.queryByTestId(`${p}-drop-none`)).not.toBeInTheDocument();
    expect(screen.queryByTestId(`${p}-update-none`)).not.toBeInTheDocument();

    drop.forEach((student, i) => {
      ["studentId", "name", "email", "section"].forEach((key) =>
        expect(
          screen.getByTestId(`${p}-drop-row-${i}-${key}`),
        ).toHaveTextContent(student[key]),
      );
    });
    update.forEach((row, i) => {
      ["studentId", "name", "field", "oldValue", "newValue"].forEach((key) =>
        expect(
          screen.getByTestId(`${p}-update-row-${i}-${key}`),
        ).toHaveTextContent(row[key]),
      );
    });

    expect(
      within(screen.getByTestId(`${p}-drop`))
        .getAllByRole("columnheader")
        .map((th) => th.textContent),
    ).toEqual(["Student ID", "Name", "Email", "Section"]);
    expect(
      within(screen.getByTestId(`${p}-update`))
        .getAllByRole("columnheader")
        .map((th) => th.textContent),
    ).toEqual([
      "Student ID",
      "Name",
      "Field",
      "Currently in CATME",
      "Should be",
    ]);
  });

  test("when only students need removing, says there is nothing to update", () => {
    render(
      <CATMEAuditResults
        results={{ studentsToUpdate: [], studentsToDrop: drop }}
      />,
    );

    expect(screen.getByTestId(`${p}-drop`)).toBeInTheDocument();
    expect(screen.queryByTestId(`${p}-drop-none`)).not.toBeInTheDocument();
    expect(screen.getByTestId(`${p}-update-none`)).toHaveTextContent(
      "No students to update.",
    );
    expect(screen.queryByTestId(`${p}-update`)).not.toBeInTheDocument();
    expect(screen.getByTestId(`${p}-update-header`)).toHaveTextContent(
      "Update in CATME (0)",
    );
  });

  test("when only students need updating, says there is nothing to remove", () => {
    render(
      <CATMEAuditResults
        results={{ studentsToUpdate: update, studentsToDrop: [] }}
      />,
    );

    expect(screen.getByTestId(`${p}-update`)).toBeInTheDocument();
    expect(screen.queryByTestId(`${p}-update-none`)).not.toBeInTheDocument();
    expect(screen.getByTestId(`${p}-drop-none`)).toHaveTextContent(
      "No students to remove.",
    );
    expect(screen.queryByTestId(`${p}-drop`)).not.toBeInTheDocument();
    expect(screen.getByTestId(`${p}-drop-header`)).toHaveTextContent(
      "Remove from CATME (0)",
    );
  });

  test("uses the given testIdPrefix", () => {
    render(
      <CATMEAuditResults
        results={{ studentsToUpdate: update, studentsToDrop: drop }}
        testIdPrefix="Other"
      />,
    );

    expect(screen.getByTestId("Other-results")).toBeInTheDocument();
    expect(screen.getByTestId("Other-drop-row-0-name")).toBeInTheDocument();
    expect(screen.getByTestId("Other-update-row-0-field")).toBeInTheDocument();
  });
});
