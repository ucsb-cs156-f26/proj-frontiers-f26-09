package edu.ucsb.cs156.frontiers.startup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CourseStaffService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class FrontiersStartupTests {

  @Mock private AdminRepository adminRepository;

  @Mock private CourseRepository courseRepository;

  @Mock private CourseStaffService courseStaffService;

  private FrontiersStartup frontiersStartup;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    frontiersStartup = new FrontiersStartup();
    frontiersStartup.adminRepository = adminRepository;
    frontiersStartup.courseRepository = courseRepository;
    frontiersStartup.courseStaffService = courseStaffService;
    frontiersStartup.adminEmails = List.of("acdamstedt@ucsb.edu", "phtcon@ucsb.edu");
  }

  @Test
  void test_AlwaysRunOnStartup_saves_admins() {
    frontiersStartup.webhookSecret = "valid_secret_123";
    frontiersStartup.alwaysRunOnStartup();

    verify(adminRepository).save(new Admin("acdamstedt@ucsb.edu"));
    verify(adminRepository).save(new Admin("phtcon@ucsb.edu"));
    verifyNoMoreInteractions(adminRepository);
  }

  @Test
  void test_AlwaysRunOnStartup_handles_exception() {
    frontiersStartup.webhookSecret = "valid_secret_123";
    doThrow(new RuntimeException("Simulated error")).when(adminRepository).save(any(Admin.class));

    frontiersStartup.alwaysRunOnStartup();

    verify(adminRepository, times(1)).save(any(Admin.class));
  }

  @Test
  void test_AlwaysRunOnStartup_fails_with_short_webhook_secret() {
    frontiersStartup.webhookSecret = "short";

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> frontiersStartup.alwaysRunOnStartup());

    assertTrue(
        exception.getMessage().contains("WEBHOOK_SECRET must be at least 10 characters long"));
    assertTrue(exception.getMessage().contains("Current length: 5"));
    verifyNoInteractions(adminRepository);
  }

  @Test
  void test_AlwaysRunOnStartup_fails_with_null_webhook_secret() {
    frontiersStartup.webhookSecret = null;

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> frontiersStartup.alwaysRunOnStartup());

    assertTrue(
        exception.getMessage().contains("WEBHOOK_SECRET must be at least 10 characters long"));
    assertTrue(exception.getMessage().contains("Current length: 0"));
    verifyNoInteractions(adminRepository);
  }

  @Test
  void test_AlwaysRunOnStartup_succeeds_with_minimum_length_webhook_secret() {
    frontiersStartup.webhookSecret = "1234567890"; // exactly 10 characters

    frontiersStartup.alwaysRunOnStartup();

    verify(adminRepository).save(new Admin("acdamstedt@ucsb.edu"));
    verify(adminRepository).save(new Admin("phtcon@ucsb.edu"));
    verifyNoMoreInteractions(adminRepository);
  }

  @Test
  void test_AlwaysRunOnStartup_adds_instructor_as_staff_for_every_course() {
    frontiersStartup.webhookSecret = "valid_secret_123";
    Course course1 = Course.builder().id(1L).instructorEmail("instructor1@ucsb.edu").build();
    Course course2 = Course.builder().id(2L).instructorEmail("instructor2@ucsb.edu").build();
    when(courseRepository.findAll()).thenReturn(List.of(course1, course2));

    frontiersStartup.alwaysRunOnStartup();

    verify(courseStaffService, times(1)).ensureInstructorIsStaff(course1);
    verify(courseStaffService, times(1)).ensureInstructorIsStaff(course2);
    verifyNoMoreInteractions(courseStaffService);
  }

  @Test
  void test_AlwaysRunOnStartup_skips_courses_without_instructor_email() {
    frontiersStartup.webhookSecret = "valid_secret_123";
    Course noEmail = Course.builder().id(1L).instructorEmail(null).build();
    Course blankEmail = Course.builder().id(2L).instructorEmail("   ").build();
    Course withEmail = Course.builder().id(3L).instructorEmail("instructor@ucsb.edu").build();
    when(courseRepository.findAll()).thenReturn(List.of(noEmail, blankEmail, withEmail));

    frontiersStartup.alwaysRunOnStartup();

    verify(courseStaffService, times(1)).ensureInstructorIsStaff(withEmail);
    verifyNoMoreInteractions(courseStaffService);
  }

  @Test
  void test_AlwaysRunOnStartup_keeps_going_when_one_course_fails() {
    frontiersStartup.webhookSecret = "valid_secret_123";
    Course badCourse = Course.builder().id(1L).instructorEmail("instructor1@ucsb.edu").build();
    Course goodCourse = Course.builder().id(2L).instructorEmail("instructor2@ucsb.edu").build();
    when(courseRepository.findAll()).thenReturn(List.of(badCourse, goodCourse));
    doThrow(new RuntimeException("Simulated error"))
        .when(courseStaffService)
        .ensureInstructorIsStaff(badCourse);

    assertDoesNotThrow(() -> frontiersStartup.alwaysRunOnStartup());

    verify(courseStaffService, times(1)).ensureInstructorIsStaff(badCourse);
    verify(courseStaffService, times(1)).ensureInstructorIsStaff(goodCourse);
  }

  @Test
  void test_AlwaysRunOnStartup_handles_error_loading_courses() {
    frontiersStartup.webhookSecret = "valid_secret_123";
    when(courseRepository.findAll()).thenThrow(new RuntimeException("Simulated error"));

    assertDoesNotThrow(() -> frontiersStartup.alwaysRunOnStartup());

    verifyNoInteractions(courseStaffService);
  }

  @Test
  void test_AlwaysRunOnStartup_adds_instructors_even_when_saving_admins_fails() {
    frontiersStartup.webhookSecret = "valid_secret_123";
    doThrow(new RuntimeException("Simulated error")).when(adminRepository).save(any(Admin.class));
    Course course = Course.builder().id(1L).instructorEmail("instructor@ucsb.edu").build();
    when(courseRepository.findAll()).thenReturn(List.of(course));

    frontiersStartup.alwaysRunOnStartup();

    verify(courseStaffService, times(1)).ensureInstructorIsStaff(course);
  }

  @Test
  void test_AlwaysRunOnStartup_does_not_add_instructors_when_webhook_secret_is_invalid() {
    frontiersStartup.webhookSecret = "short";

    assertThrows(IllegalStateException.class, () -> frontiersStartup.alwaysRunOnStartup());

    verifyNoInteractions(courseRepository);
    verifyNoInteractions(courseStaffService);
  }
}
