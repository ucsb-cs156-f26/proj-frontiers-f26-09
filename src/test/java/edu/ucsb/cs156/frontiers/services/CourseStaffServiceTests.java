package edu.ucsb.cs156.frontiers.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CourseStaffServiceTests {

  @Mock private CourseStaffRepository courseStaffRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private CourseStaffService courseStaffService;

  private static final String INSTRUCTOR_EMAIL = "instructor@ucsb.edu";

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void ensureInstructorIsStaff_doesNothing_whenInstructorIsAlreadyStaff() {
    Course course = Course.builder().id(1L).instructorEmail(INSTRUCTOR_EMAIL).build();
    when(courseStaffRepository.existsByEmailAndCourse(INSTRUCTOR_EMAIL, course)).thenReturn(true);

    courseStaffService.ensureInstructorIsStaff(course);

    verify(courseStaffRepository, times(1)).existsByEmailAndCourse(INSTRUCTOR_EMAIL, course);
    verify(courseStaffRepository, never()).save(any(CourseStaff.class));
    verifyNoInteractions(userRepository);
  }

  @Test
  public void ensureInstructorIsStaff_usesUserNamesAndPending_whenUserExistsAndCourseNotLinked() {
    Course course = Course.builder().id(1L).instructorEmail(INSTRUCTOR_EMAIL).build();
    User user =
        User.builder()
            .id(5L)
            .email(INSTRUCTOR_EMAIL)
            .givenName("Chris")
            .familyName("Gaucho")
            .build();
    when(courseStaffRepository.existsByEmailAndCourse(INSTRUCTOR_EMAIL, course)).thenReturn(false);
    when(userRepository.findByEmail(INSTRUCTOR_EMAIL)).thenReturn(Optional.of(user));

    courseStaffService.ensureInstructorIsStaff(course);

    CourseStaff expected =
        CourseStaff.builder()
            .firstName("Chris")
            .lastName("Gaucho")
            .email(INSTRUCTOR_EMAIL)
            .course(course)
            .user(user)
            .orgStatus(OrgStatus.PENDING)
            .build();
    verify(courseStaffRepository, times(1)).save(eq(expected));
  }

  @Test
  public void ensureInstructorIsStaff_usesBlankNamesAndJoinCourse_whenNoUserAndCourseLinked() {
    Course course =
        Course.builder().id(2L).instructorEmail(INSTRUCTOR_EMAIL).installationId("12345").build();
    when(courseStaffRepository.existsByEmailAndCourse(INSTRUCTOR_EMAIL, course)).thenReturn(false);
    when(userRepository.findByEmail(INSTRUCTOR_EMAIL)).thenReturn(Optional.empty());

    courseStaffService.ensureInstructorIsStaff(course);

    CourseStaff expected =
        CourseStaff.builder()
            .firstName("")
            .lastName("")
            .email(INSTRUCTOR_EMAIL)
            .course(course)
            .user(null)
            .orgStatus(OrgStatus.JOINCOURSE)
            .build();
    verify(courseStaffRepository, times(1)).save(eq(expected));
  }
}
