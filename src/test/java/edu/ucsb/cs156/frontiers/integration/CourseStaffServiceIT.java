package edu.ucsb.cs156.frontiers.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.CourseStaffService;
import edu.ucsb.cs156.frontiers.testconfig.IntegrationConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Checks, against a real database, that {@link CourseStaffService#ensureInstructorIsStaff} adds the
 * instructor as a staff member exactly once, no matter how many times it is called.
 */
@SpringBootTest
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(IntegrationConfig.class)
public class CourseStaffServiceIT {

  @Autowired private CourseStaffService courseStaffService;

  @Autowired private CourseRepository courseRepository;

  @Autowired private CourseStaffRepository courseStaffRepository;

  @Autowired private UserRepository userRepository;

  private static final String INSTRUCTOR_EMAIL = "instructor@ucsb.edu";

  private Course saveCourse() {
    return courseRepository.save(
        Course.builder()
            .courseName("CMPSC 156")
            .term("F26")
            .school(School.UCSB)
            .instructorEmail(INSTRUCTOR_EMAIL)
            .build());
  }

  private List<CourseStaff> staffFor(Course course) {
    List<CourseStaff> staff = new ArrayList<>();
    courseStaffRepository.findByCourseId(course.getId()).forEach(staff::add);
    return staff;
  }

  @Test
  public void instructorIsAddedOnce_evenWhenCalledTwice() {
    Course course = saveCourse();

    courseStaffService.ensureInstructorIsStaff(course);
    courseStaffService.ensureInstructorIsStaff(course);

    List<CourseStaff> staff = staffFor(course);
    assertEquals(1, staff.size());
    CourseStaff instructorStaff = staff.get(0);
    assertEquals(INSTRUCTOR_EMAIL, instructorStaff.getEmail());
    assertEquals("", instructorStaff.getFirstName());
    assertEquals("", instructorStaff.getLastName());
    assertEquals(OrgStatus.PENDING, instructorStaff.getOrgStatus());
    assertNull(instructorStaff.getUser());
  }

  @Test
  public void instructorStaffRow_isLinkedToExistingUser() {
    User user =
        userRepository.save(
            User.builder().email(INSTRUCTOR_EMAIL).givenName("Chris").familyName("Gaucho").build());
    Course course = saveCourse();

    courseStaffService.ensureInstructorIsStaff(course);

    List<CourseStaff> staff = staffFor(course);
    assertEquals(1, staff.size());
    CourseStaff instructorStaff = staff.get(0);
    assertEquals("Chris", instructorStaff.getFirstName());
    assertEquals("Gaucho", instructorStaff.getLastName());
    assertEquals(user.getId(), instructorStaff.getUser().getId());
  }
}
