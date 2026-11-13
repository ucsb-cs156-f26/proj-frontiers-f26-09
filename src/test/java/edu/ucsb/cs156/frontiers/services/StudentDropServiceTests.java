package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for {@link StudentDropService}. */
public class StudentDropServiceTests {

  @Mock private TeamMemberRepository teamMemberRepository;

  @InjectMocks private StudentDropService studentDropService;

  private Team red;
  private Team blue;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    red = Team.builder().id(1L).name("red").teamMembers(new ArrayList<>()).build();
    blue = Team.builder().id(2L).name("blue").teamMembers(new ArrayList<>()).build();
  }

  private RosterStudent student(long id, String email) {
    return RosterStudent.builder()
        .id(id)
        .email(email)
        .rosterStatus(RosterStatus.DROPPED)
        .teamMembers(new ArrayList<>())
        .build();
  }

  private TeamMember addToTeam(long teamMemberId, RosterStudent student, Team team) {
    TeamMember teamMember =
        TeamMember.builder().id(teamMemberId).rosterStudent(student).team(team).build();
    student.getTeamMembers().add(teamMember);
    team.getTeamMembers().add(teamMember);
    return teamMember;
  }

  @Test
  public void dropped_student_is_removed_from_all_of_their_teams() {
    RosterStudent chris = student(10L, "chris@ucsb.edu");
    RosterStudent lauren = student(11L, "lauren@ucsb.edu");
    addToTeam(100L, chris, red);
    addToTeam(101L, chris, blue);
    TeamMember laurenOnRed = addToTeam(102L, lauren, red);

    studentDropService.handleDroppedStudents(List.of(chris));

    assertTrue(chris.getTeamMembers().isEmpty());
    assertEquals(List.of(laurenOnRed), red.getTeamMembers());
    assertTrue(blue.getTeamMembers().isEmpty());
    assertEquals(List.of(laurenOnRed), lauren.getTeamMembers());
    verify(teamMemberRepository).deleteAllByIdInBatch(List.of(100L, 101L));
  }

  @Test
  public void memberships_of_several_dropped_students_are_deleted_in_one_batch() {
    RosterStudent chris = student(10L, "chris@ucsb.edu");
    RosterStudent lauren = student(11L, "lauren@ucsb.edu");
    addToTeam(100L, chris, red);
    addToTeam(102L, lauren, blue);

    studentDropService.handleDroppedStudents(List.of(chris, lauren));

    assertTrue(chris.getTeamMembers().isEmpty());
    assertTrue(lauren.getTeamMembers().isEmpty());
    assertTrue(red.getTeamMembers().isEmpty());
    assertTrue(blue.getTeamMembers().isEmpty());
    verify(teamMemberRepository).deleteAllByIdInBatch(List.of(100L, 102L));
  }

  @Test
  public void students_who_are_on_no_teams_cause_no_deletes() {
    RosterStudent chris = student(10L, "chris@ucsb.edu");
    RosterStudent noTeamList = student(11L, "lauren@ucsb.edu");
    noTeamList.setTeamMembers(null);

    studentDropService.handleDroppedStudents(List.of(chris, noTeamList));

    assertTrue(chris.getTeamMembers().isEmpty());
    assertNull(noTeamList.getTeamMembers());
    verifyNoInteractions(teamMemberRepository);
  }

  @Test
  public void an_empty_list_of_dropped_students_causes_no_deletes() {
    studentDropService.handleDroppedStudents(List.of());

    verifyNoInteractions(teamMemberRepository);
  }
}
