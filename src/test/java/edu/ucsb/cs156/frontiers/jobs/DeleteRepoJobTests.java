package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.RepositoryService.GithubRepository;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.errors.JobCancelledException;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteRepoJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private RepositoryService repositoryService;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  /** Every wait the job asked for, in milliseconds; the job is given this so tests never wait. */
  List<Long> sleeps = new ArrayList<>();

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void test_getScope_returnsCourseScope() {
    Long courseId = 1L;

    DeleteRepoJob job =
        DeleteRepoJob.builder().courseId(courseId).courseRepository(courseRepository).build();

    assertEquals("course", job.getScopeType());
    assertEquals(courseId, job.getScopeId());
  }

  @Test
  public void test_accept_courseNotFound() throws Exception {
    Long courseId = 1L;
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verify(repositoryService, never()).getRepositoriesMatchingPrefix(any(), any());
    assertTrue(jobStarted.getLog().contains("ERROR: Course with ID 1 not found"));
  }

  @Test
  public void test_accept_courseWithoutGithubOrg() throws Exception {
    Long courseId = 1L;
    Course course = Course.builder().id(courseId).courseName("Test Course").build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verify(repositoryService, never()).getRepositoriesMatchingPrefix(any(), any());
    assertTrue(jobStarted.getLog().contains("ERROR: Course has no linked GitHub organization"));
  }

  @Test
  public void test_accept_courseWithOrgNameButNoInstallationId() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId(null)
            .build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verify(repositoryService, never()).getRepositoriesMatchingPrefix(any(), any());
    assertTrue(jobStarted.getLog().contains("ERROR: Course has no linked GitHub organization"));
  }

  @Test
  public void test_accept_allReposEmpty() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();
    List<GithubRepository> repositories =
        List.of(
            new GithubRepository("lab01-student1", "test-org/lab01-student1"),
            new GithubRepository("lab01-student2", "test-org/lab01-student2"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01")).thenReturn(repositories);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1")).thenReturn(true);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student2")).thenReturn(true);

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .sleeper(sleeps::add)
            .build();

    job.accept(ctx);

    verify(repositoryService).getRepositoriesMatchingPrefix(course, "lab01");
    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student1");
    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student2");
    assertTrue(
        jobStarted.getLog().contains("Starting delete repository job for course ID: " + courseId));
    assertTrue(jobStarted.getLog().contains("prefix=lab01"));
    assertTrue(jobStarted.getLog().contains("2 repos found with prefix lab01"));
    assertTrue(jobStarted.getLog().contains("Deleted empty repository: lab01-student1"));
    assertTrue(jobStarted.getLog().contains("Deleted empty repository: lab01-student2"));
    assertTrue(jobStarted.getLog().contains("\n2 repos deleted\n0 repos retained\n0 errors"));
    assertTrue(jobStarted.getLog().contains("Done"));
  }

  @Test
  public void test_accept_someReposRetainedBecauseCommitsExist() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();
    List<GithubRepository> repositories =
        List.of(
            new GithubRepository("lab01-student1", "test-org/lab01-student1"),
            new GithubRepository("lab01-student2", "test-org/lab01-student2"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01")).thenReturn(repositories);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1")).thenReturn(true);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student2")).thenReturn(false);

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .sleeper(sleeps::add)
            .build();

    job.accept(ctx);

    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student1");
    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student2");
    assertTrue(jobStarted.getLog().contains("Deleted empty repository: lab01-student1"));
    assertTrue(jobStarted.getLog().contains("Repo lab01-student2 not deleted; commits exist."));
    assertTrue(jobStarted.getLog().contains("\n1 repos deleted\n1 repos retained\n0 errors"));
  }

  @Test
  public void test_accept_githubApiErrorDuringDeleteOrCheck() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();
    List<GithubRepository> repositories =
        List.of(
            new GithubRepository("lab01-student1", "test-org/lab01-student1"),
            new GithubRepository("lab01-student2", "test-org/lab01-student2"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01")).thenReturn(repositories);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1"))
        .thenThrow(new RuntimeException("GitHub API error"));
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student2")).thenReturn(true);

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .sleeper(sleeps::add)
            .build();

    job.accept(ctx);

    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student1");
    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student2");
    assertTrue(
        jobStarted
            .getLog()
            .contains("ERROR: Failed to delete/check repository lab01-student1: GitHub API error"));
    assertTrue(jobStarted.getLog().contains("Deleted empty repository: lab01-student2"));
    assertTrue(jobStarted.getLog().contains("\n1 repos deleted\n0 repos retained\n1 errors"));
  }

  @Test
  public void test_accept_githubApiErrorDuringList() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01"))
        .thenThrow(new RuntimeException("GitHub API error"));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .build();

    job.accept(ctx);

    verify(repositoryService).getRepositoriesMatchingPrefix(course, "lab01");
    verify(repositoryService, never()).deleteRepositoryIfEmpty(any(), any());
    assertTrue(
        jobStarted.getLog().contains("ERROR: Failed to list repositories: GitHub API error"));
    assertTrue(jobStarted.getLog().contains("Summary: found=0, deleted=0, retained=0, errors=1"));
  }

  @Test
  public void test_accept_waitsBetweenReposButNotBeforeFirstOrAfterLast() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();
    List<GithubRepository> repositories =
        List.of(
            new GithubRepository("lab01-student1", "test-org/lab01-student1"),
            new GithubRepository("lab01-student2", "test-org/lab01-student2"),
            new GithubRepository("lab01-student3", "test-org/lab01-student3"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01")).thenReturn(repositories);
    when(repositoryService.deleteRepositoryIfEmpty(any(), any())).thenReturn(true);

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .sleeper(sleeps::add)
            .build();

    job.accept(ctx);

    assertEquals(List.of(1000L, 1000L), sleeps);
    assertTrue(jobStarted.getLog().contains("3 repos found with prefix lab01"));
  }

  @Test
  public void test_accept_cancellationIsNotSwallowedAsARepoError() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();
    List<GithubRepository> repositories =
        List.of(
            new GithubRepository("lab01-student1", "test-org/lab01-student1"),
            new GithubRepository("lab01-student2", "test-org/lab01-student2"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01")).thenReturn(repositories);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1")).thenReturn(true);

    // ctx.log() is where lib-jobs notices a cancel request; simulate one arriving there. Lenient,
    // since every other log() call, with other messages, should do nothing.
    JobContext cancellingCtx = mock(JobContext.class);
    lenient()
        .doThrow(new JobCancelledException(courseId))
        .when(cancellingCtx)
        .log("Deleted empty repository: lab01-student1");

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .sleeper(sleeps::add)
            .build();

    assertThrows(JobCancelledException.class, () -> job.accept(cancellingCtx));

    verify(repositoryService, never()).deleteRepositoryIfEmpty(course, "lab01-student2");
  }

  @Test
  public void test_defaultSleeper_reallyWaits() throws Exception {
    DeleteRepoJob job = DeleteRepoJob.builder().courseId(1L).build();

    long start = System.nanoTime();
    job.sleeper.sleep(20);
    long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

    assertTrue(elapsedMillis >= 20, "slept only " + elapsedMillis + " ms");
  }
}
