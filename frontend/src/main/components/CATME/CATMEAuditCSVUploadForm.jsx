import { useForm } from "react-hook-form";
import { Button, Form } from "react-bootstrap";
import React from "react";

export default function CATMEAuditCSVUploadForm({
  submitAction,
  testIdPrefix = "CATMEAuditCSVUploadForm",
}) {
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm();

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group className="mb-2">
        <Form.Label htmlFor="catme-audit-upload">
          CATME Teammaker CSV
        </Form.Label>
        <Form.Control
          data-testid={`${testIdPrefix}-upload`}
          id="catme-audit-upload"
          type="file"
          accept=".csv"
          isInvalid={Boolean(errors.upload)}
          {...register("upload", { required: true })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.upload && "A CATME Teammaker CSV file is required."}
        </Form.Control.Feedback>
      </Form.Group>
      <Button
        type="submit"
        data-testid={`${testIdPrefix}-submit`}
        className="mt-2"
      >
        Audit
      </Button>
    </Form>
  );
}
