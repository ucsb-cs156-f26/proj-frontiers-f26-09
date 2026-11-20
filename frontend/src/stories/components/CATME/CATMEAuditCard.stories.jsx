import React from "react";
import { http, HttpResponse } from "msw";
import CATMEAuditCard from "main/components/CATME/CATMEAuditCard";

export default {
  title: "components/CATME/CATMEAuditCard",
  component: CATMEAuditCard,
};

const Template = (args) => {
  return <CATMEAuditCard {...args} />;
};

export const Default = Template.bind({});
Default.args = {
  courseId: 1,
  testIdPrefix: "CATMEAuditCard",
};
Default.parameters = {
  msw: {
    handlers: [
      http.post("/api/catme/audit", () => {
        return HttpResponse.json(
          {
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
          },
          { status: 200 },
        );
      }),
    ],
  },
};

export const NotACATMEFile = Template.bind({});
NotACATMEFile.args = {
  courseId: 1,
  testIdPrefix: "CATMEAuditCard",
};
NotACATMEFile.parameters = {
  msw: {
    handlers: [
      http.post("/api/catme/audit", () => {
        return HttpResponse.json(
          {
            message:
              "The uploaded file was not in a recognized CATME TeamMaker CSV format.",
          },
          { status: 422 },
        );
      }),
    ],
  },
};
