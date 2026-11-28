package edu.ucsb.cs156.frontiers.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.startup.FrontiersStartup;
import edu.ucsb.cs156.frontiers.testconfig.IntegrationConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(IntegrationConfig.class)
public class FrontiersStartupIT {

  @Autowired private FrontiersStartup frontiersStartup;

  @Autowired private CourseRepository courseRepository;

  @Autowired private CourseStaffRepository courseStaffRepository;

  private Course saveCourse(String instructorEmail) {
    return courseRepository.save(
        Course.builder()
            .courseName("CMPSC 156")
            .term("F26")
            .school(School.UCSB)
            .instructorEmail(instructorEmail)
            .build());
  }

  private List<CourseStaff> staffFor(Course course) {
    List<CourseStaff> staff = new ArrayList<>();
    courseStaffRepository.findByCourseId(course.getId()).forEach(staff::add);
    return staff;
  }

  @Test
  public void startup_addsMissingInstructors_once_andSkipsCoursesWithoutInstructor() {
    Course course = saveCourse("instructor@ucsb.edu");
    Course courseWithoutInstructor = saveCourse(null);

    frontiersStartup.alwaysRunOnStartup();
    frontiersStartup.alwaysRunOnStartup();

    List<CourseStaff> staff = staffFor(course);
    assertEquals(1, staff.size());
    assertEquals("instructor@ucsb.edu", staff.get(0).getEmail());
    assertEquals(0, staffFor(courseWithoutInstructor).size());
  }
}
