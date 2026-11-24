package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CourseStaffService {

  @Autowired private CourseStaffRepository courseStaffRepository;

  @Autowired private UserRepository userRepository;

  /**
   * Returns true if a staff member is the course's instructor, matched by email. A course with no
   * instructor email has no instructor staff member.
   *
   * <p>This is static so that controllers get the real check even in tests where the
   * CourseStaffService bean is mocked.
   *
   * @param course the course
   * @param staff a staff member of that course
   * @return true if the staff member's email is the course's instructor email
   */
  public static boolean isInstructorStaff(Course course, CourseStaff staff) {
    String instructorEmail = course.getInstructorEmail();
    return instructorEmail != null && instructorEmail.equals(staff.getEmail());
  }

  /**
   * Makes sure the instructor of a course is on that course's staff list. If the instructor is
   * already a staff member, nothing happens, so this is safe to call more than once.
   *
   * <p>The staff member's name is taken from the instructor's User record if they have logged in
   * before; otherwise it is left blank. The org status follows the same rule as adding a staff
   * member by hand: JOINCOURSE if the course is linked to a GitHub org, PENDING if not.
   *
   * @param course the course whose instructor should be on staff
   */
  public void ensureInstructorIsStaff(Course course) {
    String instructorEmail = course.getInstructorEmail();

    if (courseStaffRepository.existsByEmailAndCourse(instructorEmail, course)) {
      return;
    }

    Optional<User> user = userRepository.findByEmail(instructorEmail);

    CourseStaff instructorStaff =
        CourseStaff.builder()
            .firstName(user.map(User::getGivenName).orElse(""))
            .lastName(user.map(User::getFamilyName).orElse(""))
            .email(instructorEmail)
            .course(course)
            .user(user.orElse(null))
            .orgStatus(
                course.getInstallationId() != null ? OrgStatus.JOINCOURSE : OrgStatus.PENDING)
            .build();

    courseStaffRepository.save(instructorStaff);
    log.info("Added instructor {} as staff for course id {}", instructorEmail, course.getId());
  }
}
