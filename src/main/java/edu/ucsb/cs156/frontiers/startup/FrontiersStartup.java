package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CourseStaffService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** This class contains a `run` method that is called once at application startup time. */
@Slf4j
@Component
public class FrontiersStartup {

  @Value("#{'${app.admin.emails}'.split(',')}")
  List<String> adminEmails;

  @Value("${app.webhook.secret}")
  String webhookSecret;

  @Autowired AdminRepository adminRepository;

  @Autowired CourseRepository courseRepository;

  @Autowired CourseStaffService courseStaffService;

  /**
   * Called once at application startup time . Put code here if you want it to run once each time
   * the Spring Boot application starts up in all environments.
   */
  public void alwaysRunOnStartup() {
    log.info("alwaysRunOnStartup called");

    validateWebhookSecret();

    try {
      adminEmails.forEach(
          (email) -> {
            Admin admin = new Admin(email);
            adminRepository.save(admin);
          });
    } catch (Exception e) {
      log.error("Error in loading all ADMIN_EMAILS:", e);
    }

    addInstructorsAsStaff();
  }

  /**
   * Makes sure the instructor of every course is on that course's staff list, adding them where
   * they are missing. Courses without an instructor email are skipped. An error on one course is
   * logged and does not stop the others or the application from starting.
   */
  void addInstructorsAsStaff() {
    try {
      for (Course course : courseRepository.findAll()) {
        String instructorEmail = course.getInstructorEmail();
        if (instructorEmail == null || instructorEmail.isBlank()) {
          log.warn(
              "Course id {} has no instructor email; not adding an instructor as staff",
              course.getId());
          continue;
        }
        try {
          courseStaffService.ensureInstructorIsStaff(course);
        } catch (Exception e) {
          log.error("Error adding instructor as staff for course id {}:", course.getId(), e);
        }
      }
    } catch (Exception e) {
      log.error("Error adding instructors as staff for all courses:", e);
    }
  }

  private void validateWebhookSecret() {
    if (webhookSecret == null || webhookSecret.length() < 10) {
      String error =
          String.format(
              "WEBHOOK_SECRET must be at least 10 characters long. Current length: %d",
              webhookSecret == null ? 0 : webhookSecret.length());
      log.error(error);
      throw new IllegalStateException(error);
    }
    log.info("Webhook secret validation passed");
  }
}
