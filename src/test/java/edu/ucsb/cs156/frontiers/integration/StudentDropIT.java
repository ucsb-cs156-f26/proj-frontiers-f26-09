package edu.ucsb.cs156.frontiers.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.jobs.RemoveStudentsJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.CanvasService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.testconfig.IntegrationConfig;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import edu.ucsb.cs156.jobs.services.JobService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Checks, against a real database, that a student who is dropped from the course by a roster upload
 * or a Canvas roster sync is removed from all of their teams, and that the deleted team memberships
 * are not recreated when the student is saved again afterwards (which is what {@link
 * RemoveStudentsJob} does when it removes the student from the GitHub organization).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(IntegrationConfig.class)
public class StudentDropIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private CourseRepository courseRepository;

  @Autowired private RosterStudentRepository rosterStudentRepository;

  @Autowired private TeamRepository teamRepository;

  @Autowired private TeamMemberRepository teamMemberRepository;

  /** Keeps RemoveStudentsJob from calling the GitHub API. */
  @MockitoBean private OrganizationMemberService organizationMemberService;

  /** Lets the tests capture RemoveStudentsJob and run it synchronously. */
  @MockitoBean private JobService jobService;

  @MockitoBean private CanvasService canvasService;

  /** A roster that only contains Lauren, so uploading it drops Chris. */
  private static final String ROSTER_WITHOUT_CHRIS =
      """
      Student Name,Student ID,Student SIS ID,Email,Section Name
      Lauren Del Playa,88001,A222222,ldelplaya@ucsb.edu,Section 1
      """;

  private Course course;
  private RosterStudent chris;
  private RosterStudent lauren;
  private Team red;
  private Team blue;

  @BeforeEach
  public void setUp() {
    course =
        courseRepository.save(
            Course.builder()
                .courseName("CMPSC 156")
                .term("F26")
                .school(School.UCSB)
                .instructorEmail("instructor@ucsb.edu")
                .orgName("ucsb-cs156-f26")
                .installationId("1234")
                .build());

    chris =
        rosterStudentRepository.save(
            RosterStudent.builder()
                .course(course)
                .studentId("A111111")
                .firstName("Chris")
                .lastName("Gaucho")
                .email("cgaucho@ucsb.edu")
                .rosterStatus(RosterStatus.ROSTER)
                .orgStatus(OrgStatus.MEMBER)
                .githubId(1111)
                .githubLogin("cgaucho")
                .build());

    lauren =
        rosterStudentRepository.save(
            RosterStudent.builder()
                .course(course)
                .studentId("A222222")
                .firstName("Lauren")
                .lastName("Del Playa")
                .email("ldelplaya@ucsb.edu")
                .rosterStatus(RosterStatus.ROSTER)
                .orgStatus(OrgStatus.MEMBER)
                .githubId(2222)
                .githubLogin("ldelplaya")
                .build());

    red = teamRepository.save(Team.builder().name("red").course(course).build());
    blue = teamRepository.save(Team.builder().name("blue").course(course).build());
    teamMemberRepository.save(TeamMember.builder().team(red).rosterStudent(chris).build());
    teamMemberRepository.save(TeamMember.builder().team(blue).rosterStudent(chris).build());
    teamMemberRepository.save(TeamMember.builder().team(red).rosterStudent(lauren).build());
  }

  @Test
  @WithMockUser(roles = {"ADMIN", "USER"})
  public void student_dropped_by_roster_upload_is_removed_from_all_of_their_teams()
      throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "roster.csv", MediaType.TEXT_PLAIN_VALUE, ROSTER_WITHOUT_CHRIS.getBytes());

    mockMvc
        .perform(
            multipart("/api/rosterstudents/upload/csv")
                .file(file)
                .param("courseId", course.getId().toString())
                .with(csrf()))
        .andExpect(status().isOk());
    runRemoveStudentsJob();

    assertChrisIsDroppedAndOnNoTeams();
  }

  @Test
  @WithMockUser(roles = {"ADMIN", "USER"})
  public void student_dropped_by_canvas_roster_sync_is_removed_from_all_of_their_teams()
      throws Exception {
    when(canvasService.getCanvasRoster(any(Course.class)))
        .thenReturn(
            List.of(
                RosterStudent.builder()
                    .studentId("A222222")
                    .firstName("Lauren")
                    .lastName("Del Playa")
                    .email("ldelplaya@ucsb.edu")
                    .build()));

    mockMvc
        .perform(
            post("/api/courses/canvas/sync/students")
                .param("courseId", course.getId().toString())
                .with(csrf()))
        .andExpect(status().isOk());
    runRemoveStudentsJob();

    assertChrisIsDroppedAndOnNoTeams();
  }

  private void runRemoveStudentsJob() throws Exception {
    ArgumentCaptor<JobContextConsumer> job = ArgumentCaptor.forClass(JobContextConsumer.class);
    verify(jobService).runAsJob(job.capture());
    assertInstanceOf(RemoveStudentsJob.class, job.getValue());
    job.getValue().accept(mock(JobContext.class));
  }

  private void assertChrisIsDroppedAndOnNoTeams() {
    RosterStudent droppedChris = rosterStudentRepository.findById(chris.getId()).orElseThrow();
    assertEquals(RosterStatus.DROPPED, droppedChris.getRosterStatus());
    // REMOVED shows that RemoveStudentsJob saved Chris again after the drop was handled
    assertEquals(OrgStatus.REMOVED, droppedChris.getOrgStatus());
    assertEquals(List.of(), droppedChris.getTeams());

    assertEquals(
        List.of("red"), rosterStudentRepository.findById(lauren.getId()).orElseThrow().getTeams());
    assertTrue(teamRepository.findById(blue.getId()).orElseThrow().getTeamMembers().isEmpty());

    List<TeamMember> remainingMemberships = teamMemberRepository.findAll();
    assertEquals(1, remainingMemberships.size());
    assertEquals(lauren.getId(), remainingMemberships.get(0).getRosterStudent().getId());
    assertEquals(red.getId(), remainingMemberships.get(0).getTeam().getId());
  }
}
