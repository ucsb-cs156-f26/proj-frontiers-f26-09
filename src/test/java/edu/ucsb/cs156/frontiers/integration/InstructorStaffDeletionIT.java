package edu.ucsb.cs156.frontiers.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.testconfig.IntegrationConfig;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Checks, against a real database, that the instructor's staff row cannot be deleted through the
 * staff endpoint, and that a course whose only staff member is its instructor can still be deleted
 * (together with that staff row).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(IntegrationConfig.class)
public class InstructorStaffDeletionIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper mapper;

  @Autowired private CourseRepository courseRepository;

  @Autowired private CourseStaffRepository courseStaffRepository;

  private static final String INSTRUCTOR_EMAIL = "instructor@ucsb.edu";

  private Course course;
  private CourseStaff instructorStaff;

  @BeforeEach
  public void setUp() {
    course =
        courseRepository.save(
            Course.builder()
                .courseName("CMPSC 156")
                .term("F26")
                .school(School.UCSB)
                .instructorEmail(INSTRUCTOR_EMAIL)
                .build());

    instructorStaff =
        courseStaffRepository.save(
            CourseStaff.builder()
                .firstName("Chris")
                .lastName("Gaucho")
                .email(INSTRUCTOR_EMAIL)
                .course(course)
                .orgStatus(OrgStatus.PENDING)
                .build());
  }

  @Test
  @WithMockUser(roles = {"ADMIN", "USER"})
  public void deletingTheInstructorAsStaff_returns400_andKeepsTheRow() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                delete("/api/coursestaff/delete")
                    .with(csrf())
                    .param("id", instructorStaff.getId().toString())
                    .param("courseId", course.getId().toString()))
            .andExpect(status().isBadRequest())
            .andReturn();

    Map<String, String> expected =
        Map.of(
            "type", "IllegalArgumentException",
            "message", "you cannot delete the instructor as a staff member");
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
    assertTrue(courseStaffRepository.findById(instructorStaff.getId()).isPresent());
  }

  @Test
  @WithMockUser(roles = {"ADMIN", "USER"})
  public void deletingACourseWhoseOnlyStaffIsTheInstructor_deletesCourseAndStaffRow()
      throws Exception {
    mockMvc
        .perform(delete("/api/courses").with(csrf()).param("courseId", course.getId().toString()))
        .andExpect(status().isOk());

    assertTrue(courseRepository.findById(course.getId()).isEmpty());
    assertTrue(courseStaffRepository.findById(instructorStaff.getId()).isEmpty());
  }
}
