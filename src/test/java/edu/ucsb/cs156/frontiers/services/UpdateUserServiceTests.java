package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class UpdateUserServiceTests {

  @Mock private UserRepository userRepository;

  @Mock private RosterStudentRepository rosterStudentRepository;

  @Mock private CourseStaffRepository courseStaffRepository;

  @InjectMocks private UpdateUserService updateUserService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testAttachRosterStudents() {
    // Arrange
    User user = User.builder().email("test@example.com").build();

    RosterStudent student1 = new RosterStudent();
    RosterStudent student2 = new RosterStudent();

    List<RosterStudent> matchedStudents = Arrays.asList(student1, student2);

    when(rosterStudentRepository.findAllByEmail("test@example.com")).thenReturn(matchedStudents);

    // Act
    updateUserService.attachRosterStudents(user);

    // Assert
    verify(rosterStudentRepository, times(1)).findAllByEmail("test@example.com");
    verify(rosterStudentRepository, times(1)).saveAll(matchedStudents);

    assertEquals(user, student1.getUser());
    assertEquals(user, student2.getUser());
  }

  @Test
  public void testAttachRosterStudentsAllUsers() {
    // Arrange
    User user1 = User.builder().build();
    user1.setEmail("user1@example.com");

    User user2 = User.builder().build();
    user2.setEmail("user2@example.com");

    Iterable<User> allUsers = Arrays.asList(user1, user2);

    RosterStudent student1 = new RosterStudent();
    RosterStudent student2 = new RosterStudent();

    when(userRepository.findAll()).thenReturn(allUsers);
    when(rosterStudentRepository.findAllByEmail("user1@example.com"))
        .thenReturn(Collections.singletonList(student1));
    when(rosterStudentRepository.findAllByEmail("user2@example.com"))
        .thenReturn(Collections.singletonList(student2));

    // Act
    updateUserService.attachRosterStudentsAllUsers();

    // Assert
    verify(userRepository, times(1)).findAll();
    verify(rosterStudentRepository, times(1)).findAllByEmail("user1@example.com");
    verify(rosterStudentRepository, times(1)).findAllByEmail("user2@example.com");
    verify(rosterStudentRepository, times(2)).saveAll(anyList());

    assertEquals(user1, student1.getUser());
    assertEquals(user2, student2.getUser());
  }

  @Test
  public void testAttachUserToRosterStudent_userExists() {
    // Arrange
    String email = "test@example.com";
    User user = User.builder().email(email).build();

    RosterStudent rosterStudent = new RosterStudent();
    rosterStudent.setEmail(email);

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

    // Act
    updateUserService.attachUserToRosterStudent(rosterStudent);

    // Assert
    verify(userRepository, times(1)).findByEmail(email);
    verify(rosterStudentRepository, times(1)).save(rosterStudent);
    assertEquals(rosterStudent.getUser(), user);
  }

  @Test
  public void testAttachUserToRosterStudent_userDoesNotExist() {
    // Arrange
    String email = "test@example.com";
    RosterStudent rosterStudent = new RosterStudent();
    rosterStudent.setEmail(email);

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    // Act
    updateUserService.attachUserToRosterStudent(rosterStudent);

    // Assert
    verify(userRepository, times(1)).findByEmail(email);
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));
    assertNull(rosterStudent.getUser());
  }

  @Test
  public void testAttachUserToCourseStaff_userExists() {
    // Arrange
    String email = "test@example.com";
    User user = User.builder().email(email).build();

    CourseStaff courseStaff = new CourseStaff();
    courseStaff.setEmail(email);

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

    // Act
    updateUserService.attachUserToCourseStaff(courseStaff);

    // Assert
    verify(userRepository, times(1)).findByEmail(email);
    verify(courseStaffRepository, times(1)).save(courseStaff);
    assertEquals(courseStaff.getUser(), user);
  }

  @Test
  public void testAttachUserToCourseStaff_userDoesNotExist() {
    // Arrange
    String email = "test@example.com";
    CourseStaff courseStaff = new CourseStaff();
    courseStaff.setEmail(email);

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    // Act
    updateUserService.attachUserToCourseStaff(courseStaff);

    // Assert
    verify(userRepository, times(1)).findByEmail(email);
    verifyNoInteractions(courseStaffRepository);
    assertNull(courseStaff.getUser());
  }

  @Test
  public void testAttachCourseStaff() {
    // Arrange
    User user = User.builder().email("test@example.com").build();

    CourseStaff staff1 = new CourseStaff();
    CourseStaff staff2 = new CourseStaff();

    List<CourseStaff> matchedStaff = Arrays.asList(staff1, staff2);

    when(courseStaffRepository.findAllByEmail("test@example.com")).thenReturn(matchedStaff);

    // Act
    updateUserService.attachCourseStaff(user);

    // Assert
    verify(courseStaffRepository, times(1)).findAllByEmail("test@example.com");
    verify(courseStaffRepository, times(1)).saveAll(matchedStaff);

    assertEquals(user, staff1.getUser());
    assertEquals(user, staff2.getUser());
  }

  @Test
  public void testAttachUsesrToRosterStudents_userExists() {
    // Arrange
    String email = "test@example.com";
    User user = User.builder().email(email).build();

    RosterStudent rosterStudent = new RosterStudent();
    rosterStudent.setEmail(email);

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

    // Act
    updateUserService.attachUsersToRosterStudents(List.of(rosterStudent));

    // Assert
    verify(userRepository, times(1)).findByEmail(email);
    verify(rosterStudentRepository, times(1)).save(rosterStudent);
    assertEquals(rosterStudent.getUser(), user);
  }

  @Test
  public void testResetPendingInvitations_resetsOnlyInvitedStudentsAndStaff() {
    User user = User.builder().id(1L).email("test@example.com").build();

    RosterStudent invitedStudent =
        RosterStudent.builder()
            .orgStatus(OrgStatus.INVITED)
            .githubId(111)
            .githubLogin("wrong-account")
            .build();
    RosterStudent memberStudent =
        RosterStudent.builder()
            .orgStatus(OrgStatus.MEMBER)
            .githubId(222)
            .githubLogin("member-account")
            .build();
    CourseStaff invitedStaff =
        CourseStaff.builder()
            .orgStatus(OrgStatus.INVITED)
            .githubId(333)
            .githubLogin("wrong-staff-account")
            .build();
    CourseStaff ownerStaff =
        CourseStaff.builder()
            .orgStatus(OrgStatus.OWNER)
            .githubId(444)
            .githubLogin("owner-account")
            .build();

    when(rosterStudentRepository.findAllByUser(user))
        .thenReturn(List.of(invitedStudent, memberStudent));
    when(courseStaffRepository.findAllByUser(user)).thenReturn(List.of(invitedStaff, ownerStaff));

    updateUserService.resetPendingInvitations(user);

    assertEquals(OrgStatus.JOINCOURSE, invitedStudent.getOrgStatus());
    assertNull(invitedStudent.getGithubId());
    assertNull(invitedStudent.getGithubLogin());
    assertEquals(OrgStatus.MEMBER, memberStudent.getOrgStatus());
    assertEquals(222, memberStudent.getGithubId());
    assertEquals("member-account", memberStudent.getGithubLogin());

    assertEquals(OrgStatus.JOINCOURSE, invitedStaff.getOrgStatus());
    assertNull(invitedStaff.getGithubId());
    assertNull(invitedStaff.getGithubLogin());
    assertEquals(OrgStatus.OWNER, ownerStaff.getOrgStatus());
    assertEquals(444, ownerStaff.getGithubId());
    assertEquals("owner-account", ownerStaff.getGithubLogin());

    verify(rosterStudentRepository).saveAll(List.of(invitedStudent));
    verify(courseStaffRepository).saveAll(List.of(invitedStaff));
  }

  @Test
  public void testResetPendingInvitations_nothingToReset() {
    User user = User.builder().id(1L).email("test@example.com").build();
    when(rosterStudentRepository.findAllByUser(user)).thenReturn(List.of());
    when(courseStaffRepository.findAllByUser(user)).thenReturn(List.of());

    updateUserService.resetPendingInvitations(user);

    verify(rosterStudentRepository).saveAll(List.of());
    verify(courseStaffRepository).saveAll(List.of());
  }
}
