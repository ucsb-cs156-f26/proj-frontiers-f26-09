import React from "react";
import CATMETabComponent from "main/components/TabComponent/CATMETabComponent";

export default {
  title: "components/TabComponent/CATMETabComponent",
  component: CATMETabComponent,
};

const Template = (args) => {
  return <CATMETabComponent {...args} />;
};

export const Default = Template.bind({});
Default.args = {
  testIdPrefix: "CATMETabComponent",
};
