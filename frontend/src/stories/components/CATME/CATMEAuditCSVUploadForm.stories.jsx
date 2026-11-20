import React from "react";
import CATMEAuditCSVUploadForm from "main/components/CATME/CATMEAuditCSVUploadForm";

export default {
  title: "components/CATME/CATMEAuditCSVUploadForm",
  component: CATMEAuditCSVUploadForm,
};

const Template = (args) => {
  return <CATMEAuditCSVUploadForm {...args} />;
};

export const Default = Template.bind({});
Default.args = {
  submitAction: (data) => {
    console.log("Submit", data);
  },
};
