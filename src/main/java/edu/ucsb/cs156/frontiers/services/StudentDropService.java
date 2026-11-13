package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Carries out everything that needs to happen when roster students are dropped from a course (that
 * is, when their roster status becomes DROPPED, e.g. because they are missing from a newly uploaded
 * or synced roster). Code that drops students should call {@link #handleDroppedStudents(List)}, so
 * that this class is the single place to add any further work that a drop requires.
 */
@Service
public class StudentDropService {

  private final TeamMemberRepository teamMemberRepository;

  public StudentDropService(TeamMemberRepository teamMemberRepository) {
    this.teamMemberRepository = teamMemberRepository;
  }

  /**
   * Handles the consequences of dropping the given students. Currently, each student is removed
   * from every team they belong to.
   *
   * <p>The team memberships are removed from the given student and team objects as well as from the
   * database. Callers typically hold these objects outside of a transaction and save the students
   * again afterwards (e.g. {@code RemoveStudentsJob} does), so the objects must not keep referring
   * to the deleted memberships.
   *
   * @param droppedStudents the students who have been dropped from their course
   */
  public void handleDroppedStudents(List<RosterStudent> droppedStudents) {
    List<Long> teamMemberIds = new ArrayList<>();
    for (RosterStudent student : droppedStudents) {
      teamMemberIds.addAll(detachFromAllTeams(student));
    }
    if (!teamMemberIds.isEmpty()) {
      teamMemberRepository.deleteAllByIdInBatch(teamMemberIds);
    }
  }

  /**
   * Removes all of a student's team memberships from the student and from the teams.
   *
   * <p>The membership lists are replaced with new lists rather than modified in place. A list
   * loaded by Hibernate remembers the entities it was loaded with, and saving a detached student
   * whose list still remembers deleted memberships fails because Hibernate looks them up again.
   *
   * @param student the student to detach
   * @return the ids of the removed team memberships, which still need to be deleted
   */
  private List<Long> detachFromAllTeams(RosterStudent student) {
    List<Long> teamMemberIds = new ArrayList<>();
    List<TeamMember> memberships = student.getTeamMembers();
    if (memberships != null) {
      for (TeamMember membership : memberships) {
        Team team = membership.getTeam();
        List<TeamMember> remainingMembers = new ArrayList<>(team.getTeamMembers());
        remainingMembers.remove(membership);
        team.setTeamMembers(remainingMembers);
        teamMemberIds.add(membership.getId());
      }
      student.setTeamMembers(new ArrayList<>());
    }
    return teamMemberIds;
  }
}
