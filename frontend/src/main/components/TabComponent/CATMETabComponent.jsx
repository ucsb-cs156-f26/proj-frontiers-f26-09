import React from "react";

export default function CATMETabComponent({ testIdPrefix }) {
  return (
    <div data-testid={`${testIdPrefix}-catme-tab-component`}>
      <p>CATME features coming soon</p>
    </div>
  );
}
