import React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DownloadsTabComponent from "main/components/TabComponent/DownloadsTabComponent";

export default {
  title: "components/TabComponent/DownloadsTabComponent",
  component: DownloadsTabComponent,
};

const Template = (args) => {
  const queryClient = new QueryClient();
  return (
    <QueryClientProvider client={queryClient}>
      <DownloadsTabComponent {...args} />
    </QueryClientProvider>
  );
};

export const Default = Template.bind({});

Default.args = {
  courseId: 1,
  testIdPrefix: "InstructorCourseShowPage",
};
