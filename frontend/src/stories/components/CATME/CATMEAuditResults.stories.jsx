import React from "react";
import CATMEAuditResults from "main/components/CATME/CATMEAuditResults";

export default {
  title: "components/CATME/CATMEAuditResults",
  component: CATMEAuditResults,
};

const Template = (args) => {
  return <CATMEAuditResults {...args} />;
};

export const UpToDate = Template.bind({});
UpToDate.args = {
  results: { studentsToUpdate: [], studentsToDrop: [] },
};

export const ChangesNeeded = Template.bind({});
ChangesNeeded.args = {
  results: {
    studentsToUpdate: [
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
};

export const OnlyRemovals = Template.bind({});
OnlyRemovals.args = {
  results: {
    studentsToUpdate: [],
    studentsToDrop: [
      {
        studentId: "1111111",
        name: "Gaucho, Chris",
        email: "cgaucho@ucsb.edu",
        section: "0100",
      },
    ],
  },
};
